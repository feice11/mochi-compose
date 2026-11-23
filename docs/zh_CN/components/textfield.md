# 文字输入

位于 `ui.feice.mochi.basic` 包。提供 6 个面向不同场景的文字输入与表单组件：独立输入框 `MochiTextField`、原地编辑 `MochiInlineTextField`、分组卡片输入行 `MochiListTextFieldRow`、分组卡片选择行 `MochiListFormRow`、对话框输入框 `MochiDialogTextField` 以及验证码输入框 `MochiCodeField`。这组组件属于正文内控件，采用纯色或半透明填充底色，不使用液态玻璃材质。

## MochiTextField

独立输入框。单行时为胶囊外形（高 `48dp`，圆角半径 `24dp`），无边框与下划线；聚焦时容器外观保持不变，仅光标使用主题主色。

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.MochiTextField
import ui.feice.mochi.basic.MochiTextFieldDefaults
import ui.feice.mochi.icons.MochiIonicon

var query by remember { mutableStateOf("") }
MochiTextField(
    value = query,
    onValueChange = { query = it },
    placeholder = "搜索或输入消息",
    leading = { MochiIonicon("search", size = 20.dp) },
    trailing = {
        MochiIconButton(
            onClick = { send(query) },
            enabled = query.isNotBlank(),
            style = MochiButtonStyle.Filled,
            modifier = Modifier.size(MochiTextFieldDefaults.TrailingButtonSize),
        ) {
            MochiIonicon("arrow-up", size = 18.dp, contentDescription = "发送")
        }
    },
)
```

### 尺寸与槽位对齐

`MochiTextFieldDefaults.Height` 与 `MochiButtonDefaults.Height`（`48dp`）一致，同排并列的默认尺寸 `MochiButton` 或 `MochiIconButton` 与输入框自然等高共中线。`singleLine = false` 时随内容平滑增高，外形保持 `24dp` 连续圆角矩形；多行输入时，`leading`、清除按钮、密码眼睛与 `trailing` 都与最后一行底部对齐。

- **同心嵌入槽位**：行尾嵌入圆形或胶囊按钮时，尺寸使用 `MochiTextFieldDefaults.TrailingButtonSize`（`36dp`），与外框上下右三边各留 `TrailingInset`（`6dp`）保持同心；行首嵌入胶囊控件（如区号下拉菜单）时，将 `leadingInset` 设为 `MochiTextFieldDefaults.LeadingControlInset`（`6dp`）并使用 `TrailingButtonSize` 高度。
- **清除按钮与密码切换**：`clearButton = true` 时，聚焦且内容非空会在右侧原位模糊渐显清除按钮，槽位宽度固定保留，文字不因按钮显隐发生横向位移。`secure = true` 时将输入掩码为圆点，并在右侧显示眼睛切换按钮（斜杠原位绘制与收回），切换时回调 `onRevealChange(revealed)`；未显式指定键盘类型时自动切换为关闭自动纠错的密码键盘。

### 校验、字数限制与焦点

- **错误与震动反馈**：`error` 非 `null` 时，底色平滑过渡至 `colors.errorContainer`，光标切换为 `colors.error`，下方模糊淡入错误说明，同时触发横向衰减弹簧抖动与阈值触觉反馈。`shakeTrigger` 每次变为新的非 `null` 且非 `false` 值时也会触发抖动，传入递增计数即可在重复提交同一错误时每次抖动。
- **字数上限与辅助说明**：传入 `maxLength` 时超出上限的字符会被截断，右下角以滚动计数器样式显示 `当前字数 / maxLength`（达到上限时转为警示色）；`supportingText` 在未报错时显示于左下方。两者都为空时不占用垂直空间。
- **代码聚焦与全选**：需要从代码主动请求焦点时，在 `modifier` 上挂载 `Modifier.focusRequester(...)`。设置 `selectAllOnFocus = true` 可在每次获得焦点时自动全选现有文本。

### 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | `String` | 必填 | 当前文本内容 |
| `onValueChange` | `(String) -> Unit` | 必填 | 文本变更回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `placeholder` | `String` | `""` | 占位文本，开始输入时淡出、清空时淡入 |
| `enabled` | `Boolean` | `true` | 是否启用；禁用时整体透明度降为 `0.4f` |
| `singleLine` | `Boolean` | `true` | 是否单行输入 |
| `maxLines` | `Int` | `if (singleLine) 1 else 6` | 最大显示行数 |
| `secure` | `Boolean` | `false` | 是否启用密码掩码与显隐切换按钮 |
| `error` | `String?` | `null` | 错误提示文本；非 `null` 时显示错误态并触发抖动 |
| `supportingText` | `String?` | `null` | 下方辅助说明文本（`error` 非空时优先显示 `error`） |
| `maxLength` | `Int?` | `null` | 最大字符数限制，设置后在右下角显示滚动字数统计 |
| `clearButton` | `Boolean` | `true` | 聚焦且有内容时是否显示清除按钮 |
| `keyboardOptions` | `KeyboardOptions` | `KeyboardOptions.Default` | 软键盘配置 |
| `keyboardActions` | `KeyboardActions` | `KeyboardActions.Default` | 软键盘动作回调 |
| `shakeTrigger` | `Any?` | `error` | 抖动触发键；每次变为非空且非 `false` 的新值时触发横向抖动 |
| `shape` | `Shape` | `MochiTextFieldDefaults.Shape` | 容器形状 |
| `textStyle` | `TextStyle` | `MochiTheme.textStyles.body` | 输入文本与占位文本样式 |
| `colors` | `MochiTextFieldColors` | `MochiTextFieldDefaults.colors()` | 容器、文本、光标、图标与错误态配色 |
| `leading` | `(@Composable () -> Unit)?` | `null` | 行首图标或内嵌控件槽位 |
| `trailing` | `(@Composable () -> Unit)?` | `null` | 行尾按钮或图标槽位 |
| `selectAllOnFocus` | `Boolean` | `false` | 获得焦点时是否自动全选文本 |
| `leadingInset` | `Dp` | `MochiTextFieldDefaults.LeadingIconInset` | `leading` 非空时的左侧内边距 |
| `onRevealChange` | `((Boolean) -> Unit)?` | `null` | `secure` 模式下切换明文显隐时的回调 |
| `onFocusChange` | `((Boolean) -> Unit)?` | `null` | 焦点状态变化回调 |

### MochiTextFieldDefaults

| 名称 | 值 | 说明 |
|---|---|---|
| `Height` | `48.dp` | 单行高度（等于 `MochiButtonDefaults.Height`） |
| `Shape` | `MochiRoundedRectangle(24.dp)` | 默认连续圆角形状（半径 `Height / 2`） |
| `HorizontalPadding` | `16.dp` | 无行首或行尾槽位时的文本左右内边距 |
| `LeadingIconInset` | `14.dp` | 行首放置图标时的左侧间距 |
| `LeadingControlInset` | `6.dp` | 行首放置同心胶囊控件时的左侧间距（等于 `TrailingInset`） |
| `TrailingInset` | `6.dp` | 行尾槽位距容器上、下、右边缘的同心内缩距离 |
| `TrailingButtonSize` | `36.dp` | 行尾同心按钮尺寸（`Height - TrailingInset * 2`） |

## MochiInlineTextField

原地重命名控件（适用于标题、文件夹或列表名称改名）。进入编辑态时文本字号与布局位置保持静止，仅在文字墨迹外侧淡入圆角高亮底色、全选原文本并拉起软键盘。

```kotlin
var title by remember { mutableStateOf("我的提醒") }
var editing by remember { mutableStateOf(false) }

