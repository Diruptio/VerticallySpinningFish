package diruptio.verticallyspinningfish;

import diruptio.verticallyspinningfish.api.WebApiThread;
import diruptio.verticallyspinningfish.config.ApplicationConfig;
import diruptio.verticallyspinningfish.service.ContainerService;
import diruptio.verticallyspinningfish.service.DockerService;
import diruptio.verticallyspinningfish.service.GroupService;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VerticallySpinningFish {
    private static ApplicationConfig applicationConfig;
    private static DockerService dockerService;
    private static ContainerService containerService;
    private static GroupService groupService;

    public static void main(String[] args) throws IOException {
        // Initialize configuration and services
        applicationConfig = new ApplicationConfig();
        dockerService = new DockerService(applicationConfig);
        containerService = new ContainerService(dockerService, applicationConfig);
        groupService = new GroupService(containerService, dockerService, applicationConfig);

        // Load groups and existing containers
        groupService.loadGroups();
        containerService.loadExistingContainers();

        // Start web API
        new Thread(new WebApiThread(applicationConfig.getSecret())).start();

        // Schedule periodic tasks
        ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4);
        scheduledExecutor.scheduleAtFixedRate(groupService::rebuildImages, 10, 10, TimeUnit.MINUTES);
        scheduledExecutor.scheduleAtFixedRate(groupService::updateTemplates, 10, 10, TimeUnit.MINUTES);

        // Setup shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                scheduledExecutor.shutdownNow();
                dockerService.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }));

        // Main application loop
        runMainLoop();
    }

    private static void runMainLoop() {
        try {
            while (true) {
                containerService.updateContainerCache();
                groupService.manageGroupContainers();
                Thread.sleep(5000);
            }
        } catch (InterruptedException ignored) {
            System.out.println("Stopping Vertically Spinning Fish...");
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    // Legacy static methods for backward compatibility
    public static @NotNull String getContainerPrefix() {
        return applicationConfig.getContainerPrefix();
    }

    public static @NotNull com.github.dockerjava.api.DockerClient getDockerClient() {
        return dockerService.getClient();
    }

    public static @NotNull DockerService getDockerService() {
        return dockerService;
    }

    public static @NotNull ContainerService getContainerService() {
        return containerService;
    }

    public static @NotNull GroupService getGroupService() {
        return groupService;
    }

    // Legacy methods - delegated to services for backward compatibility
    public static diruptio.verticallyspinningfish.api.Container getContainer(@NotNull String id) {
        return containerService.getContainer(id);
    }

    public static @NotNull diruptio.verticallyspinningfish.api.Container createContainer(@NotNull Group group) throws IOException {
        return containerService.createContainer(group);
    }

    public static void deleteContainer(@NotNull String id) {
        containerService.deleteContainer(id);
    }

    public static void setContainerStatus(@NotNull diruptio.verticallyspinningfish.api.Container container, @NotNull diruptio.verticallyspinningfish.api.Status status) {
        containerService.setContainerStatus(container, status);
    }

    public static @NotNull Map<String, Group> getGroups() {
        return groupService.getGroups();
    }

    public static @NotNull java.util.List<diruptio.verticallyspinningfish.api.Container> getContainers() {
        return containerService.getContainers();
    }
}