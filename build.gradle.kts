plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    id("maven-publish")
}

allprojects {
    group = "com.pilldev.zenith"
    version = "2.0.0"
}