MochiInlineTextField(
    value = title,
    onValueChange = { title = it },
    editing = editing,
    onEditingChange = { editing = it },
    style = MochiTheme.textStyles.largeTitle,
)
```

### 行为说明

- **进入编辑**：`editing` 状态由调用方持有。点击文本自身（当 `editOnTap = true` 时）或通过外部「重命名」菜单项把 `editing` 设为 `true` 都可进入编辑。
- **提交与取消**：按下软键盘「完成」或移出焦点时提交修改，`onValueChange` 仅在提交且内容实际改变时调用。提交前会自动去除首尾空白；若结果为空且 `allowEmpty = false`，则放弃修改并恢复原值。按下系统返回键会取消编辑并恢复原文本。
- **零位移高亮**：圆角底色（`10dp` 连续圆角）在绘制阶段按每行文字实际墨迹边界向外扩展绘制，不改变组件测量尺寸。

### 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | `String` | 必填 | 已提交的当前文本 |
| `onValueChange` | `(String) -> Unit` | 必填 | 提交新文本时的回调 |
| `editing` | `Boolean` | 必填 | 是否处于编辑态 |
| `onEditingChange` | `(Boolean) -> Unit` | 必填 | 编辑态变更回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `style` | `TextStyle` | `MochiTheme.textStyles.body` | 文本样式 |
| `color` | `Color` | `MochiTheme.colorScheme.onSurface` | 文本颜色 |
| `maxLines` | `Int` | `1` | 最大行数 |
| `editOnTap` | `Boolean` | `true` | 非编辑态下点击文本是否直接进入编辑 |
| `allowEmpty` | `Boolean` | `false` | 是否允许提交空白结果 |
| `highlight` | `Color` | `MochiTheme.colorScheme.primary.copy(alpha = 0.12f)` | 编辑态文字背后的圆角高亮底色 |
| `cursor` | `Color` | `MochiTheme.colorScheme.primary` | 光标与文本选区把手颜色 |

## MochiListTextFieldRow 与 MochiListFormRow

用于 `MochiListSection` 或 `MochiListCard` 内的设置与资料表单行，可与标准 `MochiListRow` 混排。`MochiListTextFieldRow` 提供行内文本输入；`MochiListFormRow` 用于点击弹窗或跳页选值的表单项（如生日、地区），使所选值与上下输入行的输入区左对齐在同一列。

```kotlin
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import ui.feice.mochi.basic.MochiListFormRow
import ui.feice.mochi.basic.MochiListTextFieldRow
import ui.feice.mochi.list.MochiListSection

