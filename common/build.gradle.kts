plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.1.0")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
    }
}
