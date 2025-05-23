plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android) // Fournit une version de Kotlin
    alias(libs.plugins.kotlin.compose)
    // Supprimez : kotlin("kapt") version "2.1.21" // Conflit et KSP est utilisé
    // Assurez-vous que la version de KSP est compatible avec votre version de Kotlin (fournie par libs.plugins.kotlin.android)
    // Exemple de version KSP compatible avec Kotlin 1.9.20 : "1.9.20-1.0.13"
    // Remplacez "X.Y.Z-A.B.C" par la version correcte de KSP pour votre projet.
    // Si la version de KSP est gérée dans votre build.gradle.kts racine avec apply false,
    // alors ici, vous n'avez besoin que de : id("com.google.devtools.ksp")
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" // Exemple, VÉRIFIEZ ET ADAPTEZ CETTE VERSION !
}

android {
    namespace = "com.example.suivichantierspaysagiste"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.suivichantierspaysagiste"
        minSdk = 23
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
    implementation(libs.androidx.navigation.compose)
    val lifecycleVersion = "2.8.2" // Ou votre version stable actuelle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion") // Essentiel pour collectAsStateWithLifecycle
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
    implementation("androidx.compose.material:material-icons-extended:1.6.6")
    implementation("androidx.datastore:datastore-preferences:1.1.7") // Ou la dernière version stable

    // Room Dependencies
    val room_version = "2.7.1" // La version que vous avez trouvée

    // Room - Essentiel
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version") // Important: ksp pour les projets Kotlin

    // Room - Extensions Kotlin et support des Coroutines (fortement recommandé)
    implementation("androidx.room:room-ktx:$room_version")
}