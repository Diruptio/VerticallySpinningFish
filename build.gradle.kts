plugins {
    id("java")
}

group = "diruptio"
version = "0.2.0"

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
    implementation("com.github.docker-java:docker-java:3.5.3")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    runtimeOnly("javax.activation:activation:1.1.1")
}

tasks {
    compileJava {
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
        dependsOn("jar")
        commandLine = listOf(
            "docker", "run",
            "--rm",
            "--volume", "${projectDir}/run:/data",
            "--volume", "${jar.get().outputs.files.asPath}:/root/VerticallySpinningFish.jar",
            "--volume", "/var/run/docker.sock:/var/run/docker.sock",
            "--workdir", "/data",
            "--interactive",
            "openjdk:25",
            "java", "-jar", "/root/VerticallySpinningFish.jar")
        standardOutput = System.out
        errorOutput = System.err
        standardInput = System.`in`
    }
}
