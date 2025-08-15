package diruptio.verticallyspinningfish.template;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.google.common.hash.Hashing;
import diruptio.util.config.ConfigSection;
import diruptio.verticallyspinningfish.VerticallySpinningFish;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

public class TemplateBuilder {
    public static @NotNull TemplateStep createTemplateStep(@NotNull ConfigSection config) {
        if (!config.contains("type")) {
            throw new IllegalArgumentException("Parameter \"type\" is missing");
        }
        String type = config.get("type").toString();

        return switch (type) {
            case "papermc-fill" -> new PaperMCFillStep(config);
            case "papermc-hangar" -> new PaperMCHangarStep(config);
            default -> throw new IllegalArgumentException("Unknown template step type: " + type);
        };
    }

    public static @NotNull Path build(@NotNull List<TemplateStep> template) throws Exception {
        String hash = Hashing.sha256().hashString("", StandardCharsets.UTF_8).toString();
        Path templateDir = VerticallySpinningFish.cacheDir("templates/" + hash);
        for (TemplateStep step : template) {
            hash = Hashing.sha256().hashString(hash + step.hash(), StandardCharsets.UTF_8).toString();
            Path newTemplateDir = Path.of("cache", "templates", hash);
            if (!Files.exists(newTemplateDir)) {
                FileUtils.copyDirectory(templateDir.toFile(), newTemplateDir.toFile());
                templateDir = newTemplateDir;
                step.apply(templateDir);
            }
        }
        return templateDir;
    }
}
