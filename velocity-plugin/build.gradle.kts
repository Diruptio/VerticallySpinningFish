plugins {
    id("vsf.java-conventions")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public")
}

dependencies {
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
    implementation(project(":api"))
}

val generateSources = tasks.register<Copy>("generateSources") {
    doFirst { delete(layout.buildDirectory.dir("generated/sources/templates").get()) }
    from(file("src/main/templates"))
    into(layout.buildDirectory.dir("generated/sources/templates"))
    expand(mapOf("version" to version))
}
sourceSets.main.get().java.srcDir(generateSources.map { it.outputs })

tasks {
    compileJava {
        dependsOn(generateSources)
    }

    jar {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        archiveFileName = "VSFVelocityPlugin.jar"
    }
}
