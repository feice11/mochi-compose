// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// release 签名：优先读环境变量（CI 里来自仓库 Secrets，或工作流临时生成的 keystore），都没有就退回 debug 签名
val signingStore = System.getenv("GOO_STORE_FILE")?.let { file(it) }?.takeIf { it.exists() }

android {
    namespace = "ui.feice.mochi.demo"
    compileSdk = 36
    // Performance comparisons run instrumentation against the actual release app.
    testBuildType = providers.gradleProperty("mochiTestBuildType").orElse("debug").get()

    defaultConfig {
        applicationId = "ui.feice.mochi.demo"
        minSdk = 23
        targetSdk = 35
        versionCode = (System.getenv("GOO_VERSION_CODE") ?: property("EXAMPLE_VERSION_CODE").toString()).toInt()
        versionName = "0.1.$versionCode"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testProguardFiles("proguard-test-rules.pro")
    }

    signingConfigs {
        if (signingStore != null) {
            create("release") {
                storeFile = signingStore
                storePassword = System.getenv("GOO_STORE_PASSWORD")
                keyAlias = System.getenv("GOO_KEY_ALIAS")
                keyPassword = System.getenv("GOO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Measure and ship the optimized release; retain an explicit diagnostic build option.
            isMinifyEnabled = providers.gradleProperty("mochiMinify").orElse("true").get().toBoolean()
            // Keep dynamically named calibration assets; code optimization is independent.
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (testBuildType == "release") {
                // Only shared entry points reached by the opt-in glass test runner.
                proguardFiles("proguard-benchmark-target.pro")
            }
            signingConfig = if (signingStore != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        // 新 API 都做了 SDK_INT 判断；别让 lint 的告警卡住 release 包
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(project(":mochi-compose"))
    implementation(project(":mochi-nothing"))
    implementation(project(":mochi-ios"))
    implementation(project(":mochi-icons"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    // 只有示例用 material3（Slider / Snackbar），组件库本身不依赖它
    implementation(libs.androidx.compose.material3)

    // 转场截图测试（androidTest，只在本地跑）
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // AndroidX Test references these annotations; optimized instrumentation must resolve them.
    androidTestImplementation("com.google.errorprone:error_prone_annotations:2.38.0")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
