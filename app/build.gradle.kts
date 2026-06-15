import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val formattedDate = SimpleDateFormat("ddMMyyyy", Locale.US).format(Date())

android {
    namespace = "com.livescore.football.livescores.footballscores"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.livescore.football.livescores.footballscores"
        minSdk = 24
        targetSdk = 35
        versionCode = 8
        versionName = "0.0.8"

    }

//    signingConfigs {
//        create("release") {
//            storeFile = rootProject.file("keystore/apfolife.jks")
//            storePassword = "apfolife"
//            keyAlias = "apfolife"
//            keyPassword = "apfolife"
//        }
//    }

//    buildTypes {
//        getByName("release") {
//            signingConfig = signingConfigs.getByName("release")
//            isMinifyEnabled = true
//            isShrinkResources = true
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
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
        viewBinding = true
        buildConfig = true
        dataBinding = true

    }


    applicationVariants.all {
        val variant = this
        val type = variant.buildType.name

        // APK
        variant.outputs.all {
            val output = this as? com.android.build.gradle.api.ApkVariantOutput
            output?.outputFileName =
                "D57_LiveScore_v${variant.versionName}_c${variant.versionCode}_${formattedDate}-${type}.apk"
        }

        // AAB
        applicationVariants.all {
            val variant = this
            val type = variant.buildType.name

            // APK
            variant.outputs.all {
                val output = this as? com.android.build.gradle.api.ApkVariantOutput
                output?.outputFileName =
                    "D57_LiveScore_v${variant.versionName}_c${variant.versionCode}_${formattedDate}-${type}.apk"
            }
        }
    }
    tasks.whenTaskAdded {
        if (name == "bundleRelease") {
            doLast {
                val bundleDir = File(project.buildDir, "outputs/bundle/release")
                bundleDir.listFiles { f -> f.extension == "aab" }?.forEach { aab ->
                    val newName =
                        "D57_LiveScore_v${android.defaultConfig.versionName}_c${android.defaultConfig.versionCode}_${formattedDate}-release.aab"
                    aab.renameTo(File(aab.parent, newName))
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.firebase.config)
    implementation(libs.material)
    implementation(libs.ads.mallegan.lib.nvc)
    implementation(libs.adjust.android)
    // Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // Lifecycle & Coroutines
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.shimmer)
    // Glide
    implementation(libs.glide)
    kapt(libs.glide.compiler)
    implementation(libs.lottie)

    // AdMob
    implementation(libs.play.services.ads)

    //noinspection UseTomlInstead
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation(libs.billing.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.gms.play.services.ads)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    implementation(libs.bubbletabbar)

    //fb sdk
    implementation(libs.facebook.android.sdk)
    implementation(libs.facebook)
    implementation("com.google.ads.mediation:applovin:13.6.2.0")
    implementation("com.google.ads.mediation:inmobi:11.3.0.0")
    implementation("com.google.ads.mediation:pangle:8.0.0.5.0")
    implementation("com.google.ads.mediation:mintegral:17.1.61.0")
    implementation("com.unity3d.ads:unity-ads:4.18.0")
    implementation("com.google.ads.mediation:unity:4.18.0.0")
    implementation("com.google.ads.mediation:vungle:7.7.4.0")

    // Fix R8 Missing Class Nullsafe/Nullsafe$Mode
    compileOnly(libs.infer.annotation)


    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}