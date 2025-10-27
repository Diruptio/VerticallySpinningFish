package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.NotNull;

public record GroupDockerfileResponse(@NotNull String name, @NotNull String dockerfile) {}
