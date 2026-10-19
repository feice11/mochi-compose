# MochiSwitch

开关：iOS 26 的样子和手感（不做玻璃）。64×28 的轨道 + 38×24 的白色胶囊拇指，轨道和拇指都是 G2 连续曲率胶囊。

旋钮和未选中轨道默认读取 `MochiDefaults.controlThumb()` / `controlTrack()`；未提供 `LocalMochiControlPalette` 时保持原先白色旋钮与 `fill()` 底槽，可选外观可统一覆盖，显式传入 `colors` 仍优先。

- **点一下**：拇指带速度滑过去，沿运动方向拉长、到位压扁回弹；轨道颜色随拇指位置连续过渡。
- **按住**：拇指放大到溢出轨道，投影变深并发光。
- **拖动**：拇指跟手，拖出两端被橡皮筋拉住，过中线轻震；松手按位置决定开还是关。
- 状态改变时有确认触感。

## 用法

```kotlin
var on by remember { mutableStateOf(false) }
MochiSwitch(checked = on, onCheckedChange = { on = it })
```

## 参数

| 参数 | 说明 |
|---|---|
| checked / onCheckedChange | 状态和回调；onCheckedChange 给 null 就只显示、不响应 |
| enabled | 关掉后变淡、不响应 |
| colors | `MochiSwitchDefaults.colors(checkedTrack, uncheckedTrack, thumb, thumbShadow)`；开启颜色默认是主题主色，想要 iOS 绿就传 `MochiSwitchDefaults.IosGreen` |
| gel | 果冻拉伸强度，0 = 关 |
| hapticProfile | 触感 |

点一下切换时，`IosLike` 配置下也会轻震一下（iOS 的开关有触感）；拖过中线时已经震过，松手不再重复。

## 勾选圈 MochiCheckbox

iOS 式圆形勾选圈，用在「不再提示」、多选这类地方（和列表编辑模式的勾选圈同一个画法）：没勾是一圈细线；勾上时实心主色圆从小长大，对勾一笔画出来，轻震一下。

```kotlin
MochiCheckbox(checked, onCheckedChange = { checked = it })
```

| 参数 | 说明 |
|---|---|
| checked / onCheckedChange | 状态和回调；给 null 只显示，点击交给整行（文字和勾选圈一起可点） |
| enabled | 关掉后变淡、不响应 |
| colors | `MochiCheckboxDefaults.colors(checked, checkmark, ring)`，默认主色 / onPrimary / 半透明 onSurfaceVariant |
