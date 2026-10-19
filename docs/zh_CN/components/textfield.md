# 文字输入

iOS 26 里会打字的几种地方：输入框、原地改名、设置式表单行、对话框里的输入框、验证码格子。都在 `ui.feice.mochi.basic` 里。

## MochiTextField：输入框

```kotlin
var name by remember { mutableStateOf("") }
MochiTextField(name, { name = it }, placeholder = "昵称")
```

- 填充底色，不画边框和下划线，聚焦时外观不变，只有光标用主色。单行是胶囊（高 48dp，半径 24dp）；`singleLine = false` 时随内容长高，高度平滑过渡，变成同样半径的圆角矩形
- 聚焦且有内容时，右边的清除按钮原地模糊渐显；它的位置一直占着，文字不会因为它出现而挪动（`clearButton = false` 关掉）
- `secure = true`：显示成密码点，右边是小眼睛，点一下显示 / 隐藏，斜杠原位画出 / 收回；默认弹密码键盘
- `error` 不为 null：底色带一点警示色，下面模糊淡入说明，整个框左右抖一下并轻震（iOS 输错密码的样子）。`shakeTrigger` 变化时也会抖，比如传一个「尝试次数」，连续输错同一个说明也能每次都抖
- `supportingText`：下方的说明；`maxLength`：超出的字输不进去，右下角字数像计数器一样滚动
- `leading` / `trailing`：行首图标、行尾按钮。多行时它们和最后一行对齐（iMessage 的发送键在右下角）。行尾按钮用 `MochiTextFieldDefaults.TrailingButtonSize`（36dp），离胶囊上下右都是 `TrailingInset`（6dp），和胶囊同心
- 高度 `MochiTextFieldDefaults.Height` 就是 `MochiButtonDefaults.Height`（48dp）：旁边并排的 `MochiIconButton` / `MochiButton` 用默认尺寸就和输入框同高、同一条中线
- 聚焦时整个输入框（连同下面的说明和字数）会滚到键盘上方，下面留 16dp，不只是露出光标那一行。外层要有滚动容器，且滚动容器在键盘弹出时会变矮（例如加 `imePadding()`）

要代码聚焦，把 `Modifier.focusRequester(...)` 传给 `modifier`。

| 参数 | 说明 | 默认值 |
|---|---|---|
| placeholder | 占位文字，开始输入时淡掉 | `""` |
| singleLine / maxLines | 单行或多行；多行最多几行 | `true` / 多行时 6 |
| secure | 密码输入 | false |
| error | 错误说明 | null |
| supportingText | 普通说明 | null |
| maxLength | 字数上限，同时显示字数 | null |
| clearButton | 聚焦且有内容时显示清除按钮 | true |
| shape | 形状 | `MochiTextFieldDefaults.Shape` |
| colors | 配色 | `MochiTextFieldDefaults.colors()` |

## MochiInlineTextField：原地改名

```kotlin
var title by remember { mutableStateOf("我的提醒") }
var editing by remember { mutableStateOf(false) }
MochiInlineTextField(title, { title = it }, editing, { editing = it }, style = MochiTheme.textStyles.largeTitle)
```

- 文字原地变成输入框，位置和字号一点不动；文字后面淡入一块圆角底色（只包住文字本身），原文字全选，键盘弹出
- 键盘上的「完成」或点别处：提交。前后空白会去掉；清空后提交不生效，恢复原名（`allowEmpty = true` 允许空）
- 返回键：放弃修改，恢复原文字
- `editing` 由你持有：点文字本身（`editOnTap`），或别处的「重命名」按钮、长按菜单把它设成 true 都可以
- `onValueChange` 只在提交时调用

## MochiListTextFieldRow：表单行

放在 `MochiListSection` 里，和 `MochiListRow` 混排：

```kotlin
MochiListSection(header = "联系人") {
    MochiListTextFieldRow(name, { name = it }, label = "姓名", placeholder = "必填")
    MochiListTextFieldRow(phone, { phone = it }, label = "电话", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
    MochiListFormRow("生日", birthday, onClick = { pickBirthday() }, placeholder = "添加生日")
}
```

- 不能打字、点了去选值的行（生日、地区）用 `MochiListFormRow`：和输入行同一套布局，值从同一列开始（不像设置页靠右），点整行有按下高亮；没有值时显示淡色占位

- 行高和普通行一样（至少 `RowMinHeight`，上下至少 `RowVerticalPadding`），分隔线从文字处开始，首末留白交给卡片，和同卡片里的其他行规则一致
- 左边可选标签，`labelWidth` 默认 `MochiListTextFieldDefaults.LabelWidth`（88dp）；同一张卡片用同一个值，输入区左对齐成一列
- 点整行任何地方都会聚焦；聚焦且有内容时行尾模糊淡入清除按钮
- `singleLine = false` 时多行自然长高（备注）

## MochiDialogTextField：对话框里的输入框

放进 `MochiDialog` 的 `content`：

```kotlin
MochiDialog(
    visible = show,
    onDismissRequest = { show = false },
    title = "新建文件夹",
    message = "输入这个文件夹的名称。",
    actions = persistentListOf(
        MochiDialogAction("取消", MochiDialogActionRole.Cancel),
        MochiDialogAction("存储", MochiDialogActionRole.Primary, enabled = folder.isNotBlank()) { create(folder) },
    ),
) {
    MochiDialogTextField(folder, { folder = it }, placeholder = "名称")
}
```

- 胶囊输入框，左右伸出去和对话框按钮一样宽（离卡片边 14dp），和卡片同心；底色和对话框按钮相同
- 对话框出现时自动聚焦、弹出键盘（`autoFocus`）；对话框被键盘顶到键盘上方居中
- 内容没填好时把确认按钮的 `enabled` 设成 false
- 关掉对话框时键盘一起收起，焦点不会跑回页面上的输入框

## MochiCodeField：验证码

```kotlin
MochiCodeField(code, { code = it; wrong = false }, error = wrong, shakeTrigger = attempts, onComplete = { verify(it) })
```

- 一排圆角格子，一格一位，默认 6 位（`length`）；只收数字，弹数字键盘，支持整段粘贴和短信自动填充
- 当前那格描主色边（`mochiBorder`，连续圆角描边平滑）、光标闪烁；数字进来时模糊放大淡入，删掉时原路退回
- 输满调 `onComplete`；`error = true` 时格子带警示色，`shakeTrigger` 变化时整排抖一下

## 键盘

演示页的滚动区域加了 `imePadding()`，键盘弹出时可见区域变矮，聚焦的输入框整块滚到键盘上方。自己的页面也要这样，不然输入框会被键盘挡住。
