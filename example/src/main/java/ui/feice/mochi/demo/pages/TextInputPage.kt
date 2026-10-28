// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiCodeField
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.MochiDialogTextField
import ui.feice.mochi.basic.MochiInlineTextField
import ui.feice.mochi.basic.MochiListFormRow
import ui.feice.mochi.basic.MochiListTextFieldRow
import ui.feice.mochi.basic.MochiSearchBar
import ui.feice.mochi.basic.MochiTextField
import ui.feice.mochi.basic.Text
import ui.feice.mochi.demo.catalog.CatalogEntry
import ui.feice.mochi.demo.common.AppIcon
import ui.feice.mochi.demo.common.Code
import ui.feice.mochi.demo.common.DemoChatBubble
import ui.feice.mochi.demo.common.DemoComposerRow
import ui.feice.mochi.demo.common.DemoPage
import ui.feice.mochi.demo.common.DemoSection
import ui.feice.mochi.demo.common.Glyph
import ui.feice.mochi.demo.common.Icons
import ui.feice.mochi.demo.common.Tone
import ui.feice.mochi.demo.common.demoItem
import ui.feice.mochi.demo.common.tile
import ui.feice.mochi.effect.content.MochiBlurText
import ui.feice.mochi.effect.content.MochiBlurVisibility
import ui.feice.mochi.list.MochiListSection
import ui.feice.mochi.theme.MochiTheme

/**
 * 文字输入：iOS 26 里会打字的地方——原地改名、输入框（清除、密码小眼睛、输错抖一下、字数）、
 * 设置式表单行、对话框里的输入框、验证码格子、iMessage 式输入栏、搜索栏。全部是库里的组件。
 */
@Composable
fun TextInputPage(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    // 0 = 没开，1 = 新建文件夹，2 = Wi-Fi 密码
    var dialog by rememberSaveable { mutableIntStateOf(0) }
    var folder by rememberSaveable { mutableStateOf("") }
    var wifi by rememberSaveable { mutableStateOf("") }
    var dialogResult by remember { mutableStateOf<String?>(null) }

    DemoPage(entry, modifier) {
        demoItem {
            InlineEditSection()
        }
        demoItem {
            FieldsSection()
        }
        demoItem {
            DemoSection("多行与字数", footer = "多行时随内容长高，高度平滑过渡；单行是胶囊，长高后变成同样半径的圆角矩形。超过上限的字输不进去，字数像计数器一样滚。") {
                var bio by rememberSaveable { mutableStateOf("") }
                MochiTextField(
                    bio,
                    { bio = it },
                    Modifier.fillMaxWidth(),
                    placeholder = "介绍一下自己",
                    singleLine = false,
                    maxLines = 6,
                    maxLength = 80,
                    supportingText = "会显示在你的主页上",
                )
            }
        }
        demoItem {
            FormSection()
        }
        demoItem {
            DemoSection("对话框里的输入框", footer = dialogResult ?: "输入框和对话框按钮一样宽、同心；对话框出现时自动聚焦，被键盘顶到上方居中。没填好之前确认按钮是灰的。") {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiButton(onClick = {
                        folder = ""
                        dialog = 1
                    }, modifier = Modifier.fillMaxWidth()) { Text("新建文件夹") }
                    MochiButton(onClick = {
                        wifi = ""
                        dialog = 2
                    }, modifier = Modifier.fillMaxWidth(), style = MochiButtonStyle.Tonal) { Text("加入 Wi-Fi（密码）") }
                }
            }
        }
        demoItem {
            CodeSection()
        }
        demoItem {
            ComposerSection()
        }
        demoItem {
            DemoSection("搜索栏", footer = "聚焦时右边的 ✕ 按钮滑进来，输入框让位；详见「导航栏与搜索」。") {
                var q by rememberSaveable { mutableStateOf("") }
                MochiSearchBar(q, { q = it })
            }
        }
        demoItem {
            DemoSection("用法") {
                Code(
                    "MochiTextField(text, { text = it }, placeholder = \"昵称\")\n" +
                        "MochiTextField(pwd, { pwd = it }, secure = true, error = err)\n\n" +
                        "// 原地改名：editing 由你持有\n" +
                        "MochiInlineTextField(name, { name = it }, editing, { editing = it })\n\n" +
                        "// 表单行（放在 MochiListSection 里）\n" +
                        "MochiListTextFieldRow(phone, { phone = it }, label = \"电话\")\n\n" +
                        "// 对话框\n" +
                        "MochiDialog(..., content = {\n" +
                        "    MochiDialogTextField(name, { name = it }, placeholder = \"名称\")\n" +
                        "})\n\n" +
                        "MochiCodeField(code, { code = it }, onComplete = { verify(it) })",
                )
            }
        }
    }

    MochiDialog(
        visible = dialog == 1,
        onDismissRequest = { dialog = 0 },
        title = "新建文件夹",
        message = "输入这个文件夹的名称。",
        actions = persistentListOf(
            MochiDialogAction("取消", MochiDialogActionRole.Cancel) { dialogResult = "取消了新建" },
            MochiDialogAction("存储", MochiDialogActionRole.Primary, enabled = folder.isNotBlank()) {
                dialogResult = "已新建文件夹「${folder.trim()}」"
            },
        ),
    ) {
        MochiDialogTextField(
            folder,
            { folder = it },
            placeholder = "名称",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
    }
    MochiDialog(
        visible = dialog == 2,
        onDismissRequest = { dialog = 0 },
        title = "输入「HONOR-1059」的密码",
        actions = persistentListOf(
            MochiDialogAction("取消", MochiDialogActionRole.Cancel) { dialogResult = "没有加入" },
            MochiDialogAction("加入", MochiDialogActionRole.Primary, enabled = wifi.length >= 8) { dialogResult = "已加入「HONOR-1059」" },
        ),
    ) {
        MochiDialogTextField(wifi, { wifi = it }, placeholder = "密码（至少 8 位）", secure = true)
    }
}

