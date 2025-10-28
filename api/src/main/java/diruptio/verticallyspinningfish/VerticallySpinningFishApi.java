package diruptio.verticallyspinningfish;

import com.google.gson.Gson;
import diruptio.verticallyspinningfish.api.*;
import java.io.IOException;
import java.util.*;
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

    void reconnect() {
        if (closed || (reconnectThread != null && reconnectThread.isAlive())) {
            return;
        }
        connect();
    }

    private void connect() {
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
        } catch (IOException e) {
            if (closed) {
                return;
            }
            System.err.println("Failed to connect to VerticallySpinningFish API. Retrying in 15 seconds...");
            try {
                Thread.sleep(15000);
                if (!closed) {
                    reconnectThread = Thread.ofVirtual().start(this::connect);
                }
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }
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

    public @Nullable Group getGroupByName(@NotNull String name) {
        for (Group group : groups) {
            if (group.name().equals(name)) {
                return group;
            }
        }
        return null;
    }

    public @Nullable Group getGroupByContainer(@NotNull String containerName) {
        for (Group group : groups) {
            if (containerName.startsWith(containerPrefix + group.name() + "-")) {
                return group;
            }
        }
        return null;
    }

    public @NotNull List<Container> getContainers() {
        return containers;
    }

    public @NotNull List<Container> getContainersByGroup(@NotNull String name) {
        String prefix = containerPrefix + name + "-";
        List<Container> containers = new ArrayList<>();
        for (Container container : this.containers) {
            if (container.getName().startsWith(prefix)) {
                containers.add(container);
            }
        }
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

    public void startContainer(@NotNull String containerId) {
        Request request = new Request.Builder()
                .post(RequestBody.create(gson.toJson(new ContainerStartRequest(containerId)).getBytes()))
                .url(baseUrl + "/container/start")
                .addHeader("Authorization", secret)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Failed to start container " + containerId + ": " + response.body().string());
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace(System.err);
        }
    }

    public void stopContainer(@NotNull String containerId) {
        Request request = new Request.Builder()
                .post(RequestBody.create(gson.toJson(new ContainerStopRequest(containerId)).getBytes()))
                .url(baseUrl + "/container/stop")
                .addHeader("Authorization", secret)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Failed to stop container " + containerId + ": " + response.body().string());
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace(System.err);
        }
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

    public void updateGroupMinCount(@NotNull String name, int minCount, @NotNull Consumer<Group> onSuccess, @NotNull Consumer<String> onError) {
        GroupMinCountUpdateRequest body = new GroupMinCountUpdateRequest(name, minCount);
        Request request = new Request.Builder()
                .patch(RequestBody.create(gson.toJson(body).getBytes()))
                .url(baseUrl + "/group/min-count")
                .addHeader("Authorization", secret)
                .build();
        updateGroupProperty(request, onSuccess, onError);
    }

    public void updateGroupMinPort(@NotNull String name, int minPort, @NotNull Consumer<Group> onSuccess, @NotNull Consumer<String> onError) {
        GroupMinPortUpdateRequest body = new GroupMinPortUpdateRequest(name, minPort);
        Request request = new Request.Builder()
                .patch(RequestBody.create(gson.toJson(body).getBytes()))
                .url(baseUrl + "/group/min-port")
                .addHeader("Authorization", secret)
                .build();
        updateGroupProperty(request, onSuccess, onError);
    }

    public void updateGroupDeleteOnStop(@NotNull String name, boolean deleteOnStop, @NotNull Consumer<Group> onSuccess, @NotNull Consumer<String> onError) {
        GroupDeleteOnStopUpdateRequest body = new GroupDeleteOnStopUpdateRequest(name, deleteOnStop);
        Request request = new Request.Builder()
                .patch(RequestBody.create(gson.toJson(body).getBytes()))
                .url(baseUrl + "/group/delete-on-stop")
                .addHeader("Authorization", secret)
                .build();
        updateGroupProperty(request, onSuccess, onError);
    }

    public void updateGroupTags(@NotNull String name, @NotNull Set<String> tags, @NotNull Consumer<Group> onSuccess, @NotNull Consumer<String> onError) {
        GroupTagsUpdateRequest body = new GroupTagsUpdateRequest(name, tags);
        Request request = new Request.Builder()
                .patch(RequestBody.create(gson.toJson(body).getBytes()))
                .url(baseUrl + "/group/tags")
                .addHeader("Authorization", secret)
                .build();
        updateGroupProperty(request, onSuccess, onError);
    }

    private void updateGroupProperty(@NotNull Request request, @NotNull Consumer<Group> onSuccess, @NotNull Consumer<String> onError) {
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Group updated = gson.fromJson(response.body().string(), Group.class);
                // refresh local cache by replacing matching group
                List<Group> newGroups = new ArrayList<>(groups);
                boolean replaced = false;
                for (int i = 0; i < newGroups.size(); i++) {
                    if (newGroups.get(i).name().equals(updated.name())) {
                        newGroups.set(i, updated);
                        replaced = true;
                        break;
                    }
                }
                if (!replaced) newGroups.add(updated);
                groups = Collections.unmodifiableList(newGroups);
                onSuccess.accept(updated);
            } else {
                onError.accept(response.body() != null ? response.body().string() : ("HTTP " + response.code()));
            }
        } catch (IOException | RuntimeException e) {
            onError.accept(e.getMessage());
        }
    }

    public @Nullable String getGroupDockerfile(@NotNull String name) {
        Request request = new Request.Builder()
                .get()
                .url(baseUrl + "/group/dockerfile?name=" + name)
                .addHeader("Authorization", secret)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
            GroupDockerfileResponse body = gson.fromJson(response.body().string(), GroupDockerfileResponse.class);
            return body.dockerfile();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    public void updateGroupDockerfile(@NotNull String name, @NotNull String dockerfile, @NotNull Consumer<Group> onSuccess, @NotNull Consumer<String> onError) {
        GroupDockerfileUpdateRequest body = new GroupDockerfileUpdateRequest(name, dockerfile);
        Request request = new Request.Builder()
                .patch(RequestBody.create(gson.toJson(body).getBytes()))
                .url(baseUrl + "/group/dockerfile")
                .addHeader("Authorization", secret)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Group updated = gson.fromJson(response.body().string(), Group.class);
                // update local cache
                List<Group> newGroups = new ArrayList<>(groups);
                boolean replaced = false;
                for (int i = 0; i < newGroups.size(); i++) {
                    if (newGroups.get(i).name().equals(updated.name())) {
                        newGroups.set(i, updated);
                        replaced = true;
                        break;
                    }
                }
                if (!replaced) newGroups.add(updated);
                groups = Collections.unmodifiableList(newGroups);
                onSuccess.accept(updated);
            } else {
                onError.accept(response.body() != null ? response.body().string() : ("HTTP " + response.code()));
            }
        } catch (IOException | RuntimeException e) {
            onError.accept(e.getMessage());
        }
    }

    public @NotNull List<Map<String, Object>> getGroupTemplate(@NotNull String name) {
        Request request = new Request.Builder()
                .get()
                .url(baseUrl + "/group/template?name=" + name)
                .addHeader("Authorization", secret)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
            GroupTemplateResponse body = gson.fromJson(response.body().string(), GroupTemplateResponse.class);
            return body.template();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace(System.err);
            return Collections.emptyList();
        }
    }

    public void updateGroupTemplate(@NotNull String name, @NotNull List<Map<String, Object>> template, @NotNull Consumer<Group> onSuccess, @NotNull Consumer<String> onError) {
        GroupTemplateUpdateRequest body = new GroupTemplateUpdateRequest(name, template);
        Request request = new Request.Builder()
                .patch(RequestBody.create(gson.toJson(body).getBytes()))
                .url(baseUrl + "/group/template")
                .addHeader("Authorization", secret)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Group updated = gson.fromJson(response.body().string(), Group.class);
                // update local cache
                List<Group> newGroups = new ArrayList<>(groups);
                boolean replaced = false;
                for (int i = 0; i < newGroups.size(); i++) {
                    if (newGroups.get(i).name().equals(updated.name())) {
                        newGroups.set(i, updated);
                        replaced = true;
                        break;
                    }
                }
                if (!replaced) newGroups.add(updated);
                groups = Collections.unmodifiableList(newGroups);
                onSuccess.accept(updated);
            } else {
                onError.accept(response.body() != null ? response.body().string() : ("HTTP " + response.code()));
            }
        } catch (IOException | RuntimeException e) {
            onError.accept(e.getMessage());
        }
    }

    public static @NotNull VerticallySpinningFishApi fromCurrentContainer() {
        String containerPrefix = System.getenv("VSF_PREFIX");
        String baseUrl = "http://172.17.0.1:" + System.getenv("VSF_API_PORT");
        String secret = System.getenv("VSF_SECRET");
        return new VerticallySpinningFishApi(containerPrefix, baseUrl, secret);
    }
}
