package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.Abi

/**
 * A two-part button split between clicking the body and expanding choices: WinUI 3's
 * SplitButton (a ContentControl subclass). The left side's click is [addActionListener];
 * the right side's arrow opens [flyout].
 */
open class WSplitButton internal constructor(inspectable: ComPtr) : WControl(inspectable) {
    constructor(text: String = "") : this(
        Activation.composeDefault(Abi.CLS_SplitButton, Abi.IID_ISplitButtonFactory),
    ) {
        if (text.isNotEmpty()) this.text = text
    }

    private val contentControl: ComPtr by lazy {
        own(inspectable.queryInterface(Abi.IID_IContentControl))
    }

    /** The ISplitButton view holding Flyout / Click (also used by ToggleSplitButton). */
    private val splitButton: ComPtr by lazy {
        own(inspectable.queryInterface(Abi.IID_ISplitButton))
    }

    /** Click event tokens registered via addActionListener. */
    private val clickTokens = ListenerTokens<() -> Unit>()

    /** The button's label string (ContentControl.Content). Object-typed, so it's boxed when passed. */
    var text: String
        get() {
            val boxed = contentControl.getPtrOrNull(Abi.IContentControl_get_Content) ?: return ""
            return try {
                PropertyValues.unboxString(boxed) ?: ""
            } finally {
                boxed.release()
            }
        }
        set(value) {
            val boxed = PropertyValues.boxString(value)
            contentControl.call(Abi.IContentControl_put_Content, boxed.ptr)
            boxed.release()
        }

    /** The flyout opened by clicking the right-side arrow (SplitButton.Flyout). */
    var flyout: WFlyoutBase? = null
        set(value) {
            field = value
            splitButton.call(Abi.ISplitButton_put_Flyout, value?.flyoutBase?.ptr)
        }

    /**
     * ActionListener-like: subscribes to clicks on the body (left side).
     * Subscribes to SplitButton.Click (TypedEventHandler<SplitButton, SplitButtonClickEventArgs>) under the hood.
     */
    fun addActionListener(listener: () -> Unit) {
        val token = splitButton.addEventHandler(
            "WinUI4K.SplitButtonClickHandler",
            Abi.IID_SplitButtonClickHandler,
            Abi.ISplitButton_add_Click,
        ) { _, _ -> listener() }
        clickTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addActionListener]. */
    fun removeActionListener(listener: () -> Unit) {
        val token = clickTokens.remove(listener) ?: return
        splitButton.removeEventHandler(Abi.ISplitButton_remove_Click, token)
    }
}
