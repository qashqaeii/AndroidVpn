import java.net.URL
import java.io.FileOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// دانلود خودکار libv2ray.aar از ریلیز ازپیش‌ساخته (در صورت نبودن در app/libs)
val libsDir = file("libs")
val aarDest = file("libs/libv2ray.aar")
tasks.register("downloadLibv2rayAar") {
    onlyIf { !aarDest.exists() }
    doLast {
        libsDir.mkdirs()
        val urls = listOf(
            "https://github.com/Mronezc/V2rayNG_Android.aar/releases/download/v1.8.1/libv2ray.aar",
            "https://github.com/Mronezc/V2rayNG_Android.aar/releases/download/v1.8.4/libv2ray.aar",
        )
        for (urlStr in urls) {
            try {
                URL(urlStr).openStream().use { input ->
                    FileOutputStream(aarDest).use { output -> input.copyTo(output) }
                }
                println("downloadLibv2rayAar: دانلود از $urlStr انجام شد.")
                return@doLast
            } catch (e: Exception) {
                println("downloadLibv2rayAar: $urlStr -> ${e.message}")
            }
        }
        println("downloadLibv2rayAar: دانلود انجام نشد. libv2ray.aar را دستی در app/libs قرار دهید. راهنما: VPN_INTEGRATION.md")
    }
}
tasks.named("preBuild").configure { dependsOn("downloadLibv2rayAar") }

android {
    namespace = "com.vpn.client"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vpn.client"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // libv2ray.aar از AndroidLibV2rayLite: در app/libs قرار دهید تا هستهٔ V2Ray فعال شود
    implementation(fileTree("libs") { include("*.aar") })

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Moshi
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    // Security: AES decryption (Android has javax.crypto)
    // Certificate pinning via OkHttp
}
