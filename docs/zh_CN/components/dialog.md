# 对话框

iOS 26 的警告框（alert），不做玻璃。

```kotlin
var show by remember { mutableStateOf(false) }
MochiDialog(
    visible = show,
    onDismissRequest = { show = false },
    title = "删除这条笔记？",
    message = "删除后无法恢复。",
    actions = persistentListOf(
        MochiDialogAction("取消", MochiDialogActionRole.Cancel),
        MochiDialogAction("删除", MochiDialogActionRole.Destructive) { delete() },
    ),
)
```

## 样子

- 圆角卡片，标题（加粗）和说明靠左，可以加 `icon` 和自定义 `content`
- 要输入文字时在 `content` 里放 `MochiDialogTextField`（见[文字输入](textfield.md)）：键盘弹出时对话框被顶到键盘上方居中，关掉时键盘一起收起
- 按钮是胶囊：两个时并排，其余竖排。按钮离卡片边 14dp，卡片圆角 = 按钮半高（24dp）+ 14dp，按钮和卡片同心
- 按钮配色按角色：`Primary` 主色实心、`Default` / `Cancel` 灰色、`Destructive` 灰色红字；文字都加粗
- 出现时从 1.12 倍缩放到位、淡入，后面的页面压暗；消失时淡出、略微缩小
- 带 `icon` 时图标统一描绘出场：图标位里的 `MochiIonicon` 在卡片放大的同时一笔一笔描出来（约 0.7 秒），实心部分最后淡入（见[图标](icons.md)）。调用方只传图标，不写动画

```kotlin
MochiDialog(
    visible = show,
    onDismissRequest = { show = false },
    title = "打开玻璃？",
    message = "每一帧都要额外渲染，更耗电。",
    icon = { MochiIonicon("warning", tint = MochiTheme.colorScheme.error, size = 44.dp) },
    actions = persistentListOf(
        MochiDialogAction("取消", MochiDialogActionRole.Cancel),
        MochiDialogAction("打开", MochiDialogActionRole.Primary) { enable() },
    ),
)
```

需要用户确认才生效的设置（耗电、耗流量）用普通点按确认就够了；`holdToConfirm` 留给不可撤销的危险操作。

## 按下和滑选

按住任一按钮后，手指可以在按钮之间上下（并排时左右）滑动：滑到哪个按钮哪个亮起，震动和选择列表一致：按下和每换一个按钮轻震一下，松手选中再震一下确认；滑出按钮区再松手什么都不做。按钮只有按下的轻微放大和指尖高光，不跟手拉长、挪动，并排的两个之间不拉融球颈，整张卡片也不跟手形变。按住才生效的按钮（`holdToConfirm`）不参与滑选，用它自己的按住手势。

## 关闭

- 点任何一个按钮：先执行它的 `onClick`，再调 `onDismissRequest`
- 返回键 / 返回手势关闭，支持**预见式返回**：拖动时卡片跟着缩小（最多 0.9）、朝手势那侧偏一点，后面的压暗变浅；松手关闭，取消则弹回
- 默认点外面不关闭（iOS 的警告框也是这样），`dismissOnOutsideTap = true` 打开
- `dismissOnBack = false`：返回手势只让卡片跟着缩一下、松手弹回，不关闭；配合下面的按住按钮，确保提示被看完
- `MochiDialogAction(..., holdToConfirm = true)`：这个按钮要按住才生效（[`MochiHoldButton`](toolbar.md)），按住时一层颜色从左往右填满，填满才执行、关闭；提前松手退回。配色仍按角色
- 要「不再提示」这类勾选，在 `content` 里放一行：整行用 `toggleable`，行首放 `MochiCheckbox(checked, onCheckedChange = null)` 只负责显示

## 字号

和 iOS 26 的警告框一样：标题 17sp Semibold，正文 15sp，按钮 17sp Semibold。后面的压暗是 `MochiDefaults.scrim()`（浅色 30% 黑、深色 50%），和底部面板、长按预览用同一个。

按钮顺序按 iOS 警告框：并排两个时 `Cancel` 角色的按钮固定在左边，竖排时放在最下面，不管传进来的顺序。
