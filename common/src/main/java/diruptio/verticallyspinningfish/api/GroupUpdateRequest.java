package diruptio.verticallyspinningfish.api;

import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record GroupUpdateRequest(@NotNull String name, @Nullable Integer minCount, @Nullable Integer minPort,
                                 @Nullable Boolean deleteOnStop, @Nullable Set<String> tags) {}
