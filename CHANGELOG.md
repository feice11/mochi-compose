# Changelog / 更新日志

## Unreleased / 未发布

### Renamed from GooMenu / 从 GooMenu 改名

- Package `io.github.goomenu` → `ui.feice.mochi`; modules `goo-ui` → `mochi-compose`, `goo-nothing` → `mochi-nothing`.
  包名 `io.github.goomenu` → `ui.feice.mochi`；模块 `goo-ui` → `mochi-compose`、`goo-nothing` → `mochi-nothing`。
- Every `Goo` / `goo` prefix became `Mochi` / `mochi` (e.g. `GooTheme` → `MochiTheme`, `Modifier.gooPressGlow` → `Modifier.mochiPressGlow`).
  所有 `Goo` / `goo` 前缀改成 `Mochi` / `mochi`（如 `GooTheme` → `MochiTheme`、`Modifier.gooPressGlow` → `Modifier.mochiPressGlow`）。
- The dropdown `gooRadius` became `fuseRadius`, matching other components.
  下拉菜单的 `gooRadius` 改成和其他组件一致的 `fuseRadius`。
- No aliases for the old names are kept; replace them wholesale when upgrading.
  没有保留旧名字的别名，升级时整体替换即可。
