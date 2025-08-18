package diruptio.verticallyspinningfish;

import com.google.common.hash.Hashing;
import diruptio.util.config.Config;
import diruptio.util.config.ConfigSection;
import diruptio.util.placeholder.PlaceholderEngine;
import diruptio.verticallyspinningfish.template.TemplateBuilder;
import diruptio.verticallyspinningfish.template.TemplateStep;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Group {
    private final String name;
    private final int minCount;
    private final int minPort;
    private final boolean deleteOnStop;
    private final Set<String> tags;
    private final List<TemplateStep> template;
    private String dockerfileHash = null;
    private Set<Integer> ports = Set.of();
    private Set<String> volumes = Set.of();
    private String imageId = null;
    private Path templateDir = null;

    private Group(@NotNull String name,
                  int minCount,
                  int minPort,
                  boolean deleteOnStop,
                  @NotNull Set<String> tags,
                  @NotNull List<TemplateStep> template) {
        this.name = name;
        this.minCount = minCount;
        this.minPort = minPort;
        this.deleteOnStop = deleteOnStop;
        this.tags = Collections.unmodifiableSet(tags);
        this.template = Collections.unmodifiableList(template);
    }

    /**
     * Read a {@link Group} from a YAML file
     *
     * @param path The path of the YAML file
     * @return A {@link Group}
     * @throws IllegalArgumentException If the Dockerfile is not found
     */
    @SuppressWarnings("unchecked")
    public static @NotNull Group read(@NotNull Path path) {
        String name = path.getFileName().toString().replaceAll("\\.yml$", "");

        Path dockerfilePath = path.resolveSibling(name + ".Dockerfile");
        if (!dockerfilePath.toFile().exists()) {
            throw new IllegalArgumentException("Dockerfile not found: " + dockerfilePath);
        }

        Config config = new Config(path, Config.Type.YAML);

        int minCount = config.getInt("min-count", 0);
        int minPort = config.getInt("min-port", 5000);
        boolean deleteOnStop = config.getBoolean("delete-on-stop", true);
        Set<String> tags = new HashSet<>(config.getList("tags", List.of()));

        List<TemplateStep> template = new ArrayList<>();
        if (config.contains("template") && config.get("template") instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof Map<?,?> map) {
                    try {
                        template.add(TemplateBuilder.createTemplateStep(new ConfigSection((Map<String, Object>) map)));
                    } catch (Exception e) {
                        new Exception("Failed to load template step at index " + i + " of group: " + name, e).printStackTrace(System.err);
                    }
                }
            }
        }

        return new Group(name, minCount, minPort, deleteOnStop, tags, template);
    }

    public void rebuildImageIfNeeded() {
        try {
            Path originalPath = Path.of("groups").resolve(name + ".Dockerfile");
            List<String> lines = Files.readAllLines(originalPath);
            lines.replaceAll(new PlaceholderEngine()::resolve);

            ports = lines.stream()
                    .filter(line -> line.startsWith("EXPOSE "))
                    .flatMap(line -> Stream.of(
                            line.substring(7)
                                    .replaceAll("[\"'\\[\\]\\s]", "")
                                    .split(",")))
                    .filter(Predicate.not(String::isBlank))
                    .map(Integer::parseInt)
                    .collect(Collectors.toUnmodifiableSet());
            volumes = lines.stream()
                    .filter(line -> line.startsWith("VOLUME "))
                    .flatMap(line -> Stream.of(
                            line.substring(7)
                                    .replaceAll("[\"'\\[\\]\\s]", "")
                                    .split(",")))
                    .filter(Predicate.not(String::isBlank))
                    .collect(Collectors.toUnmodifiableSet());

            String content = String.join("\n", lines);
            String hash = Hashing.sha256().hashString(content, StandardCharsets.UTF_8).toString();
            if (dockerfileHash == null || !dockerfileHash.equals(hash)) {
                dockerfileHash = hash;
                System.out.println("Building image for group: " + name);
                Path preparedPath = Path.of("cache").resolve("dockerfiles").resolve(hash);
                Files.createDirectories(preparedPath.getParent());
                Files.writeString(preparedPath, content);
                imageId = VerticallySpinningFish.getDockerClient()
                        .buildImageCmd()
                        .withBaseDirectory(Path.of("").toAbsolutePath().toFile())
                        .withDockerfile(preparedPath.toFile())
                        .start()
                        .awaitImageId();
                VerticallySpinningFish.getDockerClient()
                        .tagImageCmd(imageId, VerticallySpinningFish.getContainerPrefix() + "group/" + name, hash)
                        .withForce()
                        .exec();
            }
        } catch (IOException e) {
            new Exception("Failed to rebuild image of group: " + name, e).printStackTrace(System.err);
        }
    }

    public @NotNull String getName() {
        return name;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMinPort() {
        return minPort;
    }

    public boolean isDeleteOnStop() {
        return deleteOnStop;
    }

    public @NotNull Set<String> getTags() {
        return tags;
    }

    public @NotNull String getDockerfileHash() {
        return dockerfileHash;
    }

    public @NotNull Set<Integer> getPorts() {
        return ports;
    }

    public @NotNull Set<String> getVolumes() {
        return volumes;
    }

    public @Nullable String getImageId() {
        return imageId;
    }

    public @NotNull List<TemplateStep> getTemplate() {
        return template;
    }

    public @Nullable Path getTemplateDir() {
        return templateDir;
    }

    public void setTemplateDir(@NotNull Path templateDir) {
        this.templateDir = templateDir;
    }
}
