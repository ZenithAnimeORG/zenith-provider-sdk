rootProject.name = "zenith-provider-sdk"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

include(":provider-sdk")
include(":provider-sdk-testkit")
