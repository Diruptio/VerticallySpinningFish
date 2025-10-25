package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.Container;
import diruptio.verticallyspinningfish.api.ContainerStartRequest;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.*;
import org.jetbrains.annotations.NotNull;

public class ContainerStartEndpoint implements Handler {
    @OpenApi(
            path = "/container/start",
            methods = HttpMethod.PATCH,
            summary = "Start a container",
            security = @OpenApiSecurity(name = "secret"),
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(from = ContainerStartRequest.class),
                    required = true),
            responses = @OpenApiResponse(status = "200"))
    @Override
    public void handle(@NotNull Context ctx) {
        ContainerStartRequest request = ctx.bodyAsClass(ContainerStartRequest.class);

        Container container = VerticallySpinningFish.getContainer(request.id());
        if (container == null) {
            throw new BadRequestResponse("Container not found");
        }
        if (container.getStatus().isOnline()) {
            throw new BadRequestResponse("Cannot start an online container");
        }

        VerticallySpinningFish.startContainer(container.getId());
    }
}
