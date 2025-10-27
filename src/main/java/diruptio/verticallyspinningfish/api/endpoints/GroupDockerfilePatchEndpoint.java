package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.Group;
import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.GroupDockerfileUpdateRequest;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public class GroupDockerfilePatchEndpoint implements Handler {
    @Override
    public void handle(@NotNull Context ctx) {
        GroupDockerfileUpdateRequest request = ctx.bodyValidator(GroupDockerfileUpdateRequest.class)
                .check(r -> r.name() != null && !r.name().matches("[A-Za-z0-9_-]+"), "Missing or invalid group name")
                .check(r -> r.dockerfile() != null && !r.dockerfile().isBlank(), "Missing Dockerfile content")
                .get();

        String name = request.name();
        String dockerfile = request.dockerfile();
        Path yamlPath = Path.of("groups").resolve(name + ".yml");
        Path dockerfilePath = Path.of("groups").resolve(name + ".Dockerfile");
        if (!Files.isRegularFile(yamlPath) || !Files.isRegularFile(dockerfilePath)) {
            throw new BadRequestResponse("Group not found");
        }

        try {
            String normalized = dockerfile.replace("\r\n", "\n");
            Files.writeString(dockerfilePath, normalized, StandardCharsets.UTF_8);

            Group group = Group.read(yamlPath);
            VerticallySpinningFish.getGroups().put(group.getName(), group);
            group.rebuildImageIfNeeded();

            diruptio.verticallyspinningfish.api.Group apiGroup = new diruptio.verticallyspinningfish.api.Group(
                    group.getName(),
                    group.getMinCount(),
                    group.getMinPort(),
                    group.isDeleteOnStop(),
                    group.getTags());
            ctx.json(apiGroup);
        } catch (IOException e) {
            throw new BadRequestResponse("Failed to write Dockerfile: " + e.getMessage());
        }
    }
}
