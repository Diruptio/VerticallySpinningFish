package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record GroupDeleteOnStopUpdate(@NotNull String name, boolean deleteOnStop) implements LiveUpdate {
    @Override
    public @NotNull String type() {
        return "group_delete_on_stop";
    }
}
