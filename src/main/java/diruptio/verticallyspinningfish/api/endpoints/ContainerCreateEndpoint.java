package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.Group;
import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.ContainerCreateRequest;
import diruptio.verticallyspinningfish.api.ContainerCreateResponse;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.openapi.*;
import java.io.IOException;

import org.jetbrains.annotations.NotNull;

public class ContainerCreateEndpoint implements Handler {
    @OpenApi(
            path = "/container",
            methods = HttpMethod.POST,
            summary = "Create a new container from a group",
            security = @OpenApiSecurity(name = "secret"),
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(from = ContainerCreateRequest.class),
                    required = true),
            responses = @OpenApiResponse(
                        status = "200",
                        content = @OpenApiContent(from = ContainerCreateResponse.class)))
    @Override
    public void handle(@NotNull Context ctx) {
        ContainerCreateRequest request = ctx.bodyAsClass(ContainerCreateRequest.class);
        Group group = VerticallySpinningFish.getGroups().get(request.group());
        if (group == null) {
            throw new BadRequestResponse("Group not found");
        }

        try {
            ctx.json(new ContainerCreateResponse(VerticallySpinningFish.createContainer(group)));
        } catch (IOException e) {
            throw new InternalServerErrorResponse("Failed to create container: " + e.getMessage());
        }
    }
}
