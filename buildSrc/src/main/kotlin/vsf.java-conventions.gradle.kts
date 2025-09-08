plugins {
    id("java")
}

group = "diruptio"
version = "0.7.22"

repositories {
    mavenCentral()
    maven("https://repo.diruptio.de/repository/maven-private") {
        credentials {
            username = (System.getenv("DIRUPTIO_REPO_USERNAME") ?: project.findProperty("maven_username") ?: "").toString()
            password = (System.getenv("DIRUPTIO_REPO_PASSWORD") ?: project.findProperty("maven_password") ?: "").toString()
        }
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }
    
    jar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}