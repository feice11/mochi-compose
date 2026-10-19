# 侧边栏

AI 应用（ChatGPT、Claude……）常用的那种侧边栏：对话列表放在左边，整页往右划就拉出来。在 `ui.feice.mochi.navigation` 里。

## iOS 26 的规范

- iPadOS / macOS 26 有正式的侧边栏：一块浮动的 Liquid Glass 面板，离屏幕边有间距、带圆角，内容延伸到它下面；iPad 上标签栏还能切换成侧边栏
- iPhone 上苹果的规定是侧边栏退化成导航栈或标签栏，没有官方的抽屉样式。AI 应用在 iPhone 上用的都是各家自己做的抽屉：整页右划打开、主页面被推开并压暗、点主页面关闭

所以这里两种都做了，用 `style` 切换。

## MochiSidebarLayout

```kotlin
val sidebar = rememberMochiSidebarState()
MochiSidebarLayout(
    state = sidebar,
    sidebar = {
        MochiSidebarHeader(query, { query = it }) {
            MochiSidebarHeaderButton(onClick = { newChat() }) { Icon(...) }
        }
        LazyColumn(Modifier.weight(1f).mochiFadeMask(MochiFadeEdges.vertical())) {
            item { MochiSidebarSectionHeader("今天") }
            items(chats) { c -> MochiSidebarItem(c.title, onClick = { open(c); sidebar.close() }, selected = c.id == current) }
        }
        MochiSidebarProfileRow("小果", onClick = { … }, subtitle = "设置")
    },
) {
    MochiNavHost(nav) { route -> … }   // 主页面，常常是这个应用自己的导航
}
```

- `MochiSidebarStyle.Push`（默认）：侧边栏盖在主页面上面，和主页面连在一起往右推：侧边栏（宽 = min(屏宽 × 0.82, 360dp)）从左边滑进来，主页面跟着往右移、右边露出一条，并且只压暗（浅色 5%），不加阴影、不变成圆角卡片（ChatGPT / Claude 的做法）。侧边栏底色默认是卡片色，和通常是分组底色的主页面拉开一层
- `MochiSidebarStyle.Floating`：iPadOS 26 的样子。面板在状态栏下方，离状态栏、屏幕底边和左边都是 8dp；顶角 28dp（搜索框圆头半径 18dp + 离边 10dp），和离面板上、左、右都是 10dp 的搜索框同心，顶上不空一截；底角等于屏幕底角圆角 − 8dp（和屏幕同心，不设上限，大圆角机型照样对齐），底部的行和面板底角同心。打开时默认藏起手势小白条（`hideNavigationBar = false` 关掉），从底边上滑还能临时叫出来；关闭后恢复。带柔和阴影，从左边滑进来；主页面不动只压暗
- 状态：`open()` / `close()` / `toggle()` 随处可调，`isOpen` 是目标状态，`progress` 是此刻的打开程度（0..1）；开合用和侧推同一条弹簧，途中随时掉头
- 侧边栏内容自带状态栏和导航栏安全区。主页面里贴底的东西（输入栏）请用 `WindowInsets.navigationBarsIgnoringVisibility`，小白条藏起来时不会跳
- 只改 graphicsLayer，主页面始终按整屏排版

### 手势和返回

- 在主页面任意位置往右划打开，1:1 跟手；松手过半或甩得够快就打开，否则收回
- 打开时：点主页面关闭（主页面里的东西收不到触摸；开始收起后不再拦，主页面的点击立刻生效）；任意位置横向拖动跟手开合；系统返回键先收起侧边栏（预测性返回跟手）
- 只接子元素没消费的拖动：横向滚动、滑块、左滑行照常
- **右划和返回谁优先**：所在页能返回时右划是返回。`swipeToOpen` 默认 `MochiSidebarDefaults.swipeToOpen()`：放在 `MochiNavHost` 的子页里时为 false，根页或不在导航里时为 true。侧边栏包着应用自己的 `MochiNavHost` 时，子页的整页右划返回先接到触摸，退到根页右划才打开侧边栏
- 侧边栏行上按下再拖动不会触发点按

## 行组件

| 组件 | 说明 |
|---|---|
| `MochiSidebarHeader(query, onQueryChange, placeholder, action)` | 顶部搜索框（`MochiSearchBar`）+ 行尾按钮 |
| `MochiSidebarHeaderButton(onClick)` | 头部圆形按钮，和搜索框同高（`MochiSearchBarDefaults.Height`，36dp） |
| `MochiSidebarSectionHeader(text)` | 「今天」「前 7 天」这类分组小标题，和行内文字左对齐 |
| `MochiSidebarItem(title, onClick, selected, leading, trailing)` | 一项：胶囊高亮（离侧边栏边 10dp，至少 44dp 高），按下有高亮；选中底色淡入淡出；标题单行省略 |
| `MochiSidebarProfileRow(name, onClick, subtitle, avatar)` | 底部个人资料行，默认头像取名字第一个字 |

颜色在 `MochiSidebarDefaults.colors()`：推开样式的底色、浮动面板底色、浮动样式压暗（`scrim`）、推开样式压暗（`pushScrim`）、选中 / 按下高亮、文字颜色。

演示见「导航 → 侧边栏」：一个模拟的 AI 聊天应用。它本身是从目录推进去的子页，模拟独立应用所以显式传了 `swipeToOpen = true`；点个人资料进设置子页可以切换两种样式、体验子页右划返回。
