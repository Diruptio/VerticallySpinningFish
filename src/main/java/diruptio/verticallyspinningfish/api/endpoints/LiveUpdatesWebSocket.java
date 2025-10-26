package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.api.LiveUpdate;
import io.javalin.websocket.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class LiveUpdatesWebSocket implements Consumer<WsConfig>, WsConnectHandler, WsCloseHandler {
    private static final List<WsContext> connections = new CopyOnWriteArrayList<>();

    @Override
    public void accept(WsConfig wsConfig) {
        wsConfig.onConnect(this);
        wsConfig.onClose(this);
    }

    @Override
    public void handleConnect(@NotNull WsConnectContext ctx) {
        ctx.enableAutomaticPings();
        connections.add(ctx);
    }

    @Override
    public void handleClose(@NotNull WsCloseContext ctx) {
        connections.remove(ctx);
    }

    public static void broadcastUpdate(@NotNull LiveUpdate update) {
        List<WsContext> connectionsToClose = new ArrayList<>();
        for (WsContext ctx : connections) {
            try {
                ctx.send(update.type());
                ctx.send(update);
            } catch (Exception ignored) {
                connectionsToClose.add(ctx);
            }
        }
        for (WsContext ctx : connectionsToClose) {
            connections.remove(ctx);
            try {
                ctx.closeSession();
            } catch (Exception ignored) {}
        }
    }
}
