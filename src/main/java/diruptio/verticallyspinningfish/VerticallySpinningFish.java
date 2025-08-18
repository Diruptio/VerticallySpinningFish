package diruptio.verticallyspinningfish;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.google.common.hash.Hashing;
import diruptio.util.config.Config;
import diruptio.verticallyspinningfish.template.TemplateBuilder;
import diruptio.verticallyspinningfish.util.ContainerUtil;
import diruptio.verticallyspinningfish.api.WebApiThread;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

public class VerticallySpinningFish {
    private static String secret;
    private static String containerPrefix;
    private static DockerClient dockerClient;
    private static final Map<String, Group> containerGroups = new ConcurrentHashMap<>();
    private static String hostWorkingDir;
    private static Integer exposedApiPort;

    public static void main(String[] args) throws IOException {
        Config config = new Config(Path.of("config.yml"), Config.Type.YAML);
        config.setDefault("docker-host", "unix:///var/run/docker.sock");
        config.setDefault("secret", Hashing.sha256().hashLong(System.currentTimeMillis()).toString());
        config.setDefault("container-prefix", "vsf-");
        secret = config.get("secret").toString();
        containerPrefix = config.get("container-prefix").toString();

        DockerClientConfig dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(Objects.requireNonNull(config.getString("docker-host")))
                .build();
        dockerClient = DockerClientBuilder.getInstance(dockerConfig).build();

        // Get information about the current container
        String currentContainerId = Objects.requireNonNull(System.getenv("HOSTNAME"));
        HostConfig currentHostConfig = dockerClient.inspectContainerCmd(currentContainerId)
                .exec()
                .getHostConfig();
        for (ExposedPort exposedPort : currentHostConfig.getPortBindings().getBindings().keySet()) {
            if (exposedPort.getPort() == 7000) {
                exposedApiPort = Integer.parseInt(currentHostConfig.getPortBindings().getBindings().get(exposedPort)[0].getHostPortSpec());
            }
        }
        for (Bind bind : currentHostConfig.getBinds()) {
            if (bind.getVolume().getPath().equals(Path.of("").toAbsolutePath().toString())) {
                hostWorkingDir = bind.getPath();
            }
        }

        // Load groups
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try (Stream<Path> groupFiles = Files.list(Path.of("groups"))) {
            groupFiles.forEach(path -> {
                if (Files.isRegularFile(path) && path.getFileName().toString().matches("[a-zA-Z0-9_-]+\\.yml")) {
                    Group group = Group.read(path);
                    System.out.println("Loading container group: " + group.getName());
                    containerGroups.put(group.getName(), group);
                    executor.submit(() -> {
                        try {
                            group.setTemplateDir(TemplateBuilder.build(group.getTemplate()));
                        } catch (Exception e) {
                            new Exception("Failed to build template for group: " + group.getName(), e).printStackTrace(System.err);
                        }
                    });
                    executor.submit(group::rebuildImageIfNeeded);
                }
            });
        }
        executor.close();

        Thread.startVirtualThread(new WebApiThread(secret));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dockerClient.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }));

        // Main loop
        try {
            while (true) {
                List<Container> allContainers = dockerClient.listContainersCmd()
                        .withShowAll(true)
                        .exec();
                for (Group group : containerGroups.values()) {
                    List<Container> containers = allContainers.stream()
                            .filter(c ->
                                    Stream.of(c.getNames()).anyMatch(name ->
                                            name.startsWith("/" + containerPrefix + group.getName() + "-")))
                            .toList();
                    containers = new ArrayList<>(containers);
                    containers.removeIf(container -> {
                        if (container.getState().equals("dead") ||
                                (group.isDeleteOnStop() && container.getState().equals("exited"))) {
                            deleteContainer(container.getNames()[0].substring(1), container.getId());
                            return true;
                        } else {
                            return false;
                        }
                    });

                    for (Container container : containers) {
                        if (container.getState().equals("exited")) {
                            System.out.println("Starting container: " + container.getNames()[0].substring(1));
                            dockerClient.startContainerCmd(container.getId()).exec();
                        }
                    }

                    for (int i = containers.size(); i < group.getMinCount(); i++) {
                        createContainer(containers, group);
                    }
                }
                Thread.sleep(5000);
            }
        } catch (InterruptedException ignored) {
            System.out.println("Stopping Vertically Spinning Fish...");
        }
    }

    public static @NotNull diruptio.verticallyspinningfish.api.Container createContainer(@NotNull Group group) throws IOException, InterruptedException {
        List<com.github.dockerjava.api.model.Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .filter(c ->
                        Stream.of(c.getNames()).anyMatch(name ->
                                name.startsWith("/" + containerPrefix + group.getName() + "-")))
                .toList();
        return createContainer(containers, group);
    }

    public static @NotNull diruptio.verticallyspinningfish.api.Container createContainer(@NotNull List<com.github.dockerjava.api.model.Container> containers,
                                                  @NotNull Group group) throws IOException, InterruptedException {
        String containerName = ContainerUtil.findContainerName(containers, group.getName());
        System.out.println("Creating container: " + containerName);

        List<PortBinding> portBindings = new ArrayList<>();
        List<Integer> ports = new ArrayList<>();
        int minPort = group.getMinPort();
        for (int port : group.getPorts()) {
            int exposedPort = ContainerUtil.findPort(containers, minPort);
            minPort = exposedPort + 1;
            ports.add(exposedPort);
            portBindings.add(new PortBinding(Ports.Binding.bindPort(exposedPort), new ExposedPort(port)));
        }

        List<Bind> binds = new ArrayList<>();
        List<String> volumes = new ArrayList<>(group.getVolumes());
        if (volumes.size() == 1) {
            Path path = Path.of("running", containerName);
            if (Files.isDirectory(path)) {
                FileUtils.deleteDirectory(path.toFile());
            } else if (Files.exists(path)) {
                Files.delete(path);
            }
            FileUtils.copyDirectory(Objects.requireNonNull(group.getTemplateDir()).toFile(), path.toFile());
            binds.add(new Bind(hostWorkingDir + "/running/" + containerName, new Volume(volumes.getFirst())));
        } else if (volumes.size() > 1) {
            throw new IllegalArgumentException("Only 1 volume per container is allowed");
        }

        String containerId = dockerClient.createContainerCmd(group.getImageId())
                .withName(containerName)
                .withHostConfig(HostConfig.newHostConfig()
                        .withPortBindings(portBindings)
                        .withBinds(binds))
                .withEnv("VSF_PREFIX=" + containerPrefix, "VSF_API_PORT=" + exposedApiPort, "VSF_SECRET=" + secret)
                .withTty(true)
                .withStdinOpen(true)
                .exec()
                .getId();

        System.out.println("Starting container: " + containerName);
        dockerClient.startContainerCmd(containerId).exec();

        return new diruptio.verticallyspinningfish.api.Container(containerId, containerName, ports);
    }

    public static void deleteContainer(@NotNull String name, @NotNull String containerId) {
        System.out.println("Deleting container: " + name);
        dockerClient.removeContainerCmd(containerId)
                .withForce(true)
                .exec();
        try {
            FileUtils.deleteDirectory(Path.of("running", name).toFile());
        } catch (IOException e) {
            new Exception("Failed to delete container data", e).printStackTrace(System.err);
        }
    }

    public static @NotNull String getContainerPrefix() {
        return containerPrefix;
    }

    public static @NotNull DockerClient getDockerClient() {
        return dockerClient;
    }

    public static @NotNull Map<String, Group> getContainerGroups() {
        return containerGroups;
    }
}
