package diruptio.verticallyspinningfish.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

public class PaperMCHangarApi {
    public static final String BASE_URL = "https://hangar.papermc.io/api";
    private static final OkHttpClient client = new OkHttpClient.Builder().build();

    public @NotNull String getLatestVersion(@NotNull String project, @NotNull String platform, @NotNull String channel) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/v1/projects/" + project + "/versions?platform=" + platform + "&channel=" + channel)
                .build();
        try (Response response = client.newCall(request).execute()) {
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonArray resultJson = json.getAsJsonArray("result");
            if (resultJson.isEmpty()) {
                throw new RuntimeException("No versions found for project: " + project + ", platform: " + platform + ", channel: " + channel);
            }
            JsonObject versionJson = resultJson.get(0).getAsJsonObject();
            return versionJson.get("name").getAsString();
        } catch (IOException | JsonParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void download(@NotNull String project, @NotNull String platform, @NotNull String version, @NotNull Path directory) {
        String name;
        String url;
        Request request = new Request.Builder()
                .url(BASE_URL + "/v1/projects/" + project + "/versions/" + version)
                .build();
        try (Response response = client.newCall(request).execute()) {
            JsonObject versionJson = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonObject downloadsJson = versionJson.getAsJsonObject("downloads");
            if (downloadsJson.keySet().isEmpty()) {
                throw new RuntimeException("No downloads found for project: " + project + ", version: " + version);
            }

            String key = null;
            for (String key2 : downloadsJson.keySet()) {
                if (key2.equalsIgnoreCase(platform)) {
                    key = key2;
                }
            }
            if (key == null) {
                throw new RuntimeException("No downloads found for project: " + project + ", version: " + version + ", platform: " + platform);
            }

            JsonObject downloadJson = downloadsJson.getAsJsonObject(key);
            name = downloadJson.getAsJsonObject("fileInfo").get("name").getAsString();
            url = downloadJson.get("downloadUrl").getAsString();
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
