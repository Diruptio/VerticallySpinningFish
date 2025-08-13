plugins {
    id("java")
}

group = "diruptio"
version = "0.0.1"

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
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }

    jar {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        archiveFileName = "VerticallySpinningFish.jar"
        manifest {
            attributes["Main-Class"] = "diruptio.verticallyspinningfish.VerticallySpinningFish"
            attributes["Implementation-Title"] = "Vertically spinning fish"
            attributes["Implementation-Version"] = version
            attributes["Implementation-Vendor"] = "Diruptio"
        }
    }
}
