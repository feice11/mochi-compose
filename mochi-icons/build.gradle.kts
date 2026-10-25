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
    namespace = "ui.feice.mochi.icons"
    compileSdk = 36
    defaultConfig { minSdk = 23 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    explicitApi()
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    api(project(":mochi-compose"))
    implementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.junit)
}

// 和 mochi-compose 一起由 JitPack 的 publishToMavenLocal 发布
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "mochi-icons"
            }
        }
    }
}
