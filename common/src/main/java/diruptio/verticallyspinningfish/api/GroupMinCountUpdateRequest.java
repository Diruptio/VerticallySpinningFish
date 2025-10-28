package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record GroupMinCountUpdateRequest(@NotNull String name, int minCount) {}
