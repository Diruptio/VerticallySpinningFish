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
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }

    jar {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        archiveFileName = "HorizontalSpinningFish.jar"
        manifest {
            attributes["Main-Class"] = "diruptio.horizontallyspinningfish.HorizontallySpinningFish"
            attributes["Implementation-Title"] = "Horizontally spinning fish"
            attributes["Implementation-Version"] = version
            attributes["Implementation-Vendor"] = "Diruptio"
        }
    }
}
