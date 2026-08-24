package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.DataTransferInterop
import com.appkitbox.winui4k.internal.winui.FoundationInterop

/**
 * The OS clipboard: Windows.ApplicationModel.DataTransfer.Clipboard (a static class).
 *
 * To copy, fill a [DataPackage] and pass it to [setContent]; to paste, receive a
 * [DataPackageView] from [getContent] and read it. Content changes can be subscribed to
 * via [addContentChangedListener].
 *
 * Clipboard access is only allowed while the window is in the foreground
 * ([setContent] / [getContent] from the background raise an HRESULT exception).
 */
object Clipboard {
    /** IClipboardStatics. The OS caches the factory, so it's reused for the process lifetime. */
    private val statics: ComPtr by lazy {
        Activation.factory(DataTransferInterop.CLS_Clipboard, DataTransferInterop.IID_IClipboardStatics)
    }

    /** IClipboardStatics2 (history and roaming related). */
    private val statics2: ComPtr by lazy {
        Activation.factory(DataTransferInterop.CLS_Clipboard, DataTransferInterop.IID_IClipboardStatics2)
    }

    /** Event tokens for ContentChanged registered via addContentChangedListener. */
    private val contentChangedTokens = ListenerTokens<Runnable>()

    /** Gets the clipboard's current content for reading (Clipboard.GetContent). */
    @JvmStatic
    fun getContent(): DataPackageView =
        DataPackageView(statics.getPtr(DataTransferInterop.IClipboardStatics_GetContent))

    /** Sets [dataPackage]'s content on the clipboard (Clipboard.SetContent). */
    @JvmStatic
    fun setContent(dataPackage: DataPackage) {
        statics.call(DataTransferInterop.IClipboardStatics_SetContent, dataPackage.dataPackage)
    }

    /**
     * Sets the content with [options] controlling history and roaming
     * (Clipboard.SetContentWithOptions). Returns true if the content was set.
     */
    @JvmStatic
    fun setContent(dataPackage: DataPackage, options: ClipboardContentOptions): Boolean =
        Ffi.backend.withScope { scope ->
            val out = scope.allocate(1, 1)
            statics2.call(
                DataTransferInterop.IClipboardStatics2_SetContentWithOptions,
                dataPackage.dataPackage,
                options.options,
                out,
            )
            Ffi.backend.memory.getByte(out, 0).toInt() != 0
        }

    /**
     * Commits the set content so it survives after the app exits (Clipboard.Flush).
     * Normally the data is only handed over when the paste target requests it while the
     * copying app is still alive (delayed rendering), so calling this before exit is safe.
     */
    @JvmStatic
    fun flush() = statics.call(DataTransferInterop.IClipboardStatics_Flush)

    /** Empties the clipboard (Clipboard.Clear). */
    @JvmStatic
    fun clear() = statics.call(DataTransferInterop.IClipboardStatics_Clear)

    /** Whether clipboard history (Win+V) is enabled in the OS settings (Clipboard.IsHistoryEnabled). */
    @JvmStatic
    val isHistoryEnabled: Boolean
        get() = statics2.getBool(DataTransferInterop.IClipboardStatics2_IsHistoryEnabled)

    /** Whether cross-device clipboard sync (roaming) is enabled in the OS settings (Clipboard.IsRoamingEnabled). */
    @JvmStatic
    val isRoamingEnabled: Boolean
        get() = statics2.getBool(DataTransferInterop.IClipboardStatics2_IsRoamingEnabled)

    /** Deletes all clipboard history (Clipboard.ClearHistory). Returns true if it was deleted. */
    @JvmStatic
    fun clearHistory(): Boolean = statics2.getBool(DataTransferInterop.IClipboardStatics2_ClearHistory)

    /**
     * Subscribes to clipboard content changes (Clipboard.ContentChanged).
     * Listeners are called on the UI thread.
     */
    @JvmSynthetic
    fun addContentChangedListener(listener: () -> Unit) {
        val adapter = Runnable { listener() }
        addContentChangedListenerForJava(adapter)
        contentChangedTokens.addKotlinAdapter(listener, adapter)
    }

    @JvmStatic
    @JvmName("addContentChangedListener")
    fun addContentChangedListenerForJava(listener: Runnable) {
        val token = statics.addEventHandler(
            "WinUI4K.ClipboardContentChangedHandler",
            FoundationInterop.IID_EventHandler_Object,
            DataTransferInterop.IClipboardStatics_add_ContentChanged,
        ) { _, _ -> WinUiUtilities.invokeLater { listener.run() } }
        contentChangedTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addContentChangedListener]. */
    @JvmSynthetic
    fun removeContentChangedListener(listener: () -> Unit) {
        val adapter = contentChangedTokens.removeKotlinAdapter(listener) ?: return
        removeContentChangedListenerForJava(adapter)
    }

    @JvmStatic
    @JvmName("removeContentChangedListener")
    fun removeContentChangedListenerForJava(listener: Runnable) {
        val token = contentChangedTokens.remove(listener) ?: return
        statics.removeEventHandler(DataTransferInterop.IClipboardStatics_remove_ContentChanged, token)
    }
}
