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
        // JitPack solo para Tesseract4Android (Apache 2.0): ningún otro grupo se resuelve ahí.
        exclusiveContent {
            forRepository { maven("https://jitpack.io") }
            filter { includeGroup("cz.adaptech.tesseract4android") }
        }
    }
}

rootProject.name = "coinden"

include(":core:domain")
include(":core:data-memory")
include(":core:data-local")
include(":composeApp")
