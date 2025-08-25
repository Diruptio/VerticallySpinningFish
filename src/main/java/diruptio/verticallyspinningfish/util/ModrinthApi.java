package diruptio.verticallyspinningfish.util;

import com.google.gson.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModrinthApi {
    public static final String BASE_URL = "https://api.modrinth.com";
    private static final OkHttpClient client = new OkHttpClient.Builder().build();

    public @NotNull String getLatestVersion(@NotNull String project,
                                            @NotNull String platform,
                                            @Nullable String minecraft,
                                            @NotNull String channel) {
        RuntimeException noVersionsFound = new RuntimeException("No versions found for " +
                "project: " + project + ", " +
                "platform: " + platform + ", " +
                "minecraft: " + minecraft + ", " +
                "channel: " + channel);
        Request request = new Request.Builder()
                .url(BASE_URL + "/v2/project/" + project + "/version?loaders=[\"" + platform + "\"]")
                .build();
        try (Response response = client.newCall(request).execute()) {
            JsonArray json = JsonParser.parseString(response.body().string()).getAsJsonArray();
            if (json.isEmpty()) {
                throw noVersionsFound;
            }

            for (JsonElement versionJson : json) {
                if (!versionJson.getAsJsonObject().get("version_type").getAsString().equalsIgnoreCase(channel)) {
                    continue;
                }

                if (minecraft == null) {
                    return versionJson.getAsJsonObject().get("version_number").getAsString();
                }

                for (JsonElement gameVersion : versionJson.getAsJsonObject().getAsJsonArray("game_versions")) {
                    if (gameVersion.getAsString().equalsIgnoreCase(minecraft)) {
                        return versionJson.getAsJsonObject().get("version_number").getAsString();
                    }
                }
            }

            throw noVersionsFound;
        } catch (IOException | JsonParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void download(@NotNull String project,
                         @NotNull String platform,
                         @NotNull String version,
                         @NotNull Path directory) {
        String name = null;
        String url = null;
        Request request = new Request.Builder()
                .url(BASE_URL + "/v2/project/" + project + "/version/" + version)
                .build();
        try (Response response = client.newCall(request).execute()) {
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonArray filesJson = json.getAsJsonArray("files");

            for (JsonElement fileJson : filesJson) {
                if (fileJson.getAsJsonObject().get("primary").getAsBoolean()) {
                    name = fileJson.getAsJsonObject().get("filename").getAsString();
                    url = fileJson.getAsJsonObject().get("url").getAsString();
                }
            }

            if (url == null) {
                throw new RuntimeException("No downloads found for " +
                        "project: " + project + ", " +
                        "platform: " + platform + ", " +
                        "version: " + version);
            }
        } catch (IOException | JsonParseException e) {
            throw new RuntimeException(e);
        }

        request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            OutputStream out = Files.newOutputStream(directory.resolve(name));
            response.body().byteStream().transferTo(out);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
