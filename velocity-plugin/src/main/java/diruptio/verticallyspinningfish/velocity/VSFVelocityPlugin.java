package diruptio.verticallyspinningfish.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import diruptio.verticallyspinningfish.Container;
import diruptio.verticallyspinningfish.Group;
import diruptio.verticallyspinningfish.VerticallySpinningFishApi;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Plugin(id = "verticallyspinningfish",
        name = "VerticallySpinningFish",
        version = BuildConstants.VERSION,
        authors = "Fabi.exe",
        url = "https://github.com/Diruptio/VerticallySpinningFish")
public class VSFVelocityPlugin {
    private final ProxyServer server;
    private final Logger logger;
    private VerticallySpinningFishApi api;

    @Inject
    public VSFVelocityPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent ignored) {
        api = VerticallySpinningFishApi.fromCurrentContainer();
        api.getContainerAddListeners().add(this::onContainerAdd);
        api.getContainerRemoveListeners().add(this::onContainerRemove);

        for (Container container : api.getContainers()) {
            if (container.ports().isEmpty()) {
                continue;
            }

            Group group = api.getGroupByContainer(container.name());
            if (group == null || !group.getTags().contains("register-velocity")) {
                continue;
            }

            String name = container.name().replace("vsf-", "");
            InetSocketAddress serverAddress = new InetSocketAddress("host.docker.internal", container.ports().getFirst());
            Optional<RegisteredServer> server = this.server.getServer(name);
            if (server.isPresent()) {
                if (server.get().getServerInfo().getAddress().equals(serverAddress)) {
                    continue;
                } else {
                    this.server.unregisterServer(server.get().getServerInfo());
                }
            }

            logger.info("Registering container {} as server {}", container.name(), name);
            this.server.registerServer(new ServerInfo(name, serverAddress));
            if (group.getTags().contains("velocity-fallback")) {
                this.server.getConfiguration().getAttemptConnectionOrder().add(name);
            }
        }
    }

    private void onContainerAdd(@NotNull Container container) {
        if (container.ports().isEmpty()) {
            return;
        }

        Group group = api.getGroupByContainer(container.name());
        if (group == null || !group.getTags().contains("register-velocity")) {
            return;
        }

        String name = container.name().replace("vsf-", "");
        InetSocketAddress serverAddress = new InetSocketAddress("host.docker.internal", container.ports().getFirst());
        Optional<RegisteredServer> server = this.server.getServer(name);
        if (server.isPresent()) {
            if (server.get().getServerInfo().getAddress().equals(serverAddress)) {
                return;
            } else {
                this.server.unregisterServer(server.get().getServerInfo());
            }
        }

        logger.info("Registering container {} as server {}", container.name(), name);
        this.server.registerServer(new ServerInfo(name, serverAddress));
        if (group.getTags().contains("velocity-fallback")) {
            this.server.getConfiguration().getAttemptConnectionOrder().add(name);
        }
    }

    private void onContainerRemove(@NotNull Container container) {
        String name = container.name().replace("vsf-", "");
        logger.info("Unregistering server {}", name);
        server.getServer(name).ifPresent(server -> this.server.unregisterServer(server.getServerInfo()));
        server.getConfiguration().getAttemptConnectionOrder().remove(name);
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent ignored) {
        api.close();
    }
}
