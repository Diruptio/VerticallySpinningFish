tasks {
    register<Exec>("run") {
        group = "Application"
        description = "Run the application in Docker"
        dependsOn(jar)
        commandLine = listOf(
            "docker", "run",
            "--rm",
            "--volume", "${projectDir}/run:/data",
            "--volume", "${jar.get().outputs.files.asPath}:/root/VerticallySpinningFish.jar",
            "--volume", "/var/run/docker.sock:/var/run/docker.sock",
            "--publish", "7000:7000",
            "--workdir", "/data",
            "--interactive",
            "openjdk:25",
            "java", "-jar", "/root/VerticallySpinningFish.jar")
        standardOutput = System.out
        errorOutput = System.err
        standardInput = System.`in`
    }

    register("publishDockerImage") {
        group = "Publishing"
        description = "Build and publish Docker image"
        dependsOn(jar)
        doFirst {
            ProcessBuilder(
                "docker", "build",
                "--tag", "docker-public.diruptio.de/diruptio/vertically-spinning-fish:${version}",
                ".")
                .inheritIO().start().waitFor()
            ProcessBuilder(
                "docker", "tag",
                "docker-public.diruptio.de/diruptio/vertically-spinning-fish:${version}",
                "docker-public.diruptio.de/diruptio/vertically-spinning-fish:latest")
                .inheritIO().start().waitFor()
            val username = (System.getenv("DIRUPTIO_REPO_USERNAME") ?: project.findProperty("docker_username") ?: "").toString()
            val password = (System.getenv("DIRUPTIO_REPO_PASSWORD") ?: project.findProperty("docker_password") ?: "").toString()
            ProcessBuilder(
                "docker", "login",
                "--username", username,
                "--password", password,
                "docker-public.diruptio.de")
                .inheritIO().start().waitFor()
            ProcessBuilder(
                "docker", "push",
                "docker-public.diruptio.de/diruptio/vertically-spinning-fish:${version}")
                .inheritIO().start().waitFor()
            ProcessBuilder(
                "docker", "push",
                "docker-public.diruptio.de/diruptio/vertically-spinning-fish:latest")
                .inheritIO().start().waitFor()
        }
    }
}