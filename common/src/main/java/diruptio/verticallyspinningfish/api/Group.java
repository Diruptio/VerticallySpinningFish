package diruptio.verticallyspinningfish.api;

import java.util.Set;
import org.jetbrains.annotations.NotNull;

public record Group(@NotNull String name, int minCount, int minPort, boolean deleteOnStop, @NotNull Set<String> tags) {}
