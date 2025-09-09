package diruptio.verticallyspinningfish.config;

import com.google.common.hash.Hashing;
import diruptio.util.config.Config;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Configuration management for the Vertically Spinning Fish application.
 * Handles loading and accessing application configuration values.
 */
public class ApplicationConfig {
    private final Config config;
    private final String dockerHost;
    private final String secret;
    private final String containerPrefix;

    public ApplicationConfig() {
        this.config = new Config(Path.of("config.yml"), Config.Type.YAML);
        setupDefaults();
        
        this.dockerHost = Objects.requireNonNull(config.getString("docker-host"));
        this.secret = config.get("secret").toString();
        this.containerPrefix = config.get("container-prefix").toString();
    }

    private void setupDefaults() {
        config.setDefault("docker-host", "unix:///var/run/docker.sock");
        config.setDefault("secret", Hashing.sha256().hashLong(System.currentTimeMillis()).toString());
        config.setDefault("container-prefix", "vsf-");
    }

    public @NotNull String getDockerHost() {
        return dockerHost;
    }

    public @NotNull String getSecret() {
        return secret;
    }

    public @NotNull String getContainerPrefix() {
        return containerPrefix;
    }

    public Config getConfig() {
        return config;
    }
}