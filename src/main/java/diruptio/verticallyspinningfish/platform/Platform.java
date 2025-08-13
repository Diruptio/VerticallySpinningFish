package diruptio.verticallyspinningfish.platform;

import diruptio.verticallyspinningfish.Container;
import org.jetbrains.annotations.NotNull;

public interface Platform {
    void apply(@NotNull Container container);
}