MochiListSection(header = "联系人") {
    MochiListTextFieldRow(
        value = name,
        onValueChange = { name = it },
        label = "姓名",
        placeholder = "必填",
    )
    MochiListTextFieldRow(
        value = phone,
        onValueChange = { phone = it },
        label = "电话",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    )
    MochiListFormRow(
        label = "生日",
        value = birthday,
        onClick = { pickBirthday() },
        placeholder = "添加生日",
    )
}
```

### 行为与对齐规则

- **行高与分隔线**：最小行高（`MochiListDefaults.RowMinHeight`，`52dp`）、上下留白（`RowVerticalPadding`，`15dp`）及卡片首末行留白规则与 `MochiListRow` 一致；分隔线从左侧文字起点（`16dp`）延伸至卡片右缘。
- **列对齐**：左侧标签默认占 `MochiListTextFieldDefaults.LabelWidth`（`88dp`）；同一张卡片内各行保持相同 `labelWidth`，右侧输入区与表单值即可严格对齐为同一列。
- **交互细节**：点击 `MochiListTextFieldRow` 任意位置都会聚焦输入框，聚焦且内容非空时行尾原位模糊渐显清除按钮；`singleLine = false` 时随多行文本自然增高。`MochiListFormRow` 点击整行带有列表按压高亮，`value` 为 `null` 时显示淡色 `placeholder`，值更新时执行模糊过渡替换。

### MochiListTextFieldRow 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | `String` | 必填 | 当前文本内容 |
| `onValueChange` | `(String) -> Unit` | 必填 | 文本变更回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `label` | `String?` | `null` | 左侧字段标签；为 `null` 时输入区从行首文字起点开始 |
| `placeholder` | `String` | `""` | 占位文本 |
| `enabled` | `Boolean` | `true` | 是否启用 |
| `singleLine` | `Boolean` | `true` | 是否单行输入 |
| `maxLines` | `Int` | `if (singleLine) 1 else 5` | 最大显示行数 |
| `secure` | `Boolean` | `false` | 是否使用密码掩码与密码键盘 |
| `labelWidth` | `Dp` | `MochiListTextFieldDefaults.LabelWidth` | 左侧标签列宽度（默认 `88.dp`） |
| `keyboardOptions` | `KeyboardOptions` | `KeyboardOptions.Default` | 软键盘配置 |
| `keyboardActions` | `KeyboardActions` | `KeyboardActions.Default` | 软键盘动作回调 |
| `colors` | `MochiListColors` | `MochiListDefaults.colors()` | 列表行配色 |

### MochiListFormRow 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `label` | `String` | 必填 | 左侧字段标签 |
| `value` | `String?` | 必填 | 当前已选值；为 `null` 时显示 `placeholder` |
| `onClick` | `() -> Unit` | 必填 | 点击整行的回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `placeholder` | `String` | `""` | 未选值时的占位文本 |
| `enabled` | `Boolean` | `true` | 是否启用 |
| `labelWidth` | `Dp` | `MochiListTextFieldDefaults.LabelWidth` | 左侧标签列宽度（默认 `88.dp`） |
| `colors` | `MochiListColors` | `MochiListDefaults.colors()` | 列表行配色 |

## MochiDialogTextField

专为 `MochiDialog` 正文插槽设计的胶囊输入框（适用于「新建文件夹」「输入密码」等弹窗）。

```kotlin
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.MochiDialogTextField

