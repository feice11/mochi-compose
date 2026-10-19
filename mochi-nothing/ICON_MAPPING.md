# Nothing all-dot icon mapping / 全点阵图标映射

56/56 demo semantics use dot artwork, including every component alias; no known ordinary-icon fallback. Official OTA dot artwork is preserved with each asset’s native density, dot positions and diameter ratios. Settings supplies the seven-dot forward/list arrow (mirrored for back) and nine-dot apps glyph; SystemUI supplies the five-dot down arrow; Weather supplies its 25-dot unslashed Wi-Fi; Camera supplies its 31-dot camera and Nothing X the 13-dot close and seven-dot lightning; Launcher tools and social glyphs also replace third-party options/chat art. No uniform grid, point count, point radius, or optical centroid correction is imposed on official sources. Unmatched confirmation uses original MIT [Pixelarticons](https://github.com/halfmage/pixelarticons/tree/c9ecc2da61afde85dbe539025e3d931c6152ad75) 2x2 cell centers converted directly to equal circular dots with diameter 94% of the nearest diagonal-neighbor distance, without area sampling. Remaining existing third-party glyphs are repaired using their original MDI outlines from the [NDOT FiraCode Nerd source](https://github.com/GeneralKaos666/ndot-firacode-nerd/tree/a2c370b24ba6f21a20bb50e42866ffb7345da99d). Each glyph uses one regular square matrix and one circle radius; the per-glyph recipe selects 9/10/11/12/13 columns per 24dp frame and makes the dot diameter 94% of the pitch, leaving a fine 6% seam between axial neighbors. Coverage selects filled bodies; source stroke guides preserve fine rods, loops and arrows. Circular source holes set the lattice phase; no point is placed freely along a curve. Circled-down and compass use outline variants to preserve their interior symbols. These remain local third-party derivatives (MDI Apache-2.0, source font OFL-1.1), not Nothing-original artwork or unmodified upstream release glyphs. The source silhouettes are not hand-drawn. Add/remove preserve their existing 22dp/em role size, other MDI glyphs 25dp/em. Native firmware navigation symbols retain their authored point layouts; the list arrow keeps its tight 8x14 viewport. Third-party artwork applies 30% ink-area-centroid compensation capped at 0.75dp and bounded by the viewport; slot geometry never moves. Native Pixelarticons keep their author-supplied 24x24 frame and stroke-cell positions. Both variant requests retain the same source dot glyph. Unknown custom app keys retain the caller fallback.

| Demo semantic | Key | Result | Source |
|---|---|---|---|
| SHARE | `share` | third-party-dot | share_variant.svg |
| WIFI | `wifi` | official-dot | ic_main_no_connection.xml |
| USER | `person` | third-party-dot | account_outline.svg |
| PIN | `pin` | third-party-dot | pin_outline.svg |
| LOCK | `lock-closed` | third-party-dot | lock_outline.svg |
| NOTE | `document-text` | official-dot | nt_ic_custom_reading.xml |
| MOON | `moon` | official-dot | ic_clear_night.xml |
| HEART | `heart` | official-dot | nt_ic_custom_favourites.xml |
| STAR | `star` | third-party-dot | star.svg |
| BELL | `notifications` | third-party-dot | bell_outline.svg |
| TRASH | `trash` | third-party-dot | delete_outline.svg |
| CAMERA | `camera` | official-dot | widget_camera_icon.xml |
| VOLUME_HIGH | `volume-high` | third-party-dot | volume_high.svg |
| VOLUME_LOW | `volume-low` | third-party-dot | volume_low.svg |
| SEARCH | `search` | third-party-dot | magnify.svg |
| SCAN | `scan` | third-party-dot | scan_helper.svg |
| PLUS | `add` | third-party-dot | plus.svg |
| PLANE | `airplane` | third-party-dot | airplane.svg |
| MIC | `mic` | third-party-dot | microphone.svg |
| LINK | `link` | third-party-dot | link_variant.svg |
| HOME | `home` | third-party-dot | home_outline.svg |
| EDIT | `create` | third-party-dot | pencil.svg |
| COPY | `copy` | third-party-dot | content_copy.svg |
| FLAG | `flag` | third-party-dot | flag_outline.svg |
| BLUETOOTH | `bluetooth` | third-party-dot | bluetooth.svg |
| LAPTOP | `laptop` | third-party-dot | laptop.svg |
| PHONE | `phone-portrait` | third-party-dot | cellphone.svg |
| DESKTOP | `desktop` | third-party-dot | monitor.svg |
| SUN | `sunny` | official-dot | ic_sunny.xml |
| PULSE | `pulse` | third-party-dot | pulse.svg |
| BACK | `chevron-back` | official-dot | ic_double_summary_arrow.xml |
| CHEVRON_RIGHT | `chevron-forward` | official-dot | ic_double_summary_arrow.xml |
| CLOSE | `close` | official-dot | widget_list_icon.xml |
| CHECK | `checkmark` | third-party-dot | check.svg |
| DROPDOWN | `chevron-down-circle` | third-party-dot | chevron_down_circle_outline.svg |
| BUTTON | `radio-button-on` | third-party-dot | radiobox_marked.svg |
| LIST | `list` | third-party-dot | format_list_bulleted.svg |
| NAVBAR | `compass` | third-party-dot | compass_outline.svg |
| CONTEXT | `layers` | third-party-dot | layers_outline.svg |
| TABBAR | `albums` | third-party-dot | card_multiple_outline.svg |
| TRANSITION | `expand` | third-party-dot | arrow_expand_all.svg |
| SEGMENTED | `swap-horizontal` | third-party-dot | swap_horizontal.svg |
| SWITCH | `toggle` | official-dot | nt_widgets_quick_settings_icon.xml |
| SLIDER | `options` | official-dot | nt_ic_custom_tools.xml |
| TOAST | `chatbubble-ellipses` | official-dot | nt_ic_custom_social.xml |
| DIALOG | `chatbox` | official-dot | nt_ic_custom_social.xml |
| SHEET | `reader` | official-dot | nt_ic_custom_reading.xml |
| GLOW | `sunny` | official-dot | ic_sunny.xml |
| ELASTIC | `pulse` | third-party-dot | pulse.svg |
| METABALL | `water` | third-party-dot | water.svg |
| BOUNCE | `swap-vertical` | third-party-dot | swap_vertical.svg |
| CONTENT | `sparkles` | third-party-dot | creation.svg |
| HDR | `flash` | official-dot | nothing_widget_battery_charging_image.xml |
| GRID | `apps` | official-dot | ic_apps.xml |
| SPARKLE | `sparkles` | third-party-dot | creation.svg |
| PALETTE | `color-palette` | third-party-dot | palette_outline.svg |

Full paths, source hashes, bounds and transformations: [icon-provenance.json](icon-provenance.json). Rebuild derived assets: `python mochi-nothing/tools/build_third_party_dots.py`, and `python mochi-nothing/tools/build_pixelart_dots.py`, then `python mochi-nothing/tools/import_icons.py`.
