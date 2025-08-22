package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.Container;
import diruptio.verticallyspinningfish.api.ContainerStatusRequest;
import diruptio.verticallyspinningfish.api.ContainerStatusUpdate;
import diruptio.verticallyspinningfish.api.ApiBridge;
import diruptio.verticallyspinningfish.api.Status;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.jetbrains.annotations.NotNull;

public class ContainerStatusEndpoint implements Handler {
    @OpenApi(
            path = "/container/status",
            methods = HttpMethod.PATCH,
            summary = "Update the status of a container",
            security = @OpenApiSecurity(name = "secret"),
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(from = ContainerStatusRequest.class),
                    required = true),
            responses = @OpenApiResponse(status = "200"))
    @Override
    public void handle(@NotNull Context ctx) {
        ContainerStatusRequest request = ctx.bodyAsClass(ContainerStatusRequest.class);

        Container container = VerticallySpinningFish.getContainerCache().get(request.id());
        if (container == null) {
            throw new BadRequestResponse("Container not found");
        }

        if (container.getStatus().isOffline()) {
            throw new BadRequestResponse("Cannot update status of an offline container");
        } else if (request.status() == Status.AVAILABLE || request.status() == Status.UNAVAILABLE) {
            ApiBridge.setContainerStatus(container, request.status());
            LiveUpdatesWebSocket.broadcastUpdate(new ContainerStatusUpdate(request.id(), request.status()));
        } else {
            throw new BadRequestResponse("Invalid status. Only AVAILABLE or UNAVAILABLE are allowed.");
        }

        ctx.status(HttpStatus.OK);
    }
}
