# Compose / AndroidX 自带 consumer rules，这里不需要额外规则。

# Same-process release comparisons use these internal controls by name.
# Keep the controls callable, while allowing optimization of their implementations.
-keep,allowoptimization class ui.feice.mochi.effect.glass.*Kt {
    public static *** getGlass*(...);
    public static void setGlass*(...);
}
-keep,allowoptimization class ui.feice.mochi.basic.MochiGlassTabBarKt {
    public static *** getGlass*(...);
    public static void setGlass*(...);
}
-keep,allowoptimization class ui.feice.mochi.demo.onboarding.GlassTintPickerKt {
    public static *** getGlassPreview*(...);
    public static void setGlassPreview*(...);
    public static *** resetGlassPreview*ForBenchmark(...);
}
