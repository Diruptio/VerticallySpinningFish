package diruptio.verticallyspinningfish.util;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Set;

public class ContainerUtil {
    public static @NotNull String findContainerName(@NotNull List<Container> containers, @NotNull String groupName) {
        int number = 1;
        while (true) {
            String containerName = "vsf-" + groupName + "-" + number;
            boolean nameExists = containers.stream().anyMatch(c -> Set.of(c.getNames()).contains(containerName));
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
        while (true) {
            try (ServerSocket ignored = new ServerSocket(port)) {
                break;
            } catch (IOException e) {
                port++;
            }
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
