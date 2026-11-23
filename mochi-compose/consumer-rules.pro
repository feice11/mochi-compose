# Compose 和 Capsule 自带 consumer rules。
# 按压反馈要知道自己是否在滚动容器里（在里面时延迟按下、滚动接手时收回），
# 按名字读取 foundation 滚动容器节点的 TraverseKey（见 gesture/MochiPressRhythm.kt），混淆时保留它，
# 并禁止 R8 横向类合并将其空伴生类与其他 TraverseKey 合并（traverseAncestors 同时比较 javaClass）。
-keepnames class androidx.compose.foundation.gestures.ScrollableContainerNode
-keepclassmembers class androidx.compose.foundation.gestures.ScrollableContainerNode {
    public static ** TraverseKey;
}
-keep,allowobfuscation,allowshrinking class androidx.compose.foundation.gestures.ScrollableContainerNode$TraverseKey
