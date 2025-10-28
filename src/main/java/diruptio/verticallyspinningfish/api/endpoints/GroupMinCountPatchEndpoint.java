package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.GroupMinCountUpdateRequest;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public class GroupMinCountPatchEndpoint implements Handler {
    @OpenApi(
            path = "/group/min-count",
            methods = HttpMethod.PATCH,
            summary = "Update the minimum count of a group",
            security = @OpenApiSecurity(name = "secret"),
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(from = GroupMinCountUpdateRequest.class),
                    required = true),
            responses = @OpenApiResponse(status = "200"))
    @Override
    public void handle(@NotNull Context ctx) {
        GroupMinCountUpdateRequest request = ctx.bodyAsClass(GroupMinCountUpdateRequest.class);
        String name = request.name();
        if (name == null || name.isBlank()) {
            throw new BadRequestResponse("Missing group name");
        }

        Path yamlPath = Path.of("groups").resolve(name + ".yml");
        if (!Files.isRegularFile(yamlPath)) {
            throw new BadRequestResponse("Group not found");
        }

        int value = request.minCount();
        if (value < 0) {
            throw new BadRequestResponse("minCount must be >= 0");
        }

        VerticallySpinningFish.updateGroupMinCount(name, value);
    }
}
