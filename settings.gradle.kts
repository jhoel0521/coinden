pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "coinden"

include(":core:domain")
include(":core:data-memory")
include(":core:data-local")
include(":composeApp")
