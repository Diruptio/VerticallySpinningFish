package diruptio.verticallyspinningfish.template;

import java.io.IOException;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public interface TemplateStep {
    void update();
    @NotNull String hash();
    void apply(@NotNull Path directory) throws IOException;
}
