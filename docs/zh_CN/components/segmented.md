# 分段选择器

iOS 26 的分段选择器（不做玻璃）：不透明的灰色胶囊轨道里（放在卡片、面板、玻璃或网页上都不透出下面的内容），选中项是一块抬起来的亮色胶囊，各段等宽。高光、形变和[标签栏](tabbar.md)是同一套。

```kotlin
var i by remember { mutableIntStateOf(0) }
MochiSegmentedControl(
    selectedIndex = i,
    onSelect = { i = it },
    items = persistentListOf("日", "周", "月", "年"),
    modifier = Modifier.fillMaxWidth(),
)
```

## 交互

- 点别的段：松手后选中块滑过去，后沿慢半拍，整块被拉长成一条胶囊、略微变薄，到位后收回（只有一个形状，过程中不会鼓起来）
- 按住选中块：它放大、胀出轨道、发光；拖着它滑选，经过每一段轻震；拖出两端被橡皮筋拉住，轨道也被手指带着轻微挪动
- 选中项文字加粗；选中块经过时，文字的粗细和颜色连续过渡
- 按住别的段还没松手时，那段变淡；滑出轨道再松手不算点中

## 宽度

给了固定宽度（比如 `fillMaxWidth()`）就等分；不给宽度时，每段按最长的文字加上 `segmentPadding` 定宽，整条自己收窄。

## 参数

| 参数 | 说明 |
|---|---|
| `colors` | `MochiSegmentedDefaults.colors(track, thumb, selectedContent, content, shadow)` |
| `behavior.height` | 胶囊高度，默认 36dp（参数行里可以用 32dp） |
| `behavior.segmentPadding` | 每段文字左右至少留多少，默认 14dp |
| `behavior.gel` | 果冻拉伸强度，0 = 关 |
| `behavior.trail` | 拖尾：后沿落后多少（0..1），0 = 不拖尾、改用果冻拉伸 |
| `behavior.pressScale` | 按住选中块时放大到多少，默认 1.14 |
