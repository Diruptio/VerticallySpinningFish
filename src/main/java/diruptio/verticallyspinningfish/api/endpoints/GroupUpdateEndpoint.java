package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.util.config.Config;
import diruptio.verticallyspinningfish.Group;
import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.GroupUpdateRequest;
import diruptio.verticallyspinningfish.api.GroupUpdateUpdate;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class GroupUpdateEndpoint implements Handler {
    @OpenApi(
            path = "/group",
            methods = HttpMethod.PATCH,
            summary = "Update properties of a group",
            security = @OpenApiSecurity(name = "secret"),
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(from = GroupUpdateRequest.class),
                    required = true),
            responses = @OpenApiResponse(status = "200"))
    @Override
    public void handle(@NotNull Context ctx) {
        GroupUpdateRequest request = ctx.bodyAsClass(GroupUpdateRequest.class);
        String name = request.name();
        if (name == null || name.isBlank()) {
            throw new BadRequestResponse("Missing group name");
        }

        Path yamlPath = Path.of("groups").resolve(name + ".yml");
        if (!Files.isRegularFile(yamlPath)) {
            throw new BadRequestResponse("Group not found");
        }

        // Load current config
        Config config = new Config(yamlPath, Config.Type.YAML);

        // Validate and apply updates (partial)
        if (request.minCount() != null) {
            int value = request.minCount();
            if (value < 0) {
                throw new BadRequestResponse("minCount must be >= 0");
            }
            config.set("min-count", value);
        }
        if (request.minPort() != null) {
            int value = request.minPort();
            if (value < 1 || value > 65535) {
                throw new BadRequestResponse("minPort must be within 1..65535");
            }
            config.set("min-port", value);
        }
        if (request.deleteOnStop() != null) {
            config.set("delete-on-stop", request.deleteOnStop());
        }
        if (request.tags() != null) {
            Set<String> tags = new HashSet<>(request.tags());
            config.set("tags", tags.stream().toList());
        }

        // Persist changes
        config.save();

        // Reload, replace in-memory, and rebuild if needed
        Group group = Group.read(yamlPath);
        VerticallySpinningFish.getGroups().put(group.getName(), group);
        group.rebuildImageIfNeeded();

        // Broadcast update
        diruptio.verticallyspinningfish.api.Group apiGroup = new diruptio.verticallyspinningfish.api.Group(
                group.getName(),
                group.getMinCount(),
                group.getMinPort(),
                group.isDeleteOnStop(),
                group.getTags());
        LiveUpdatesWebSocket.broadcastUpdate(new GroupUpdateUpdate(apiGroup));
    }
}
