package diruptio.verticallyspinningfish.api;

import java.util.Set;
import org.jetbrains.annotations.NotNull;

public record GroupTagsUpdateRequest(@NotNull String name, @NotNull Set<String> tags) {}
