import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// ── Keystore loading ────────────────────────────────────────────────
// Local: reads `keystore.properties` (gitignored).
// CI:    reads env vars set by GitHub Actions secrets.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun keystoreVal(key: String): String? =
    System.getenv(key) ?: keystoreProps.getProperty(key)

android {
    namespace = "com.aetherbound.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aetherbound.game"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("AETHER_VERSION_CODE")?.toIntOrNull()) ?: 1
        versionName = System.getenv("AETHER_VERSION_NAME") ?: "0.1.0"
    }

    signingConfigs {
        create("release") {
            val storePath = keystoreVal("AETHER_KEYSTORE_PATH")
            val storePass = keystoreVal("AETHER_KEYSTORE_PASS")
            val alias = keystoreVal("AETHER_KEY_ALIAS")
            val keyPass = keystoreVal("AETHER_KEY_PASS")
            if (storePath != null && storePass != null && alias != null && keyPass != null) {
                storeFile = file(storePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Only attach the signingConfig when we actually have credentials,
            // otherwise local devs without the keystore can still build debug.
            val cfg = signingConfigs.getByName("release")
            if (cfg.storeFile != null) signingConfig = cfg
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
            )
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
}
