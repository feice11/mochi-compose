# 日历

月历选日期（iOS UIDatePicker 的 `.inline` / `.graphical`）。可以选一天，也可以选一段。

```kotlin
var s by remember { mutableStateOf<MochiDateSelection>(MochiDateSelection.Single(MochiDate.today())) }
MochiCalendar(s, { s = it }, min = MochiDate(2020, 1, 1), max = MochiDate(2030, 12, 31))

// 选一段：先点起点再点终点
var r by remember { mutableStateOf<MochiDateSelection>(MochiDateSelection.Range(null, null)) }
MochiCalendar(r, { r = it })
```

## 交互

- 左右滑动或点右上角箭头翻月
- 点顶上的「2026年10月 ›」：日历区域原地换成年月滚筒（模糊替换），箭头转到朝下；再点换回来
- 选一天时，实心圆用弹簧滑到新日期；从别的月份选进来时，圆从 0 放大
- 选范围时，起止日期是实心圆，中间按周连成胶囊，换行处贴到格子边；点在起点之前会换成新的起点
- 今天的数字用强调色；超出 `min` / `max` 的日子变淡、点不了；选中时轻震

## 参数

| 参数 | 说明 |
|---|---|
| `firstDayOfWeek` | 每周从哪天开始，1 = 周一 … 7 = 周日；默认跟随系统区域（`MochiCalendarDefaults.firstDayOfWeek()`） |
| `colors` | `MochiCalendarDefaults.colors(accent, onAccent, rangeFill, content, secondaryContent)` |
| `MochiCalendarDefaults.CellHeight` | 每格高度，44dp；日历区域固定 6 行高，翻月时不跳 |

放在表单行尾的紧凑写法见[滚筒与时间日期](picker.md)的 `MochiDateField`。
