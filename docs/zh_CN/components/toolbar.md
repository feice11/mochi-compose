# 工具栏与长按确认

## 底部工具栏

```kotlin
MochiBottomToolbar {
    MochiBarGroup {
        MochiBarGroupItem(onBack) { … }
        MochiBarGroupItem(onForward) { … }
    }
    Spacer(Modifier.weight(1f))
    MochiBarIconButton(onShare) { … }
}
```

iOS 26 的底部工具栏：

- 屏幕底部、导航栏上方的一排玻璃按钮，自带导航栏安全区（`windowInsets`）
- 按钮用[顶栏的同一套](navbar.md)：相关的按钮放进 `MochiBarGroup`，共用一个胶囊
- 组和组之间放 `Spacer(Modifier.weight(1f))`，把它们推到两边
- 背后有一段往上渐隐的底色（滚动边缘效果），内容滚到工具栏下面时被柔和地盖住。底色用 `edgeColor` 设，应等于页面底色

## 键盘工具栏

```kotlin
Box(Modifier.fillMaxSize()) {
    页面()
    MochiKeyboardToolbar(
        onDone = { focus.clearFocus() },
        modifier = Modifier.align(Alignment.BottomCenter),
        onPrevious = { first.requestFocus() },
        onNext = { second.requestFocus() },
    )
}
```

对应 iOS 的 inputAccessoryView：

- 只在键盘弹出时出现，贴在键盘上方（自带 `imePadding`），跟着键盘一起升降
- 左边是「上一项 / 下一项」一组按钮，右边是实心的「完成」
- 整条没有底色，浮在键盘上方的只有按钮本身；每个按钮带一圈细描边（`outline`，默认 `outlineVariant`），下面是什么内容都看得清按钮边界。不要在外面再给它加背景
- `onPrevious` / `onNext` 为 null 时，对应的箭头变成不可用
- `content` 可以在两者之间加别的按钮

## 长按确认

```kotlin
MochiHoldButton(onConfirm = { deleteAll() }) { Text("按住删除全部") }
```

用于删除、重置这类危险操作，必须按住 `holdMillis`（默认 1.2 秒）才执行：

- **按住时**：一层颜色从左往右匀速填满，每填满一成轻震一下
- **填满时**：重震一下并执行 `onConfirm`，随后进度退回
- **提前松手或手指滑走**：进度用弹簧退回，什么都不执行
- **按下时**：整颗按钮轻微形变
- **配色**：默认是危险色，可以通过 `MochiHoldButtonDefaults.colors(container, content, progress)` 修改
