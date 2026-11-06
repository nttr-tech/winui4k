package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [WComponent.getComponentAt] / [WContainer.getComponentAt] / [WContainer.findComponentAt], which correspond to
 * Component.getComponentAt and Container.getComponentAt / findComponentAt.
 * Expected values come from Swing's rules (getComponentAt is the child containing the point (itself if none, null outside itself), and findComponentAt is
 * the deepest descendant at the point). Coordinates are decided from the positions and sizes placed on the Canvas.
 *
 * Screen: on a 200 x 100 [WCanvas], an 80 x 30 button at (10, 10) and an 80 x 60 inner [WCanvas] at (100, 10)
 * (with a 60 x 30 label at its (0, 0)).
 */
@Tags("Integration")
class WContainerComponentAtTest : FunSpec() {
    private class Scene(val outer: WCanvas, val button: WButton, val inner: WCanvas, val label: WLabel)

    private fun withScene(check: (Scene) -> Unit) {
        val scene = onUiThreadGet {
            val button = WButton("Press")
            val label = WLabel("Label")
            val inner = WCanvas().also {
                it.add(label, 0.0, 0.0)
                label.setSize(60.0, 30.0)
            }
            val outer = WCanvas().also {
                it.width = 200.0
                it.height = 100.0
                it.add(button, 10.0, 10.0)
                button.setSize(80.0, 30.0)
                it.add(inner, 100.0, 10.0)
                inner.setSize(80.0, 60.0)
            }
            Scene(outer, button, inner, label)
        }
        UiTestHarness.attachAndAwaitLoaded(scene.outer)
        try {
            onUiThreadGet { scene.outer.updateLayout() }
            check(scene)
        } finally {
            UiTestHarness.detach(scene.outer)
        }
    }

    init {
        context("Layout") {
            test("getComponentAt returns the child containing the point, and itself where there is no child") {
                withScene { scene ->
                    onUiThreadGet {
                        listOf(
                            scene.outer.getComponentAt(20, 20) === scene.button,
                            scene.outer.getComponentAt(89, 39) === scene.button,
                            scene.outer.getComponentAt(110, 20) === scene.inner,
                            scene.outer.getComponentAt(195, 95) === scene.outer,
                            scene.outer.getComponentAt(90, 20) === scene.outer,
                        )
                    } shouldBe listOf(true, true, true, true, true)
                }
            }

            test("findComponentAt returns the deepest descendant at the point (the label inside the inner panel)") {
                withScene { scene ->
                    onUiThreadGet {
                        listOf(
                            scene.outer.findComponentAt(110, 20) === scene.label,
                            scene.outer.findComponentAt(110, 60) === scene.inner,
                            scene.outer.findComponentAt(20, 20) === scene.button,
                            scene.outer.findComponentAt(195, 95) === scene.outer,
                        )
                    } shouldBe listOf(true, true, true, true)
                }
            }

            test("the form taking a WPoint is the same as the form taking x and y") {
                withScene { scene ->
                    onUiThreadGet {
                        (scene.outer.getComponentAt(WPoint(110, 20)) === scene.inner) to
                            (scene.outer.findComponentAt(WPoint(110, 20)) === scene.label)
                    } shouldBe (true to true)
                }
            }

            test("getComponentAt of a component that is not a container is itself inside and null outside (the same as Component.getComponentAt)") {
                withScene { scene ->
                    onUiThreadGet {
                        (scene.button.getComponentAt(5, 5) === scene.button) to scene.button.getComponentAt(80, 5)
                    } shouldBe (true to null)
                }
            }

            test("a hidden (isVisible = false) child is not hit, and the parent is returned at that place (Swing's getComponentAt also returns hidden children)") {
                withScene { scene ->
                    onUiThreadGet {
                        scene.button.isVisible = false
                        scene.outer.updateLayout()
                        scene.outer.getComponentAt(20, 20) === scene.outer
                    } shouldBe true
                }
            }

            test("for overlapping children, the one added later (drawn in front in XAML) is returned (in Swing the one added first is in front)") {
                withScene { scene ->
                    onUiThreadGet {
                        val cover = WPanel().also { it.background = WColor.BLACK }
                        scene.outer.add(cover, 0.0, 0.0)
                        cover.setSize(50.0, 50.0)
                        scene.outer.updateLayout()
                        scene.outer.getComponentAt(20, 20) === cover
                    } shouldBe true
                }
            }
        }

        context("Boundaries and exceptions") {
            test("points outside itself (negative coordinates, the right edge, the bottom edge) are null") {
                withScene { scene ->
                    onUiThreadGet {
                        listOf(
                            scene.outer.getComponentAt(-1, 10),
                            scene.outer.getComponentAt(200, 10),
                            scene.outer.getComponentAt(10, 100),
                            scene.outer.findComponentAt(-1, -1),
                        )
                    } shouldBe listOf(null, null, null, null)
                }
            }

            test("a container not placed in a window (size 0) is null at every point") {
                onUiThreadGet {
                    val panel = WPanel().also { it.add(WButton("Press")) }
                    panel.getComponentAt(0, 0) to panel.findComponentAt(0, 0)
                } shouldBe (null to null)
            }
        }
    }
}
