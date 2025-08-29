package diruptio.verticallyspinningfish;

import com.google.gson.Gson;
import diruptio.verticallyspinningfish.api.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VerticallySpinningFishApi {
    private static final OkHttpClient httpClient = new OkHttpClient.Builder().build();
    private static final Gson gson = new Gson();
    private final String containerPrefix;
    private final String baseUrl;
    private final String secret;
    boolean closed = false;
    Thread reconnectThread;
    WebSocket liveUpdatesWebSocket;
    List<Group> groups = Collections.emptyList();
    List<Container> containers = Collections.emptyList();
    private final List<Consumer<Container>> containerAddListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Container>> containerRemoveListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Container>> containerStatusListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<PlayerConnectUpdate>> playerConnectListeners = new CopyOnWriteArrayList<>();

    public VerticallySpinningFishApi(@NotNull String containerPrefix, @NotNull String baseUrl, @NotNull String secret) {
        this.containerPrefix = containerPrefix;
        this.baseUrl = baseUrl;
        this.secret = secret;
        connect();
    }

    void connect() {
        if (reconnectThread != null && reconnectThread.isAlive()) {
            return;
        }
        reconnectThread = Thread.ofVirtual().start(() -> {
            while (!closed) {
                try {
                    Request request = new Request.Builder()
                            .get()
                            .url(baseUrl + "/groups")
                            .addHeader("Authorization", secret)
                            .build();
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (!response.isSuccessful()) throw new IOException("Failed to get groups: " + response.code());
                        groups = Collections.unmodifiableList(gson.fromJson(response.body().string(), GroupsResponse.class).groups());
                    }

                    request = new Request.Builder()
                            .get()
                            .url(baseUrl + "/containers")
                            .addHeader("Authorization", secret)
                            .build();
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (!response.isSuccessful()) throw new IOException("Failed to get containers: " + response.code());
                        containers = Collections.unmodifiableList(gson.fromJson(response.body().string(), ContainersResponse.class).containers());
                    }

                    request = new Request.Builder()
                            .url(baseUrl + "/live-updates")
                            .addHeader("Authorization", secret)
                            .build();
                    liveUpdatesWebSocket = httpClient.newWebSocket(request, new LiveUpdatesWebSocketListener(this));
                    System.out.println("Successfully connected to VerticallySpinningFish API.");
                    break;
                } catch (IOException e) {
                    System.err.println("Failed to connect to VerticallySpinningFish API. Retrying in 15 seconds...");
                    try {
                        Thread.sleep(15_000);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
    }

    public void close() {
        closed = true;
        if (reconnectThread != null) {
            reconnectThread.interrupt();
        }
        if (liveUpdatesWebSocket != null) {
            liveUpdatesWebSocket.close(1000, "Disconnect");
        }
    }

    public @NotNull String getContainerPrefix() {
        return containerPrefix;
    }

    public @NotNull List<Group> getGroups() {
        return groups;
    }

    public @Nullable Group getGroupByContainer(@NotNull String containerName) {
        for (Group group : groups) {
            if (containerName.startsWith(containerPrefix + group.name())) {
                return group;
            }
        }
        return null;
    }

    public @NotNull List<Container> getContainers() {
        return containers;
    }

    public @NotNull String getCurrentContainerId() {
        return Objects.requireNonNull(System.getenv("HOSTNAME"));
    }

    public @Nullable Container getContainer(@NotNull String id) {
        for (Container container : containers) {
            if (container.getId().startsWith(id) || id.startsWith(container.getId())) {
                return container;
            }
        }
        return null;
    }

    public @Nullable Container createContainer(@NotNull String group) {
        Request request = new Request.Builder()
                .post(RequestBody.create(gson.toJson(new ContainerCreateRequest(group)).getBytes()))
                .url(baseUrl + "/container")
                .addHeader("Authorization", secret)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return gson.fromJson(response.body().string(), ContainerCreateResponse.class).container();
            } else {
                throw new RuntimeException("Failed to create a container from group " + group + ": " + response.body().string());
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    public void connectPlayer(@NotNull UUID player, @NotNull String containerId) {
        Request request = new Request.Builder()
                .post(RequestBody.create(gson.toJson(new PlayerConnectRequest(player, containerId)).getBytes()))
                .url(baseUrl + "/player/connect")
                .addHeader("Authorization", secret)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Failed to connect player " + player + " to container " + containerId + ": " + response.body().string());
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace(System.err);
        }
    }

    public void setContainerStatus(@NotNull String containerId, @NotNull Status status) {
        Request request = new Request.Builder()
                .patch(RequestBody.create(gson.toJson(new ContainerStatusRequest(containerId, status)).getBytes()))
                .url(baseUrl + "/container/status")
                .addHeader("Authorization", secret)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Failed to set status of container " + containerId + " to " + status + ": " + response.body().string());
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace(System.err);
        }
    }

    public @NotNull List<Consumer<Container>> getContainerAddListeners() {
        return containerAddListeners;
    }

    public @NotNull List<Consumer<Container>> getContainerRemoveListeners() {
        return containerRemoveListeners;
    }

    public @NotNull List<Consumer<PlayerConnectUpdate>> getPlayerConnectListeners() {
        return playerConnectListeners;
    }

    public @NotNull List<Consumer<Container>> getContainerStatusListeners() {
        return containerStatusListeners;
    }

    public static @NotNull VerticallySpinningFishApi fromCurrentContainer() {
        String containerPrefix = System.getenv("VSF_PREFIX");
        String baseUrl = "http://172.17.0.1:" + System.getenv("VSF_API_PORT");
        String secret = System.getenv("VSF_SECRET");
        return new VerticallySpinningFishApi(containerPrefix, baseUrl, secret);
    }
}
