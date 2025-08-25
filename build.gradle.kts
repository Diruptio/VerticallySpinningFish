plugins {
    id("java")
}

group = "diruptio"
version = "0.7.1"

repositories {
    mavenCentral()
    maven("https://repo.diruptio.de/repository/maven-private") {
        credentials {
            username = (System.getenv("DIRUPTIO_REPO_USERNAME") ?: project.findProperty("maven_username") ?: "").toString()
            password = (System.getenv("DIRUPTIO_REPO_PASSWORD") ?: project.findProperty("maven_password") ?: "").toString()
        }
    }
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2")
    implementation("diruptio:DiruptioUtil:1.6.28")
    implementation("com.github.docker-java:docker-java:3.6.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("io.javalin:javalin:6.7.0")
    implementation("io.javalin.community.openapi:javalin-openapi-plugin:6.7.0")
    implementation("io.javalin.community.openapi:javalin-swagger-plugin:6.7.0")
    annotationProcessor("io.javalin.community.openapi:openapi-annotation-processor:6.7.0")
    runtimeOnly("javax.activation:activation:1.1.1")
}

val addSubprojectJars = tasks.register<Copy>("addSubprojectJars") {
    val velocityPluginTask = project(":velocity-plugin").tasks.jar.get()
    dependsOn(velocityPluginTask)
    doFirst { delete(layout.buildDirectory.dir("generated/sources/resources").get()) }
    from(velocityPluginTask.outputs)
    into(layout.buildDirectory.dir("generated/sources/resources"))
}

sourceSets.main {
    java.srcDir(rootProject.file("common/src/main/java"))
    resources.srcDir(rootProject.file("common/src/main/resources"))
    resources.srcDir(addSubprojectJars.map { it.outputs })
}

tasks {
    compileJava {
        dependsOn(addSubprojectJars)
        options.encoding = "UTF-8"
        options.release = 21
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
