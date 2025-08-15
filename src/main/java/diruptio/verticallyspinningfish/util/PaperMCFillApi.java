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

public class PaperMCFillApi {
    public static final String BASE_URL = "https://fill.papermc.io";
    private static final OkHttpClient client = new OkHttpClient.Builder().build();

    public @NotNull String getLatestVersion(@NotNull String project) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/v3/projects/" + project)
                .build();
        try (Response response = client.newCall(request).execute()) {
            JsonObject projectJson = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonObject versionsJson = projectJson.getAsJsonObject("versions");
            if (versionsJson.isEmpty()) {
                throw new RuntimeException("No versions found for project: " + project);
            }
            JsonArray versionJson = versionsJson.getAsJsonArray(versionsJson.keySet().iterator().next());
            if (versionJson.isEmpty()) {
                throw new RuntimeException("No versions found for project: " + project);
            }
            return versionJson.get(0).getAsString();
        } catch (IOException | JsonParseException e) {
            throw new RuntimeException(e);
        }
    }

    public int getLatestBuild(@NotNull String project, @NotNull String version) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/v3/projects/" + project + "/versions/" + version + "/builds/latest")
                .build();
        try (Response response = client.newCall(request).execute()) {
            JsonObject buildJson = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return buildJson.get("id").getAsInt();
        } catch (IOException | JsonParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void download(@NotNull String project, @NotNull String version, int build, @NotNull Path directory) {
        String name;
        String url;
        Request request = new Request.Builder()
                .url(BASE_URL + "/v3/projects/" + project + "/versions/" + version + "/builds/" + build)
                .build();
        try (Response response = client.newCall(request).execute()) {
            JsonObject buildJson = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonObject downloadsJson = buildJson.getAsJsonObject("downloads");
            if (downloadsJson.keySet().isEmpty()) {
                throw new RuntimeException("No downloads found for project: " + project + ", version: " + version);
            }
            JsonObject downloadJson = downloadsJson.getAsJsonObject(downloadsJson.keySet().iterator().next());
            name = downloadJson.get("name").getAsString();
            url = downloadJson.get("url").getAsString();
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
