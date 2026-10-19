// Copyright 2026, mochi-compose contributors
// SPDX-License-Identifier: Apache-2.0

package ui.feice.mochi.nothing

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import ui.feice.mochi.theme.MochiIconProvider

/** Complete dot artwork: OTA dots, native-cell third-party dots and per-glyph regular-grid dots. Unknown app keys retain their fallback. */
public object NothingIcons : MochiIconProvider {
    @Composable
    override fun resolve(key: String, filled: Boolean): ImageVector? {
        val resource = when (key) {
            "document-text" -> R.drawable.nothing_document_text
            "moon" -> R.drawable.nothing_moon
            "heart" -> R.drawable.nothing_heart
            "sunny" -> R.drawable.nothing_sunny
            "toggle" -> R.drawable.nothing_toggle
            "chatbubble-ellipses" -> R.drawable.nothing_chatbubble_ellipses
            "reader" -> R.drawable.nothing_reader
            "flash" -> R.drawable.nothing_flash
            "ellipsis-horizontal" -> R.drawable.nothing_ellipsis_horizontal
            "apps" -> R.drawable.nothing_apps
            "wifi" -> R.drawable.nothing_wifi
            "camera" -> R.drawable.nothing_camera
            "close" -> R.drawable.nothing_close
            "chevron-forward" -> R.drawable.nothing_chevron_forward
            "chevron-back" -> R.drawable.nothing_chevron_back
            "list.chevron" -> R.drawable.nothing_list_chevron
            "chevron-down" -> R.drawable.nothing_chevron_down
            "options" -> R.drawable.nothing_options
            "chatbox" -> R.drawable.nothing_chatbox
            "share" -> R.drawable.nothing_share
            "person" -> R.drawable.nothing_person
            "pin" -> R.drawable.nothing_pin
            "lock-closed" -> R.drawable.nothing_lock_closed
            "star" -> R.drawable.nothing_star
            "notifications" -> R.drawable.nothing_notifications
            "trash" -> R.drawable.nothing_trash
            "volume-high" -> R.drawable.nothing_volume_high
            "volume-low" -> R.drawable.nothing_volume_low
            "search" -> R.drawable.nothing_search
            "scan" -> R.drawable.nothing_scan
            "add" -> R.drawable.nothing_add
            "remove" -> R.drawable.nothing_remove
            "airplane" -> R.drawable.nothing_airplane
            "mic" -> R.drawable.nothing_mic
            "link" -> R.drawable.nothing_link
            "home" -> R.drawable.nothing_home
            "create" -> R.drawable.nothing_create
            "copy" -> R.drawable.nothing_copy
            "flag" -> R.drawable.nothing_flag
            "bluetooth" -> R.drawable.nothing_bluetooth
            "laptop" -> R.drawable.nothing_laptop
            "phone-portrait" -> R.drawable.nothing_phone_portrait
            "desktop" -> R.drawable.nothing_desktop
            "pulse" -> R.drawable.nothing_pulse
            "checkmark" -> R.drawable.nothing_checkmark
            "chevron-down-circle" -> R.drawable.nothing_chevron_down_circle
            "radio-button-on" -> R.drawable.nothing_radio_button_on
            "list" -> R.drawable.nothing_list
            "compass" -> R.drawable.nothing_compass
            "layers" -> R.drawable.nothing_layers
            "albums" -> R.drawable.nothing_albums
            "expand" -> R.drawable.nothing_expand
            "swap-horizontal" -> R.drawable.nothing_swap_horizontal
            "water" -> R.drawable.nothing_water
            "swap-vertical" -> R.drawable.nothing_swap_vertical
            "sparkles" -> R.drawable.nothing_sparkles
            "color-palette" -> R.drawable.nothing_color_palette
            "control.plus" -> R.drawable.nothing_control_plus
            "control.minus" -> R.drawable.nothing_control_minus
            else -> return null
        }
        return ImageVector.vectorResource(resource)
    }
}
