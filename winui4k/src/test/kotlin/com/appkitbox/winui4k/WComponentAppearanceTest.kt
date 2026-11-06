package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import io.kotest.assertions.withClue
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.Collections
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Tests for the appearance and placement properties of [WComponent] ([WComponent.maxWidth] / [WComponent.setMargin]) and
 * adding and removing ActualThemeChanged, Loaded and SizeChanged listeners.
 */
@Tags("E2E")
class WComponentAppearanceTest : FunSpec() {
    init {
        context("Defaults") {
            test("maxWidth defaults to unlimited (Double.POSITIVE_INFINITY)") {
                onUiThreadGet { WButton("Press").maxWidth } shouldBe Double.POSITIVE_INFINITY
            }
        }

        context("Reading and writing") {
            test("maxWidth returns the set value, and refers to the same FrameworkElement.MaxWidth as the width of maximumSize") {
                onUiThreadGet {
                    val button = WButton("Press")
                    button.maxWidth = 120.0
                    button.maxWidth to button.maximumSize.width
                } shouldBe (120.0 to 120.0)
            }

            test("even when a wider width is assigned than maxWidth, the width after layout is capped at maxWidth") {
                val button = onUiThreadGet {
                    WButton("Press").also {
                        it.width = 300.0
                        it.maxWidth = 120.0
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(button)
                try {
                    onUiThreadGet {
                        button.updateLayout()
                        button.actualWidth
                    } shouldBe 120.0
                } finally {
                    UiTestHarness.detach(button)
                }
            }

            test("the left, top, right and bottom of setMargin go into FrameworkElement.Margin in that order") {
                onUiThreadGet {
                    val button = WButton("Press")
                    button.setMargin(1.0, 2.0, 3.0, 4.0)
                    XamlStructs.getThickness(button.frameworkElement, XamlInterop.IFrameworkElement_get_Margin).toList()
                } shouldBe listOf(1.0, 2.0, 3.0, 4.0)
            }
        }

        context("Events") {
            test("changing requestedTheme calls ActualThemeChanged listeners, and removed listeners are not called") {
                val kept = LinkedBlockingQueue<ElementTheme>()
                val removed = Collections.synchronizedList(ArrayList<ElementTheme>())
                val button = onUiThreadGet { WButton("Press") }
                UiTestHarness.attachAndAwaitLoaded(button)
                try {
                    onUiThreadGet {
                        val removedListener: () -> Unit = { removed.add(button.actualTheme) }
                        button.addActualThemeChangedListener { kept.add(button.actualTheme) }
                        button.addActualThemeChangedListener(removedListener)
                        button.removeActualThemeChangedListener(removedListener)
                        val target = if (button.actualTheme == ElementTheme.DARK) ElementTheme.LIGHT else ElementTheme.DARK
                        button.requestedTheme = target
                        target
                    }.let { target ->
                        withClue("ActualThemeChanged did not arrive") {
                            kept.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe target
                        }
                    }
                    onUiThreadGet { removed.toList() }.shouldBeEmpty()
                } finally {
                    UiTestHarness.detach(button)
                }
            }

            test("listeners removed with removeLoadedListener and removeSizeChangedListener are not called even when loaded") {
                val loaded = LinkedBlockingQueue<String>()
                val removedCalls = Collections.synchronizedList(ArrayList<String>())
                val button = onUiThreadGet {
                    val button = WButton("Press")
                    val removedLoaded: () -> Unit = { removedCalls.add("loaded") }
                    val removedSize: () -> Unit = { removedCalls.add("size") }
                    button.addLoadedListener(removedLoaded)
                    button.addSizeChangedListener(removedSize)
                    button.addLoadedListener { loaded.add("loaded") }
                    button.removeLoadedListener(removedLoaded)
                    button.removeSizeChangedListener(removedSize)
                    button
                }
                UiTestHarness.attach(button)
                try {
                    withClue("Loaded of the kept listener did not arrive") {
                        loaded.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS).shouldNotBeNull()
                    }
                    onUiThreadGet {
                        button.updateLayout()
                        removedCalls.toList()
                    }.shouldBeEmpty()
                } finally {
                    UiTestHarness.detach(button)
                }
            }
        }
    }
}
