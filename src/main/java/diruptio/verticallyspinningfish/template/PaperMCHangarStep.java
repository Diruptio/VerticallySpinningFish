package diruptio.verticallyspinningfish.template;

import com.google.common.hash.Hashing;
import diruptio.util.config.ConfigSection;
import diruptio.verticallyspinningfish.util.PaperMCHangarApi;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class PaperMCHangarStep implements TemplateStep {
    private static final PaperMCHangarApi hangarApi = new PaperMCHangarApi();
    private final String project;
    private final String platform;
    private final String version;
    private final String channel;
    private final String file;
    private String effectiveVersion;
    private String hash;

    public PaperMCHangarStep(@NotNull ConfigSection config) {
        if (!config.contains("project")) {
            throw new IllegalArgumentException("Parameter \"project\" is missing");
        }
        project = config.get("project").toString();

        if (!config.contains("platform")) {
            throw new IllegalArgumentException("Parameter \"platform\" is missing");
        }
        platform = config.get("platform").toString();

        if (config.contains("version")) {
            version = config.get("version").toString();
        } else {
            version = "latest";
        }

        if (config.contains("channel")) {
            channel = config.get("channel").toString();
        } else {
            channel = "release";
        }

        if (!config.contains("file")) {
            throw new IllegalArgumentException("Parameter \"file\" is missing");
        }
        file = config.get("file").toString();

        update();
    }

    @Override
    public void update() {
        if (version.equalsIgnoreCase("latest")) {
            effectiveVersion = hangarApi.getLatestVersion(project, platform, channel);
        }

        hash = "papermc-hangar:" + project + ":" + platform + ":" + effectiveVersion + ":" + file;
        hash = Hashing.sha256().hashString(hash, StandardCharsets.UTF_8).toString();
    }

    @Override
    public @NotNull String hash() {
        return hash;
    }

    @Override
    public void apply(@NotNull Path directory) throws IOException {
        Path downloadCacheDir = Path.of(
                "cache",
                "papermc-hangar",
                "downloads",
                project,
                platform,
                effectiveVersion);
        if (!Files.exists(downloadCacheDir)) {
            Files.createDirectories(downloadCacheDir);
            hangarApi.download(project, platform, effectiveVersion, downloadCacheDir);
        }

        try (Stream<Path> files = Files.list(downloadCacheDir)) {
            Optional<Path> file = files.findFirst();
            if (file.isPresent()) {
                Path target = directory.resolve(this.file);
                Files.createDirectories(target.getParent());
                Files.copy(file.get(), target);
            }
        }
    }
}
