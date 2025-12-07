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

rootProject.name = "my-cycle"
include(":app")
include(":core:designsystem")
include(":core:model")
include(":core:data")
include(":feature:calendar")
include(":feature:log")
include(":feature:insights")
include(":feature:reminders")
include(":feature:settings")
