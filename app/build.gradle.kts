import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
//    alias(libs.plugins.hilt)
    id("kotlin-parcelize")
}

val localPropsFile = rootProject.file("local.properties")
val props = Properties().apply {
    if (localPropsFile.exists()) {
        load(localPropsFile.inputStream())
    }
}

val releaseSigningAvailable = listOf(
    "RELEASE_STORE_FILE",
    "RELEASE_STORE_PASSWORD",
    "RELEASE_KEY_ALIAS",
    "RELEASE_KEY_PASSWORD"
).all { props[it]?.toString()?.isNotBlank() == true }

kotlin {
    compilerOptions {
        optIn.add(
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi"
        )
    }
}

android {
    signingConfigs {
        if (releaseSigningAvailable) {
            create("release") {
                storeFile = file(props["RELEASE_STORE_FILE"] as String)
                storePassword = props["RELEASE_STORE_PASSWORD"] as String
                keyAlias = props["RELEASE_KEY_ALIAS"] as String
                keyPassword = props["RELEASE_KEY_PASSWORD"] as String
            }
        }
    }
    namespace = "me.kavishdevar.librepods"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.andpop.android"
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 33
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        versionCode = 7
        versionName = "1.5.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            externalNativeBuild {
                cmake {
                    arguments += "-DCMAKE_BUILD_TYPE=Release"
                }
            }
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
            versionNameSuffix = "-debug"
        }
    }
    productFlavors {
        create("foss") {
            dimension = "env"
            buildConfigField("Boolean", "PLAY_BUILD", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    androidResources {
        generateLocaleConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    ndkVersion = "29.0.14206865"

    flavorDimensions += "env"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.annotations)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.haze)
    implementation(libs.haze.materials)
    implementation(libs.androidx.dynamicanimation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material.icons.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.aboutlibraries)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.backdrop)
//    implementation(libs.hilt)
//    implementation(libs.hilt.compiler)
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigationevent)
}
