plugins {
    id("com.android.application") version "8.9.1"
    id("org.jetbrains.kotlin.android") version "1.8.22"
}

android {
    namespace = "com.example.routesmsapp"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.routesmsapp"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation(libs.monitor)
    implementation(libs.ext.junit)
    testImplementation(libs.junit.junit)
}