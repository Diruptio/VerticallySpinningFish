package diruptio.verticallyspinningfish.api;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public record PlayerConnectRequest(@NotNull UUID player, @NotNull String containerId) {}
