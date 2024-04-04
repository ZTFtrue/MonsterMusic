pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(
            // for Tarsos
            url = "https://mvn.0110.be/releases"
        )
        maven(
            url = "https://jitpack.io"
        )
    }
}

rootProject.name = "MusicPitch"
include(":app")
 