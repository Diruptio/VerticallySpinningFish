package diruptio.verticallyspinningfish.template;

import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public interface TemplateStep {
    void update();
    @NotNull String hash();
    void apply(@NotNull Path directory) throws Exception;
}
