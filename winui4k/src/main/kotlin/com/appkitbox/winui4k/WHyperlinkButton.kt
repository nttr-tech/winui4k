package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * JButton-like hyperlink display: WinUI 3's HyperlinkButton.
 * Setting [navigateUri] makes a click open the default browser.
 * If left unset, it behaves like a normal button and can handle Click via [addActionListener].
 */
class WHyperlinkButton(text: String = "", navigateUri: String = "") : WButtonBase(
    Activation.composeDefault(XamlInterop.CLS_HyperlinkButton, XamlInterop.IID_IHyperlinkButtonFactory),
) {
    /**
     * The URI opened on click (HyperlinkButton.NavigateUri). Set to "" to clear it.
     * Creates a Windows.Foundation.Uri via IUriRuntimeClassFactory.CreateUri and passes it in.
     */
    var navigateUri: String
        get() {
            val uri = inspectable.getPtrOrNull(XamlInterop.IHyperlinkButton_get_NavigateUri) ?: return ""
            return try {
                uri.getString(FoundationInterop.IUriRuntimeClass_get_AbsoluteUri)
            } finally {
                uri.release()
            }
        }
        set(value) {
            if (value.isEmpty()) {
                inspectable.call(XamlInterop.IHyperlinkButton_put_NavigateUri, null)
                return
            }
            val factory = Activation.factory(FoundationInterop.CLS_Uri, FoundationInterop.IID_IUriRuntimeClassFactory)
            val uri = Hstring.use(value) { h ->
                factory.getPtr(FoundationInterop.IUriRuntimeClassFactory_CreateUri, h)
            }
            factory.release()
            inspectable.call(XamlInterop.IHyperlinkButton_put_NavigateUri, uri.ptr)
            uri.release()
        }

    init {
        if (text.isNotEmpty()) this.text = text
        if (navigateUri.isNotEmpty()) this.navigateUri = navigateUri
    }
}
