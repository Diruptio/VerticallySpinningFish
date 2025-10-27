package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.api.GroupDockerfileResponse;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public class GroupDockerfileGetEndpoint implements Handler {
    @Override
    public void handle(@NotNull Context ctx) {
        String name = ctx.queryParam("name");
        if (name == null || name.isBlank()) {
            throw new BadRequestResponse("Missing group name");
        }
        if (!name.matches("[A-Za-z0-9_-]+")) {
            throw new BadRequestResponse("Invalid group name");
        }
        Path dockerfilePath = Path.of("groups").resolve(name + ".Dockerfile");
        if (!Files.isRegularFile(dockerfilePath)) {
            throw new BadRequestResponse("Group or Dockerfile not found");
        }
        try {
            String content = Files.readString(dockerfilePath);
            ctx.json(new GroupDockerfileResponse(name, content));
        } catch (IOException e) {
            throw new BadRequestResponse("Failed to read Dockerfile: " + e.getMessage());
        }
    }
}
