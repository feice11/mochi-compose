// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}

// 格式检查：ktlint + Compose 规则，所有源码文件带 Apache-2.0 许可头（和 miuix 一致）
spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    val composeRules = "io.nlopez.compose.rules:ktlint:${libs.versions.composeRules.get()}"
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "spotless/**")
        ktlint(ktlintVersion).customRuleSets(listOf(composeRules))
        licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**", "spotless/**")
        ktlint(ktlintVersion)
        // 许可头后面正文的第一行
        licenseHeaderFile(rootProject.file("spotless/copyright.kts"), "^(plugins|import|pluginManagement|rootProject|@file)")
    }
}

// 基线配置文件（baseline-prof.txt）：系统按它预先编译库和演示代码，刚装好时第一次出现的转场、模糊不必解释执行。
// 打包工具不展开按包的通配，只认逐个类的规则，所以按编译产物生成。新增类以后跑一次：./gradlew updateBaselineProfiles
tasks.register("updateBaselineProfiles") {
    group = "build"
    description = "按 release 编译产物重新生成 mochi-compose 和 example 的 baseline-prof.txt"
    dependsOn(":mochi-compose:compileReleaseKotlin", ":example:compileReleaseKotlin")
    val targets = listOf(
        Triple(file("mochi-compose/build/tmp/kotlin-classes/release"), file("mochi-compose/src/main/baseline-prof.txt"), "ui/feice/mochi/"),
        Triple(file("example/build/tmp/kotlin-classes/release"), file("example/src/main/baseline-prof.txt"), "ui/feice/mochi/demo/"),
    )
    doLast {
        for ((classes, out, prefix) in targets) {
            val names = classes.walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .map { it.relativeTo(classes).invariantSeparatorsPath.removeSuffix(".class") }
                .filter { it.startsWith(prefix) }
                .sorted()
                .toList()
            out.writeText(names.joinToString("") { "HSPL$it;->**(**)**\nL$it;\n" })
            println("${out.path}: ${names.size} classes")
        }
    }
}
