import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt.android)
}

// Release signing credentials live in a gitignored keystore.properties (see keystore.properties.example)
// so the keystore path and passwords never land in source control.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}
val hasReleaseSigning = keystorePropertiesFile.exists()

android {
    namespace = "com.rainyday.saveableapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.rainyday.saveableapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
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
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    flavorDimensions += "environment"
    productFlavors {
        // Points at the "Saveable App - Stage" Firebase project (saveableapp-5b75b).
        // Config file: app/src/stage/google-services.json
        // Suffixed package name (com.rainyday.saveableapp.stage) so it can be registered as its own
        // Android OAuth client — the base package + debug SHA-1 combo is claimed by another project.
        create("stage") {
            dimension = "environment"
            applicationIdSuffix = ".stage"
            buildConfigField(
                "String",
                "AI_PARSE_ENDPOINT",
                "\"https://us-central1-saveableapp-5b75b.cloudfunctions.net/aiParse\""
            )
            buildConfigField(
                "String",
                "AI_ENRICH_ENDPOINT",
                "\"https://us-central1-saveableapp-5b75b.cloudfunctions.net/enrichItem\""
            )
        }
        create("prod") {
            dimension = "environment"
            buildConfigField(
                "String",
                "AI_PARSE_ENDPOINT",
                "\"https://us-central1-saveable-app-prod.cloudfunctions.net/aiParse\""
            )
            buildConfigField(
                "String",
                "AI_ENRICH_ENDPOINT",
                "\"https://us-central1-saveable-app-prod.cloudfunctions.net/enrichItem\""
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES"
            )
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.reorderable)
    implementation(libs.play.services.auth)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.google.api.client.android) {
        exclude(group = "org.apache.httpcomponents")
        // google-http-client's optional OpenCensus tracing pulls io.grpc:grpc-context at a newer
        // version than Firestore's own (pinned, mutually-compatible) grpc-* stack, which bumps
        // grpc-api/grpc-context app-wide and produces a binary-incompatible mix at runtime
        // (NoClassDefFoundError: Lio/grpc/InternalGlobalInterceptors). Neither is needed for plain
        // REST calls to the Drive API, so drop them rather than fight the version conflict.
        exclude(group = "io.grpc")
        exclude(group = "io.opencensus")
    }
    implementation(libs.google.api.services.drive) {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "io.grpc")
        exclude(group = "io.opencensus")
    }
    implementation(libs.google.http.client.gson) {
        exclude(group = "io.grpc")
        exclude(group = "io.opencensus")
    }
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}