# 弹出层与确认

从按钮弹出一块放任意内容的面板（iOS 的 popover），以及操作确认和提示气泡。形变、果冻和[下拉菜单](dropdown.md)是同一套，所以两者手感一致；内容在形状展开到 `motion.contentAt` 之后才显影。

```kotlin
var open by remember { mutableStateOf(false) }
MochiPopoverButton(
    expanded = open,
    onExpandedChange = { open = it },
    popover = {
        Text("存储空间")
        MochiButton({ open = false }) { Text("知道了") }
    },
) { Text("显示详情") }
```

## 弹出层

- **触发者是按钮时用 `MochiPopoverButton`**：按钮和面板是同一个组件，和[下拉菜单](dropdown.md)共用同一套实现。按下放大、指尖发光、按钮颜色融进面板、融球颈、果冻、橡皮筋都一样，只是面板里放任意内容、按内容尺寸长出来。按钮外观取 `colors`（`buttonColor` / `buttonContentColor`），`behavior.openMode = Cover` 时按钮整个长成面板
- **按钮色必须不透明**：打开时真按钮藏起来、浮层里画一个克隆，下面还垫着面板阴影。半透明的灰会让阴影从按钮里透出来，看起来像一块突然浮起的半透明遮罩，收起时又随浮层一下消失。按钮放在卡片里要用灰色时，先合成到卡片底色上：`MochiDefaults.fill().compositeOver(MochiDefaults.groupedContainer())`。没合成的半透明色引擎会兜底合成到面板色上
- **`MochiPopoverBox` 只用于锚点不是按钮的情况**（程序主动弹出的提示、自定义控件）：锚点不归它管，所以没有按压反馈和融色
- 面板里放的是一串选项时，不要用弹出层再自己摆 `MochiListRow`，直接用 `MochiDropdown` / `MochiIconDropdown`：选项的高亮块、按住滑选、滑过轻震都是菜单引擎提供的

`MochiPopoverBox` 的行为：

- 默认出现在锚点下面，下面放不下、上面更宽裕时翻到上面；横向按 `align` 对齐锚点，再夹在屏幕边距里
- `cover = true`：面板从锚点所在位置铺开，盖住锚点（iOS 26 按钮「长成」面板的样子），适合 ＋、⋯ 这类图标按钮
- 锚点有底色时，把它传给 `colors.anchor`：两者之间会长出融球颈，看起来像按钮「流」出面板。锚点没有底色（纯文字、列表行尾）时不传，面板直接从锚点位置长出来
- 点面板外面或按返回键关闭；内容比屏幕高时可以滚
- `arrow = true` 时画一个指向锚点中心的小尖角，尖角不会进入面板圆角

## 操作确认

```kotlin
// 有来源按钮：iOS 26 从按钮弹出，点外面就是取消
MochiConfirmationDialogBox(open, { open = it }, actions, title = "这张照片会从所有设备上删除。") { Text("删除…") }
// 没有来源按钮：iOS 26 的居中对话框
MochiConfirmationDialog(show, { show = false }, actions, title = "要怎么处理这个文稿？")
```

- 从按钮弹出的那种就是一个带标题的选择列表（同一个菜单引擎）：按住按钮直接滑到选项上松手即执行，滑出面板被橡皮筋拉住
- 没有来源按钮时：iOS 26 去掉了从底部滑上来的操作列表，换成和警告框同一种居中对话框。`MochiConfirmationDialog` 直接就是 [`MochiDialog`](dialog.md)：标题、说明在上，胶囊按钮两个时并排、更多时竖排，取消排在最下面；和警告框不同，点外面等同于取消
- `actions` 用 [`MochiDialogAction`](dialog.md)：`Destructive` 显示红字；`Cancel` 在对话框里排最后，从按钮弹出时不显示（点外面就是取消）
- 点某一项时先执行它的 onClick，再调 `onDismissRequest`

## 提示

- `MochiTipCard`（TipKit 的 TipView）：放在页面里的提示卡。点关闭后整张卡模糊淡出、高度平滑收起
- `MochiTipPopover`（popoverTip）：按钮和气泡是同一个组件，走菜单引擎（和 `MochiPopoverButton` 一样）：按下放大发光、按钮颜色融进气泡、融球颈连着按钮，收起时缩回按钮。融球颈就是指向按钮的尖角，不另画尖角
  - 给了 `onClick` 时，轻点执行按钮自己的操作（比如收藏），长按才展开提示；不给时轻点就展开
  - 程序主动显示（`visible = true`）时也从按钮长出来；点外面、返回键、右上角关闭都调 `onVisibleChange(false)`
- 两者都只负责显示：`visible` 由调用方管理，记不记住「不再显示」也由调用方决定
