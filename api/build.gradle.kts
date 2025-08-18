plugins {
    id("java")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2")
    implementation(project(":common"))
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.google.code.gson:gson:2.13.1")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }

    jar {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

publishing {
    repositories {
        maven("https://repo.diruptio.de/repository/maven-private-releases") {
            name = "DiruptioPrivate"
            credentials {
                username = (System.getenv("DIRUPTIO_REPO_USERNAME") ?: project.findProperty("maven_username") ?: "").toString()
                password = (System.getenv("DIRUPTIO_REPO_PASSWORD") ?: project.findProperty("maven_password") ?: "").toString()
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            artifactId = "VerticallySpinningFish"
            from(components["java"])
        }
    }
}