MochiDialog(
    visible = showDialog,
    onDismissRequest = { showDialog = false },
    title = "新建文件夹",
    message = "请为此文件夹输入名称。",
    actions = persistentListOf(
        MochiDialogAction("取消", MochiDialogActionRole.Cancel),
        MochiDialogAction("存储", MochiDialogActionRole.Primary, enabled = folder.isNotBlank()) {
            createFolder(folder)
        },
    ),
) {
    MochiDialogTextField(
        value = folder,
        onValueChange = { folder = it },
        placeholder = "名称",
    )
}
```

### 行为与几何细节

- **同心宽度与底色**：输入框向左右各外扩 `10dp`，使两端与对话框操作按钮对齐（距对话框卡片边缘 `MochiDialogDefaults.ButtonInset`，即 `14dp`），与外层卡片保持同心，并采用与对话框次要按钮一致的容器色。
- **自动聚焦与键盘联动**：`autoFocus = true` 时，对话框出现后自动请求焦点并拉起软键盘，对话框卡片随之上移并在键盘上方保持居中；关闭对话框时软键盘同步收起，焦点不会跳回底层页面的输入框。校验未通过时，将确认动作 `MochiDialogAction` 的 `enabled` 设为 `false`。
- **转场期间光标控制**：对话框开合、跟手形变、改变高度或随软键盘移动期间会暂停绘制光标，待动画稳定后恢复闪烁；在此期间输入焦点、文本选区与输入法会话保持连续。

### 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | `String` | 必填 | 当前文本内容 |
| `onValueChange` | `(String) -> Unit` | 必填 | 文本变更回调 |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `placeholder` | `String` | `""` | 占位文本 |
| `secure` | `Boolean` | `false` | 是否启用密码掩码与眼睛切换按钮 |
| `autoFocus` | `Boolean` | `true` | 对话框显示时是否自动聚焦并弹出键盘 |
| `error` | `String?` | `null` | 错误提示文本；非 `null` 时显示错误态并抖动 |
| `keyboardOptions` | `KeyboardOptions` | `KeyboardOptions.Default` | 软键盘配置 |
| `keyboardActions` | `KeyboardActions` | `KeyboardActions.Default` | 软键盘动作回调 |
| `colors` | `MochiTextFieldColors` | `MochiTextFieldDefaults.colors(container = ...)` | 输入框配色（默认容器色在浅色下为 `surfaceContainerHigh`、深色下为 `surfaceContainerHighest`） |

## MochiCodeField

分格数字验证码输入框，每格显示一位数字。

```kotlin
MochiCodeField(
    value = code,
    onValueChange = {
        code = it
        wrong = false
    },
    error = wrong,
    shakeTrigger = attempts,
    onComplete = { verify(it) },
)
```

### 行为说明

- **输入与自动填充**：默认 6 格（单格最小高 `56dp`、最大宽 `56dp`、间距 `8dp`、`14dp` 连续圆角），仅接收数字字符并拉起数字键盘，支持整段粘贴与系统短信验证码自动填充。
- **焦点描边与动效**：当前待输入格使用 `mochiBorder` 绘制 `2dp` 连续圆角主色描边并显示闪烁光标；输入数字时模糊放大淡入，删除时沿原路径退回。
- **完成与错误态**：输入满 `length` 位时触发 `onComplete`；`error = true` 时所有格子切换为警示底色与警示描边，`shakeTrigger` 变化时整排格子横向抖动并伴随触觉反馈。

### 参数

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `value` | `String` | 必填 | 当前已输入的数字字符串 |
| `onValueChange` | `(String) -> Unit` | 必填 | 数字变更回调（已自动过滤非数字并截断至 `length`） |
| `modifier` | `Modifier` | `Modifier` | 修饰符 |
| `length` | `Int` | `6` | 验证码位数 |
| `enabled` | `Boolean` | `true` | 是否启用 |
| `error` | `Boolean` | `false` | 是否显示错误警示配色 |
| `shakeTrigger` | `Any?` | `null` | 抖动触发键；每次变为非空且非 `false` 的新值时整排横向抖动 |
| `onComplete` | `(String) -> Unit` | `{}` | 输入满 `length` 位时的回调 |
| `colors` | `MochiTextFieldColors` | `MochiTextFieldDefaults.colors()` | 格子底色、数字、光标与错误态配色 |

## 键盘

`MochiTextField`、`MochiListTextFieldRow` 与 `MochiCodeField` 内置了与 `WindowInsets.imeAnimationTarget` 同步的视口滚动请求：聚焦且软键盘升起时，组件在第一帧即按软键盘目标高度将整个输入框（含下方说明、字数统计及 `16dp` 底部留白）随键盘动画同步滚入可见区域，并在键盘停稳后自动释放请求，避免拦截页面内其他按钮的首次点击。

在自定义可滚动页面中使用上述输入框时，请为滚动容器添加 `Modifier.imePadding()`，使可见视口随软键盘收缩；`MochiDialog` 与 `MochiSheet` 已内置软键盘避让，无需额外配置。
