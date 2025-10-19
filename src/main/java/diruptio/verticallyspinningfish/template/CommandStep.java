package diruptio.verticallyspinningfish.template;

import com.google.common.hash.Hashing;
import diruptio.util.config.ConfigSection;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class CommandStep implements TemplateStep {
    private final boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
    private final String command;
    private final String input;
    private String hash;

    public CommandStep(@NotNull ConfigSection config) {
        if (!config.contains("command")) {
            throw new IllegalArgumentException("Parameter \"command\" is missing");
        }
        command = config.get("command").toString();
        
        input = config.getString("input");

        update();
    }

    @Override
    public void update() {
        hash = "command:" + command + (input != null ? ":" + input : "");
        hash = Hashing.sha256().hashString(hash, StandardCharsets.UTF_8).toString();
    }

    @Override
    public @NotNull String hash() {
        return hash;
    }

    @Override
    public void apply(@NotNull Path directory) throws IOException {
        Process process = new ProcessBuilder()
                .command(windows ? List.of("cmd", "/c", this.command) : List.of("sh", "-c", this.command))
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();

        if (input != null) {
            process.outputWriter().append(input).append('\n');
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Command exited with code " + exitCode + " in directory "
                        + directory.toAbsolutePath() + ": " + command);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command was interrupted in directory " 
                    + directory.toAbsolutePath() + ": " + command, e);
        }
    }
}
