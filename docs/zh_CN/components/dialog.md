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

## 高光和形变

和标签栏同一套：按钮按下放大、发光、跟手，并排的两个按钮之间拉出融球颈；整张卡片（连同投影、文字、按钮）被手指带着挪动、朝手指那侧略微拉长，边缘接到按钮的环境光。卡片不裁切，按钮放大时可以探出一点，和别处的按钮手感一致。

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
