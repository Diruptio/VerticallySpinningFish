package diruptio.verticallyspinningfish;

import com.google.gson.Gson;
import diruptio.verticallyspinningfish.api.Container;
import diruptio.verticallyspinningfish.api.ContainerAddUpdate;
import diruptio.verticallyspinningfish.api.ContainerRemoveUpdate;
import diruptio.verticallyspinningfish.api.PlayerConnectUpdate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LiveUpdatesWebSocketListener extends WebSocketListener {
    private static final Gson gson = new Gson();
    private final VerticallySpinningFishApi api;
    private String updateType = null;

    LiveUpdatesWebSocketListener(@NotNull VerticallySpinningFishApi api) {
        this.api = api;
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        if (!api.closed) {
            api.connect();
        }
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        if (!api.closed) {
            api.connect();
        }
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        if (updateType == null) {
            updateType = text;
            return;
        }

        switch (updateType) {
            case "container_add" -> {
                ContainerAddUpdate update = gson.fromJson(text, ContainerAddUpdate.class);
                List<Container> containers = new ArrayList<>(api.containers);
                containers.add(update.container());
                api.containers = Collections.unmodifiableList(containers);
                for (Consumer<Container> listener : api.getContainerAddListeners()) {
                    listener.accept(update.container());
                }
            }

            case "container_remove" -> {
                ContainerRemoveUpdate update = gson.fromJson(text, ContainerRemoveUpdate.class);
                Container container = null;
                for (Container otherContainer : api.containers) {
                    if (otherContainer.getId().equals(update.id())) {
                        container = otherContainer;
                        break;
                    }
                }

                if (container != null) {
                    List<Container> containers = new ArrayList<>(api.containers);
                    containers.remove(container);
                    api.containers = Collections.unmodifiableList(containers);
                    for (Consumer<Container> listener : api.getContainerRemoveListeners()) {
                        listener.accept(container);
                    }
                }
            }

            case "player_connect" -> {
                PlayerConnectUpdate update = gson.fromJson(text, PlayerConnectUpdate.class);
                for (Consumer<PlayerConnectUpdate> listener : api.getPlayerConnectListeners()) {
                    listener.accept(update);
                }
            }
        }
        updateType = null;
    }
}
