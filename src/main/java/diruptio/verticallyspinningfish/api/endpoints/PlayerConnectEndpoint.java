package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.api.PlayerConnectRequest;
import diruptio.verticallyspinningfish.api.PlayerConnectUpdate;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.jetbrains.annotations.NotNull;

public class PlayerConnectEndpoint implements Handler {
    @OpenApi(
            path = "/player/connect",
            methods = HttpMethod.POST,
            summary = "Connect a player to a server",
            security = @OpenApiSecurity(name = "secret"),
            requestBody = @OpenApiRequestBody(
                    content = @OpenApiContent(from = PlayerConnectRequest.class),
                    required = true),
            responses = @OpenApiResponse(status = "200"))
    @Override
    public void handle(@NotNull Context ctx) {
        PlayerConnectRequest request = ctx.bodyAsClass(PlayerConnectRequest.class);
        LiveUpdatesWebSocket.broadcastUpdate(new PlayerConnectUpdate(request.player(), request.containerId()));
        ctx.status(HttpStatus.OK);
    }
}
