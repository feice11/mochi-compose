// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

group = "ui.feice.mochi"
version = (findProperty("VERSION_NAME") as String?) ?: "0.1.0"

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
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
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
}

// JitPack 跑 publishToMavenLocal，这里声明发布内容
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "mochi-compose"
            }
        }
    }
}
