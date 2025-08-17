package diruptio.verticallyspinningfish;

import com.google.gson.Gson;
import diruptio.verticallyspinningfish.api.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VerticallySpinningFishApi {
    private static final OkHttpClient httpClient = new OkHttpClient.Builder().build();
    private static final Gson gson = new Gson();
    private final String baseUrl;
    private final String secret;
    boolean closed = false;
    WebSocket liveUpdatesWebSocket;
    List<Group> groups;
    List<Container> containers;
    private final List<Consumer<Container>> containerAddListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Container>> containerRemoveListeners = new CopyOnWriteArrayList<>();

    public VerticallySpinningFishApi(@NotNull String baseUrl, @NotNull String secret) {
        this.baseUrl = baseUrl;
        this.secret = secret;
        connect();
    }

    void connect() {
        Request request = new Request.Builder()
                .get()
                .url(baseUrl + "/groups")
                .addHeader("Authorization", secret)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            groups = Collections.unmodifiableList(gson.fromJson(response.body().string(), GroupsResponse.class).groups());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        request = new Request.Builder()
                .get()
                .url(baseUrl + "/containers")
                .addHeader("Authorization", secret)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            containers = Collections.unmodifiableList(gson.fromJson(response.body().string(), ContainersResponse.class).containers());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        request = new Request.Builder()
                .url(baseUrl + "/live-updates")
                .addHeader("Authorization", secret)
                .build();
        liveUpdatesWebSocket = httpClient.newWebSocket(request, new LiveUpdatesWebSocketListener(this));
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
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    public void close() {
        closed = true;
        liveUpdatesWebSocket.close(1000, "Disconnect");
    }

    public @NotNull List<Group> getGroups() {
        return groups;
    }

    public @Nullable Group getGroupByContainer(@NotNull String containerName) {
        for (Group group : groups) {
            if (containerName.startsWith("vsf-" + group.name())) {
                return group;
            }
        }
        return null;
    }

    public @NotNull List<Container> getContainers() {
        return containers;
    }

    public @NotNull List<Consumer<Container>> getContainerAddListeners() {
        return containerAddListeners;
    }

    public @NotNull List<Consumer<Container>> getContainerRemoveListeners() {
        return containerRemoveListeners;
    }

    public static @NotNull VerticallySpinningFishApi fromCurrentContainer() {
        String baseUrl = "http://host.docker.internal:" + System.getenv("VSF_API_PORT");
        String secret = System.getenv("VSF_SECRET");
        return new VerticallySpinningFishApi(baseUrl, secret);
    }
}
