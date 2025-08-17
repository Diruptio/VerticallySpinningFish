package diruptio.verticallyspinningfish.api.endpoints;

import com.github.dockerjava.api.model.ContainerPort;
import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.Container;
import diruptio.verticallyspinningfish.api.ContainersResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.*;
import java.util.stream.Stream;
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
        ctx.json(new ContainersResponse(VerticallySpinningFish.getDockerClient()
                .listContainersCmd()
                .withShowAll(true)
                .exec()
                .stream()
                .filter(container ->
                        Stream.of(container.getNames()).anyMatch(name -> name.startsWith("/vsf-")))
                .map(container -> new Container(
                        container.getId(),
                        String.join("", container.getNames()).substring(1),
                        Stream.of(container.ports).map(ContainerPort::getPublicPort).toList()))
                .toList()));
    }
}
