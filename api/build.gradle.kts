plugins {
    id("vsf.java-conventions")
    alias(libs.plugins.maven.publish)
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(project(":common"))
    implementation(libs.okhttp)
    implementation(libs.gson)
}

tasks {
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
