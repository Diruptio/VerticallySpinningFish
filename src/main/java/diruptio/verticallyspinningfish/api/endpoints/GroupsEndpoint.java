package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.Group;
import diruptio.verticallyspinningfish.api.GroupsResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.*;
import org.jetbrains.annotations.NotNull;

public class GroupsEndpoint implements Handler {
    @OpenApi(
            path = "/groups",
            methods = HttpMethod.GET,
            summary = "Get all container groups",
            security = @OpenApiSecurity(name = "secret"),
            responses = @OpenApiResponse(
                    status = "200",
                    content = @OpenApiContent(from = GroupsResponse.class)))
    @Override
    public void handle(@NotNull Context ctx) {
        ctx.json(new GroupsResponse(VerticallySpinningFish.getContainerGroups()
                .values()
                .stream()
                .map(group -> new Group(
                        group.getName(),
                        group.getMinCount(),
                        group.getMinPort(),
                        group.isDeleteOnStop(),
                        group.getTags()))
                .toList()));
    }
}
