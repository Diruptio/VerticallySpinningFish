package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record GroupDockerfileUpdateRequest(@NotNull String name, @NotNull String dockerfile) {}
