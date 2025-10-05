rootProject.name = "VerticallySpinningFish"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public")
    }
}

include("common")
include("api")
include("paper-plugin")
include("velocity-plugin")
