package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.ContainersResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.*;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

public class ContainersEndpoint implements Handler {
    @OpenApi(
            path = "/containers",
            methods = HttpMethod.GET,
            summary = "Get all containers created by Vertically Spinning Fish",
            security = @OpenApiSecurity(name = "secret"),
            responses = @OpenApiResponse(
                    status = "200",
                    content = @OpenApiContent(from = ContainersResponse.class)))
    @Override
    public void handle(@NotNull Context ctx) {
        ctx.json(new ContainersResponse(new ArrayList<>(VerticallySpinningFish.getContainerCache().values())));
    }
}