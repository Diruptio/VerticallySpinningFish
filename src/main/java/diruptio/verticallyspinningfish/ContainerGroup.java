package diruptio.verticallyspinningfish;

import com.github.dockerjava.core.dockerfile.Dockerfile;
import diruptio.util.config.Config;
import diruptio.verticallyspinningfish.platform.Platform;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ContainerGroup(@NotNull String name,
                             @NotNull Dockerfile dockerfile,
                             @Nullable Platform platform,
                             int minCount,
                             @Nullable Integer memory,
                             @Nullable Integer minPort) {
    /**
     * Read a {@link ContainerGroup} from a YAML file
     *
     * @param path The path of the YAML file
     * @return A {@link ContainerGroup}
     * @throws IllegalArgumentException If the Dockerfile is not found
     */
    public static @NotNull ContainerGroup read(@NotNull Path path) {
        String name = path.getFileName().toString().replaceAll("\\.yml$", "");

        Path dockerfilePath = path.resolveSibling(name + ".Dockerfile");
        if (!dockerfilePath.toFile().exists()) {
            throw new IllegalArgumentException("Dockerfile not found: " + dockerfilePath);
        }
        Dockerfile dockerfile = new Dockerfile(
                dockerfilePath.toFile(),
                path.getParent().toFile());

        Config config = new Config(path, Config.Type.YAML);

        Platform platform = null;
        int minCount = config.getInt("min-count", 0);
        Integer memory = config.contains("memory") ? config.getInt("memory") : null;
        Integer minPort = config.contains("min-port") ? config.getInt("min-port") : null;

        return new ContainerGroup(name, dockerfile, platform, minCount, memory, minPort);
    }
}
