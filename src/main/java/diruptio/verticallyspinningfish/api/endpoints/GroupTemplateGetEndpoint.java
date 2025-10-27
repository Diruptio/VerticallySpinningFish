package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.util.config.Config;
import diruptio.verticallyspinningfish.api.GroupTemplateResponse;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class GroupTemplateGetEndpoint implements Handler {
    @Override
    public void handle(@NotNull Context ctx) {
        String name = ctx.queryParam("name");
        if (name == null || name.isBlank()) {
            throw new BadRequestResponse("Missing group name");
        }
        if (!name.matches("[A-Za-z0-9_-]+")) {
            throw new BadRequestResponse("Invalid group name");
        }
        Path yamlPath = Path.of("groups").resolve(name + ".yml");
        if (!Files.isRegularFile(yamlPath)) {
            throw new BadRequestResponse("Group not found");
        }

        Config config = new Config(yamlPath, Config.Type.YAML);
        List<Map<String, Object>> templateList = new ArrayList<>();
        if (config.contains("template") && config.get("template") instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typed = (Map<String, Object>) map;
                    templateList.add(typed);
                }
            }
        }
        ctx.json(new GroupTemplateResponse(name, templateList));
    }
}
