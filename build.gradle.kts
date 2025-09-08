plugins {
    id("vsf.java-conventions")
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(project(":common"))
    implementation(libs.diruptio.util)
    implementation(libs.docker.java)
    implementation(libs.okhttp)
    implementation(libs.slf4j.simple)
    implementation(libs.bundles.javalin)
    annotationProcessor(libs.javalin.annotation.processor)
    runtimeOnly(libs.activation)
}

val addSubprojectJars = tasks.register<Copy>("addSubprojectJars") {
    val velocityPluginTask = project(":velocity-plugin").tasks.jar.get()
    dependsOn(velocityPluginTask)
    doFirst { delete(layout.buildDirectory.dir("generated/sources/resources").get()) }
    from(velocityPluginTask.outputs)
    into(layout.buildDirectory.dir("generated/sources/resources"))
}

sourceSets.main {
    resources.srcDir(addSubprojectJars.map { it.outputs })
}

tasks {
    compileJava {
        dependsOn(addSubprojectJars)
    }

    jar {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
            exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        manifest {
            attributes["Main-Class"] = "diruptio.verticallyspinningfish.VerticallySpinningFish"
            attributes["Implementation-Title"] = "Vertically Spinning Fish"
            attributes["Implementation-Version"] = version
            attributes["Implementation-Vendor"] = "Diruptio"
        }
        archiveFileName = "VerticallySpinningFish.jar"
    }

    register<Exec>("run") {
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
        dependsOn(jar)
        group = "Publishing"
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

subprojects {
    group = rootProject.group
    version = rootProject.version
}
