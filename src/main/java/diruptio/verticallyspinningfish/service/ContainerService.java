package diruptio.verticallyspinningfish.service;

import com.github.dockerjava.api.model.*;
import diruptio.verticallyspinningfish.Group;
import diruptio.verticallyspinningfish.api.ApiBridge;
import diruptio.verticallyspinningfish.api.Container;
import diruptio.verticallyspinningfish.api.ContainerAddUpdate;
import diruptio.verticallyspinningfish.api.ContainerRemoveUpdate;
import diruptio.verticallyspinningfish.api.ContainerStatusUpdate;
import diruptio.verticallyspinningfish.api.Status;
import diruptio.verticallyspinningfish.api.endpoints.LiveUpdatesWebSocket;
import diruptio.verticallyspinningfish.config.ApplicationConfig;
import diruptio.verticallyspinningfish.template.TemplateBuilder;
import diruptio.verticallyspinningfish.util.ContainerUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Service for managing container lifecycle operations.
 */
public class ContainerService {
    private final DockerService dockerService;
    private final ApplicationConfig config;
    private final List<Container> containers = new ArrayList<>();

    public ContainerService(@NotNull DockerService dockerService, @NotNull ApplicationConfig config) {
        this.dockerService = dockerService;
        this.config = config;
    }

    /**
     * Load existing containers from Docker and populate the internal cache.
     */
    public void loadExistingContainers() {
        List<com.github.dockerjava.api.model.Container> dockerContainers = dockerService.getClient()
                .listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .filter(container ->
                        Stream.of(container.getNames()).anyMatch(name -> 
                            name.startsWith("/" + config.getContainerPrefix())))
                .toList();
        
        for (com.github.dockerjava.api.model.Container dockerContainer : dockerContainers) {
            containers.add(toApiContainer(dockerContainer));
        }
    }

    /**
     * Update container cache with current Docker state.
     */
    public void updateContainerCache() {
        List<com.github.dockerjava.api.model.Container> dockerContainers = dockerService.getClient()
                .listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .filter(container ->
                        Stream.of(container.getNames()).anyMatch(name -> 
                            name.startsWith("/" + config.getContainerPrefix())))
                .toList();

        for (com.github.dockerjava.api.model.Container dockerContainer : dockerContainers) {
            Container container = getContainer(dockerContainer.getId());
            if (container == null) {
                container = toApiContainer(dockerContainer);
                containers.add(container);
                LiveUpdatesWebSocket.broadcastUpdate(new ContainerAddUpdate(container));
            } else {
                Status newStatus = toApiStatus(dockerContainer.getState());
                if (newStatus.isOnline() != container.getStatus().isOnline()) {
                    setContainerStatus(container, newStatus);
                }
            }
        }
    }

    /**
     * Create a new container for the specified group.
     */
    public @NotNull Container createContainer(@NotNull Group group) throws IOException {
        List<com.github.dockerjava.api.model.Container> dockerContainers = dockerService.getClient()
                .listContainersCmd()
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
            Path templatePath = TemplateBuilder.build(group.getTemplate());
            Path path = Path.of("running", containerName);
            if (Files.isDirectory(path)) {
                FileUtils.deleteDirectory(path.toFile());
            } else if (Files.exists(path)) {
                Files.delete(path);
            }
            FileUtils.copyDirectory(templatePath.toFile(), path.toFile());
            binds.add(new Bind(dockerService.getHostWorkingDir() + "/running/" + containerName, new Volume(volumes.getFirst())));
        } else if (volumes.size() > 1) {
            throw new IllegalArgumentException("Only 1 volume per container is allowed");
        }

        String containerId = dockerService.getClient().createContainerCmd(group.getImageId())
                .withName(containerName)
                .withHostConfig(HostConfig.newHostConfig()
                        .withPortBindings(portBindings)
                        .withBinds(binds))
                .withEnv("VSF_PREFIX=" + config.getContainerPrefix(), 
                        "VSF_API_PORT=" + dockerService.getExposedApiPort(), 
                        "VSF_SECRET=" + config.getSecret())
                .withTty(true)
                .withStdinOpen(true)
                .exec()
                .getId();

        Container container = new Container(containerId, containerName, ports, Status.ONLINE);
        containers.add(container);

        System.out.println("Starting container: " + containerName);
        dockerService.getClient().startContainerCmd(containerId).exec();

        LiveUpdatesWebSocket.broadcastUpdate(new ContainerAddUpdate(container));

        return container;
    }

    /**
     * Delete a container by ID.
     */
    public void deleteContainer(@NotNull String id) {
        Container container = getContainer(id);
        if (container == null) {
            return;
        }

        System.out.println("Deleting container: " + container.getName());
        dockerService.getClient().removeContainerCmd(container.getId())
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

    /**
     * Set container status and broadcast update.
     */
    public void setContainerStatus(@NotNull Container container, @NotNull Status status) {
        ApiBridge.setContainerStatus(container, status);
        LiveUpdatesWebSocket.broadcastUpdate(new ContainerStatusUpdate(container.getId(), status));
        System.out.println("Status of container " + container.getId() + " changed to " + status);
    }

    /**
     * Start a container.
     */
    public void startContainer(@NotNull Container container) {
        System.out.println("Starting container: " + container.getName());
        dockerService.getClient().startContainerCmd(container.getId()).exec();
        LiveUpdatesWebSocket.broadcastUpdate(new ContainerStatusUpdate(container.getId(), Status.ONLINE));
    }

    /**
     * Get container by ID (supports partial ID matching).
     */
    public @Nullable Container getContainer(@NotNull String id) {
        for (Container container : containers) {
            if (container.getId().startsWith(id) || id.startsWith(container.getId())) {
                return container;
            }
        }
        return null;
    }

    /**
     * Get all containers.
     */
    public @NotNull List<Container> getContainers() {
        return new ArrayList<>(containers);
    }

    /**
     * Get containers for a specific group.
     */
    public @NotNull List<Container> getContainersForGroup(@NotNull Group group) {
        return containers.stream()
                .filter(c -> c.getName().startsWith(config.getContainerPrefix() + group.getName() + "-"))
                .toList();
    }

    private Container toApiContainer(com.github.dockerjava.api.model.Container container) {
        return new Container(
                container.getId(),
                String.join("", container.getNames()).substring(1),
                Stream.of(container.getPorts()).map(com.github.dockerjava.api.model.ContainerPort::getPublicPort).filter(java.util.Objects::nonNull).toList(),
                toApiStatus(container.getState()));
    }

    private Status toApiStatus(String dockerStatus) {
        return switch (dockerStatus) {
            case "restarting", "running" -> Status.ONLINE;
            case "created", "exited", "dead", "removing" -> Status.OFFLINE;
            default -> throw new IllegalArgumentException("Unknown docker container status: " + dockerStatus);
        };
    }
}