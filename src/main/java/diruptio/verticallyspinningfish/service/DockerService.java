package diruptio.verticallyspinningfish.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import diruptio.verticallyspinningfish.config.ApplicationConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Service for managing Docker client operations and container environment detection.
 */
public class DockerService {
    private final DockerClient dockerClient;
    private final String hostWorkingDir;
    private final Integer exposedApiPort;

    public DockerService(@NotNull ApplicationConfig config) {
        DockerClientConfig dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(config.getDockerHost())
                .build();
        this.dockerClient = DockerClientBuilder.getInstance(dockerConfig).build();
        
        // Detect current container environment
        String currentContainerId = Objects.requireNonNull(System.getenv("HOSTNAME"));
        HostConfig currentHostConfig = dockerClient.inspectContainerCmd(currentContainerId)
                .exec()
                .getHostConfig();
        
        this.exposedApiPort = findExposedApiPort(currentHostConfig);
        this.hostWorkingDir = findHostWorkingDirectory(currentHostConfig);
    }

    @Nullable
    private Integer findExposedApiPort(@NotNull HostConfig hostConfig) {
        for (ExposedPort exposedPort : hostConfig.getPortBindings().getBindings().keySet()) {
            if (exposedPort.getPort() == 7000) {
                return Integer.parseInt(hostConfig.getPortBindings().getBindings().get(exposedPort)[0].getHostPortSpec());
            }
        }
        return null;
    }

    @Nullable
    private String findHostWorkingDirectory(@NotNull HostConfig hostConfig) {
        String currentWorkingDir = Path.of("").toAbsolutePath().toString();
        for (Bind bind : hostConfig.getBinds()) {
            if (bind.getVolume().getPath().equals(currentWorkingDir)) {
                return bind.getPath();
            }
        }
        return null;
    }

    public @NotNull DockerClient getClient() {
        return dockerClient;
    }

    public @Nullable String getHostWorkingDir() {
        return hostWorkingDir;
    }

    public @Nullable Integer getExposedApiPort() {
        return exposedApiPort;
    }

    public void close() throws IOException {
        dockerClient.close();
    }
}