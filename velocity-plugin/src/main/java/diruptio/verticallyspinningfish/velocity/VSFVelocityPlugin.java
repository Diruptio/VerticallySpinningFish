package diruptio.verticallyspinningfish.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import diruptio.verticallyspinningfish.VerticallySpinningFishApi;
import diruptio.verticallyspinningfish.api.Container;
import diruptio.verticallyspinningfish.api.Group;
import diruptio.verticallyspinningfish.api.PlayerConnectUpdate;
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
    private final ProxyServer proxyServer;
    private final Logger logger;
    private VerticallySpinningFishApi api;

    @Inject
    public VSFVelocityPlugin(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent ignored) {
        api = VerticallySpinningFishApi.fromCurrentContainer();
        api.getContainerAddListeners().add(this::onContainerAdd);
        api.getContainerRemoveListeners().add(this::onContainerRemove);
        api.getContainerStatusListeners().add(this::onContainerStatusUpdate);
        api.getPlayerConnectListeners().add(this::onPlayerConnect);

        for (Container container : api.getContainers()) {
            if (container.getStatus().isOnline()) {
                registerServer(container);
            }
        }
    }

    private void onContainerAdd(@NotNull Container container) {
        if (container.getStatus().isOnline()) {
            registerServer(container);
        }
    }

    private void onContainerRemove(@NotNull Container container) {
        unregisterServer(container);
    }

    private void onContainerStatusUpdate(@NotNull Container container) {
        if (container.getStatus().isOnline()) {
            registerServer(container);
        } else {
            unregisterServer(container);
        }
    }

    private void onPlayerConnect(PlayerConnectUpdate update) {
        Optional<Player> player = proxyServer.getPlayer(update.player());
        if (player.isEmpty()) {
            logger.warn("Could not find player with UUID {} to connect to container {}", update.player(), update.containerId());
            return;
        }
        Container container = api.getContainer(update.containerId());
        if (container == null) {
            logger.warn("Could not find container with id {} to connect player {} to", update.containerId(), update.player());
            return;
        }
        String serverName = container.getName().replace(api.getContainerPrefix(), "");
        Optional<RegisteredServer> targetServer = proxyServer.getServer(serverName);
        if (targetServer.isEmpty()) {
            logger.warn("Could not find server with name {} (from container {}) to connect player {} to", serverName, update.containerId(), update.player());
            return;
        }
        logger.info("Connecting player {} to server {} (container {})", player.get().getUsername(), serverName, update.containerId());
        player.get().createConnectionRequest(targetServer.get()).fireAndForget();
    }

    private void registerServer(Container container) {
        if (container.getPorts().isEmpty()) {
            return;
        }

        Group group = api.getGroupByContainer(container.getName());
        if (group == null || !group.tags().contains("register-velocity")) {
            return;
        }

        String name = container.getName().replace(api.getContainerPrefix(), "");
        InetSocketAddress serverAddress = new InetSocketAddress("172.17.0.1", container.getPorts().getFirst());
        Optional<RegisteredServer> server = proxyServer.getServer(name);
        if (server.isPresent()) {
            if (server.get().getServerInfo().getAddress().equals(serverAddress)) {
                return;
            } else {
                proxyServer.unregisterServer(server.get().getServerInfo());
            }
        }

        logger.info("Registering container {} as server {}", container.getName(), name);
        proxyServer.registerServer(new ServerInfo(name, serverAddress));
        if (group.tags().contains("velocity-fallback")) {
            proxyServer.getConfiguration().getAttemptConnectionOrder().add(name);
        }
    }

    private void unregisterServer(Container container) {
        String name = container.getName().replace(api.getContainerPrefix(), "");
        logger.info("Unregistering server {}", name);
        proxyServer.getServer(name).ifPresent(server -> proxyServer.unregisterServer(server.getServerInfo()));
        proxyServer.getConfiguration().getAttemptConnectionOrder().remove(name);
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent ignored) {
        api.close();
    }
}
