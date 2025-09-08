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
    implementation(libs.commons.io)
    implementation(libs.guava)
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
}

// Apply Docker-related tasks
apply(from = "gradle/docker.gradle.kts")

subprojects {
    group = rootProject.group
    version = rootProject.version
}
