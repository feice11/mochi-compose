// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import java.lang.ref.WeakReference

/**
 * 震动策略。
 * - IosLike（默认）：长按打开（一下清脆的 click）、手指滑过新选项（tick）、确认一个选择（开关拨动、松手选中，轻 click）。
 *   轻点打开、橡皮筋不震，学的是 iOS「少而准」的做法。
 * - Rich：在上面基础上，轻点打开也给一下轻的。
 * - Off：全关。
 */
public enum class MochiHapticProfile { IosLike, Rich, Off }

/**
 * 清脆感来自「组合原语」(VibrationEffect.Composition)：厂商调好的短脉冲，没有马达拖尾。
 * 原语只在 API 33+ 用，因为要配 USAGE_TOUCH，才会跟随系统「触摸反馈」强度设置；
 * 设备不支持原语、没声明 VIBRATE 权限、或者更老的系统，就退回 View.performHapticFeedback。
 * 不用 createOneShot / vibrate(ms)：官方明确说这种整段马达驱动会发闷发麻。
 */
@Stable
public class MochiHaptics internal constructor(private val view: View, private val profile: MochiHapticProfile) {
    private val caps: VibratorCaps = VibratorCaps.of(view.context)
    private var lastTickAt = 0L

    /** 轻点打开：IosLike 不震 */
    public fun tapOpen() {
        if (profile != MochiHapticProfile.Rich) return
        if (!primitive(HapticsApi30.PRIMITIVE_TICK, 0.5f, caps.tickOk)) perform(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /** 长按打开：一下 click，像 iOS 的 Haptic Touch */
    public fun longPress() {
        if (profile == MochiHapticProfile.Off) return
        if (!primitive(HapticsApi30.PRIMITIVE_CLICK, 0.8f, caps.clickOk)) perform(HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * 手指滑过新选项：一下短促的 tick，24ms 内只震一次，快划时不会连成一串嗡嗡。
     * 用 TICK 原语（不用 LOW_TICK：部分机型上 LOW_TICK 轻到几乎感觉不到，像是丢了）。
     */
    public fun tick() {
        if (profile == MochiHapticProfile.Off) return
        val now = SystemClock.uptimeMillis()
        if (now - lastTickAt < TickGapMillis) return
        lastTickAt = now
        val played = primitive(HapticsApi30.PRIMITIVE_TICK, 0.7f, caps.tickOk) ||
            primitive(HapticsApi31.PRIMITIVE_LOW_TICK, 1f, caps.lowTickOk)
        if (!played) {
            perform(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.SEGMENT_TICK
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                },
            )
        }
    }

    /** 确认一个选择（开关拨动、松手选中、选中一个选项）：一下轻 click。IosLike 和 Rich 都震 */
    public fun confirm() {
        if (profile == MochiHapticProfile.Off) return
        if (!primitive(HapticsApi30.PRIMITIVE_CLICK, 0.55f, caps.clickOk)) {
            perform(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.VIRTUAL_KEY
                },
            )
        }
    }

    /**
     * 越过一个「松手就会生效」的阈值：左滑过头会直接删除、下拉面板过头会关闭。
     * 来回越过时每次都震，告诉用户「现在松手会发生什么变了」（iOS 的做法）
     */
    public fun threshold() {
        if (profile == MochiHapticProfile.Off) return
        if (!primitive(HapticsApi30.PRIMITIVE_CLICK, 0.6f, caps.clickOk)) {
            perform(
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.CONFIRM
                    else -> HapticFeedbackConstants.VIRTUAL_KEY
                },
            )
        }
    }

    private fun primitive(id: Int, scale: Float, supported: Boolean): Boolean {
        if (!supported || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val v = caps.vibrator ?: return false
        return HapticsApi33.vibratePrimitive(v, id, scale)
    }

    private fun perform(constant: Int) {
        // 不看 View 自己的 isHapticFeedbackEnabled（有的容器会关掉它，震动就悄悄丢了）；系统的触摸反馈总开关照样生效
        @Suppress("DEPRECATION")
        view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
    }
}

private const val TickGapMillis = 24L

/** 进程级缓存马达能力与权限探测结果，避免滚动列表中每个控件构造 MochiHaptics 时重复触发 Binder IPC。 */
private class VibratorCaps(
    val vibrator: Vibrator?,
    val canCompose: Boolean,
    val lowTickOk: Boolean,
    val tickOk: Boolean,
    val clickOk: Boolean,
) {
    companion object {
        @Volatile
        private var cachedApp: WeakReference<Context>? = null

        @Volatile
        private var cachedCaps: VibratorCaps? = null

        fun of(context: Context): VibratorCaps {
            val app = context.applicationContext ?: context
            val existing = cachedCaps
            if (existing != null && cachedApp?.get() === app) return existing
            return synchronized(this) {
                val again = cachedCaps
                if (again != null && cachedApp?.get() === app) {
                    again
                } else {
                    query(app).also {
                        cachedApp = WeakReference(app)
                        cachedCaps = it
                    }
                }
            }
        }

        private fun query(context: Context): VibratorCaps {
            val vibrator = runCatching { context.vibratorOrNull() }.getOrNull()
            val canCompose = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                runCatching { vibrator?.hasVibrator() == true }.getOrDefault(false) &&
                runCatching {
                    context.checkSelfPermission(Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED
                }.getOrDefault(false)
            val lowTickOk = canCompose && HapticsApi31.supportsLowTick(vibrator)
            val tickOk = canCompose && HapticsApi30.supports(vibrator, HapticsApi30.PRIMITIVE_TICK)
            val clickOk = canCompose && HapticsApi30.supports(vibrator, HapticsApi30.PRIMITIVE_CLICK)
            return VibratorCaps(vibrator, canCompose, lowTickOk, tickOk, clickOk)
        }
    }
}

private fun Context.vibratorOrNull(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    HapticsApi31.defaultVibrator(this)
} else {
    @Suppress("DEPRECATION")
    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
}

@RequiresApi(30)
private object HapticsApi30 {
    const val PRIMITIVE_CLICK = VibrationEffect.Composition.PRIMITIVE_CLICK
    const val PRIMITIVE_TICK = VibrationEffect.Composition.PRIMITIVE_TICK

    fun supports(vibrator: Vibrator?, primitive: Int): Boolean {
        if (vibrator == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return try {
            vibrator.areAllPrimitivesSupported(primitive)
        } catch (_: RuntimeException) {
            false
        }
    }
}

@RequiresApi(31)
private object HapticsApi31 {
    const val PRIMITIVE_LOW_TICK = VibrationEffect.Composition.PRIMITIVE_LOW_TICK

    fun defaultVibrator(context: Context): Vibrator? = context.getSystemService(VibratorManager::class.java)?.defaultVibrator

    fun supportsLowTick(vibrator: Vibrator?): Boolean = HapticsApi30.supports(vibrator, PRIMITIVE_LOW_TICK)
}

@RequiresApi(33)
private object HapticsApi33 {
    private val TouchUsage: VibrationAttributes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    }

    fun vibratePrimitive(vibrator: Vibrator, id: Int, scale: Float): Boolean = try {
        val effect = VibrationEffect.startComposition().addPrimitive(id, scale).compose()
        vibrator.vibrate(effect, TouchUsage)
        true
    } catch (_: RuntimeException) {
        false
    }
}

/**
 * 拿一个按库里统一规则震动的 [MochiHaptics]，自己的组件也能用同一套：
 * - tick：滑过新选项、刻度、过中线
 * - longPress：长按呼出（菜单、预览）
 * - threshold：越过「松手就生效」的阈值（左滑删除、下拉关闭）
 * - confirm：确认一个选择（开关、松手选中）
 * - tapOpen：只有 Rich 档才震
 */
@Composable
public fun rememberMochiHaptics(profile: MochiHapticProfile = MochiHapticProfile.IosLike): MochiHaptics {
    val view = LocalView.current
    return remember(view, profile) { MochiHaptics(view, profile) }
}
