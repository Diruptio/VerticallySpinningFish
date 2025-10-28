package diruptio.verticallyspinningfish.api;

import java.util.Set;
import org.jetbrains.annotations.NotNull;

public record GroupTagsUpdate(@NotNull String name, @NotNull Set<String> tags) implements LiveUpdate {
    @Override
    public @NotNull String type() {
        return "group_tags";
    }
}
