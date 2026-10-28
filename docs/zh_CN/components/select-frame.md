# 选择卡片与缩小预览

「选一个」的卡片，里面是实时预览：浅色 / 深色 / 自动、应用风格、玻璃样式。预览是真实界面缩小后的样子，不是示意图。

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    shades.forEach { s ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MochiSelectFrame(selected = s == shade, onClick = { shade = s }, Modifier.width(84.dp).aspectRatio(0.56f), cornerRadius = 20.dp) {
                MochiTheme(colors = s.scheme) {
                    MochiMiniature(scale = 0.5f) { SettingsPreview() }
                }
            }
            Text(s.label)
            MochiSelectionCheck(s == shade)
        }
    }
}
```

## 组件

- `MochiSelectFrame(selected, onClick, modifier, cornerRadius) { 预览 }`：没选中时一圈细描边、稍微缩小；选中时描边加粗变成主色、带回弹放大，里面的预览往里让出 6dp 的同心缝。整块按压跟手形变。预览收不到触摸。无障碍读作单选项。
- `MochiSelectionCheck(selected)`：卡片下面的勾选点，选中是主色圆点加对勾，没选中是一圈描边，切换时模糊替换。
- `MochiMiniature(scale) { 内容 }`：内容按 `1 / scale` 倍的尺寸排版，再整体缩回来，字号、控件、留白都是真实比例。外面再套一层 `MochiTheme` 就能预览另一套配色。
- `MochiPrerendered(key) { 内容 }`（`utils`）：先正常画几帧，再整块截成位图，之后只画这张图。滚动列表里放很贵的静态预览（缩小界面里有玻璃、模糊）时用；`key` 变了就重新画一次再截。截下来的图不会动，也不能点。

## 细节

- 描边没选中 1.5dp、选中 2.5dp，颜色从 `outlineVariant` 过渡到 `primary`。
- 默认圆角 22dp（`MochiSelectFrameDefaults.CornerRadius`），预览的圆角是它减去缝宽，两者同心。
- 标签和 `MochiSelectionCheck` 放在卡片下面，不放进卡片：预览应该和真实界面一样。
