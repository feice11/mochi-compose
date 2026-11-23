# Android version support

All Mochi modules (`mochi-compose`, `mochi-icons`, `mochi-ios`, `mochi-nothing`) target `minSdk = 23` (Android 6.0) with `compileSdk = 36`. Every component, G2 continuous-curvature shape, spring animation, gesture, navigation host, sheet, and theme token maintains 100% functional parity from API 23 upward. Version-gated graphics, color, and haptic capabilities activate automatically on newer releases and degrade cleanly on older releases without requiring `Build.VERSION.SDK_INT` checks in application code.

## Platform capability matrix

| Android Release | Active Platform Capabilities | Fallback on Earlier Releases |
|---|---|---|
| **Android 15+** (API 35+) | `MochiHdr` requests up to `3×` HDR headroom via `Window.setDesiredHdrHeadroom`, plus all capabilities from API 34 and below | API 34 relies directly on `COLOR_MODE_HDR` and the display's reported `hdrSdrRatio` |
| **Android 14** (API 34) | Experimental HDR UI highlights (`MochiHdr`: `COLOR_MODE_HDR` window mode and live `Display.hdrSdrRatio` tracking on HDR displays); system-level predictive back animations alongside in-app back progress; `SEGMENT_TICK` and `GESTURE_THRESHOLD_ACTIVATE` haptic constants | API 33 and below (or non-HDR displays) render entirely in SDR, identical to `MochiHdr.enabled = false` |
| **Android 13** (API 33) | Full AGSL `RuntimeShader` pipeline: three-pass `MochiGlass` liquid glass with smooth union, metaball neck and color-blending shaders, analytic dithered rounded-rect Gaussian shadow shader, single-pass SDF cutout shader for `MochiThemeRevealHost`, top-bar scroll-edge blur (`MochiScrollEdge` / `mochiScrollEdgeBlur` uniform and variable blur), four-edge progressive blur (`mochiProgressiveBlur`), single-pass animated `MochiEdgeGlow`, `MenuLens` displacement, and `MochiIntensitySlider` field shader; `VibrationEffect.Composition` touch primitives (`USAGE_TOUCH`); in-app predictive back gestures | See API 31–32 fallbacks below |
| **Android 12–12L** (API 31–32) | Hardware `RenderEffect` blur: content blur transitions (`MochiBlurCrossfade`, `MochiBlurText`, `MochiCounterText`), `MochiThemeRevealState.crossfade`, shared toolbar transition blur, `MochiGlassGroup` item content blur, and `MochiVibrant` backdrop blur; `MochiGlass` applies system blur plus the light/dark face color matrix; Monet wallpaper seed colors (`ColorSchemeMode.Monet*`); physical display corner radii (`WindowInsets.getRoundedCorner`) | `MochiGlass` omits refraction and smooth-union necks; top-bar `MochiScrollEdge` falls back to `SolidFade` (opaque fade) and `mochiProgressiveBlur` draws unblurred; `MochiEdgeGlow` draws a rounded sweep-gradient stroke; metaballs draw crisp G2 paths; `MochiThemeRevealHost` falls back to a G2 `Path` plus `BlurMaskFilter` cutout; rounded-rect shadows use `Paint.setShadowLayer`; haptics use system feedback constants |
| **Android 9–11** (API 28–30) | Hardware-accelerated arbitrary-path and rounded-rect soft shadows via `Paint.setShadowLayer` (API 28+) and transparent sheet navigation bar dividers (API 28+); `MochiVibrant` offscreen color-matrix compositing, linear extended-sRGB smooth gradients (`MochiSmoothGradient`), and disabling enforced system bar contrast scrims (`mochiEnableEdgeToEdge`, API 29+); `HapticFeedbackConstants.CONFIRM` (API 30+) | `MochiGlass` draws its solid `fallbackColor`; content blur transitions animate scale, slide, and opacity without blur; `Monet*` modes use `ThemeController.keyColor`; without physical screen corners, page and sheet transitions use built-in corner radii (`28dp` / `32dp` / `24dp`) |
| **Android 6.0–8.1** (API 23–27) | 100% functional parity across all components, G2 shapes, springs, and gestures; Android 8.0+ (API 26+) adds `PixelCopy` window capture for `MochiThemeRevealHost`, wide-gamut windows (`COLOR_MODE_WIDE_COLOR_GAMUT`), and `java.time.LocalDate` / `LocalTime` converters on `MochiDate` and `MochiTime` | Below API 28, `drawMochiShadow` renders 4 concentric translucent capsule rings; below API 29, `MochiVibrant` draws normal foreground colors; below API 26, `MochiThemeRevealHost` switches themes immediately without `PixelCopy`, and `MochiDate` / `MochiTime` operate standalone without `java.time`; haptics use `LONG_PRESS`, `CLOCK_TICK`, and `VIRTUAL_KEY` |

