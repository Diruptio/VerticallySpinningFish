package diruptio.verticallyspinningfish.api;

import diruptio.verticallyspinningfish.api.endpoints.ContainersEndpoint;
import diruptio.verticallyspinningfish.api.endpoints.GroupsEndpoint;
import diruptio.verticallyspinningfish.api.endpoints.LiveUpdatesWebSocket;
import io.javalin.Javalin;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.plugin.bundled.CorsPluginConfig;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

import static io.javalin.apibuilder.ApiBuilder.*;

public class WebApiThread implements Runnable {
    private final String secret;

    public WebApiThread(@NotNull String secret) {
        this.secret = secret;
    }

    @Override
    public void run() {
        Javalin javalin = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.useVirtualThreads = true;
            config.http.asyncTimeout = 10000L;
            config.bundledPlugins.enableCors(cors -> cors.addRule(CorsPluginConfig.CorsRule::anyHost));
            config.registerPlugin(new OpenApiPlugin(openApiConfig -> {
                openApiConfig.withDocumentationPath("/openapi.json");
                openApiConfig.withDefinitionConfiguration((version, definition) -> {
                    definition.withInfo(info -> {
                        info.description("Vertically Spinning Fish API documentation");
                        info.title("Vertically Spinning Fish API");
                    });
                    definition.withServer(server -> {
                        server.description("Local development server");
                        server.url("http://localhost:7000");
                    });
                    definition.withSecurity(security -> {
                        security.withBearerAuth("secret");
                    });
                });
            }));
            config.registerPlugin(new SwaggerPlugin(swaggerConfiguration -> {
                swaggerConfiguration.setDocumentationPath("/openapi.json");
            }));
            config.router.apiBuilder(() -> {
                before("containers", new AuthenticationHandler(secret));
                get("containers", new ContainersEndpoint());
                before("groups", new AuthenticationHandler(secret));
                get("groups", new GroupsEndpoint());
                before("live-updates", new AuthenticationHandler(secret));
                ws("live-updates", new LiveUpdatesWebSocket());
                after(ctx -> {
                    ctx.header("Access-Control-Allow-Origin", "*");
                    if (ctx.status().isError()) {
                        ctx.json(Map.of("error", Objects.requireNonNull(ctx.result())));
                    }
                });
            });
        });
        Runtime.getRuntime().addShutdownHook(new Thread(javalin::stop));
        javalin.start(7000);
    }
}
