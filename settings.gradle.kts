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

include(":app:app")
include(":app:ui")

include(":core:base")
include(":core:crashreporter")
include(":core:compat")
include(":core:preferences")
include(":core:ktx")
include(":core:i18n")
include(":data:database")
include(":core:permissions")
include(":core:shared")

include(":data:appshortcuts")
include(":data:customattrs")
include(":data:applications")
include(":data:calendar")
include(":data:themes")
include(":data:currencies")
include(":data:unitconverter")
include(":data:widgets")
include(":data:weather")
include(":data:notifications")
include(":data:searchable")
include(":data:plugins")

include(":services:tags")
include(":services:search")
include(":services:badges")
include(":services:icons")
include(":services:backup")
include(":services:music")

include(":libs:material-color-utilities")
include(":libs:address-formatter")
include(":services:global-actions")
include(":services:widgets")
include(":services:favorites")
include(":services:focus")

include(":plugins:sdk")
include(":services:plugins")
include(":core:devicepose")
include(":core:profiles")
include(":data:i18n")
