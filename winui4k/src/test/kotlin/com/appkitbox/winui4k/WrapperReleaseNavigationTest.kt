package com.appkitbox.winui4k

import com.appkitbox.winui4k.UiTestHarness.onUiThread
import com.appkitbox.winui4k.UiTestHarness.onUiThreadGet
import com.appkitbox.winui4k.internal.winui.XamlInterop
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import java.lang.ref.WeakReference

/**
 * Tests that the listener handlers of tab and navigation components (WTabView and others) do not strongly capture the wrapper and that the components are collected
 * after being placed in the window and removed (continuation of [WrapperReleaseTest], split off to avoid detekt's LargeClass). Waits for collection while prompting GC ([Reclamation]).
 */
@Tags("Memory")
class WrapperReleaseNavigationTest : FunSpec() {
    /** What the listeners received (checks that even if the listener captures test objects, it does not capture the wrapper). */
    private val received = mutableListOf<Any>()

    /** Places [component] in the shared window, waits for Loaded, and removes it. Also waits until the Unloaded after the removal has been processed on the UI thread. */
    private fun showAndHide(component: WComponent) {
        UiTestHarness.attachAndAwaitLoaded(component)
        UiTestHarness.detach(component)
        Reclamation.awaitWithGc { !onUiThreadGet { component.frameworkElement.getBool(XamlInterop.IFrameworkElement_get_IsLoaded) } }
            .shouldBeTrue()
        onUiThread { } // Wait for the Unloaded handler to finish running in the same turn
    }

    /** Places a component created by [create] (created on the UI thread) in the window, removes it, and returns only a weak reference. */
    private fun shownAndHidden(create: () -> WComponent): WeakReference<WComponent> {
        val component = onUiThreadGet(create)
        showAndHide(component)
        return WeakReference(component)
    }

    init {
        test("a WTabView with a close button listener is also collected after being placed in the window and removed (the handler does not capture the tab view)") {
            val sink = received
            val reference = shownAndHidden {
                WTabView().also { tabView ->
                    tabView.addTab(WTabViewItem("A"))
                    tabView.addTabCloseRequestedListener { index -> sink += index }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WTabView with a ChangeListener that does not reference the tab view is also collected after being placed in the window and removed") {
            val sink = received
            val reference = shownAndHidden {
                WTabView().also { tabView ->
                    tabView.addTab("A", WLabel("a"))
                    tabView.addChangeListener { e -> sink += e.source.javaClass }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WNavigationView with a selection listener is also collected after being placed in the window and removed (the handler does not capture the navigation view)") {
            val sink = received
            val reference = shownAndHidden {
                WNavigationView().also { view ->
                    view.addItem(WNavigationViewItem("A"))
                    view.addSelectionListener { item -> sink += item?.text ?: "" }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WNavigationView with a ChangeListener and items with ActionListeners are also collected after being placed in the window and removed") {
            val sink = received
            var item: WeakReference<WNavigationViewItem>? = null
            val reference = shownAndHidden {
                WNavigationView().also { view ->
                    val listening = WNavigationViewItem("A").also { it.addActionListener { e -> sink += e.actionCommand ?: "" } }
                    item = WeakReference(listening)
                    view.addItem(listening)
                    view.addChangeListener { e -> sink += e.source.javaClass }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
            Reclamation.awaitCollected(checkNotNull(item)).shouldBeTrue()
        }

        test("a WBreadcrumbBar with an ActionListener and a WSemanticZoom with a ChangeListener are also collected after being placed in the window and removed") {
            val sink = received
            val bar = shownAndHidden {
                WBreadcrumbBar().also { bar ->
                    bar.setItems(listOf("C:", "Users"))
                    bar.addActionListener { e -> sink += e.actionCommand ?: "" }
                }
            }
            val zoom = shownAndHidden {
                WSemanticZoom(WList(listOf("a1")), WList(listOf("a"))).also { zoom ->
                    zoom.addChangeListener { e -> sink += e.source.javaClass }
                }
            }
            Reclamation.awaitCollected(bar).shouldBeTrue()
            Reclamation.awaitCollected(zoom).shouldBeTrue()
        }

        test("a WScrollPane with a viewport ChangeListener is also collected after being put in a window and removed (the handler does not capture the scroll pane)") {
            val sink = received
            val reference = shownAndHidden {
                WScrollPane(WLabel("a")).also { pane ->
                    pane.viewport.addChangeListener { e -> sink += e.source.javaClass }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WScrollPane with a scroll bar AdjustmentListener is also collected after being put in a window and removed") {
            val sink = received
            val reference = shownAndHidden {
                WScrollPane(WLabel("a")).also { pane ->
                    pane.verticalScrollBar.addAdjustmentListener { e -> sink += e.value }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WScrollView with a ChangeListener is also collected after being put in a window and removed") {
            val sink = received
            val reference = shownAndHidden {
                WScrollView(WLabel("a")).also { view ->
                    view.addChangeListener { e -> sink += e.source.javaClass }
                }
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }

        test("a WTabView that was never placed is also collected once all its ChangeListeners are removed") {
            val reference = onUiThreadGet {
                val tabView = WTabView()
                val listener = WChangeListener { }
                tabView.addChangeListener(listener)
                tabView.removeChangeListener(listener)
                WeakReference(tabView)
            }
            Reclamation.awaitCollected(reference).shouldBeTrue()
        }
    }
}
