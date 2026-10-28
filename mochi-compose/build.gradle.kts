// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "ui.feice.mochi"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        targetSdk = 35
        // 截图测试（Robolectric + Roborazzi）要读库里的资源
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.foundation)
    // 形状：iOS 式连续曲率圆角 https://github.com/Kyant0/Capsule
    api(libs.kyant.capsule)
    // 玻璃材质抓背后的内容 https://github.com/Kyant0/AndroidLiquidGlass
    implementation(libs.kyant.backdrop)
    // 公开 API 里用到 ImmutableList
    api(libs.kotlinx.collections.immutable)
    // Monet：从种子色生成 MD3 tonal 配色
    implementation(libs.materialkolor.utilities)
    implementation(libs.androidx.activity.compose)

    // 渲染基准（androidTest，只在本地跑，不随库发布）
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.junit)
    // 截图回归：JVM 上渲染组件和基准图逐像素比，不占模拟器。基准图在 src/test/screenshots，
    // 改了外观先 ./gradlew :mochi-compose:recordRoborazziDebug 重录，再看 git diff 里的图确认
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}
