# 选择芯片

筛选条件、标签、一排短选项。按压和按钮是同一套（放大、跟手、高光），选中时底色和文字颜色渐变。

```kotlin
// 单选：靠底色区分，放不下时横向滑动，还能滑的那一边自动渐隐
MochiChipGroup {
    options.forEach { MochiChip(it, selected = it == current, onClick = { current = it }) }
}

// 放在已有 16dp 左右边距的容器里（LazyColumn 的 contentPadding 等）：滑动区域伸到屏幕边，第一个芯片仍和卡片对齐
MochiChipGroup(contentPadding = PaddingValues(0.dp), bleed = 16.dp) { … }

// 多选：选中时文字前面一笔画出对勾，芯片随之变宽；选项不多时折行排满
MochiChipGroup(wrap = true) {
    tags.forEach { MochiChip(it, selected = it in picked, onClick = { toggle(it) }, showCheck = true) }
}

// 已生效的条件：整个芯片点了就移除，后面的 ✕ 只是提示
MochiRemovableChip("价格 < 100", onRemove = { … })

// 文字前的小图标（16–18dp）
MochiChip("未读", selected, onClick, leadingIcon = { MochiIonicon("mail-unread", size = 17.dp) })
```

## 和其他选择控件怎么分

| 情况 | 用 |
|---|---|
| 2～5 个短选项，占满一行，必须选一个 | `MochiSegmentedControl` |
| 筛选、标签、选项多或长度不一 | `MochiChip` + `MochiChipGroup` |
| 选项要进下一页看说明再选 | `MochiListPickerRow` |
| 开 / 关 | 行尾 `MochiSwitch` |

## 细节

- 高度 34dp，文字 subhead 中等字重；单选和多选的字重一样，切换时文字宽度不跳。
- 没选中是半透明灰色填充，放在灰色页面底和白卡片上都看得清；选中是主色实底。
- `MochiChipGroup` 横排时两端留 `contentPadding`（默认 16dp，和卡片外边对齐），滑到头有弹性。
- 横排两端自带渐隐遮罩：还能往哪边滑，哪边就渐隐（iOS 外观同时渐进模糊）；滑到头那一边是实的。不用再自己套 `mochiScrollFadeMask`。
- 放在已经有左右边距的容器里时，传 `contentPadding = PaddingValues(0.dp)` 和 `bleed` = 容器的边距。只传 `PaddingValues(0.dp)` 的话，滑动区域停在容器边距以内，渐隐落在卡片边上，屏幕边和卡片边之间空着一条。
- 遮罩是按自己矩形裁切的离屏图层，组件上下已经给按压放大留了余量，报给父布局的高度不变。
- 读屏：`MochiChip` 带选中状态；`MochiRemovableChip` 读作「移除 + 文字」，可以用 `removeDescription` 换成别的语言。

## 不要

- 用 `MochiButton` 改 `minHeight` / padding 拼芯片：没有选中状态，选中切换是瞬切。
- 按钮文字里写「×」来做可移除的条件。
- 一组芯片里有的带对勾有的不带。
