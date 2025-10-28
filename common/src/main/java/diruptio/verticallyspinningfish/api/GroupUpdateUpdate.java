package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record GroupUpdateUpdate(@NotNull Group group) implements LiveUpdate {
    @Override
    public @NotNull String type() {
        return "group_update";
    }
}
