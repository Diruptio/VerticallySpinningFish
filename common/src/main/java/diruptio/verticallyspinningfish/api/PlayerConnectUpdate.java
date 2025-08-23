package diruptio.verticallyspinningfish.api;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public record PlayerConnectUpdate(@NotNull UUID player, @NotNull String containerId) implements LiveUpdate {
    @Override
    public @NotNull String type() {
        return "player_connect";
    }
}
