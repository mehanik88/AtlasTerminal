plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

tasks.register("verify") {
    group = "verification"
    description = "Runs JVM unit tests and Android lint."
    dependsOn(":app:testDebugUnitTest", ":app:lintDebug")
}
