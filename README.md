<div align="center">

<img src="docs/assets/mochi-cover.svg" alt="Mochi 麻薯：软软弹弹、跟手顺滑的 Compose 组件库" width="100%">

# Mochi Compose · 麻薯

**软、弹、跟手的 Jetpack Compose 组件库**<br>
MD3 Expressive 的配色和字阶 · iOS 26 的交互和动态 · G2 连续圆角

[![JitPack](https://jitpack.io/v/feice11/mochi-compose.svg)](https://jitpack.io/#feice11/mochi-compose)
[![Build](https://github.com/feice11/mochi-compose/actions/workflows/build.yml/badge.svg)](https://github.com/feice11/mochi-compose/actions/workflows/build.yml)
![minSdk](https://img.shields.io/badge/minSdk-23-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-1.10-4285F4?logo=jetpackcompose&logoColor=white)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![爱发电](https://img.shields.io/badge/赞助-爱发电-946ce6)](https://afdian.com/a/feice)

简体中文 · [English](README.en.md)

</div>

## 预览

<table>
  <tr>
    <td align="center"><img src="docs/assets/preview/home.png" width="200" alt="组件目录"><br><sub>组件目录</sub></td>
    <td align="center"><img src="docs/assets/preview/menu.gif" width="200" alt="按钮长成菜单"><br><sub>按钮长成菜单</sub></td>
    <td align="center"><img src="docs/assets/preview/icon.gif" width="200" alt="图标原位变形"><br><sub>图标原位变形</sub></td>
    <td align="center"><img src="docs/assets/preview/counter.gif" width="200" alt="计数器滚轮"><br><sub>计数器滚轮</sub></td>
  </tr>
</table>

## 特点

- 🫧 **融球菜单**：按钮直接「长」成菜单，中间有融球颈；按住直接滑选，拖出面板有橡皮筋。
- ✨ **跟手的反馈**：按压从指尖发光，形变跟着手指走，松手按速度回弹。
- 🔄 **内容不瞬切**：图标原位变形，文字模糊替换，数字像计数器一样逐位滚动。
- 🧭 **完整导航**：侧推、打开应用式容器转场、共享顶栏按钮、预测性返回。
- 🎨 **MD3E 主题**：从种子色生成配色，Android 12+ 跟随系统取色；可选 [Nothing 外观](docs/zh_CN/guide/nothing-theme.md)。
- 📐 **G2 连续圆角**：所有形状用 [Kyant0/Capsule](https://github.com/Kyant0/Capsule)，同心嵌套，半径拉满时过渡成胶囊。
- 📳 **清脆的触感**：用组合原语震动，不用整段马达驱动带来的发麻余震。
- 🪶 **不依赖 material3**，各系统版本有明确的降级路径。

> 从 GooMenu 改名而来，迁移说明见 [CHANGELOG](CHANGELOG.md)。

## 安装

通过 [JitPack](https://jitpack.io) 分发。在 `settings.gradle.kts` 里加仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

在模块的 `build.gradle.kts` 里加依赖，`<tag>` 换成发布的 tag：

```kotlin
implementation("com.github.feice11.mochi-compose:mochi-compose:<tag>")
```

要求 compileSdk 36+、Kotlin 2.3+（Capsule 依赖 Compose 1.10）。

## 快速开始

```kotlin
setContent {
    MochiTheme(controller = remember { ThemeController(ColorSchemeMode.MonetSystem) }) {
        var expanded by remember { mutableStateOf(false) }
        var selected by remember { mutableIntStateOf(0) }
        val items = remember { persistentListOf(MochiItem("最近"), MochiItem("名称"), MochiItem("大小")) }

        MochiDropdown(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            items = items,
            onItemClick = { selected = it },
            selectedIndex = selected,
        ) {
            Text("排序：${items[selected].label}")
        }
    }
}
```

做完整应用时，可以直接复制 [MochiAppStarter.kt](example/src/main/java/ui/feice/mochi/starter/MochiAppStarter.kt)。这是一个三页骨架：首页卡片 → 列表（容器变形）→ 详情（侧推与共享顶栏），带文字和计数过渡、卡片展开、提示。它不依赖示例 app 的私有工具，注册方式见 [接入指南](docs/zh_CN/guide/application-integration.md#3-复制完整应用骨架)。

## 使用本库开发应用必读

先读 [应用接入规范](docs/zh_CN/guide/application-integration.md) 和 [设计规范](docs/zh_CN/guide/design-principles.md)。AI 从 [llms.txt](llms.txt) 读起。要点：

- 根部接 `MochiTheme`、`ProvideMochiOverscroll` 和 `MochiNavHost`。
- 正文里有卡片外的分组小标题时，用 `MochiCompactTitleScaffold`，见 [标题规范](docs/zh_CN/components/navbar.md#默认标题规则)。
- 侧推页默认接共享顶栏：
  - 用 `MochiBackButton`；
  - `MochiBar*` 系列给稳定的 `navId`；
  - 图标给稳定的 `contentKey`。
- 从卡片打开页面用 `mochiNavSource` + `push(from = key)`，整页一起变形。
- 复用库里的卡片、行、控件和反馈。颜色、字阶、形状都从主题取，同一个尺寸不要叠两层动画。

规范没写到的情况，按 [设计理念](docs/zh_CN/guide/philosophy.md) 的判断顺序推。消费项目应该在自己的开发说明里引用接入规范。

## 文档

| 分类 | 内容 |
|---|---|
| **组件** | [下拉菜单](docs/zh_CN/components/dropdown.md) · [标签栏](docs/zh_CN/components/tabbar.md) · [按钮](docs/zh_CN/components/button.md) · [开关](docs/zh_CN/components/switch.md) · [滑块](docs/zh_CN/components/slider.md) · [步进器](docs/zh_CN/components/stepper.md) · [列表](docs/zh_CN/components/list.md) · [底部面板](docs/zh_CN/components/sheet.md) · [分段选择器](docs/zh_CN/components/segmented.md) · [对话框](docs/zh_CN/components/dialog.md) · [文字输入](docs/zh_CN/components/textfield.md) · [提示](docs/zh_CN/components/toast.md) · [长按预览](docs/zh_CN/components/context-menu.md) · [大标题与搜索栏](docs/zh_CN/components/navbar.md) · [侧边栏](docs/zh_CN/components/sidebar.md) · [滚筒与时间日期](docs/zh_CN/components/picker.md) · [日历](docs/zh_CN/components/calendar.md) · [颜色选择](docs/zh_CN/components/color-picker.md) · [弹出层与确认](docs/zh_CN/components/popover.md) · [轮播与页面指示器](docs/zh_CN/components/pager.md) · [下拉刷新与空状态](docs/zh_CN/components/refresh.md) · [徽标、头像与仪表](docs/zh_CN/components/badge.md) · [工具栏与长按确认](docs/zh_CN/components/toolbar.md) |
| **指南** | [应用接入规范](docs/zh_CN/guide/application-integration.md) · [主题](docs/zh_CN/guide/theme.md) · [通用效果](docs/zh_CN/guide/effects.md) · [内容变换](docs/zh_CN/guide/content-transitions.md) · [导航与页面转场](docs/zh_CN/guide/navigation.md) · [越界回弹](docs/zh_CN/guide/overscroll.md) · [触感](docs/zh_CN/guide/haptics.md) · [HDR 高光（实验性）](docs/zh_CN/guide/hdr.md) · [Nothing 外观](docs/zh_CN/guide/nothing-theme.md) · [兼容性](docs/zh_CN/guide/compatibility.md) |
| **规范** | [设计理念](docs/zh_CN/guide/philosophy.md) · [设计规范](docs/zh_CN/guide/design-principles.md) · [新增组件](docs/zh_CN/guide/adding-components.md) · [贡献指南](CONTRIBUTING.md) |

## 系统要求和降级

目前只支持 Android，minSdk 23。各版本的差异见 [兼容性](docs/zh_CN/guide/compatibility.md)。

- 融球、阴影、融色需要 API 33+（`RuntimeShader`）。更低的版本降级成两块纯色的连续曲率形状，手势和其他动画照常。
- Monet 系统取色需要 Android 12+。更老的系统用 `ThemeController.keyColor` 生成配色。
- 组合原语震动需要 API 33+ 并且设备支持，否则走 `performHapticFeedback`。
- 系统关了动画时，果冻、融球、橡皮筋这些弹性效果全部关闭，对应 iOS 的「减弱动态效果」。

## 构建

需要 JDK 17 和 Android SDK 36。

| 任务 | 命令 |
|---|---|
| 组件库 | `./gradlew :mochi-compose:assembleRelease` |
| 本地发布 | `./gradlew :mochi-compose:publishToMavenLocal` |
| 示例 app | `./gradlew :example:assembleRelease`（没配签名时用 debug key） |
| 格式 | `./gradlew spotlessCheck` / `spotlessApply` |

CI 会跑格式检查、构建组件库，并出示例 release APK。想用自己的签名，在仓库 Secrets 里配 `GOO_KEYSTORE_BASE64`、`GOO_STORE_PASSWORD`、`GOO_KEY_ALIAS`、`GOO_KEY_PASSWORD`。推送 tag（如 `v0.1.0`）后，JitPack 按 `jitpack.yml` 发布。

| 依赖 | 版本 |
|---|---|
| AGP / Gradle | 8.9.1 / 8.14.3 |
| Kotlin | 2.3.0 |
| Compose | 1.10（由 Capsule 带入） |
| Capsule | 2.1.3 |
| materialkolor | 4.1.1 |
| minSdk / compileSdk | 23 / 36 |

## 赞助者

Mochi 由我一个人在业余时间开发维护。赞助会用在打磨手感、补组件和测试设备上。感谢以下 [爱发电](https://afdian.com/a/feice) 赞助者：

<table>
  <tr>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/5bd4b31acb1111f1a76f5254001e7c00/avatar/8a418c5d7086900eee258a1f2127257b_w1080_h810_s795.jpeg?imageView2/1/w/240/h/240" width="64" height="64" alt="Cremy_2449"><br><sub>Cremy_2449</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/ba19a0909c5311eb8ebb52540025c377/avatar/dfb1d6a1f872b590f06b0a3c81e60c06_w1000_h1000_s858.png?imageView2/1/w/240/h/240" width="64" height="64" alt="hrsthrt74"><br><sub>hrsthrt74</sub></td>
    <td align="center"><img src="https://pic1.afdiancdn.com/user/d1676108cac211f18b7252540025c377/avatar/f6eae4f52d41374ec53ec52086ea0f82_w650_h650_s580.png?imageView2/1/w/240/h/240" width="64" height="64" alt="虚言_Xenith"><br><sub>虚言_Xenith</sub></td>
  </tr>
</table>

## 致谢

- 形状：[Kyant0/Capsule](https://github.com/Kyant0/Capsule)
- 计数器的滚轮动画（`MochiCounterText`）：[Bear](https://github.com/mif9499)
- 工程规范参考：[miuix](https://github.com/compose-miuix-ui/miuix)
- 示例 app 的图标：[Ionicons](https://ionic.io/ionicons)（MIT）

## 参考资料

- [Meet Liquid Glass – WWDC25](https://developer.apple.com/videos/play/wwdc2025/219/)、[Adopting Liquid Glass – Apple](https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass)
- [Haptics design principles – Android](https://developer.android.com/develop/ui/views/haptics/haptics-principles)、[Custom haptic effects – Android](https://developer.android.com/develop/ui/views/haptics/custom-haptic-effects)
- androidx 源码 `ExpressiveMotionTokens` / `StandardMotionTokens` / `ShapeTokens`

## 许可

[Apache License 2.0](LICENSE)。形状来自 [Kyant0/Capsule](https://github.com/Kyant0/Capsule)（Apache-2.0），配色算法来自 [materialkolor](https://github.com/jordond/MaterialKolor)（Apache-2.0），第三方声明见 [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES.md)。
