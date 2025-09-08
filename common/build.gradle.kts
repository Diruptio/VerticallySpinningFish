plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2-1")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }
}