package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record GroupMinPortUpdateRequest(@NotNull String name, int minPort) {}
