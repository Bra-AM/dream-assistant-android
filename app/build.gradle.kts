plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.dreamassistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.dreamassistant"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "GOOGLE_AI_API_KEY", "\"${project.findProperty("GOOGLE_AI_API_KEY") ?: ""}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // CRITICAL: Enable NDK for llama.cpp
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        // External native build for llama.cpp
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-frtti", "-fexceptions")
                arguments += listOf("-DANDROID_STL=c++_shared", "-DLLAMA_ANDROID=ON")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            isShrinkResources = false
        }

        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
        dataBinding = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    // CRITICAL: Handle large GGUF model files
    androidResources {
        noCompress += listOf("gguf", "bin", "tflite", "json", "model")
    }

    // Handle native libraries and model files
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"

            // Handle potential conflicts with ML libraries
            pickFirsts += "**/libc++_shared.so"
            pickFirsts += "**/libjsc.so"
            pickFirsts += "**/libllama.so"
            pickFirsts += "**/libllama-android.so"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    // Configure CMake for llama.cpp
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // FIXED: Replace deprecated dexOptions
    // Gradle handles dex optimization automatically now
}

dependencies {
    // ===== CORE ANDROID =====
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ===== JETPACK COMPOSE =====
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    // ===== VIEWMODEL & LIFECYCLE =====
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")

    // ===== COROUTINES =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // ===== REMOVED: Non-existent llama library =====
    // REMOVED: implementation("com.github.kherud:llama:2.2.0")
    // We'll use our own JNI implementation instead

    // JSON processing for model configuration and tokenizers
    implementation("com.google.code.gson:gson:2.10.1")

    // File operations for custom model loading
    implementation("commons-io:commons-io:2.15.1")

    // ===== AUDIO & SPEECH (For sister's voice input) =====
    implementation("androidx.media:media:1.7.0")

    // ===== PERMISSIONS =====
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // ===== NAVIGATION =====
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // ===== BACKUP: Google AI (Keep for potential fallback) =====
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")

    // ===== PERFORMANCE MONITORING =====
    implementation("androidx.tracing:tracing:1.2.0")

    // ===== TESTING =====
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito:mockito-core:5.7.0")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation("androidx.compose.ui:ui-tooling")
}