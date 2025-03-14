plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.logoapplicationyolo"
    compileSdk = 35
    packaging {
        resources {
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
            excludes += "/META-INF/NOTICE.md"
        }
    }
    defaultConfig {
        applicationId = "com.example.logoapplicationyolo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
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
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    //implementation(libs.firebase.ml.vision)
    implementation(libs.play.services.mlkit.text.recognition.common)
    implementation(libs.image.labeling.common)
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(libs.androidx.databinding.compiler.common)
    implementation(libs.litert.support.api)
    implementation(libs.litert)
    implementation(libs.androidx.cardview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.play.services.vision)
    implementation(libs.play.services.location) // Location API
    implementation(libs.okhttp)
    //implementation(libs.play.services.vision)
    //implementation(libs.vision.text.recognition)
    //implementation(libs.play.services.location.v2101)
    implementation(libs.image.labeling)  // ML Kit for Logo Detection

    implementation(libs.material.v161)
    implementation(libs.androidx.appcompat.v141)
    implementation(libs.androidx.core.ktx.v170)

    // CameraX for capturing images
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Google Location Services API
    implementation(libs.play.services.location)

    // Google Cloud Vision API (HTTP Requests)
    implementation(libs.gson)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.recyclerview)
    implementation(libs.onnxruntime.android.v1150)



}