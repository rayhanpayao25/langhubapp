pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        jcenter()  // You can remove this if jcenter is deprecated
        maven { url = uri("https://jitpack.io") }  // Correct Kotlin DSL syntax for Maven repository
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Correct Kotlin DSL syntax for Maven repository
    }
}

rootProject.name = "langhub"
include(":app")
