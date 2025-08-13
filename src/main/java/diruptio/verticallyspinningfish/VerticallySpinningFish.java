package diruptio.verticallyspinningfish;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import diruptio.util.config.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class VerticallySpinningFish {
    private static final Map<String, ContainerGroup> containerGroups = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        Config config = new Config(Path.of("config.yml"), Config.Type.YAML);
        config.setDefault("docker-host", "unix:///var/run/docker.sock");

        DockerClientConfig dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(Objects.requireNonNull(config.getString("docker-host")))
                .build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(dockerConfig).build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dockerClient.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }));

        try (Stream<Path> groupFiles = Files.list(Path.of("groups"))) {
            groupFiles.forEach(path -> {
                if (Files.isRegularFile(path) && path.getFileName().toString().matches("[a-zA-Z0-9_-]+\\.yml")) {
                    ContainerGroup group = ContainerGroup.read(path);
                    containerGroups.put(group.name(), group);
                    System.out.println("Loaded container group: " + group.name());
                }
            });
        }
    }

    public static @NotNull Map<String, ContainerGroup> getContainerGroups() {
        return containerGroups;
    }
}