/** 原地更改：文件名、列表标题 */
@Composable
private fun InlineEditSection() {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var name by rememberSaveable { mutableStateOf("周末采购清单") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("我的提醒") }
    var editingTitle by rememberSaveable { mutableStateOf(false) }
    DemoSection(
        "原地更改",
        footer = "点名字或「重命名」：文字原地变成输入框、全选，后面淡入一块底色，位置和字号一点不动。按「完成」或点别处提交，按返回放弃；清空了提交会恢复原名。",
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(Icons.NOTE, Tone.Primary.tile(), 44.dp, corner = 12.dp)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                MochiInlineTextField(name, { name = it }, editing, { editing = it }, style = ts.navTitle)
                Text("12 KB · 今天 09:41", style = ts.footnote, color = cs.onSurfaceVariant, maxLines = 1)
            }
            MochiBlurVisibility(!editing) {
                MochiButton(
                    onClick = { editing = true },
                    style = MochiButtonStyle.Tonal,
                    minHeight = 34.dp,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                ) { Text("重命名", style = ts.subhead) }
            }
        }
        MochiInlineTextField(
            title,
            { title = it },
            editingTitle,
            { editingTitle = it },
            modifier = Modifier.padding(horizontal = 4.dp),
            style = ts.largeTitle,
            color = cs.primary,
        )
    }
}

