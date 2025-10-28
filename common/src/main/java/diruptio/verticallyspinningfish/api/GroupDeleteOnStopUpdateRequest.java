package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record GroupDeleteOnStopUpdateRequest(@NotNull String name, boolean deleteOnStop) {}
