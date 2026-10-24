import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)

    // En el MVP solo Android; Desktop e iOS se agregan después sin mover la UI de commonMain.
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:data-memory"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.navigation.compose)
        }
        androidMain.dependencies {
            implementation(libs.litertlm.android)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.lifecycle.process)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.tesseract4android)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.pdfbox.android)
            implementation(libs.androidx.work.runtime)
            implementation(project(":core:data-local"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.work.testing)
            }
        }
    }
}

android {
    namespace = "com.coinden.app"
    compileSdk = 36
    ndkVersion = "30.0.15729638"
    defaultConfig {
        applicationId = "com.coinden.app"
        minSdk = 26
        targetSdk = 36
        // En el CI salen de la etiqueta vX.Y.Z (-PversionName=X.Y.Z -PversionCode=N).
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = findProperty("versionName") as String? ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Whisper (código nativo) solo para teléfonos de 64 bits y el emulador.
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }
    externalNativeBuild {
        cmake {
            path = file("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // La llave de subida nunca entra al repositorio: se lee de keystore.properties (ignorado por git)
    // o de variables de entorno (secretos del CI). Sin llave, la release se firma con la de depuración
    // para probarla en el teléfono; ese archivo no sirve para subir a Play.
    val keystore =
        Properties().apply {
            val file = rootProject.file("keystore.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }

    fun secret(
        key: String,
        env: String,
    ): String? = keystore.getProperty(key) ?: System.getenv(env)

    val storePath = secret("storeFile", "COINDEN_KEYSTORE_FILE")
    signingConfigs {
        if (storePath != null) {
            create("upload") {
                storeFile = rootProject.file(storePath)
                storePassword = secret("storePassword", "COINDEN_KEYSTORE_PASSWORD")
                keyAlias = secret("keyAlias", "COINDEN_KEY_ALIAS")
                keyPassword = secret("keyPassword", "COINDEN_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
        }
    }
}
