package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/** Native-side processing of [WComponent]'s members (split out so that WComponent does not exceed detekt's LargeClass). Used from the UI thread. */
internal object ComponentSupport {
    /** The body of [WComponent.scrollRectToVisible]. Scrolls the ancestors so that [rect] inside [uiElement] (borrowed) becomes visible. */
    fun scrollRectToVisible(uiElement: ComPtr, rect: WRectangle) {
        val options = Activation.activate(XamlInterop.CLS_BringIntoViewOptions, XamlInterop.IID_IBringIntoViewOptions)
        try {
            options.putBool(XamlInterop.IBringIntoViewOptions_put_AnimationDesired, false)
            val boxed = PropertyValues.boxRect(rect.x.toDouble(), rect.y.toDouble(), rect.width.toDouble(), rect.height.toDouble())
            val target = try {
                boxed.queryInterface(FoundationInterop.IID_IReference_Rect)
            } finally {
                boxed.release()
            }
            try {
                options.call(XamlInterop.IBringIntoViewOptions_put_TargetRect, target.ptr)
            } finally {
                target.release()
            }
            uiElement.call(XamlInterop.IUIElement_StartBringIntoViewWithOptions, options.ptr)
        } finally {
            options.release()
        }
    }

    /**
     * Reads [WComponent.toolTipText]. If the ToolTipService.ToolTip of [dependencyObject] (borrowed) is a string, that string;
     * the string if it is a ToolTip ([WToolTip]) whose content is a string, otherwise or if there is none, null.
     */
    fun toolTipText(dependencyObject: ComPtr): String? {
        val current = toolTipServiceStatics.getPtrOrNull(XamlInterop.IToolTipServiceStatics_GetToolTip, dependencyObject)
            ?: return null
        try {
            val contentControl = current.queryInterfaceOrNull(XamlInterop.IID_IContentControl)
                ?: return PropertyValues.unboxString(current) // A string hint
            try {
                // A ToolTip (WToolTip) hint: its string if the content is a string
                val content = contentControl.getPtrOrNull(XamlInterop.IContentControl_get_Content) ?: return null
                try {
                    return PropertyValues.unboxString(content)
                } finally {
                    content.release()
                }
            } finally {
                contentControl.release()
            }
        } finally {
            current.release()
        }
    }

    /**
     * Removes the first accelerator whose key is [key] (a Windows virtual key) and whose modifiers are [modifiers]
     * (VirtualKeyModifiers) from the KeyboardAccelerators of [uiElement] (borrowed). true if removed, false if there is none.
     */
    fun removeKeyboardAccelerator(uiElement: ComPtr, key: Int, modifiers: Int): Boolean {
        val accelerators = uiElement.getPtr(XamlInterop.IUIElement_get_KeyboardAccelerators)
        try {
            for (index in 0 until accelerators.getInt(FoundationInterop.IVector_get_Size)) {
                val accelerator = accelerators.getPtr(FoundationInterop.IVector_GetAt, index)
                val matches = try {
                    accelerator.getInt(XamlInterop.IKeyboardAccelerator_get_Key) == key &&
                        accelerator.getInt(XamlInterop.IKeyboardAccelerator_get_Modifiers) == modifiers
                } finally {
                    accelerator.release()
                }
                if (matches) {
                    accelerators.call(FoundationInterop.IVector_RemoveAt, index)
                    return true
                }
            }
            return false
        } finally {
            accelerators.release()
        }
    }
}
