// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ui.feice.mochi.demo.catalog.catalog
import ui.feice.mochi.navigation.MochiNavHost
import ui.feice.mochi.navigation.MochiNavigator
import ui.feice.mochi.navigation.rememberMochiNavigator
import ui.feice.mochi.theme.ColorSchemeMode
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.ThemeController

/**
 * 新组件整体巡检（只在模拟器上跑）：依次打开 9 个新演示页，浅色 / 深色各走一遍脚本化交互，
 * 关键状态截图存到 files/mochi-tour（文件名 = 序号-模式-页面-状态），动画抽几帧连拍（burst-*）。
 * 断言不中断流程：全部走完后一次性报告所有失败。
 */
@RunWith(AndroidJUnit4::class)
class NewComponentsTour {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    private val inst = InstrumentationRegistry.getInstrumentation()
    private val failures = mutableListOf<String>()
    private var shotIndex = 0
    private var mode = "light"
    private var page = ""
    private var sceneId = 0
    private var navigator: MochiNavigator? = null
    private val density get() = inst.targetContext.resources.displayMetrics.density
    private val screenW get() = inst.targetContext.resources.displayMetrics.widthPixels
    private val screenH get() = inst.targetContext.resources.displayMetrics.heightPixels
    private val cjk get() = inst.targetContext.resources.configuration.locales[0].language in setOf("zh", "ja", "ko")

