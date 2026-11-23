# 触感反馈

位于 `ui.feice.mochi.utils` 包。Mochi 全库遵循统一的「少而准」触感原则：仅在离散状态实际改变，或「此刻松手将触发的结果」发生改变时才发出短促脉冲；常规轻点打开或橡皮筋越界拖拽默认保持静默。

## 基本用法

在自定义组件中通过 `rememberMochiHaptics` 获取与内置控件一致的触感实例：

```kotlin
import ui.feice.mochi.utils.MochiHapticProfile
import ui.feice.mochi.utils.rememberMochiHaptics

val haptics = rememberMochiHaptics(profile = MochiHapticProfile.IosLike)

// 手指滑过新刻度或选项时调用
haptics.tick()
```

内置交互控件通过参数或 `Behavior` 配置支持切换档位（例如 `MochiSwitch`、`MochiSlider`、`MochiListSliderRow`、`MochiSwipeRow` 的 `hapticProfile` 参数，以及 `MochiDropdownBehavior`、`MochiSegmentedBehavior`、`MochiTabBarBehavior` 的 `hapticProfile` 属性）。

## 语义方法（`MochiHaptics`）

| 方法 | 触发时机 | 启用档位 | 原语强度与系统回退常量 |
|---|---|---|---|
| `tick()` | 滑过菜单项、滑块刻度、滚筒选择器条目或开关跨过中线（内置 `24ms` 节流防连震） | `IosLike`、`Rich` | `PRIMITIVE_TICK`（`0.7f`）→ `PRIMITIVE_LOW_TICK`（`1.0f`）→ `SEGMENT_TICK` / `CLOCK_TICK` |
| `longPress()` | 长按呼出上下文菜单或长按预览 | `IosLike`、`Rich` | `PRIMITIVE_CLICK`（`0.8f`）→ `LONG_PRESS` |
| `threshold()` | 双向跨过「松手即生效/取消」的动作阈值（左滑删除、下拉刷新触发线、底部面板关闭阈值，来回每次越过都触发） | `IosLike`、`Rich` | `PRIMITIVE_CLICK`（`0.6f`）→ `GESTURE_THRESHOLD_ACTIVATE` / `CONFIRM` / `VIRTUAL_KEY` |
| `confirm()` | 确认离散选择（拨动开关、松手选中菜单项、点选枚举项） | `IosLike`、`Rich` | `PRIMITIVE_CLICK`（`0.55f`）→ `CONFIRM` / `VIRTUAL_KEY` |
| `tapOpen()` | 轻点展开下拉菜单或轮盘菜单 | 仅 `Rich` | `PRIMITIVE_TICK`（`0.5f`）→ `VIRTUAL_KEY` |

## 触感档位（`MochiHapticProfile`）

`rememberMochiHaptics(profile: MochiHapticProfile = MochiHapticProfile.IosLike)` 支持三档策略：

| 档位 | 说明 |
|---|---|
| `IosLike` | **默认值**。长按呼出、滑动经过刻度/选项、越过动作阈值与确认选择时震动；轻点打开不震动 |
| `Rich` | 在 `IosLike` 基础上，为 `tapOpen()` 轻点打开补充一次轻触脉冲 |
| `Off` | 关闭所有触感调用 |

## 平台分发与权限

- **组合原语优先（Android 13+）**：当运行于 Android 13（API 33）及以上、宿主应用声明了 `android.permission.VIBRATE` 权限且设备线性马达支持对应的 `VibrationEffect.Composition` 原语时，`MochiHaptics` 发送厂商调校的短脉冲，并附带 `VibrationAttributes.USAGE_TOUCH` 属性以跟随系统「触摸反馈」强度设置。库内不使用 `VibrationEffect.createOneShot` 等定长波形，避免线性马达产生拖尾闷震。
- **高频节流与原语选择**：`tick()` 以 `PRIMITIVE_TICK` 为主原语（部分机型上 `PRIMITIVE_LOW_TICK` 幅度过弱），并限制 `24ms` 内最多触发一次，确保快速划过密集刻度时颗粒清晰、不会连成嗡鸣。
- **视图触感回退**：未声明 `VIBRATE` 权限、系统低于 Android 13 或硬件不支持组合原语时，自动回退至 `View.performHapticFeedback` 并传入 `HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING`，防止父容器关闭单视图 `isHapticFeedbackEnabled` 导致反馈静默丢失，同时仍严格遵循系统全局触摸反馈总开关。
