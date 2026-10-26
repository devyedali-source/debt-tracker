import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePath: String? = System.getenv("SIGNING_KEYSTORE_PATH")

// AdMob IDs. Google's TEST IDs are used until the real ones from the AdMob account are put here.
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testBanner = "ca-app-pub-3940256099942544/9214589741"
val testInterstitial = "ca-app-pub-3940256099942544/1033173712"
val realAdmobAppId = "ca-app-pub-4858639248453099~5742799655"
val realBanner = "ca-app-pub-4858639248453099/2376727720"
val realInterstitial = "ca-app-pub-4858639248453099/1895266010"

android {
    namespace = "com.elyedali.debttracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.elyedali.debttracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        manifestPlaceholders["admobAppId"] = realAdmobAppId
        buildConfigField("String", "AD_UNIT_BANNER", "\"$realBanner\"")
        buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"$realInterstitial\"")
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Test builds always show test ads (never click real ads on your own phone).
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            buildConfigField("String", "AD_UNIT_BANNER", "\"$testBanner\"")
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"$testInterstitial\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.android.billingclient:billing:8.0.0")
    implementation("com.google.android.gms:play-services-ads:24.3.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
}
