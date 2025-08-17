package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record ContainerAddUpdate(@NotNull Container container) implements LiveUpdate {
    @Override
    public @NotNull String type() {
        return "container_add";
    }
}
