package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.PrefixResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.*;
import org.jetbrains.annotations.NotNull;

public class PrefixEndpoint implements Handler {
    @OpenApi(
            path = "/prefix",
            methods = HttpMethod.GET,
            summary = "Get the container prefix",
            security = @OpenApiSecurity(name = "secret"),
            responses = @OpenApiResponse(
                    status = "200",
                    content = @OpenApiContent(from = PrefixResponse.class)))
    @Override
    public void handle(@NotNull Context ctx) {
        ctx.json(new PrefixResponse(VerticallySpinningFish.getContainerPrefix()));
    }
}
