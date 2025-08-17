package diruptio.verticallyspinningfish;

import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class Group {
    private String name;
    private int minCount;
    private int minPort;
    private boolean deleteOnStop;
    private Set<String> tags;

    Group(@NotNull String name, int minCount, int minPort, boolean deleteOnStop, @NotNull Set<String> tags) {
        this.name = name;
        this.minCount = minCount;
        this.minPort = minPort;
        this.deleteOnStop = deleteOnStop;
        this.tags = tags;
    }

    public @NotNull String getName() {
        return name;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMinPort() {
        return minPort;
    }

    public boolean isDeleteOnStop() {
        return deleteOnStop;
    }

    public @NotNull Set<String> getTags() {
        return tags;
    }
}
