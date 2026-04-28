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
    }
}

// Ensure this name matches exactly the folder name "Padel-Tracker"
rootProject.name = "Padel-Tracker"

include(":app")
include(":shared")
include(":wear") // If you have a wear module as seen in your previous screenshots