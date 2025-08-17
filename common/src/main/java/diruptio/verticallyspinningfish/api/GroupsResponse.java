package diruptio.verticallyspinningfish.api;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public record GroupsResponse(@NotNull List<Group> groups) { }
