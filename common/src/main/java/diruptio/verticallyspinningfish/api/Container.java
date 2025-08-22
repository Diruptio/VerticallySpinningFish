package diruptio.verticallyspinningfish.api;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public class Container {
    private final String id;
    private final String name;
    private final List<Integer> ports;
    private Status status;

    public Container(@NotNull String id, @NotNull String name, @NotNull List<Integer> ports, @NotNull Status status) {
        this.id = id;
        this.name = name;
        this.ports = ports;
        this.status = status;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull List<Integer> getPorts() {
        return ports;
    }

    public @NotNull Status getStatus() {
        return status;
    }

    void setStatus(@NotNull Status status) {
        this.status = status;
    }
}