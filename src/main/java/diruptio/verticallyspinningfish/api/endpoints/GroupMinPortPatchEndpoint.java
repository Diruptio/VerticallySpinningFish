package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.GroupMinPortUpdateRequest;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public class GroupMinPortPatchEndpoint implements Handler {
    @OpenApi(
            path = "/group/min-port",
            methods = HttpMethod.PATCH,
            summary = "Update the minimum port of a group",
            security = @OpenApiSecurity(name = "secret"),
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(from = GroupMinPortUpdateRequest.class),
                    required = true),
            responses = @OpenApiResponse(status = "200"))
    @Override
    public void handle(@NotNull Context ctx) {
        GroupMinPortUpdateRequest request = ctx.bodyAsClass(GroupMinPortUpdateRequest.class);
        String name = request.name();
        if (name == null || name.isBlank()) {
            throw new BadRequestResponse("Missing group name");
        }

        Path yamlPath = Path.of("groups").resolve(name + ".yml");
        if (!Files.isRegularFile(yamlPath)) {
            throw new BadRequestResponse("Group not found");
        }

        int value = request.minPort();
        if (value < 1 || value > 65535) {
            throw new BadRequestResponse("minPort must be within 1..65535");
        }

        VerticallySpinningFish.updateGroupMinPort(name, value);
    }
}
