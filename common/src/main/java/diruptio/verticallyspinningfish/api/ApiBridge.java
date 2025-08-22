package diruptio.verticallyspinningfish.api;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ApiBridge {
    public static void setContainerStatus(Container container, Status status) {
        container.setStatus(status);
    }
}
