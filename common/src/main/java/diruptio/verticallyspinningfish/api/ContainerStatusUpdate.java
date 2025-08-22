package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record ContainerStatusUpdate(@NotNull String id, @NotNull Status status) implements LiveUpdate {
    @Override
    public @NotNull String type() {
        return "container_status";
    }
}
