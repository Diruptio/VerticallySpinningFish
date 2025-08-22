package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record ContainerStatusRequest(@NotNull String id, @NotNull Status status) {}
