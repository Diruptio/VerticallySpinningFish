plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-SNAPSHOT"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public")
    maven("https://repo.diruptio.de/repository/maven-private") {
        credentials {
            username = (System.getenv("DIRUPTIO_REPO_USERNAME") ?: project.findProperty("maven_username") ?: "").toString()
            password = (System.getenv("DIRUPTIO_REPO_PASSWORD") ?: project.findProperty("maven_password") ?: "").toString()
        }
    }
}

dependencies {
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
    implementation(project(":api"))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }

    processResources {
        filesMatching("paper-plugin.yml") {
            expand(mapOf("version" to version))
        }
    }

    jar {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        archiveFileName = "VSFPaperPlugin-unobf.jar"
    }

    reobfJar {
        outputJar = file("build/libs/VSFPaperPlugin.jar")
    }

    assemble {
        dependsOn(reobfJar)
    }
}
