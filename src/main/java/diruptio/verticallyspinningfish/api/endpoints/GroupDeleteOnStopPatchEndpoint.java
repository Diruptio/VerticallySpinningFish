package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.GroupDeleteOnStopUpdateRequest;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public class GroupDeleteOnStopPatchEndpoint implements Handler {
    @OpenApi(
            path = "/group/delete-on-stop",
            methods = HttpMethod.PATCH,
            summary = "Update the delete-on-stop property of a group",
            security = @OpenApiSecurity(name = "secret"),
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(from = GroupDeleteOnStopUpdateRequest.class),
                    required = true),
            responses = @OpenApiResponse(status = "200"))
    @Override
    public void handle(@NotNull Context ctx) {
        GroupDeleteOnStopUpdateRequest request = ctx.bodyAsClass(GroupDeleteOnStopUpdateRequest.class);
        String name = request.name();
        if (name == null || name.isBlank()) {
            throw new BadRequestResponse("Missing group name");
        }

        Path yamlPath = Path.of("groups").resolve(name + ".yml");
        if (!Files.isRegularFile(yamlPath)) {
            throw new BadRequestResponse("Group not found");
        }

        VerticallySpinningFish.updateGroupDeleteOnStop(name, request.deleteOnStop());
    }
}
