package diruptio.verticallyspinningfish.template;

import com.google.common.hash.Hashing;
import diruptio.util.config.ConfigSection;
import diruptio.verticallyspinningfish.util.PaperMCFillApi;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class PaperMCFillStep implements TemplateStep {
    private static final PaperMCFillApi fillApi = new PaperMCFillApi();
    private final String project;
    private final String version;
    private final String build;
    private final String file;
    private String effectiveVersion;
    private int effectiveBuild;
    private String hash;

    public PaperMCFillStep(@NotNull ConfigSection config) {
        if (!config.contains("project")) {
            throw new IllegalArgumentException("Parameter \"project\" is missing");
        }
        project = config.get("project").toString();

        if (config.contains("version")) {
            version = config.get("version").toString();
            if (config.contains("build")) {
                build = config.get("build").toString();
            } else {
                build = "latest";
            }
        } else {
            version = "latest";
            build = "latest";
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
            effectiveVersion = fillApi.getLatestVersion(project);
        }

        if (build.equalsIgnoreCase("latest")) {
            effectiveBuild = fillApi.getLatestBuild(project, effectiveVersion);
        } else {
            try {
                effectiveBuild = Integer.parseInt(build);
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("Parameter \"build\" must be an integer");
            }
        }

        hash = "papermc-fill:" + project + ":" + effectiveVersion + ":" + effectiveBuild + ":" + file;
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
                "papermc-fill",
                "downloads",
                project + "-" + effectiveVersion + "-" + effectiveBuild);
        if (!Files.exists(downloadCacheDir)) {
            Files.createDirectories(downloadCacheDir);
            fillApi.download(project, effectiveVersion, effectiveBuild, downloadCacheDir);
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
