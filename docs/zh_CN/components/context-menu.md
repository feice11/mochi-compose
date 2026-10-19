# MochiContextMenu

长按预览（iOS 的 context menu，不做模糊和玻璃）。

- **按住**：内容慢慢缩小一点，告诉你「再按一会儿会发生什么」；松手或滑动就恢复，点击和滚动照常。
- **按满时长**：轻震，内容浮起放大、投出阴影，背景变暗，操作菜单从靠内容的那一角长出来。菜单放在下面；放不下放上面；都放不下就把内容往上挪给菜单让位。
- **选择**：不松手直接滑到菜单项上松手 = 选中，滑过每一项轻震。松手在别处菜单保持打开，点外面或按返回关闭。

## 用法

```kotlin
MochiContextMenu(
    items = listOf(MochiItem("拷贝"), MochiItem("删除", destructive = true)),
    onItemClick = { index -> … },
    shape = RoundedCornerShape(20.dp),   // 和内容的外形一致，用来投阴影
) {
    PhotoCard(…)
}
```

内容会在浮层里再组合一次（预览用），所以里面不要放有内部状态的东西。

| 参数 | 说明 |
|---|---|
| items | 菜单项（和 MochiDropdown 共用 `MochiItem`：文字、警示色、图标） |
| onItemClick | 选中回调 |
| shape | 浮起时的阴影形状 |
| enabled / hapticProfile | 开关、触感 |

## 外观

菜单和 [MochiDropdown](dropdown.md) 是同一套选择列表：同样的面板、滑动高亮块（滑动时被拉长）、按着选项缩小跟手、选项高光、逐项错开出现，从靠内容的那一角带果冻感长出来。`colors` / `behavior` / `physics` / `motion` / `menuWidth` 和 MochiDropdown 的同名参数一样（比如 `behavior(itemLayout = MochiItemLayout.Ios)`）。

关闭时内容平滑落回原位、恢复原大小，和原卡片交接时不跳。

## 和下拉菜单同一个引擎

菜单不是另画的：卡片就是锚点，面板从卡片里带融球颈、果冻长出来，滑出面板被橡皮筋拉住，面板按压发光、两层投影，长列表能滚、贴边自动滚——下拉菜单有的全都有。`cornerRadius` 填内容的圆角（投影和融球颈按它算）。
