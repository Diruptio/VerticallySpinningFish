package diruptio.verticallyspinningfish;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.google.common.hash.Hashing;
import diruptio.util.config.Config;
import diruptio.verticallyspinningfish.api.*;
import diruptio.verticallyspinningfish.api.Container;
import diruptio.verticallyspinningfish.api.endpoints.LiveUpdatesWebSocket;
import diruptio.verticallyspinningfish.template.CopyStep;
import diruptio.verticallyspinningfish.template.TemplateBuilder;
import diruptio.verticallyspinningfish.template.TemplateStep;
import diruptio.verticallyspinningfish.util.ContainerUtil;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VerticallySpinningFish {
    private static DockerClient dockerClient;
    private static String secret;
    private static String containerPrefix;
    private static final Map<String, Group> groups = new ConcurrentHashMap<>();
    private static final List<Container> containers = new ArrayList<>();
    private static String hostWorkingDir;
    private static Integer exposedApiPort;

    public static void main(String[] args) throws IOException {
        Config config = new Config(Path.of("config.yml"), Config.Type.YAML);
        config.setDefault("docker-host", "unix:///var/run/docker.sock");
        config.setDefault("secret", Hashing.sha256().hashLong(System.currentTimeMillis()).toString());
        config.setDefault("container-prefix", "vsf-");

        // Docker connection
        DockerClientConfig dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(Objects.requireNonNull(config.getString("docker-host")))
                .build();
        dockerClient = DockerClientBuilder.getInstance(dockerConfig).build();

        secret = config.get("secret").toString();
        containerPrefix = config.get("container-prefix").toString();

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
                    groups.put(group.getName(), group);
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

        // Load containers
        List<com.github.dockerjava.api.model.Container> dockerContainers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .filter(container ->
                        Stream.of(container.getNames()).anyMatch(name -> name.startsWith("/" + containerPrefix)))
                .toList();
        for (com.github.dockerjava.api.model.Container dockerContainer : dockerContainers) {
            containers.add(toApiContainer(dockerContainer));
        }

        Thread.startVirtualThread(new WebApiThread(secret));

        // Prepare images and templates
        ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4, Thread.ofVirtual().factory());
        for (Group group : groups.values()) {
            scheduledExecutor.scheduleAtFixedRate(group::rebuildImageIfNeeded, 10, 10, TimeUnit.MINUTES);
            scheduledExecutor.scheduleAtFixedRate(() -> {
                for (TemplateStep step : group.getTemplate()) {
                    if (step instanceof CopyStep) {
                        step.update();
                    }
                }
            }, 30, 30, TimeUnit.SECONDS);
            scheduledExecutor.scheduleAtFixedRate(() -> {
                try {
                    for (TemplateStep step : group.getTemplate()) {
                        if (!(step instanceof CopyStep)) {
                            step.update();
                        }
                    }
                    group.setTemplateDir(TemplateBuilder.build(group.getTemplate()));
                } catch (Exception e) {
                    new Exception("Failed to build template for group: " + group.getName(), e).printStackTrace(System.err);
                }
            }, 10, 10, TimeUnit.MINUTES);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                scheduledExecutor.shutdownNow();
                dockerClient.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }));

        // Main loop
        try {
            while (true) {
                dockerContainers = dockerClient.listContainersCmd()
                        .withShowAll(true)
                        .exec()
                        .stream()
                        .filter(container ->
                                Stream.of(container.getNames()).anyMatch(name -> name.startsWith("/" + containerPrefix)))
                        .toList();

                // Update cache
                for (com.github.dockerjava.api.model.Container dockerContainer : dockerContainers) {
                    Container container = getContainer(dockerContainer.getId());
                    if (container == null) {
                        container = toApiContainer(dockerContainer);
                        containers.add(container);
                        LiveUpdatesWebSocket.broadcastUpdate(new ContainerAddUpdate(container));
                    } else {
                        Status newStatus = toApiStatus(dockerContainer.getStatus());
                        if (newStatus.isOnline() != container.getStatus().isOnline()) {
                            setContainerStatus(container, newStatus);
                        }
                    }
                }

                for (Group group : groups.values()) {
                    List<Container> containers = VerticallySpinningFish.containers
                            .stream()
                            .filter(c -> c.getName().startsWith(containerPrefix + group.getName() + "-"))
                            .toList();

                    containers = new ArrayList<>(containers);
                    containers.removeIf(container -> {
                        if (container.getStatus() == diruptio.verticallyspinningfish.api.Status.UNAVAILABLE) {
                            return true;
                        } else if (group.isDeleteOnStop() &&
                                container.getStatus() == diruptio.verticallyspinningfish.api.Status.OFFLINE) {
                            deleteContainer(container.getId());
                            return true;
                        } else {
                            return false;
                        }
                    });

                    for (Container container : containers) {
                        if (container.getStatus() == diruptio.verticallyspinningfish.api.Status.OFFLINE) {
                            System.out.println("Starting container: " + container.getName());
                            dockerClient.startContainerCmd(container.getId()).exec();
                            LiveUpdatesWebSocket.broadcastUpdate(new ContainerStatusUpdate(container.getId(), diruptio.verticallyspinningfish.api.Status.ONLINE));
                        }
                    }

                    for (int i = containers.size(); i < group.getMinCount(); i++) {
                        createContainer(group);
                    }
                }
                Thread.sleep(5000);
            }
        } catch (InterruptedException ignored) {
            System.out.println("Stopping Vertically Spinning Fish...");
        }
    }

    private static Container toApiContainer(com.github.dockerjava.api.model.Container container) {
        return new Container(
                container.getId(),
                String.join("", container.getNames()).substring(1),
                Stream.of(container.getPorts()).map(com.github.dockerjava.api.model.ContainerPort::getPublicPort).filter(java.util.Objects::nonNull).toList(),
                toApiStatus(container.getState()));
    }

    private static diruptio.verticallyspinningfish.api.Status toApiStatus(String dockerStatus) {
        return switch (dockerStatus) {
            case "running" -> diruptio.verticallyspinningfish.api.Status.ONLINE;
            case "created", "exited", "dead" -> diruptio.verticallyspinningfish.api.Status.OFFLINE;
            default -> diruptio.verticallyspinningfish.api.Status.UNAVAILABLE;
        };
    }

    public static @Nullable Container getContainer(@NotNull String id) {
        for (Container container : containers) {
            if (container.getId().startsWith(id) || id.startsWith(container.getId())) {
                return container;
            }
        }
        return null;
    }

    public static @NotNull Container createContainer(@NotNull Group group) throws IOException {
        List<com.github.dockerjava.api.model.Container> dockerContainers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec();

        String containerName = ContainerUtil.findContainerName(dockerContainers, group.getName());
        System.out.println("Creating container: " + containerName);

        List<PortBinding> portBindings = new ArrayList<>();
        List<Integer> ports = new ArrayList<>();
        int minPort = group.getMinPort();
        for (int port : group.getPorts()) {
            int exposedPort = ContainerUtil.findPort(dockerContainers, minPort);
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

        Container container = new Container(containerId, containerName, ports, diruptio.verticallyspinningfish.api.Status.ONLINE);
        containers.add(container);

        System.out.println("Starting container: " + containerName);
        dockerClient.startContainerCmd(containerId).exec();

        LiveUpdatesWebSocket.broadcastUpdate(new ContainerAddUpdate(container));

        return container;
    }

    public static void deleteContainer(@NotNull String id) {
        Container container = getContainer(id);
        if (container == null) {
            return;
        }

        System.out.println("Deleting container: " + container.getName());
        dockerClient.removeContainerCmd(container.getId())
                .withForce(true)
                .exec();
        try {
            FileUtils.deleteDirectory(Path.of("running", container.getName()).toFile());
        } catch (IOException e) {
            new Exception("Failed to delete container data", e).printStackTrace(System.err);
        }
        containers.remove(container);
        LiveUpdatesWebSocket.broadcastUpdate(new ContainerRemoveUpdate(container.getId()));
    }

    public static void setContainerStatus(@NotNull Container container, @NotNull Status status) {
        ApiBridge.setContainerStatus(container, status);
        LiveUpdatesWebSocket.broadcastUpdate(new ContainerStatusUpdate(container.getId(), status));
        System.out.println("Status of container " + container.getId() + " changed to " + status);
    }

    public static @NotNull String getContainerPrefix() {
        return containerPrefix;
    }

    public static @NotNull DockerClient getDockerClient() {
        return dockerClient;
    }

    public static @NotNull Map<String, Group> getGroups() {
        return groups;
    }

    public static @NotNull List<Container> getContainers() {
        return containers;
    }
}