package diruptio.verticallyspinningfish.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.google.common.hash.Hashing;
import diruptio.util.config.ConfigSection;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

public class TemplateBuilder {
    public static @NotNull TemplateStep createTemplateStep(@NotNull ConfigSection config) {
        if (!config.contains("type")) {
            throw new IllegalArgumentException("Parameter \"type\" is missing");
        }
        String type = config.get("type").toString();

        return switch (type) {
            case "command" -> new CommandStep(config);
            case "copy" -> new CopyStep(config);
            case "modrinth" -> new ModrinthStep(config);
            case "papermc-fill" -> new PaperMCFillStep(config);
            case "papermc-hangar" -> new PaperMCHangarStep(config);
            case "paper-plugin" -> new PaperPluginStep();
            case "velocity-plugin" -> new VelocityPluginStep();
            default -> throw new IllegalArgumentException("Unknown template step type: " + type);
        };
    }

    public static @NotNull Path build(@NotNull List<TemplateStep> template) throws IOException {
        String hash = Hashing.sha256().hashString("", StandardCharsets.UTF_8).toString();
        Path templateDir = Path.of("cache", "templates", hash);
        if (!Files.exists(templateDir)) {
            Files.createDirectories(templateDir);
        }

        for (TemplateStep step : template) {
            if (step instanceof CopyStep) {
                step.update();
            }

            hash = Hashing.sha256().hashString(hash + step.hash(), StandardCharsets.UTF_8).toString();
            Path newTemplateDir = Path.of("cache", "templates", hash);

            if (!Files.exists(newTemplateDir)) {
                FileUtils.copyDirectory(templateDir.toFile(), newTemplateDir.toFile());
                step.apply(newTemplateDir);
            }

            templateDir = newTemplateDir;
        }

        return templateDir;
    }
}
