package diruptio.verticallyspinningfish.api;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public record GroupTemplateUpdateRequest(@NotNull String name, @NotNull List<Map<String, Object>> template) {}
