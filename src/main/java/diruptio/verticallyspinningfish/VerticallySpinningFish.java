package diruptio.verticallyspinningfish;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import diruptio.util.config.Config;
import diruptio.verticallyspinningfish.template.TemplateBuilder;
import diruptio.verticallyspinningfish.util.ContainerUtil;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

public class VerticallySpinningFish {
    private static DockerClient dockerClient;
    private static final Map<String, ContainerGroup> containerGroups = new ConcurrentHashMap<>();
    private static String hostWorkingDir;

    public static void main(String[] args) throws IOException {
        Config config = new Config(Path.of("config.yml"), Config.Type.YAML);
        config.setDefault("docker-host", "unix:///var/run/docker.sock");

        DockerClientConfig dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(Objects.requireNonNull(config.getString("docker-host")))
                .build();
        dockerClient = DockerClientBuilder.getInstance(dockerConfig).build();

        String currentContainerId = Objects.requireNonNull(System.getenv("HOSTNAME"));
        for (Bind bind : dockerClient.inspectContainerCmd(currentContainerId)
                .exec()
                .getHostConfig()
                .getBinds()) {
            if (bind.getVolume().getPath().equals(Path.of("").toAbsolutePath().toString())) {
                hostWorkingDir = bind.getPath();
                System.out.println("Host working directory: " + hostWorkingDir);
            }
        }

        try (Stream<Path> groupFiles = Files.list(Path.of("groups"))) {
            groupFiles.forEach(path -> {
                if (Files.isRegularFile(path) && path.getFileName().toString().matches("[a-zA-Z0-9_-]+\\.yml")) {
                    ContainerGroup group = ContainerGroup.read(path);
                    containerGroups.put(group.getName(), group);
                    System.out.println("Loaded container group: " + group.getName());
                    try {
                        group.setTemplateDir(TemplateBuilder.build(group.getTemplate()));
                    } catch (Exception e) {
                        new Exception("Failed to build template for group: " + group.getName(), e).printStackTrace(System.err);
                    }
                    group.rebuildImageIfNeeded();
                }
            });
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dockerClient.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }));

        try {
            while (true) {
                List<Container> allContainers = dockerClient.listContainersCmd()
                        .withShowAll(true)
                        .exec();
                for (ContainerGroup group : containerGroups.values()) {
                    List<Container> containers = allContainers.stream()
                            .filter(c ->
                                    Stream.of(c.getNames()).anyMatch(name ->
                                            name.startsWith("/vsf-" + group.getName() + "-")))
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
                            System.out.println("Starting container: " + container.getNames()[0]);
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

    public static @NotNull String createContainer(@NotNull List<Container> containers,
                                                  @NotNull ContainerGroup group) throws IOException, InterruptedException {
        String containerName = ContainerUtil.findContainerName(containers, group.getName());
        System.out.println("Creating container: " + containerName);

        List<PortBinding> portBindings = new ArrayList<>();
        int minPort = group.getMinPort();
        for (int port : group.getPorts()) {
            int exposedPort = ContainerUtil.findPort(containers, minPort);
            minPort = exposedPort + 1;
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
                .exec()
                .getId();

        System.out.println("Starting container: " + containerName);
        dockerClient.startContainerCmd(containerId).exec();

        return containerId;
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

    public static @NotNull DockerClient getDockerClient() {
        return dockerClient;
    }

    public static @NotNull Map<String, ContainerGroup> getContainerGroups() {
        return containerGroups;
    }
}
