package diruptio.verticallyspinningfish.api;

import io.javalin.http.*;
import org.jetbrains.annotations.NotNull;

public class AuthenticationHandler implements Handler {
    private final String secret;

    public AuthenticationHandler(@NotNull String secret) {
        this.secret = secret;
    }

    @Override
    public void handle(@NotNull Context ctx) {
        if (ctx.method() == HandlerType.OPTIONS) {
            return;
        }

        String authorizationHeader = ctx.header("Authorization");
        if (authorizationHeader == null) {
            throw new BadRequestResponse("Authorization is required");
        }

        if (!authorizationHeader.equals(secret)) {
            throw new UnauthorizedResponse("Invalid secret");
        }
    }
}
