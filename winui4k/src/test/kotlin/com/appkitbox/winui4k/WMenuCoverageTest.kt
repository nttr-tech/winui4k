package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests for the defaults and reading and writing of the public members of menus and toolbars that had no tests (WCommandBar's WinUI properties and content,
 * WMenuFlyoutItem.keyboardAcceleratorText). Defaults are the WinUI CommandBar / AppBar defaults.
 */
@Tags("E2E")
class WMenuCoverageTest : FunSpec() {
    /** Whether the CommandBar's Content (ContentControl.Content) is the same object as the native of [component]. */
    private fun nativeContentIs(bar: WCommandBar, component: WComponent?): Boolean {
        val control = bar.inspectable.queryInterface(XamlInterop.IID_IContentControl)
        try {
            val content = control.getPtrOrNull(XamlInterop.IContentControl_get_Content) ?: return component == null
            return try {
                component != null && MenuBarItemMenuEvents.identityOf(content) == MenuBarItemMenuEvents.identityOf(component.inspectable)
            } finally {
                content.release()
            }
        } finally {
            control.release()
        }
    }

    init {
        context("Defaults") {
            test("WCommandBar is closed, not sticky, its closed display is COMPACT, labels are BOTTOM, and overflow is AUTO with dynamic overflow") {
                onUiThreadGet {
                    val bar = WCommandBar()
                    listOf(
                        bar.isOpen,
                        bar.isSticky,
                        bar.closedDisplayMode,
                        bar.defaultLabelPosition,
                        bar.overflowButtonVisibility,
                        bar.isDynamicOverflowEnabled,
                        bar.content,
                        nativeContentIs(bar, null),
                    )
                } shouldContainExactly listOf(
                    false,
                    false,
                    AppBarClosedDisplayMode.COMPACT,
                    CommandBarDefaultLabelPosition.BOTTOM,
                    CommandBarOverflowButtonVisibility.AUTO,
                    true,
                    null,
                    true,
                )
            }

            test("WMenuFlyoutItem's keyboardAcceleratorText is empty") {
                onUiThreadGet { WMenuFlyoutItem("Save").keyboardAcceleratorText } shouldBe ""
            }
        }

        context("Reading and writing") {
            test("WCommandBar's isSticky, closedDisplayMode, defaultLabelPosition, overflowButtonVisibility and isDynamicOverflowEnabled read back the set values") {
                onUiThreadGet {
                    val bar = WCommandBar()
                    bar.isSticky = true
                    bar.closedDisplayMode = AppBarClosedDisplayMode.MINIMAL
                    bar.defaultLabelPosition = CommandBarDefaultLabelPosition.RIGHT
                    bar.overflowButtonVisibility = CommandBarOverflowButtonVisibility.COLLAPSED
                    bar.isDynamicOverflowEnabled = false
                    val first = listOf(
                        bar.isSticky,
                        bar.closedDisplayMode,
                        bar.defaultLabelPosition,
                        bar.overflowButtonVisibility,
                        bar.isDynamicOverflowEnabled,
                    )
                    bar.closedDisplayMode = AppBarClosedDisplayMode.HIDDEN
                    bar.defaultLabelPosition = CommandBarDefaultLabelPosition.COLLAPSED
                    bar.overflowButtonVisibility = CommandBarOverflowButtonVisibility.VISIBLE
                    first + listOf(bar.closedDisplayMode, bar.defaultLabelPosition, bar.overflowButtonVisibility)
                } shouldContainExactly listOf(
                    true,
                    AppBarClosedDisplayMode.MINIMAL,
                    CommandBarDefaultLabelPosition.RIGHT,
                    CommandBarOverflowButtonVisibility.COLLAPSED,
                    false,
                    AppBarClosedDisplayMode.HIDDEN,
                    CommandBarDefaultLabelPosition.COLLAPSED,
                    CommandBarOverflowButtonVisibility.VISIBLE,
                )
            }

            test("a component put in WCommandBar's content becomes the native Content and null removes it") {
                onUiThreadGet {
                    val bar = WCommandBar()
                    val label = WLabel("Format")
                    bar.content = label
                    val set = listOf(bar.content === label, nativeContentIs(bar, label))
                    bar.content = null
                    set + listOf(bar.content == null, nativeContentIs(bar, null))
                } shouldContainExactly listOf(true, true, true, true)
            }

            test("WCommandBar's isOpen reads back the set value (opening and closing the overflow menu)") {
                val bar = onUiThreadGet { WCommandBar().also { it.addSecondaryCommand(WAppBarButton("Settings")) } }
                UiTestHarness.attachAndAwaitLoaded(bar)
                try {
                    onUiThreadGet {
                        bar.isOpen = true
                        val opened = bar.isOpen
                        bar.isOpen = false
                        opened to bar.isOpen
                    } shouldBe (true to false)
                } finally {
                    UiTestHarness.detach(bar)
                }
            }

            test("WMenuFlyoutItem's keyboardAcceleratorText reads back the set text, accelerator does not change, and it can be reset to empty") {
                onUiThreadGet {
                    val item = WMenuFlyoutItem("Save")
                    item.keyboardAcceleratorText = "Ctrl+S"
                    val set = listOf(item.keyboardAcceleratorText, item.accelerator)
                    item.keyboardAcceleratorText = ""
                    set + item.keyboardAcceleratorText
                } shouldContainExactly listOf("Ctrl+S", null, "")
            }
        }
    }
}
