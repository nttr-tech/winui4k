package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * WinUI 3's SemanticZoom (a Control subclass). Toggles between two views of a single collection
 * ([zoomedInView] = detail, [zoomedOutView] = summary) to make large collections easier to
 * navigate. No Swing equivalent, so we keep WinUI's class name as-is.
 *
 * Each view must be a ListViewBase (which implements ISemanticZoomInformation), so pass a
 * [WList]. Switch views via [toggleActiveView] or Ctrl+mouse wheel.
 */
class WSemanticZoom(zoomedInView: WList, zoomedOutView: WList) : WControl(
    Activation.activate(Abi.CLS_SemanticZoom, Abi.IID_ISemanticZoom), // created via the default factory
) {
    /** ViewChangeStarted event tokens registered via addViewChangeStartedListener. */
    private val viewChangeStartedTokens = ListenerTokens<() -> Unit>()

    /** The detail view (SemanticZoom.ZoomedInView). The ListViewBase is passed as ISemanticZoomInformation. */
    val zoomedInView: WList = zoomedInView

    /** The summary view (SemanticZoom.ZoomedOutView). */
    val zoomedOutView: WList = zoomedOutView

    /** Whether the detail view is currently shown (SemanticZoom.IsZoomedInViewActive). false means the summary view. */
    var isZoomedInViewActive: Boolean
        get() = inspectable.getBool(Abi.ISemanticZoom_get_IsZoomedInViewActive)
        set(value) = inspectable.putBool(Abi.ISemanticZoom_put_IsZoomedInViewActive, value)

    /** Whether the user is allowed to switch views (SemanticZoom.CanChangeViews, default true). */
    var canChangeViews: Boolean
        get() = inspectable.getBool(Abi.ISemanticZoom_get_CanChangeViews)
        set(value) = inspectable.putBool(Abi.ISemanticZoom_put_CanChangeViews, value)

    /** Whether the "zoom out" button in the bottom-left of the summary view is enabled (SemanticZoom.IsZoomOutButtonEnabled). */
    var isZoomOutButtonEnabled: Boolean
        get() = inspectable.getBool(Abi.ISemanticZoom_get_IsZoomOutButtonEnabled)
        set(value) = inspectable.putBool(Abi.ISemanticZoom_put_IsZoomOutButtonEnabled, value)

    /** Switches between the detail and summary views (SemanticZoom.ToggleActiveView). */
    fun toggleActiveView() {
        inspectable.call(Abi.ISemanticZoom_ToggleActiveView)
    }

    /** Subscribes to the start of a view switch (SemanticZoom.ViewChangeStarted). */
    fun addViewChangeStartedListener(listener: () -> Unit) {
        val token = inspectable.addEventHandler(
            "WinUI4K.SemanticZoomViewChangedEventHandler",
            Abi.IID_SemanticZoomViewChangedEventHandler,
            Abi.ISemanticZoom_add_ViewChangeStarted,
        ) { _, _ -> listener() }
        viewChangeStartedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addViewChangeStartedListener]. */
    fun removeViewChangeStartedListener(listener: () -> Unit) {
        val token = viewChangeStartedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(Abi.ISemanticZoom_remove_ViewChangeStarted, token)
    }

    init {
        // Obtain the ListViewBase as ISemanticZoomInformation and set it as the detail/summary view
        val zoomedInInformation = zoomedInView.inspectable.queryInterface(Abi.IID_ISemanticZoomInformation)
        inspectable.call(Abi.ISemanticZoom_put_ZoomedInView, zoomedInInformation.ptr)
        val zoomedOutInformation = zoomedOutView.inspectable.queryInterface(Abi.IID_ISemanticZoomInformation)
        inspectable.call(Abi.ISemanticZoom_put_ZoomedOutView, zoomedOutInformation.ptr)
    }
}
