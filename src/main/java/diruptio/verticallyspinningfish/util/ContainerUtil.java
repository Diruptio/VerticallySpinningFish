package diruptio.verticallyspinningfish.util;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import diruptio.verticallyspinningfish.VerticallySpinningFish;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class ContainerUtil {
    public static @NotNull String findContainerName(@NotNull List<Container> containers, @NotNull String groupName) {
        int number = 1;
        while (true) {
            String containerName = VerticallySpinningFish.getContainerPrefix() + groupName + "-" + number;
            boolean nameExists = containers.stream().anyMatch(c -> Set.of(c.getNames()).contains("/" + containerName));
            if (nameExists) {
                number++;
            } else {
                return containerName;
            }
        }
    }

    public static int findPort(@NotNull List<Container> containers, int minPort) {
        int port = minPort;
        while (isPortUsed(containers, port)) {
            port++;
        }
        return port;
    }

    private static boolean isPortUsed(@NotNull List<Container> containers, int port) {
        for (Container container : containers) {
            ContainerPort[] containerPorts = container.getPorts();
            for (ContainerPort containerPort : containerPorts) {
                Integer publicPort = containerPort.getPublicPort();
                if (publicPort != null && publicPort == port) {
                    return true;
                }
            }
        }
        return false;
    }
}