## Subsystem fallback details

### Materials, shaders, and blur (`MochiGlass`, `MochiScrollEdge`, `MochiGraphics`)

- **Liquid glass (`MochiGlass`)**: Android 13+ (API 33+) runs the full three-pass AGSL pipeline (supercircle SDF, polynomial smooth union, 13-tap blur pyramid, inner/outer refraction, edge bleed, stroke and diffuse highlights, tinting, and press glow). Android 12 (API 31–32) applies system `RenderEffect` blur plus the recipe's face color matrix (`MochiGlassBlobs` draws separate pieces). Android 11 and below—or any version without a `MochiBackdrop`—draws the solid `MochiGlass.fallbackColor(dark)`.
- **Scroll-edge and content blur**: Content transitions (`MochiBlurCrossfade`, `MochiBlurVisibility`), `MochiVibrant` backdrop blur, and theme `crossfade` use hardware `RenderEffect` blur starting on Android 12 (API 31+, pre-warmed in the background by `MochiBlurWarmup`). Top-bar scroll-edge blur (`mochiScrollEdgeBlur` for `Frosted` uniform blur and `GradientBlur` variable blur) and four-edge progressive blur (`mochiProgressiveBlur`) run on API 33+; on Android 12 and below, top bars fall back automatically to `MochiTopBarStyle.SolidFade`.
- **Graphics tier interaction (`MochiGraphics`)**: Even on Android 13+, setting `MochiGraphics.level = MochiGraphicsLevel.Mid` disables AGSL shaders (routing liquid glass, metaballs, and `MochiEdgeGlow` to their solid or stroke paths) and disables reveal feathering while keeping system blur and soft shadows to reduce power draw. Capabilities unsupported by the OS remain off regardless of tier.

### Shadows and high dynamic range (`MochiShadows`, `MochiHdr`)

- **Three-tier shadow rendering (`drawMochiShadow`)**:
  - **API 33+**: Rounded-rectangle and capsule shadows evaluate an analytic Gaussian convolution in a single AGSL pass with `±0.5/255` dithering to eliminate 8-bit banding and avoid per-frame mask texture allocations during animations.
  - **API 28–32** (and arbitrary `Path` overloads on API 33+): Uses hardware-accelerated `Paint.setShadowLayer` for both ambient and contact layers.
  - **API 23–27**: Draws 4 outward-fading concentric capsule rings around the shape bounds so switch thumbs, floating bars, and menus retain clear elevation cues.
- **Wide color gamut and HDR (`MochiHdr`)**: API 26+ enables `COLOR_MODE_WIDE_COLOR_GAMUT` on wide-gamut displays; API 29+ interpolates smooth gradient stops in `LinearExtendedSrgb` to avoid 8-bit banding; API 34+ activates linear-light HDR highlights when `MochiHdr.enabled` is true on HDR displays, and API 35+ requests up to `3×` headroom via `setDesiredHdrHeadroom`.

### Screen corners, back gestures, and haptics (`MochiScreenCorners`, `MochiHaptics`)

- **Physical screen corners**: Android 12+ (API 31+) reads hardware corner radii via `WindowInsets.getRoundedCorner` (updating on rotation) so page transitions, floating sheets, `MochiThemeRevealHost` masks, and `MochiEdgeGlow` match the device bezel with G2 continuity. API 30 and below return `MochiScreenCorners.None`, and navigation and sheet transitions use their built-in fallback radii.
- **Haptic feedback (`MochiHaptics`)**: Android 13+ (API 33+) plays `VibrationEffect.Composition` primitives (`PRIMITIVE_TICK`, `PRIMITIVE_LOW_TICK`, `PRIMITIVE_CLICK` with `VibrationAttributes.USAGE_TOUCH`, respecting the user's system touch-feedback intensity) whenever `VIBRATE` permission is granted and the actuator supports the primitive. Without permission, actuator support, or API 33, haptics fall back automatically to `View.performHapticFeedback` (`SEGMENT_TICK` / `GESTURE_THRESHOLD_ACTIVATE` on API 34+, `CONFIRM` on API 30+, and `LONG_PRESS` / `CLOCK_TICK` / `VIRTUAL_KEY` on API 23–29).

## Static API verification

All version-gated platform calls are isolated behind `SDK_INT` checks and `@RequiresApi` annotations, passing Android Lint's `NewApi` check cleanly at `minSdk = 23`. After adding or modifying code that calls platform APIs, verify with:

```bash
./gradlew :mochi-compose:lintRelease
```
