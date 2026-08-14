import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val secureSigningScript = providers.gradleProperty("secure.signing")
    .orNull
    ?.let(rootProject::file)
val hasReleaseSigning = secureSigningScript?.isFile == true
val baseVersionCode = 2
val baseVersionName = "1.0.1"

fun currentGitBranch(): String {
    return runCatching {
        val process = ProcessBuilder("git", "branch", "--show-current")
            .directory(rootProject.projectDir)
            .redirectErrorStream(true)
            .start()
        val branch = process.inputStream.bufferedReader().use { it.readText().trim() }
        if (process.waitFor() == 0) branch else ""
    }.getOrDefault("")
}

val buildBranch = providers.gradleProperty("branch.name").orNull
    ?: System.getenv("GIT_BRANCH")?.substringAfterLast('/')
    ?: currentGitBranch()
val sanitizedBranch = buildBranch
    .ifBlank { "detached" }
    .replace(Regex("[^A-Za-z0-9._-]+"), "-")
    .trim('-')
    .lowercase(Locale.ROOT)
    .take(32)
    .ifBlank { "detached" }
val appVersionName = if (buildBranch == "main") {
    baseVersionName
} else {
    "$baseVersionName-$sanitizedBranch"
}

base {
    archivesName.set("$appVersionName[$baseVersionCode]AtlasTerminal")
}

android {
    namespace = "com.mmwtl.atlasterminal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mmwtl.atlasterminal"
        minSdk = 26
        targetSdk = 36
        versionCode = baseVersionCode
        versionName = appVersionName
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    if (hasReleaseSigning) {
        apply(secureSigningScript!!)
    }
}

if (!hasReleaseSigning) {
    tasks.configureEach {
        if (name == "packageRelease" || name == "bundleRelease") {
            doFirst {
                throw GradleException(
                    "Release signing is required. Configure secure.signing or build the debug variant."
                )
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.adb.shell)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
