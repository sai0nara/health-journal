plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("io.qameta.allure")
}

allure {
    version.set("2.25.0")
    adapter {
        aspectjVersion.set("1.9.22")
        frameworks {
            junit4 {
                enabled.set(true)
            }
        }
    }
}

android {
    namespace = "com.example.healthjournal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.healthjournal"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["listener"] = "io.qameta.allure.kotlin.junit4.AllureJunit4"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

tasks.register<Exec>("pullAllureResults") {
    group = "verification"
    description = "Pulls Allure results from the connected device"
    commandLine("adb", "pull", "/sdcard/Download/allure-results/.", "${project.buildDir}/allure-results")
    doFirst {
        val resultsDir = file("${project.buildDir}/allure-results")
        if (!resultsDir.exists()) resultsDir.mkdirs()
    }
}

dependencies {
    val room_version = "2.6.1"
    val work_version = "2.9.0"

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.tracing:tracing:1.2.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Room
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // Google Sign-In (Credential Manager)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Google Drive REST API
    implementation("com.google.api-client:google-api-client-android:2.6.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20240521-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    // WorkManager for sync
    implementation("androidx.work:work-runtime-ktx:$work_version")

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("androidx.credentials:credentials:1.3.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("io.mockk:mockk-android:1.13.9")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("io.qameta.allure:allure-kotlin-android:2.4.0")
    androidTestImplementation("io.qameta.allure:allure-kotlin-junit4:2.4.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
