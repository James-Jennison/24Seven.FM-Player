import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

providers.environmentVariable("TWENTYFOURSEVEN_ANDROID_BUILD_DIR")
    .orNull
    ?.let { layout.buildDirectory.set(file(it)) }

val uploadStoreFile = providers.environmentVariable("TWENTYFOURSEVEN_UPLOAD_STORE_FILE").orNull
val uploadStorePassword = providers.environmentVariable("TWENTYFOURSEVEN_UPLOAD_STORE_PASSWORD").orNull
val uploadKeyAlias = providers.environmentVariable("TWENTYFOURSEVEN_UPLOAD_KEY_ALIAS").orNull
val uploadKeyPassword = providers.environmentVariable("TWENTYFOURSEVEN_UPLOAD_KEY_PASSWORD").orNull
val uploadSigningValues = listOf(uploadStoreFile, uploadStorePassword, uploadKeyAlias, uploadKeyPassword)
val hasAnyUploadSigningValue = uploadSigningValues.any { !it.isNullOrBlank() }
val hasCompleteUploadSigning = uploadSigningValues.all { !it.isNullOrBlank() }

require(!hasAnyUploadSigningValue || hasCompleteUploadSigning) {
    "Incomplete Play upload signing environment. Supply all four TWENTYFOURSEVEN_UPLOAD_* values or none."
}

android {
    namespace = "com.codeframe78.twentyfourseven.player"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.codeframe78.twentyfourseven.player"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.0-alpha01"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appLabel"] = "24Seven.FM Player"
    }
    buildFeatures { compose = true }
    signingConfigs {
        if (hasCompleteUploadSigning) {
            create("playUpload") {
                storeFile = file(uploadStoreFile!!)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "24Seven.FM Player (Debug)"
        }
        getByName("release") {
            if (hasCompleteUploadSigning) {
                signingConfig = signingConfigs.getByName("playUpload")
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.browser:browser:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.media3:media3-common:1.10.1")
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-session:1.10.1")
    implementation("androidx.mediarouter:mediarouter:1.8.1")
    implementation("org.jsoup:jsoup:1.22.2")
    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20251224")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
