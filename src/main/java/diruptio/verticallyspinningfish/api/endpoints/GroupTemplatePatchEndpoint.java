package diruptio.verticallyspinningfish.api.endpoints;

import diruptio.util.config.Config;
import diruptio.util.config.ConfigSection;
import diruptio.verticallyspinningfish.Group;
import diruptio.verticallyspinningfish.VerticallySpinningFish;
import diruptio.verticallyspinningfish.api.GroupTemplateUpdateRequest;
import diruptio.verticallyspinningfish.template.CopyStep;
import diruptio.verticallyspinningfish.template.TemplateBuilder;
import diruptio.verticallyspinningfish.template.TemplateStep;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class GroupTemplatePatchEndpoint implements Handler {
    @OpenApi(
            path = "/group/template",
            methods = HttpMethod.PATCH,
            summary = "Replace template section of a group",
            security = @OpenApiSecurity(name = "secret"),
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = Object.class), required = true),
            responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = diruptio.verticallyspinningfish.api.Group.class)))
    @Override
    public void handle(@NotNull Context ctx) {
        GroupTemplateUpdateRequest request = ctx.bodyAsClass(GroupTemplateUpdateRequest.class);
        String name = request.name();
        List<Map<String, Object>> template = request.template();
        if (name == null || name.isBlank()) throw new BadRequestResponse("Missing group name");
        if (!name.matches("[A-Za-z0-9_-]+")) throw new BadRequestResponse("Invalid group name");
        if (template == null) throw new BadRequestResponse("Missing template");
        if (template.size() > 1000) throw new BadRequestResponse("Template too large");

        Path yamlPath = Path.of("groups").resolve(name + ".yml");
        if (!Files.isRegularFile(yamlPath)) {
            throw new BadRequestResponse("Group not found");
        }

        // Validate that provided template entries can be parsed into TemplateStep objects
        List<TemplateStep> parsedSteps = new ArrayList<>();
        try {
            for (int i = 0; i < template.size(); i++) {
                Map<String, Object> raw = template.get(i);
                if (raw == null) continue;
                // Create a defensive copy with String keys
                Map<String, Object> copy = new HashMap<>();
                for (Map.Entry<String, Object> e : raw.entrySet()) {
                    copy.put(e.getKey(), e.getValue());
                }
                parsedSteps.add(TemplateBuilder.createTemplateStep(new ConfigSection(copy)));
            }
        } catch (Exception e) {
            throw new BadRequestResponse("Invalid template entry: " + e.getMessage());
        }

        // Persist to YAML
        Config config = new Config(yamlPath, Config.Type.YAML);
        config.set("template", template);
        config.save();

        // Reload group and update memory
        Group group = Group.read(yamlPath);
        VerticallySpinningFish.getGroups().put(group.getName(), group);

        // Trigger updates for non-copy steps (best-effort)
        try {
            for (TemplateStep step : group.getTemplate()) {
                if (!(step instanceof CopyStep)) {
                    step.update();
                }
            }
        } catch (Exception ignored) {
        }

        diruptio.verticallyspinningfish.api.Group apiGroup = new diruptio.verticallyspinningfish.api.Group(
                group.getName(),
                group.getMinCount(),
                group.getMinPort(),
                group.isDeleteOnStop(),
                group.getTags());
        ctx.json(apiGroup);
    }
}
