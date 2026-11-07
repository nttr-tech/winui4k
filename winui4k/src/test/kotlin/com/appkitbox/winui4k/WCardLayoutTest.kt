package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests that verify [WCardLayout], which corresponds to java.awt.CardLayout. The expected values come from Swing's rules (only the first child added is visible, show is by name,
 * next / previous wrap around from one end to the other in the order added, all children are the same size filling the area, the preferred size is the children's maximum plus
 * twice the gaps and the insets, a non-string constraint throws IllegalArgumentException, and passing another panel throws IllegalArgumentException).
 */
@Tags("E2E")
class WCardLayoutTest : FunSpec() {
    /** A panel with three cards named a, b and c (canvases 40, 60 and 50 wide and 20, 30 and 10 tall). */
    private class Cards {
        val layout = WCardLayout(5.0, 7.0)
        val a = WCanvas().also {
            it.width = 40.0
            it.height = 20.0
        }
        val b = WCanvas().also {
            it.width = 60.0
            it.height = 30.0
        }
        val c = WCanvas().also {
            it.width = 50.0
            it.height = 10.0
        }
        val panel = WLayoutPanel(layout).also {
            it.add(a, "a")
            it.add(b, "b")
            it.add(c, "c")
        }

        /** The name of the shown child. */
        fun visible(): List<String> = listOf("a" to a, "b" to b, "c" to c).filter { it.second.isVisible }.map { it.first }
    }

    init {
        context("switching") {
            test("only the first child added is visible") {
                onUiThreadGet { Cards().visible() } shouldBe listOf("a")
            }

            test("show makes only the named child visible, and does nothing for a name that does not exist") {
                onUiThreadGet {
                    val cards = Cards()
                    cards.layout.show(cards.panel, "c")
                    val shown = cards.visible()
                    cards.layout.show(cards.panel, "missing")
                    shown + cards.visible()
                } shouldBe listOf("c", "c")
            }

            test("next advances in the order added, and after the last returns to the first") {
                onUiThreadGet {
                    val cards = Cards()
                    (1..3).map {
                        cards.layout.next(cards.panel)
                        cards.visible().single()
                    }
                } shouldBe listOf("b", "c", "a")
            }

            test("previous goes back in the order added, and before the first becomes the last") {
                onUiThreadGet {
                    val cards = Cards()
                    (1..3).map {
                        cards.layout.previous(cards.panel)
                        cards.visible().single()
                    }
                } shouldBe listOf("c", "b", "a")
            }

            test("first and last show the first and last children") {
                onUiThreadGet {
                    val cards = Cards()
                    cards.layout.last(cards.panel)
                    val last = cards.visible()
                    cards.layout.first(cards.panel)
                    last + cards.visible()
                } shouldBe listOf("c", "a")
            }

            test("after adding again with the same name, show shows the child added later") {
                onUiThreadGet {
                    val cards = Cards()
                    val replacement = WCanvas()
                    cards.panel.add(replacement, "a")
                    cards.layout.show(cards.panel, "a")
                    replacement.isVisible to cards.a.isVisible
                } shouldBe (true to false)
            }
        }

        context("size and placement") {
            test("the preferred size is the children's maximum width and height plus twice the gaps and the insets") {
                onUiThreadGet {
                    val cards = Cards()
                    cards.panel.insets = WInsets(top = 1.0, left = 2.0, bottom = 3.0, right = 4.0)
                    cards.layout.preferredLayoutSize(cards.panel)
                } shouldBe WSize(60.0 + 10.0 + 6.0, 30.0 + 14.0 + 4.0)
            }

            test("the shown child is placed filling the area minus the gaps") {
                val cards = onUiThreadGet {
                    Cards().also {
                        it.panel.width = 200.0
                        it.panel.height = 100.0
                    }
                }
                UiTestHarness.attachAndAwaitLoaded(cards.panel)
                try {
                    repeat(2) { onUiThread { cards.panel.validate() } }
                    onUiThreadGet {
                        cards.layout.show(cards.panel, "b")
                        cards.panel.validate()
                        cards.panel.updateLayout()
                        cards.b.bounds
                    } shouldBe WRectangle(5, 7, 190, 86)
                } finally {
                    UiTestHarness.detach(cards.panel)
                }
            }

            test("after the shown child is removed, the first child is visible when laid out again") {
                onUiThreadGet {
                    val cards = Cards()
                    cards.layout.show(cards.panel, "b")
                    cards.panel.remove(cards.b)
                    cards.layout.layoutContainer(cards.panel)
                    cards.a.isVisible to cards.c.isVisible
                } shouldBe (true to false)
            }
        }

        context("exceptions") {
            test("a constraint that is not a string (including null) throws IllegalArgumentException, and the child is not added") {
                onUiThreadGet {
                    val panel = WLayoutPanel(WCardLayout())
                    shouldThrow<IllegalArgumentException> { panel.add(WCanvas(), 1 as Any) }
                    shouldThrow<IllegalArgumentException> { panel.add(WCanvas(), null as Any?) }
                    panel.componentCount
                } shouldBe 0
            }

            test("passing a panel that does not use this manager throws IllegalArgumentException") {
                onUiThreadGet {
                    val layout = WCardLayout()
                    val other = WLayoutPanel(WCardLayout())
                    shouldThrow<IllegalArgumentException> { layout.next(other) }
                    shouldThrow<IllegalArgumentException> { layout.show(other, "a") }
                    true
                } shouldBe true
            }
        }
    }
}
