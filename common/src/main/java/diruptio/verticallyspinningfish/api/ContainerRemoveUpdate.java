package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record ContainerRemoveUpdate(@NotNull String id) implements LiveUpdate {
    @Override
    public @NotNull String type() {
        return "container_remove";
    }
}
