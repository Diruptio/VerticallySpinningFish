package diruptio.verticallyspinningfish.template;

import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class PaperPluginStep implements TemplateStep {
    private String hash;

    public PaperPluginStep() {
        update();
    }

    @Override
    public void update() {
        try {
            URL url = Objects.requireNonNull(getClass().getResource("/VSFPaperPlugin.jar"));
            byte[] paperPlugin = Resources.toByteArray(url);
            hash = "paper-plugin:" + Hashing.sha256().hashBytes(paperPlugin);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return;
        }
        hash = Hashing.sha256().hashString(hash, StandardCharsets.UTF_8).toString();
    }

    @Override
    public @NotNull String hash() {
        return hash;
    }

    @Override
    public void apply(@NotNull Path directory) throws IOException {
        Files.createDirectories(directory.resolve("plugins"));
        InputStream in = Objects.requireNonNull(getClass().getResourceAsStream("/VSFPaperPlugin.jar"));
        OutputStream out = Files.newOutputStream(directory.resolve("plugins").resolve("VerticallySpinningFish.jar"));
        in.transferTo(out);
        in.close();
        out.close();
    }
}