    @Test
    fun tour() {
        val dir = outDir()
        dir.listFiles()?.forEach { it.delete() }
        SystemClock.sleep(400)
        val modes = InstrumentationRegistry.getArguments().getString("tourModes") ?: "light,dark"
        for (dark in modes.split(",").map { it.trim() == "dark" }) {
            mode = if (dark) "dark" else "light"
            val only = InstrumentationRegistry.getArguments().getString("tourPages")?.split(",")?.map { it.trim() }
            for ((route, script) in scripts().filter { only == null || it.first in only }) {
                page = route
                try {
                    show(route, dark)
                    shot("top")
                    script()
                } catch (t: Throwable) {
                    failures += "[$mode/$route] 异常：${t::class.java.simpleName}: ${t.message}"
                    runCatching { shot("crash") }
                }
            }
        }
        File(dir, "report.txt").writeText(
            if (failures.isEmpty()) "ALL PASSED\n" else failures.joinToString("\n", postfix = "\n"),
        )
        assertTrue("巡检有 ${failures.size} 处失败：\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    private fun scripts(): List<Pair<String, () -> Unit>> = listOf(
        "picker" to ::pickerScript,
        "calendar" to ::calendarScript,
        "color" to ::colorScript,
        "popover" to ::popoverScript,
        "carousel" to ::carouselScript,
        "refresh" to ::refreshScript,
        "badge" to ::badgeScript,
        "list-edit" to ::listEditScript,
        "toolbar" to ::toolbarScript,
    )

    // ───────────── 每页的脚本 ─────────────

    private fun pickerScript() {
        val wheel = scrollTo("水果滚筒")
        val before = textWithPrefix("选中：")
        // 慢拖两格
        swipe(wheel.exactCenterX(), wheel.exactCenterY() + dp(36), wheel.exactCenterX(), wheel.exactCenterY() - dp(36), 500)
        settle(900)
        check({
            before != null && textWithPrefix("选中：").let { it != null && it != before }
        }, { "滚筒拖动后选中值没变：$before → ${textWithPrefix("选中：")}" })
        // 甩一下，连拍惯性
        swipe(wheel.exactCenterX(), wheel.exactCenterY() + dp(60), wheel.exactCenterX(), wheel.exactCenterY() - dp(60), 90, wait = false)
        burst("wheel-fling", 6, 70)
        settle(1500)
        shot("wheel-after-fling")

        // 1 月 31 日 → 把月份滚到 2 月：停稳后日应该回滚到 28 日
        val date = scrollTo("日期滚筒")
        val monthX = date.left + date.width() * if (cjk) (1.2f + 0.5f) / 3.2f else 0.7f / 3.4f
        swipe(monthX, date.exactCenterY() + dp(18), monthX, date.exactCenterY() - dp(18) - 2f, 400)
        SystemClock.sleep(350)
        shot("date-feb31-gray")
        settle(1600)
        check({ find("日期：2026-02-28") != null }, { "2 月 31 日没有回滚到 28 日，现在是 ${textWithPrefix("日期：")}" })
        shot("date-rolled-back")

        // 表单：紧凑日期弹出日历
        val row = scrollTo("重复")
        val dateRow = requireNode("日期", exact = true)
        tapAt(screenW - dp(70), centerOf(dateRow).second)
        burst("datefield-open", 5, 60)
        settle(700)
        shot("datefield-open")
        check({ nodes().any { label(it)?.let { s -> s.contains("2026年10月") || s.contains("October 2026") } == true } }, { "紧凑日期没有弹出月历" })
        tapAt(screenW / 2f, dp(120))
        settle(700)
        // 菜单式选择
        tapAt(row.exactCenterX(), row.exactCenterY())
        settle(700)
        shot("menu-picker-open")
        tap("每天")
        settle(700)
        check({ find("每天") != null }, { "菜单式选择没有选中「每天」" })
    }

    private fun calendarScript() {
        val cal = scrollTo("单选日历")
        val before = textWithPrefix("选中：")
        tapAt(cellX(cal, 3), cellY(cal, 2))
        burst("calendar-slide", 5, 50)
        settle(600)
        check({ textWithPrefix("选中：") != before }, { "点日历格子没有改选中日期（$before）" })
        shot("single-picked")
        // 点年月切到滚筒
        tapAt(cal.left + dp(60), cal.top + dp(22))
        settle(900)
        shot("month-wheels")
        tapAt(cal.left + dp(60), cal.top + dp(22))
        settle(900)

        val range = scrollTo("范围日历")
        tapAt(cellX(range, 1), cellY(range, 1))
        settle(400)
        check({ textWithPrefix("范围：")?.contains("再点终点") == true }, { "范围选择第一下没有变成「再点终点」：${textWithPrefix("范围：")}" })
        tapAt(cellX(range, 4), cellY(range, 3))
        settle(600)
        check({ textWithPrefix("范围：")?.contains("到") == true }, { "范围选择没有完成：${textWithPrefix("范围：")}" })
        shot("range-picked")
        // 左滑翻月
        swipe(range.right - dp(30), cellY(range, 2), range.left + dp(30), cellY(range, 2), 250)
        settle(700)
        shot("range-next-month")
    }

    private fun colorScript() {
        val row = requireNode("强调色", exact = true)
        val before = find("#0A84FF")
        check({ before != null }, { "初始颜色值不对" })
        tapAt(screenW - dp(46), centerOf(row).second)
        settle(900)
        shot("sheet-grid")
        val grid = requireNode("网格", exact = true)
        tapAt(screenW * 0.3f, rectOf(grid).bottom + dp(110))
        settle(500)
        shot("grid-picked")
        tap("色谱")
        settle(700)
        tapAt(screenW * 0.65f, rectOf(requireNode("色谱", exact = true)).bottom + dp(120))
        settle(500)
        shot("spectrum-picked")
        tap("滑块")
        settle(700)
        shot("sliders")
        pressBack()
        settle(900)
        check({ find("#0A84FF") == null }, { "选色后强调色的值没有变" })
        shot("after-close")
    }

    private fun popoverScript() {
        tap("显示详情")
        burst("popover-open", 6, 45)
        settle(700)
        shot("popover")
        check({ find("知道了") != null }, { "弹出层没有出现" })
        tap("知道了")
        settle(800)
        check({ find("知道了") == null }, { "弹出层没有关闭" })

        tap("新建")
        settle(800)
        shot("cover-popover")
        tap("扫描文稿")
        settle(800)
        check({ find("上次选择：扫描文稿") != null }, { "包裹式弹出层的选项没生效" })

        tap("删除…")
        settle(800)
        shot("confirm-anchored")
        tap("删除照片")
        settle(800)
        check({ find("上次选择：删除照片") != null }, { "从按钮弹出的确认没生效" })

        tap("无来源确认")
        burst("action-sheet", 5, 60)
        settle(800)
        shot("action-sheet")
        check({ find("取消") != null }, { "无来源确认对话框没有取消按钮" })
        tap("拷贝链接")
        settle(800)
        check({ find("上次选择：拷贝链接") != null }, { "无来源确认对话框选项没生效" })

        // 星星轻点是收藏、长按才展开提示；这里用程序主动显示
        scrollTo("重新显示提示")
        tap("重新显示提示")
        settle(900)
        shot("tip-popover")
        check({ find("点星星把它加到收藏，下次在首页就能找到。") != null }, { "提示气泡没有出现" })
        tapAt(screenW / 2f, dp(120))
        settle(800)
        tap("关闭提示")
        settle(900)
        shot("tip-card-closed")
        check({ find("长按照片可以预览，再往上拖出菜单。") == null }, { "提示卡没有收起" })
    }

    private fun carouselScript() {
        val c = requireRect("轮播演示")
        swipe(c.right - dp(40), c.exactCenterY(), c.left + dp(40), c.exactCenterY(), 220, wait = false)
        burst("carousel-swipe", 6, 60)
        settle(900)
        shot("carousel-page2")
        val pc = scrollTo("页面指示器演示")
        tapAt(pc.right - dp(20), pc.exactCenterY())
        settle(600)
        check({ find("第 5 页，共 15 页") != null }, { "点指示器右半边没有翻到下一页：${textWithPrefix("第 ", "共 15 页")}" })
        // 按住拖动快速翻页
        val pitch = dp(18)
        dragHold(pc.exactCenterX(), pc.exactCenterY(), pc.exactCenterX() + pitch * 3, pc.exactCenterY(), holdMs = 450, moveMs = 600) {
            shot("page-scrub")
        }
        settle(700)
        val text = textWithPrefix("第 ", "共 15 页") ?: ""
        val n = Regex("第 (\\d+) 页").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        check({ n >= 7 }, { "按住拖动没有快速翻页：$text" })
        shot("after-scrub")
    }

    private fun refreshScript() {
        val list = requireRect("刷新列表")
        dragHold(list.exactCenterX(), list.top + dp(40), list.exactCenterX(), list.top + dp(480), holdMs = 0, moveMs = 900) {
            SystemClock.sleep(150)
            shot("pulling")
        }
        burst("refresh-hold", 4, 120)
        settle(2200)
        check({ find("新笔记 1") != null }, { "下拉刷新没有触发" })
        shot("refreshed")
        tap("清空列表")
        settle(900)
        check({ find("没有笔记") != null }, { "清空后没有显示空状态" })
        shot("empty")
        tap("加载示例")
        settle(900)
        check({ find("晨会纪要") != null }, { "加载示例没有恢复列表" })
    }

    private fun badgeScript() {
        tap("+1")
        burst("badge-bump", 5, 50)
        tap("+1")
        settle(600)
        shot("badge-5")
        tap("红点")
        tap("清除")
        settle(700)
        shot("badge-cleared")
        repeat(3) { tap("加一人") }
        settle(500)
        shot("avatars")
        val slider = scrollTo("仪表数值")
        swipe(slider.left + slider.width() * 0.72f, slider.exactCenterY(), slider.left + slider.width() * 0.15f, slider.exactCenterY(), 500)
        settle(900)
        shot("gauges-low")
    }

    private fun listEditScript() {
        tap("显示与亮度")
        settle(800)
        check({ find("日落到日出") != null }, { "可展开行没有展开" })
        shot("disclosure-open")
        tap("编辑")
        settle(800)
        shot("editing")
        scrollTo("旅行计划")
        tap("购物清单")
        tap("旅行计划")
        settle(500)
        check({ find("已选 2 项") != null }, { "多选数量不对：${textWithPrefix("已选")}" })
        shot("selected-2")
        tap("删除所选")
        settle(800)
        check({ find("购物清单") == null }, { "批量删除没生效" })
        tap("完成")
        settle(600)
        val bar = scrollTo("索引")
        dragHold(bar.exactCenterX(), bar.top + dp(10), bar.exactCenterX(), bar.bottom - dp(10), holdMs = 120, moveMs = 900) {
            shot("index-bubble")
        }
        settle(600)
        check({ textWithPrefix("跳到：") != null }, { "字母索引没有回调" })
        shot("index-after")
    }

    private fun toolbarScript() {
        // 输入框的占位文字不进无障碍树：按分组小标题定位第一个输入框
        val section = scrollTo("键盘工具栏")
        tapAt(screenW / 2f, section.bottom + dp(48))
        settle(1400)
        shot("keyboard-toolbar")
        check({ find("完成") != null }, { "键盘工具栏没有出现" })
        tap("下一项")
        settle(400)
        tap("完成")
        settle(1200)
        check({ find("下一项") == null }, { "完成后键盘工具栏没有收起" })
        val hold = scrollTo("按住删除")
        // 中途松手：不执行
        dragHold(hold.exactCenterX(), hold.exactCenterY(), hold.exactCenterX(), hold.exactCenterY(), holdMs = 500, moveMs = 0) {
            shot("hold-half")
        }
        settle(700)
        check({ find("还没删除") != null }, { "中途松手也执行了" })
        // 按满：执行一次
        dragHold(hold.exactCenterX(), hold.exactCenterY(), hold.exactCenterX(), hold.exactCenterY(), holdMs = 1700, moveMs = 0) {}
        settle(900)
        check({ find("已删除 1 次") != null }, { "按满没有执行：${textWithPrefix("已删除") ?: textWithPrefix("还没")}" })
        shot("hold-done")
    }

    // ───────────── 日历格子坐标 ─────────────

    private fun cellX(cal: Rect, col: Int) = cal.left + cal.width() * (col + 0.5f) / 7f
    private fun cellY(cal: Rect, row: Int) = cal.top + dp(44) + dp(33) + dp(44) * (row + 0.5f)

    // ───────────── 场景与截图 ─────────────

    private fun show(route: String, dark: Boolean) {
        val id = ++sceneId
        rule.scenario.onActivity { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            activity.setContent {
                key(id) {
                    val controller = remember { ThemeController(if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light) }
                    MochiTheme(controller) {
                        M3Bridge {
                            val nav = rememberMochiNavigator(route)
                            SideEffect { navigator = nav }
                            MochiNavHost(nav) { r ->
                                val entry = catalog.first { it.id == r }
                                entry.page!!.invoke(entry)
                            }
                        }
                    }
                }
            }
        }
        inst.waitForIdleSync()
        SystemClock.sleep(700)
    }

    private fun outDir(): File = File(inst.targetContext.getExternalFilesDir(null), "mochi-tour").apply { mkdirs() }

    private fun shot(state: String) {
        val drawn = CountDownLatch(1)
        rule.scenario.onActivity { activity ->
            val view = activity.window.decorView
            view.viewTreeObserver.registerFrameCommitCallback { drawn.countDown() }
            view.invalidate()
        }
        drawn.await(2, TimeUnit.SECONDS)
        save("%03d-%s-%s-%s".format(++shotIndex, mode, page, state))
    }

    /** 连拍：不等帧提交，尽快抓 n 张（动画抽帧） */
    private fun burst(name: String, n: Int, intervalMs: Long) {
        val base = "%03d-%s-%s-burst-%s".format(++shotIndex, mode, page, name)
        repeat(n) { i ->
            save("$base-$i")
            SystemClock.sleep(intervalMs)
        }
    }

    private fun save(name: String) {
        val bitmap = inst.uiAutomation.takeScreenshot() ?: return
        File(outDir(), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    /** 条件在 2.5 秒内成立就算过（界面和无障碍树都有一点延迟）；不成立记一条失败并截图，流程继续 */
    private fun check(ok: () -> Boolean, message: () -> String) {
        val deadline = SystemClock.uptimeMillis() + 2500
        while (!runCatching(ok).getOrDefault(false)) {
            if (SystemClock.uptimeMillis() > deadline) {
                failures += "[$mode/$page] ${runCatching(message).getOrDefault("?")}"
                runCatching { shot("FAIL") }
                return
            }
            SystemClock.sleep(120)
        }
    }

    // ───────────── 查找 ─────────────

    private fun nodes(): List<AccessibilityNodeInfo> {
        // 等无障碍事件发完，节点的文字和位置才是最新的（滚动后不等会拿到旧坐标）
        runCatching { inst.uiAutomation.waitForIdle(120, 2000) }
        val out = ArrayList<AccessibilityNodeInfo>()
        fun walk(n: AccessibilityNodeInfo) {
            if (n.isVisibleToUser) out += n
            repeat(n.childCount) { i -> n.getChild(i)?.let(::walk) }
        }
        inst.uiAutomation.rootInActiveWindow?.let(::walk)
        // 浮层（弹出层、面板）挂在同一个窗口的 DecorView 上，rootInActiveWindow 已经包含
        return out
    }

    private fun label(n: AccessibilityNodeInfo): String? = n.text?.toString() ?: n.contentDescription?.toString()

    private fun find(text: String): AccessibilityNodeInfo? = nodes().firstOrNull { n ->
        n.text?.toString() == text || n.contentDescription?.toString() == text
    }

    private fun textWithPrefix(prefix: String, suffix: String = ""): String? =
        nodes().mapNotNull { label(it) }.firstOrNull { it.startsWith(prefix) && it.endsWith(suffix) }

    private fun requireNode(text: String, exact: Boolean = true): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 2500
        while (SystemClock.uptimeMillis() < deadline) {
            val n = if (exact) find(text) else nodes().firstOrNull { label(it)?.contains(text) == true }
            if (n != null) return n
            SystemClock.sleep(50)
        }
        throw AssertionError("找不到：$text")
    }

    private fun rectOf(n: AccessibilityNodeInfo): Rect = Rect().also { n.getBoundsInScreen(it) }
    private fun requireRect(text: String): Rect = rectOf(requireNode(text))
    private fun centerOf(n: AccessibilityNodeInfo): Pair<Float, Float> = rectOf(n).let { it.exactCenterX() to it.exactCenterY() }

    /** 页面往上滚，直到 [text] 出现在屏幕中间一段 */
    private fun scrollTo(text: String): Rect {
        repeat(14) {
            val n = find(text)
            if (n != null) {
                val r = rectOf(n)
                if (r.top > screenH * 0.12f && r.bottom < screenH * 0.85f) return r
                if (r.top <= screenH * 0.12f) {
                    pageScroll(screenH * 0.35f, screenH * 0.6f)
                    settle(500)
                    return@repeat
                }
            }
            pageScroll(screenH * 0.72f, screenH * 0.42f)
            settle(500)
        }
        return requireRect(text)
    }

    // ───────────── 输入 ─────────────

    private fun dp(v: Int): Float = v * density
    private fun dp(v: Float): Float = v * density

    private fun inject(action: Int, down: Long, x: Float, y: Float) {
        val e = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
        try {
            inst.uiAutomation.injectInputEvent(e, true)
        } finally {
            e.recycle()
        }
    }

    private fun tapAt(x: Float, y: Float) {
        val t = SystemClock.uptimeMillis()
        inject(MotionEvent.ACTION_DOWN, t, x, y)
        SystemClock.sleep(40)
        inject(MotionEvent.ACTION_UP, t, x, y)
        SystemClock.sleep(120)
    }

    private fun tap(text: String) {
        val (x, y) = centerOf(requireNode(text))
        tapAt(x, y)
        settle(350)
    }

    private fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long, wait: Boolean = true) {
        val t = SystemClock.uptimeMillis()
        inject(MotionEvent.ACTION_DOWN, t, x1, y1)
        val steps = (durationMs / 12).coerceAtLeast(3)
        for (i in 1..steps) {
            val f = i / steps.toFloat()
            SystemClock.sleep(durationMs / steps)
            inject(MotionEvent.ACTION_MOVE, t, x1 + (x2 - x1) * f, y1 + (y2 - y1) * f)
        }
        inject(MotionEvent.ACTION_UP, t, x2, y2)
        if (wait) SystemClock.sleep(80)
    }

    /** 按下 → 停 [holdMs] → 用 [moveMs] 移到终点 → 执行 [whileDown]（手指还按着）→ 松手 */
    private fun dragHold(x1: Float, y1: Float, x2: Float, y2: Float, holdMs: Long, moveMs: Long, whileDown: () -> Unit) {
        val t = SystemClock.uptimeMillis()
        inject(MotionEvent.ACTION_DOWN, t, x1, y1)
        var held = 0L
        while (held < holdMs) {
            SystemClock.sleep(50)
            held += 50
            inject(MotionEvent.ACTION_MOVE, t, x1, y1)
        }
        if (moveMs > 0) {
            val steps = (moveMs / 16).coerceAtLeast(3)
            for (i in 1..steps) {
                val f = i / steps.toFloat()
                SystemClock.sleep(moveMs / steps)
                inject(MotionEvent.ACTION_MOVE, t, x1 + (x2 - x1) * f, y1 + (y2 - y1) * f)
            }
        }
        whileDown()
        inject(MotionEvent.ACTION_UP, t, x2, y2)
        SystemClock.sleep(80)
    }

    /** 页面滚动：在右边缘的页边距里拖（不碰卡片里的滚筒 / 日历），停住再松手，免得留下惯性让下一次点按只是接住滚动 */
    private fun pageScroll(fromY: Float, toY: Float) {
        val x = screenW - dp(6)
        dragHold(x, fromY, x, toY, holdMs = 0, moveMs = 450) { SystemClock.sleep(150) }
        settle(300)
    }

    private fun pressBack() {
        rule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        SystemClock.sleep(400)
    }

    private fun settle(ms: Long) {
        SystemClock.sleep(ms)
        inst.waitForIdleSync()
    }
}
