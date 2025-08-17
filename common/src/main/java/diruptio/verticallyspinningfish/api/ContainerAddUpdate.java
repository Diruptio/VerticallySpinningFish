package diruptio.verticallyspinningfish.api;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public record ContainerAddUpdate(@NotNull String id,
                                 @NotNull String name,
                                 @NotNull List<Integer> ports) implements LiveUpdate {
    @Override
    public @NotNull String type() {
        return "container_add";
    }
}
