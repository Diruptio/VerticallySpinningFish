package diruptio.verticallyspinningfish.service;

import diruptio.verticallyspinningfish.Group;
import diruptio.verticallyspinningfish.api.Container;
import diruptio.verticallyspinningfish.api.Status;
import diruptio.verticallyspinningfish.config.ApplicationConfig;
import diruptio.verticallyspinningfish.template.CopyStep;
import diruptio.verticallyspinningfish.template.TemplateStep;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Service for managing container groups and their lifecycle.
 */
public class GroupService {
    private final ContainerService containerService;
    private final DockerService dockerService;
    private final ApplicationConfig config;
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    public GroupService(@NotNull ContainerService containerService, @NotNull DockerService dockerService, @NotNull ApplicationConfig config) {
        this.containerService = containerService;
        this.dockerService = dockerService;
        this.config = config;
    }

    /**
     * Load all groups from the groups directory.
     */
    public void loadGroups() throws IOException {
        ExecutorService executor = Executors.newCachedThreadPool();
        try (Stream<Path> groupFiles = Files.list(Path.of("groups"))) {
            groupFiles.forEach(path -> {
                if (Files.isRegularFile(path) && path.getFileName().toString().matches("[a-zA-Z0-9_-]+\\.yml")) {
                    Group group = Group.read(path);
                    System.out.println("Loading container group: " + group.getName());
                    groups.put(group.getName(), group);
                    for (TemplateStep step : group.getTemplate()) {
                        if (!(step instanceof CopyStep)) {
                            executor.submit(step::update);
                        }
                    }
                    executor.submit(() -> group.rebuildImageIfNeeded(dockerService, config.getContainerPrefix()));
                }
            });
        }
        executor.close();
    }

    /**
     * Manage containers for all groups according to their configuration.
     */
    public void manageGroupContainers() throws IOException {
        for (Group group : groups.values()) {
            List<Container> containers = containerService.getContainersForGroup(group);

            // Remove unavailable and optionally stopped containers
            containers = new ArrayList<>(containers);
            containers.removeIf(container -> {
                if (container.getStatus() == Status.UNAVAILABLE) {
                    return true;
                } else if (group.isDeleteOnStop() && container.getStatus() == Status.OFFLINE) {
                    containerService.deleteContainer(container.getId());
                    return true;
                } else {
                    return false;
                }
            });

            // Start stopped containers
            for (Container container : containers) {
                if (container.getStatus() == Status.OFFLINE) {
                    containerService.startContainer(container);
                }
            }

            // Create additional containers to meet minimum count
            for (int i = containers.size(); i < group.getMinCount(); i++) {
                containerService.createContainer(group);
            }
        }
    }

    /**
     * Update templates for all groups.
     */
    public void updateTemplates() {
        for (Group group : groups.values()) {
            try {
                for (TemplateStep step : group.getTemplate()) {
                    if (!(step instanceof CopyStep)) {
                        step.update();
                    }
                }
            } catch (Exception e) {
                new Exception("Failed to build template for group: " + group.getName(), e).printStackTrace(System.err);
            }
        }
    }

    /**
     * Rebuild images for all groups if needed.
     */
    public void rebuildImages() {
        for (Group group : groups.values()) {
            group.rebuildImageIfNeeded(dockerService, config.getContainerPrefix());
        }
    }

    /**
     * Get a group by name.
     */
    public Group getGroup(@NotNull String name) {
        return groups.get(name);
    }

    /**
     * Get all groups.
     */
    public @NotNull Map<String, Group> getGroups() {
        return Map.copyOf(groups);
    }
}