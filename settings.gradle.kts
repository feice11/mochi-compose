// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "mochi"
include(":mochi-compose", ":example")
include(":mochi-nothing")
include(":mochi-ios")
include(":mochi-icons")
// 键盘和独立输入法还没公开：目录只在本地检出里有（.gitignore 排除），有才加入，公开仓库和 CI 不编译
listOf("mochi-keyboard", "mochi-ime").filter { file("$it/build.gradle.kts").exists() }.forEach { include(":$it") }
// 宣传片镜头渲染（只在本地用，-Ppromo 时才加入，CI 不编译）
if (providers.gradleProperty("promo").isPresent) {
    include(":promo-shots")
    project(":promo-shots").projectDir = file("promo/shots")
}
