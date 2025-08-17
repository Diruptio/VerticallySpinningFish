package diruptio.verticallyspinningfish.api;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public record Container(@NotNull String id, @NotNull String name, @NotNull List<Integer> ports) {}
