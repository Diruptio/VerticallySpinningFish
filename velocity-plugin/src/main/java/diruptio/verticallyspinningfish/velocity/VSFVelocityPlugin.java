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
        api.getPlayerConnectListeners().add(this::onPlayerConnect);

        for (Container container : api.getContainers()) {
            if (container.getPorts().isEmpty()) {
                continue;
            }

            Group group = api.getGroupByContainer(container.getName());
            if (group == null || !group.tags().contains("register-velocity")) {
                continue;
            }

            String name = container.getName().replace(api.getContainerPrefix(), "");
            InetSocketAddress serverAddress = new InetSocketAddress("172.17.0.1", container.getPorts().getFirst());
            Optional<RegisteredServer> server = this.server.getServer(name);
            if (server.isPresent()) {
                if (server.get().getServerInfo().getAddress().equals(serverAddress)) {
                    continue;
                } else {
                    this.server.unregisterServer(server.get().getServerInfo());
                }
            }

            logger.info("Registering container {} as server {}", container.getName(), name);
            this.server.registerServer(new ServerInfo(name, serverAddress));
            if (group.tags().contains("velocity-fallback")) {
                this.server.getConfiguration().getAttemptConnectionOrder().add(name);
            }
        }
    }

    private void onContainerAdd(@NotNull Container container) {
        if (container.getPorts().isEmpty()) {
            return;
        }

        Group group = api.getGroupByContainer(container.getName());
        if (group == null || !group.tags().contains("register-velocity")) {
            return;
        }

        String name = container.getName().replace(api.getContainerPrefix(), "");
        InetSocketAddress serverAddress = new InetSocketAddress("172.17.0.1", container.getPorts().getFirst());
        Optional<RegisteredServer> server = this.server.getServer(name);
        if (server.isPresent()) {
            if (server.get().getServerInfo().getAddress().equals(serverAddress)) {
                return;
            } else {
                this.server.unregisterServer(server.get().getServerInfo());
            }
        }

        logger.info("Registering container {} as server {}", container.getName(), name);
        this.server.registerServer(new ServerInfo(name, serverAddress));
        if (group.tags().contains("velocity-fallback")) {
            this.server.getConfiguration().getAttemptConnectionOrder().add(name);
        }
    }

    private void onContainerRemove(@NotNull Container container) {
        String name = container.getName().replace(api.getContainerPrefix(), "");
        logger.info("Unregistering server {}", name);
        server.getServer(name).ifPresent(server -> this.server.unregisterServer(server.getServerInfo()));
        server.getConfiguration().getAttemptConnectionOrder().remove(name);
    }

    private void onPlayerConnect(PlayerConnectUpdate update) {
        Optional<Player> player = server.getPlayer(update.player());
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
        Optional<RegisteredServer> targetServer = server.getServer(serverName);
        if (targetServer.isEmpty()) {
            logger.warn("Could not find server with name {} (from container {}) to connect player {} to", serverName, update.containerId(), update.player());
            return;
        }
        logger.info("Connecting player {} to server {} (container {})", player.get().getUsername(), serverName, update.containerId());
        player.get().createConnectionRequest(targetServer.get()).fireAndForget();
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent ignored) {
        api.close();
    }
}
