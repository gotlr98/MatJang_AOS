import java.io.FileInputStream
import java.util.Properties
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.lang.System.getProperty

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android) version "2.1.0"
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.google.gms.google-services")
    id ("kotlin-parcelize")
}

val properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())



android {
    namespace = "com.example.matjang_aos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.matjang_aos"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["NATIVE_APP_KEY"] = project.properties["NATIVE_APP_KEY"].toString()
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = project.properties["KAKAO_NATIVE_APP_KEY"].toString()
        manifestPlaceholders["KAKAO_REST_API_KEY"] = project.properties["KAKAO_REST_API_KEY"].toString()

        buildConfigField("String", "KAKAO_REST_API_KEY", properties.getProperty("KAKAO_REST_API_KEY"))
    }



    buildFeatures {
        viewBinding = true
        buildConfig = true
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.kakao.sdk:v2-all:2.20.0")
    implementation("com.kakao.maps.open:android:2.12.8")
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.9.0")
}
