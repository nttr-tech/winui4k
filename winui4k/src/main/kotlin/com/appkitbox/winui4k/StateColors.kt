package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * A table of the theme resource keys (lightweight styling) for the colors of each visual state of button-type controls,
 * and the factor by which the color's opacity is multiplied in that state.
 * The keys are the names the control's template references as `{ThemeResource ...}` in the visual state Storyboards
 * (microsoft-ui-xaml controls/dev/CommonStyles/<Control>_themeresources.xaml). Putting a color with the same key in the
 * control's own FrameworkElement.Resources changes the color of that state.
 * The normal state (the color decided by the Style's Setter) does not pick up keys put after Loaded, so the caller also
 * sets local values of Control.Background / Foreground (doc/troubleshooting.md).
 */
internal class StateColorKeys(private val factors: Map<String, Double>) {
    /** Puts each key's state color ([color] with its opacity multiplied by the factor) in [control]'s Resources (replacing if present). */
    fun put(control: WControl, color: WColor) {
        withResources(control) { resources ->
            for ((key, factor) in factors) resources.put(key, color.withAlphaScaled(factor))
        }
    }

    /** Removes the keys put with [put] (back to the theme's default colors). */
    fun remove(control: WControl) {
        withResources(control) { resources ->
            for (key in factors.keys) resources.remove(key)
        }
    }

    companion object {
        /** A table with no keys (for controls without per-visual-state colors). */
        val NONE = StateColorKeys(emptyMap())

        /**
         * A table of the keys made by appending a state name (a key of [suffixes]; "" is the normal state) to [baseKey], and
         * their factors. If [prefixes] is given, the keys with each of them prepended to [baseKey] are all included as well.
         */
        fun of(baseKey: String, suffixes: Map<String, Double>, prefixes: List<String> = listOf("")): StateColorKeys =
            StateColorKeys(prefixes.flatMap { prefix -> suffixes.map { (suffix, factor) -> "$prefix$baseKey$suffix" to factor } }.toMap())

        /** A table holding all [keys] with factor 1 (the same color). */
        fun uniform(keys: List<String>): StateColorKeys = StateColorKeys(keys.associateWith { 1.0 })
    }
}

// Opacity factors for pointer over and pressed. The background matches AccentFillColorSecondary / Tertiary at 0.9 / 0.8
// (Common_themeresources_any.xaml), and the text matches TextFillColorSecondary / Primary (about 0.69 in the light theme)
internal const val POINTER_OVER_BACKGROUND_OPACITY = 0.9
internal const val PRESSED_BACKGROUND_OPACITY = 0.8
internal const val PRESSED_FOREGROUND_OPACITY = 0.7

/** Background factors for normal, pointer over and pressed (the background of WButton and WToggleButton). */
internal val BUTTON_BACKGROUND_SHADES = mapOf("" to 1.0, "PointerOver" to POINTER_OVER_BACKGROUND_OPACITY, "Pressed" to PRESSED_BACKGROUND_OPACITY)

/** Text color factors for normal, pointer over and pressed (the text color of WButton and WToggleButton). */
internal val BUTTON_FOREGROUND_SHADES = mapOf("" to 1.0, "PointerOver" to 1.0, "Pressed" to PRESSED_FOREGROUND_OPACITY)

/** Sets the local value of Control.Background to a solid brush of [color]. If null, clears the setting and returns to the theme default. */
internal fun WControl.putLocalBackground(color: WColor?) {
    if (color == null) {
        clearControlProperty(XamlInterop.IControlStatics_get_BackgroundProperty)
        return
    }
    val brush = color.createBrush()
    try {
        control.call(XamlInterop.IControl_put_Background, brush.ptr)
    } finally {
        brush.release()
    }
}

/** The color of the effective value of Control.Background (null if it is not a solid brush). */
internal fun WControl.nativeBackground(): WColor? {
    val brush = control.getPtrOrNull(XamlInterop.IControl_get_Background) ?: return null
    return try {
        colorOf(brush)
    } finally {
        brush.release()
    }
}

private inline fun withResources(control: WControl, block: (ElementResources) -> Unit) {
    val dictionary = control.frameworkElement.getPtr(XamlInterop.IFrameworkElement_get_Resources)
    val map = try {
        dictionary.queryInterface(FoundationInterop.IID_IMap_Object_Object)
    } finally {
        dictionary.release()
    }
    try {
        block(ElementResources(map))
    } finally {
        map.release()
    }
}

/** The color with only its opacity multiplied by [factor] (rounded). */
private fun WColor.withAlphaScaled(factor: Double): WColor = WColor(red, green, blue, Math.round(alpha * factor).toInt())

/** Puts color brushes into and removes them from FrameworkElement.Resources (IMap<Object, Object>). */
private class ElementResources(private val map: ComPtr) {
    /** Puts a SolidColorBrush of [color] at [key] (replacing any existing one). */
    fun put(key: String, color: WColor) {
        val boxedKey = PropertyValues.boxString(key)
        val brush = color.createBrush()
        try {
            Ffi.backend.withScope { scope ->
                val replaced = scope.allocate(1, 1)
                map.call(FoundationInterop.IMap_Insert, boxedKey.ptr, brush.ptr, replaced)
            }
        } finally {
            brush.release()
            boxedKey.release()
        }
    }

    /** Removes [key] if present (IMap.Remove fails for a missing key, so check with HasKey). */
    fun remove(key: String) {
        val boxedKey = PropertyValues.boxString(key)
        try {
            val present = Ffi.backend.withScope { scope ->
                val out = scope.allocate(1, 1)
                map.call(FoundationInterop.IMap_HasKey, boxedKey.ptr, out)
                Ffi.backend.memory.getByte(out, 0) != 0.toByte()
            }
            if (present) map.call(FoundationInterop.IMap_Remove, boxedKey.ptr)
        } finally {
            boxedKey.release()
        }
    }
}