/** 普通输入框：图标、清除、校验、密码 */
@Composable
private fun FieldsSection() {
    val cs = MochiTheme.colorScheme
    var nick by rememberSaveable { mutableStateOf("") }
    var mail by rememberSaveable { mutableStateOf("") }
    var pwd by rememberSaveable { mutableStateOf("") }
    var pwdError by remember { mutableStateOf<String?>(null) }
    var attempts by rememberSaveable { mutableIntStateOf(0) }
    var signedIn by rememberSaveable { mutableStateOf(false) }
    val mailError = if (mail.length > 3 && '@' !in mail) "邮箱里少了 @" else null
    DemoSection(
        "输入框",
        footer = "聚焦且有内容时右边模糊淡入清除按钮。密码框点小眼睛显示 / 隐藏，斜杠原位画出来。密码输错整个框左右抖一下、轻震（密码是 mochi）。",
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MochiTextField(
                nick,
                { nick = it },
                Modifier.fillMaxWidth(),
                placeholder = "昵称",
                leading = { Glyph(Icons.USER, cs.onSurfaceVariant, size = 20) },
            )
            MochiTextField(
                mail,
                { mail = it },
                Modifier.fillMaxWidth(),
                placeholder = "邮箱",
                error = mailError,
                leading = { Glyph(Icons.LINK, cs.onSurfaceVariant, size = 20) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            MochiTextField(
                pwd,
                {
                    pwd = it
                    pwdError = null
                    signedIn = false
                },
                Modifier.fillMaxWidth(),
                placeholder = "密码",
                secure = true,
                error = pwdError,
                shakeTrigger = attempts,
                leading = { Glyph(Icons.LOCK, cs.onSurfaceVariant, size = 20) },
            )
            MochiButton(
                onClick = {
                    if (pwd == "mochi") {
                        signedIn = true
                        pwdError = null
                    } else {
                        pwdError = "密码不对，再试一次"
                        attempts++
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = pwd.isNotEmpty(),
            ) { MochiBlurText(if (signedIn) "已登录" else "登录") }
        }
    }
}

/** 设置式表单：输入行和普通行混排，行高一致 */
@Composable
private fun FormSection() {
    var first by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var mail by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var birthday by remember { mutableStateOf<String?>("3月14日") }
    MochiListSection(
        header = "表单",
        footer = "输入行和选值行（生日）放在同一张卡片里：行高、分隔线、首末留白都一样，内容从同一列开始。点输入行任何地方都会聚焦；点生日切换有无。",
    ) {
        MochiListTextFieldRow(first, { first = it }, label = "姓名", placeholder = "必填")
        MochiListTextFieldRow(
            phone,
            { phone = it },
            label = "电话",
            placeholder = "手机号",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        MochiListTextFieldRow(
            mail,
            { mail = it },
            label = "邮箱",
            placeholder = "name@example.com",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        MochiListFormRow("生日", birthday, onClick = { birthday = if (birthday == null) "3月14日" else null }, placeholder = "添加生日")
    }
    MochiListSection(header = "备注") {
        MochiListTextFieldRow(note, { note = it }, placeholder = "添加备注", singleLine = false)
    }
}

/** 验证码：6 格 */
@Composable
private fun CodeSection() {
    val cs = MochiTheme.colorScheme
    val ts = MochiTheme.textStyles
    var code by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }
    var attempts by rememberSaveable { mutableIntStateOf(0) }
    var passed by rememberSaveable { mutableStateOf(false) }
    DemoSection("验证码", footer = "当前那格描主色边、光标闪烁，数字进来时模糊放大淡入。输入 123456 通过，别的数字整排抖一下、变红。") {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("验证码已发送到 138 **** 0000", style = ts.subhead, color = cs.onSurfaceVariant)
            MochiCodeField(
                code,
                {
                    code = it
                    wrong = false
                    passed = false
                },
                error = wrong,
                shakeTrigger = attempts,
                onComplete = {
                    if (it == "123456") {
                        passed = true
                    } else {
                        wrong = true
                        attempts++
                    }
                },
            )
            MochiBlurText(
                when {
                    passed -> "验证通过"
                    wrong -> "验证码不对"
                    else -> "60 秒后可以重新发送"
                },
                style = ts.footnote,
                color = when {
                    passed -> cs.primary
                    wrong -> cs.error
                    else -> cs.onSurfaceVariant
                },
            )
        }
    }
}

/** iMessage 式输入栏：胶囊输入框随内容长高，有字时麦克风换成发送键 */
@Composable
private fun ComposerSection() {
    val sent = remember { mutableStateListOf("晚上一起吃饭吗？") }
    DemoSection("消息输入栏", footer = "胶囊输入框随内容长高，发送键落在最后一行右边；有字时麦克风模糊替换成发送键。") {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            sent.forEach { m -> DemoChatBubble(true, m) }
        }
        DemoComposerRow("信息", { m ->
            sent.add(m)
            if (sent.size > 4) sent.removeAt(0)
        })
    }
}
