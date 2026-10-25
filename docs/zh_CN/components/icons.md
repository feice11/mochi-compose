# 图标（mochi-icons）

内置全套 [Ionicons](https://ionic.io/ionicons) 8.1.0（MIT）：515 个图标，大多有描边、实心、直角三种画法。放在单独的 `mochi-icons` 模块里，核心库 `mochi-compose` 不带图标资源。

```kotlin
implementation("com.github.feice11.mochi-compose:mochi-icons:<版本>")
```

```kotlin
MochiIonicon("heart")                                   // 描边、22dp、当前内容色
MochiIonicon("heart", style = MochiIconStyle.Filled)    // 实心
MochiIonicon("chevron-forward", optical = false)        // 跟着文字走的符号
MochiIonicon("trash", contentDescription = "删除")       // 图标是按钮唯一的内容时写读屏说明

val icon = rememberMochiIonicon("heart")                 // 拿 ImageVector，给 MochiMorphIcon 之类用
MochiMorphIcon(rememberMochiIonicon(if (liked) "heart" else "heart-dislike")!!)
```

## 画法怎么选

| 画法 | 用在 |
|---|---|
| `Outline` 描边（默认） | 菜单项、工具栏按钮、未选中的标签、行首符号 |
| `Filled` 实心 | 设置式图标块、选中的标签、需要强调的地方 |
| `Sharp` 直角 | 整套界面偏硬朗时统一换成它，不和另外两种混用 |

某个图标没有要的画法时（品牌图标 `logo-*` 大多只有实心）自动退回实心。名字也可以直接写全名（`"heart-outline"`），这时忽略 `style`。

## 大小、粗细、居中

- 默认 22dp，是 17sp 正文旁边的符号大小；列表行首的符号图标由行自己定尺寸。
- 描边粗细跟 `LocalMochiSymbolWeight` 走（SF Symbols 的做法：符号按旁边文字的字重变粗变细），列表行会放进标题的字重，不用自己传。
- 默认做光学归一：Ionicons 每个图标在画布里留白不同，按墨迹面积缩放到同样「看起来的大小」，按视觉重心居中，圆的不显大、扁的不显小。箭头、对勾、关闭、加号这类跟文字字面大小走的符号传 `optical = false`。

## 找名字

- 示例 app 的「图标」页：按字母分组，右边按住字母快速跳，中英文都能搜，点一个看三种画法并复制代码。
- 代码里：`MochiIonicons.catalog(context)` 列出全部，`MochiIonicons.search(catalog, "喜欢")` 按名字、英文标签和中文关键词搜。第一次读目录要几十毫秒，放到后台线程。

## 主题替换

`MochiIonicon` 先问当前的 `MochiIconProvider`，认得这个名字就用它的：应用自己的图标集可以这样按 Ionicons 名字替换。

放在对话框的图标位（`MochiDialog(icon = …)`）里时，`MochiIonicon` 读 `LocalMochiIconDraw` 自动描绘出场：描边一段一段画出来，实心部分最后淡入，所有带图标的对话框一致，不用自己写动画。警告类提示用描边图标（例如 `"warning"`）效果最好；自己画的图标也可以读这个进度做同样的出场。

## 性能

图标数据是模块里的一份约 600KB 的文本资源（APK 里压缩后更小），第一次用到时读入，之后每个图标按（名字、画法、粗细）缓存。启动时可以在后台调一次 `MochiIonicons.preload(context)`，第一屏的图标就不用等读文件。

## 不要

- 引 Material Icons、手画 `Path`、用 emoji 或文字符号（✓ ✕ ›）当图标。
- 用「✨」sparkles 代表 AI 功能，用说明功能本身的图标。
- 同一屏描边、实心、直角混用来制造层次。
