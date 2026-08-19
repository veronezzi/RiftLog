import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}

// Riot API key lives in the developer's local, gitignored local.properties (same pattern as a
// Google Maps API key) - never committed, never entered by the end user at runtime.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}
val riotApiKey: String = localProperties.getProperty("RIOT_API_KEY", "")

// Release signing, same local.properties pattern as RIOT_API_KEY above - never hardcoded,
// never committed. The keystore file itself must also stay out of git (see .gitignore).
// Missing on a fresh checkout is expected (debug builds don't need it) - only assembleRelease
// / bundleRelease should fail, with a clear message instead of a cryptic signing error.
val releaseSigningKeys = listOf(
    "RELEASE_STORE_FILE", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD"
)
val hasReleaseSigning = releaseSigningKeys.all { !localProperties.getProperty(it).isNullOrBlank() }

android {
    namespace = "com.veronezzi.riftlog"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.veronezzi.riftlog"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "RIOT_API_KEY", "\"$riotApiKey\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(localProperties.getProperty("RELEASE_STORE_FILE"))
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

if (!hasReleaseSigning) {
    tasks.matching { it.name.startsWith("assembleRelease") || it.name.startsWith("bundleRelease") }
        .configureEach {
            doFirst {
                error(
                    "Release build needs RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, " +
                        "RELEASE_KEY_ALIAS and RELEASE_KEY_PASSWORD set in local.properties " +
                        "(generate a keystore with keytool -genkeypair -v -keystore " +
                        "release-keystore.jks -alias riftlog -keyalg RSA -keysize 2048 " +
                        "-validity 10000)."
                )
            }
        }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.rift.tracker.design.system)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)

    // No Room: its KSP compiler is currently incompatible with AGP 9's built-in Kotlin
    // (KSP hard-refuses to run under it), and the classic Kotlin Gradle plugin can't be
    // applied under AGP 9 either (throws on apply). The player/static-data cache is a
    // plain android.database.sqlite.SQLiteOpenHelper instead - same DAO shapes, zero
    // extra dependency, no annotation processor needed.

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.coil)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
