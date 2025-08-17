package diruptio.verticallyspinningfish.template;

import com.google.common.hash.Hashing;
import diruptio.util.config.ConfigSection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

public class CopyStep implements TemplateStep {
    private final String from;
    private final String into;
    private String hash;

    public CopyStep(@NotNull ConfigSection config) {
        if (!config.contains("from")) {
            throw new IllegalArgumentException("Parameter \"from\" is missing");
        }
        from = config.get("from").toString();

        if (!config.contains("into")) {
            throw new IllegalArgumentException("Parameter \"into\" is missing");
        }
        into = config.get("into").toString();

        update();
    }

    @Override
    public void update() {
        try {
            hash = "copy:" + from + ":" + into + ":" + Files.getLastModifiedTime(Path.of(from)).toMillis();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return;
        }
        hash = Hashing.sha256().hashString(hash, StandardCharsets.UTF_8).toString();
    }

    @Override
    public @NotNull String hash() {
        return hash;
    }

    @Override
    public void apply(@NotNull Path directory) throws IOException {
        Path source = Path.of(from);
        Path target = directory.resolve(into);
        Files.createDirectories(target.getParent());
        if (Files.isDirectory(source)) {
            FileUtils.copyDirectory(source.toFile(), target.toFile());
        } else if (Files.isRegularFile(source)) {
            Files.copy(source, target);
        }
    }
}
