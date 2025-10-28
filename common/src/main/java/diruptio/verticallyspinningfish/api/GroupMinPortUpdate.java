package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record GroupMinPortUpdate(@NotNull String name, int minPort) implements LiveUpdate {
    @Override
    public @NotNull String type() {
        return "group_min_port";
    }
}
