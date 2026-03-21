pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "arisucast"

include(":app")

// Core modules
include(":core:core-common")
include(":core:core-network")
include(":core:core-database")
include(":core:core-datastore")
include(":core:core-media")
include(":core:core-download")
include(":core:core-ui")

// Feature modules
include(":feature:feature-home")
include(":feature:feature-player")
include(":feature:feature-subscriptions")
include(":feature:feature-episodes")
include(":feature:feature-search")
include(":feature:feature-settings")
