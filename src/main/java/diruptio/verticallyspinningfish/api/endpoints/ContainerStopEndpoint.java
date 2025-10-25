package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.Container;
import diruptio.verticallyspinningfish.api.ContainerStopRequest;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.*;
import org.jetbrains.annotations.NotNull;

public class ContainerStopEndpoint implements Handler {
    @OpenApi(
            path = "/container/stop",
            methods = HttpMethod.PATCH,
            summary = "Stop a container",
            security = @OpenApiSecurity(name = "secret"),
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(from = ContainerStopRequest.class),
                    required = true),
            responses = @OpenApiResponse(status = "200"))
    @Override
    public void handle(@NotNull Context ctx) {
        ContainerStopRequest request = ctx.bodyAsClass(ContainerStopRequest.class);

        Container container = VerticallySpinningFish.getContainer(request.id());
        if (container == null) {
            throw new BadRequestResponse("Container not found");
        }
        if (container.getStatus().isOffline()) {
            throw new BadRequestResponse("Cannot stop an offline container");
        }

        VerticallySpinningFish.stopContainer(container.getId());
    }
}
