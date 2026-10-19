# Android version support

minSdk is 23 (Android 6.0). Everything works on every version; newer systems get the full effect, older ones fall back as below.

| Android | What you get | Fallback |
|---|---|---|
| 13+ (API 33) | Everything: metaball shader, composed vibration primitives. Predictive back with in-app animations (system animation from 14) | — |
| 12 – 12L (31–32) | Physical screen corners, dynamic color, vibrator primitives | Metaball uses the non-shader path |
| 9 – 11 (28–30) | Thumb shadows via `setShadowLayer` | No screen corners: transitions use a 28dp corner. No dynamic color: fixed seed color. Haptics use system feedback constants |
| 6 – 8.1 (23–27) | All components and interactions | Thumb shadow drawn as stacked soft rings. Back is the back key only. Haptics fall back to the basic constants. The example app uses a round layered icon instead of an adaptive icon |

Lint's `NewApi` check runs clean at minSdk 23. Run `./gradlew :mochi-compose:lintRelease` after adding code that touches platform APIs.
