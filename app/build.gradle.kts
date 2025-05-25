plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Assurez-vous que la version de KSP est compatible avec votre version de Kotlin
    // id("com.google.devtools.ksp") version "1.9.20-1.0.13" // Exemple, si Kotlin est 1.9.20
    // Votre version actuelle :
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" // Gardez cette version si elle fonctionne pour vous
}

android {
    namespace = "com.example.suivichantierspaysagiste"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.suivichantierspaysagiste"
        minSdk = 23
        targetSdk = 35
        versionCode = 1 // Vous pourriez incrémenter ceci si vous faites des releases
        versionName = "1.0" // Idem

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
    implementation(libs.androidx.navigation.compose)
    val lifecycleVersion = "2.8.2" // Ou votre version stable actuelle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.compose.material:material-icons-extended:1.6.6") // Votre version actuelle
    implementation("androidx.datastore:datastore-preferences:1.1.7") // Votre version actuelle

    // Room Dependencies
    val room_version = "2.7.1" // Votre version actuelle
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // NOUVELLES DÉPENDANCES POUR GOOGLE MAPS ET LOCALISATION
    implementation(libs.google.maps.sdk) // Ajouté via libs.versions.toml
    implementation(libs.google.maps.compose) // Ajouté via libs.versions.toml
    implementation(libs.google.location.services) // Ajouté via libs.versions.toml
}