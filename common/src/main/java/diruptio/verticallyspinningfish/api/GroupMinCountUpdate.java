package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record GroupMinCountUpdate(@NotNull String name, int minCount) implements LiveUpdate {
    @Override
    public @NotNull String type() {
        return "group_min_count";
    }
}
