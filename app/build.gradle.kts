plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.secrets.gradle.plugin) // Añade este plugin
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "net.iessochoa.pablolopez.mercaelx"
    compileSdk = 34 // Actualizado a la última versión estable

    defaultConfig {
        applicationId = "net.iessochoa.pablolopez.mercaelx"
        minSdk = 24
        targetSdk = 34 // Actualizado para coincidir con compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Añade esta configuración para Maps
        manifestPlaceholders["MAPS_API_KEY"] = "AIzaSyAcFOzrna9ap_vjfyP_g4JYxxcluz-5TFs"
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

    // Añade esto para evitar errores de duplicación
    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")


    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation(libs.firebase.firestore)
    implementation("com.google.firebase:firebase-analytics")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")


    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

}