package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.XamlInterop
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/**
 * WinUI 3's SemanticZoom (a Control subclass). Toggles between two views of a single collection
 * ([zoomedInView] = detail, [zoomedOutView] = summary) to make large collections easier to
 * navigate. No Swing equivalent, so we keep WinUI's class name as-is.
 *
 * Each view must be a ListViewBase (which implements ISemanticZoomInformation), so pass a
 * [WList]. Switch views via [toggleActiveView] or Ctrl+mouse wheel.
 */
class WSemanticZoom(zoomedInView: WList, zoomedOutView: WList) : WControl(
    Activation.activate(XamlInterop.CLS_SemanticZoom, XamlInterop.IID_ISemanticZoom), // created via the default factory
) {
    /** ViewChangeStarted event tokens registered via addViewChangeStartedListener. */
    private val viewChangeStartedTokens = ListenerTokens<Runnable>()

    /** The ChangeListeners. A single ViewChangeCompleted handler is registered when the first listener is added. */
    private val changeListeners = ChangeListeners(this) { addViewChangeCompletedHandler() }

    /** The [isZoomedInViewActive] last notified. */
    private var notifiedZoomedIn = true

    /** The detail view (SemanticZoom.ZoomedInView). The ListViewBase is passed as ISemanticZoomInformation. */
    val zoomedInView: WList = zoomedInView

    /** The summary view (SemanticZoom.ZoomedOutView). */
    val zoomedOutView: WList = zoomedOutView

    /** Whether the detail view is currently shown (SemanticZoom.IsZoomedInViewActive). false means the summary view. */
    var isZoomedInViewActive: Boolean
        get() = inspectable.getBool(XamlInterop.ISemanticZoom_get_IsZoomedInViewActive)
        set(value) = inspectable.putBool(XamlInterop.ISemanticZoom_put_IsZoomedInViewActive, value)

    /** Whether the user is allowed to switch views (SemanticZoom.CanChangeViews, default true). */
    var canChangeViews: Boolean
        get() = inspectable.getBool(XamlInterop.ISemanticZoom_get_CanChangeViews)
        set(value) = inspectable.putBool(XamlInterop.ISemanticZoom_put_CanChangeViews, value)

    /** Whether the "zoom out" button in the bottom-left of the summary view is enabled (SemanticZoom.IsZoomOutButtonEnabled). */
    var isZoomOutButtonEnabled: Boolean
        get() = inspectable.getBool(XamlInterop.ISemanticZoom_get_IsZoomOutButtonEnabled)
        set(value) = inspectable.putBool(XamlInterop.ISemanticZoom_put_IsZoomOutButtonEnabled, value)

    /** Switches between the detail and summary views (SemanticZoom.ToggleActiveView). */
    fun toggleActiveView() {
        inspectable.call(XamlInterop.ISemanticZoom_ToggleActiveView)
    }

    /** Subscribes to the start of a view switch (SemanticZoom.ViewChangeStarted). */
    @JvmSynthetic
    fun addViewChangeStartedListener(listener: () -> Unit) {
        val adapter = Runnable(listener)
        addViewChangeStartedListener(adapter)
        viewChangeStartedTokens.addKotlinAdapter(listener, adapter)
    }

    fun addViewChangeStartedListener(listener: Runnable) {
        val token = inspectable.addEventHandler(
            "WinUI4K.SemanticZoomViewChangedEventHandler",
            XamlInterop.IID_SemanticZoomViewChangedEventHandler,
            XamlInterop.ISemanticZoom_add_ViewChangeStarted,
        ) { _, _ -> listener.run() }
        viewChangeStartedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addViewChangeStartedListener]. */
    @JvmSynthetic
    fun removeViewChangeStartedListener(listener: () -> Unit) {
        val adapter = viewChangeStartedTokens.removeKotlinAdapter(listener) ?: return
        removeViewChangeStartedListener(adapter)
    }

    fun removeViewChangeStartedListener(listener: Runnable) {
        val token = viewChangeStartedTokens.remove(listener) ?: return
        inspectable.removeEventHandler(XamlInterop.ISemanticZoom_remove_ViewChangeStarted, token)
    }

    /**
     * ChangeListener-style: receives a [WChangeEvent] (whose source is this control) every time a view switch finishes
     * (SemanticZoom.ViewChangeCompleted). Inside the listener, [isZoomedInViewActive] returns the value after the switch.
     * It is called after the switching animation, so it may not have been called yet when [toggleActiveView] or setting [isZoomedInViewActive] returns.
     */
    fun addChangeListener(listener: WChangeListener) {
        changeListeners.add(listener)
    }

    /** Removes one [WChangeListener] added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener) {
        changeListeners.remove(listener)
    }

    /** The ChangeListeners currently added (in the order they were added). */
    fun getChangeListeners(): Array<WChangeListener> = changeListeners.toArray()

    private fun notifyIfChanged() {
        val old = notifiedZoomedIn
        val now = isZoomedInViewActive
        notifiedZoomedIn = now
        changeListeners.fireIfChanged(old, now)
    }

    private fun addViewChangeCompletedHandler() {
        notifiedZoomedIn = isZoomedInViewActive
        // The handler stays on the native side, so do not strongly capture this control; look it up through the anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        inspectable.addEventHandler(
            "WinUI4K.SemanticZoomViewChangeCompletedHandler",
            XamlInterop.IID_SemanticZoomViewChangedEventHandler,
            XamlInterop.ISemanticZoom_add_ViewChangeCompleted,
        ) { _, _ -> (anchor.get() as? WSemanticZoom)?.notifyIfChanged() }
    }

    init {
        // Get the ListViewBase as ISemanticZoomInformation and set it as the zoomed-in/out view (the setter holds its own reference, so release the one obtained)
        val zoomedInInformation = zoomedInView.inspectable.queryInterface(XamlInterop.IID_ISemanticZoomInformation)
        try {
            inspectable.call(XamlInterop.ISemanticZoom_put_ZoomedInView, zoomedInInformation.ptr)
        } finally {
            zoomedInInformation.release()
        }
        val zoomedOutInformation = zoomedOutView.inspectable.queryInterface(XamlInterop.IID_ISemanticZoomInformation)
        try {
            inspectable.call(XamlInterop.ISemanticZoom_put_ZoomedOutView, zoomedOutInformation.ptr)
        } finally {
            zoomedOutInformation.release()
        }
    }
}
