# 触感

整个库用同一套规则，学 iOS「少而准」：只在状态真的变了、或者「现在松手会发生什么」变了的时候震。

| 时机 | 方法 | 用在 |
|---|---|---|
| 滑过新选项、刻度、过中线 | `tick()` | 下拉菜单滑选、滑块刻度、开关拖过中线、长按菜单滑选 |
| 长按呼出 | `longPress()` | 下拉菜单长按打开、长按预览 |
| 越过「松手就生效」的阈值 | `threshold()` | 左滑删除越过阈值（来回越过每次都震） |
| 确认 | `confirm()` | 开关拨动、松手选中、选中一个选项（IosLike 和 Rich 都震） |
| 轻点打开 | `tapOpen()` | 只在 Rich 档 |

档位 `MochiHapticProfile`：`IosLike`（默认）、`Rich`、`Off`。

自己的组件也用同一套：

```kotlin
val haptics = rememberMochiHaptics()
haptics.tick()
```

实现上优先用 API 33+ 的组合原语（厂商调好的短脉冲，跟随系统触摸反馈强度），不支持时退回 `performHapticFeedback`（带 `FLAG_IGNORE_VIEW_SETTING`，不会因为某个容器关了 View 的触感而悄悄丢掉；系统触摸反馈总开关照样生效）。tick 用 TICK 原语（LOW_TICK 在部分机型上轻到几乎感觉不到，像是丢了），24ms 内只震一次，快划时不会连成一串。
