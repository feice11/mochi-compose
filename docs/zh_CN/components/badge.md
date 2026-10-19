# 徽标、头像与仪表

## 徽标

```kotlin
MochiBadgedBox(badge = "3") { 图标 }   // "" = 小红点，null = 没有
MochiBadge(text)                       // 自己摆位置
```

- 角标中心落在图标右上角外侧一点（iOS 的位置），不占布局
- 出现时从 0 弹出，消失时缩回；缩放原点在左下角，看起来贴着图标长出来
- 内容变了会带果冻回弹「弹一下」；数字按计数器滚动，变大往上、变小往下
- 宽度只跟着计数器自己的逐位宽度动画走（一套动画），文字不会被裁；左边缘钉住，变长时只往右长，不会盖住图标
- [标签栏](tabbar.md)的 `MochiTabItem.badge` 用的就是它

## 头像

```kotlin
MochiAvatar("王")                        // 灰色渐变底 + 白色首字母（iOS 通讯录没照片时的样子）
MochiAvatar { Image(…) }                 // 任意内容，已经裁成圆
MochiAvatarGroup(names, max = 4)         // 头像组
```

- 头像组里，左边的头像压住右边的一截；每个头像外面描一圈底色（`borderColor`，应等于头像组所在的底色）
- 人数超过 `max` 时，最后一个位置显示「+N」

### 框式头像

iOS 26 的账户按钮：头像放在一个圆形按钮里，四周和按钮边缘留一圈缝（`MochiAvatarDefaults.FrameInset`，3dp），两者同心。

```kotlin
MochiAvatarButton(onClick = { … }) { Image(…, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
MochiFramedAvatar(size = 48.dp) { Image(…) }   // 不可点的版本，外框用填充色
```

- 可点的头像统一用 `MochiAvatarButton`：外框和顶栏按钮同一套颜色，按下时从指尖发光、轻轻形变；默认 44dp，和顶栏按钮一样大
- 列表行首、资料卡这类不可点的地方用 `MochiFramedAvatar`
- 不要自己画圈或描边来做头像框

## 仪表

```kotlin
MochiGauge(value, range = 0f..100f, label = "电量 %", minLabel = "0", maxLabel = "100")
MochiLinearGauge(used, range = 0f..128f, label = "存储空间", valueSuffix = " GB")
```

- `MochiGauge`：开口朝下的 270° 弧（基于 `MochiProgressRing` 的 `sweep`），弧内侧一圈刻度，中间显示数值和单位，开口两边标最小值和最大值
- `MochiLinearGauge`：圆头的容量条，上面一行是标题和当前值，下面两端标最小值和最大值
- 两种仪表的值变化时，弧或填充用弹簧过渡，数字按计数器滚动
