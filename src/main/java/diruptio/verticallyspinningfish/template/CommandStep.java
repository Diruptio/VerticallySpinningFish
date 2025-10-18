package diruptio.verticallyspinningfish.template;

import com.google.common.hash.Hashing;
import diruptio.util.config.ConfigSection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public class CommandStep implements TemplateStep {
    private final String command;
    private final boolean isWindows;
    private String hash;

    public CommandStep(@NotNull ConfigSection config) {
        if (!config.contains("command")) {
            throw new IllegalArgumentException("Parameter \"command\" is missing");
        }
        command = config.get("command").toString();
        
        // Cache OS detection result
        String os = System.getProperty("os.name").toLowerCase();
        isWindows = os.contains("win");

        update();
    }

    @Override
    public void update() {
        // Include OS type in hash to ensure different behavior on different platforms is cached separately
        String osType = isWindows ? "windows" : "unix";
        hash = "command:" + osType + ":" + command;
        hash = Hashing.sha256().hashString(hash, StandardCharsets.UTF_8).toString();
    }

    @Override
    public @NotNull String hash() {
        return hash;
    }

    @Override
    public void apply(@NotNull Path directory) throws IOException {
        String[] shellCommand;
        
        if (isWindows) {
            shellCommand = new String[]{"cmd", "/c", command};
        } else {
            shellCommand = new String[]{"sh", "-c", command};
        }
        
        ProcessBuilder processBuilder = new ProcessBuilder(shellCommand);
        processBuilder.directory(directory.toFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Command exited with code " + exitCode + ": " + command);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command was interrupted: " + command, e);
        }
    }
}
