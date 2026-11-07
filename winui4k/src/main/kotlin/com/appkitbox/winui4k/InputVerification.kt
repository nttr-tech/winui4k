package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * The underpinnings of [WComponent.inputVerifier]. On UIElement.LosingFocus (raised before focus moves, bubbling up from the element focus moves away from),
 * asks the [WInputVerifier], and if it does not pass, cancels the focus move with LosingFocusEventArgs.TryCancel.
 */
internal object InputVerification {
    /** Whether a check is in progress. Focus moves that happen during the check are not checked again (Swing's InputVerifier does not nest either). */
    private var verifying = false

    /** Whether a verifier cancelled a focus move during the last [focusUnlessRefused]. */
    private var refused = false

    /**
     * The result of [focus] (which calls UIElement.Focus). However, false if a verifier cancelled the focus move in the meantime
     * (UIElement.Focus returns true even when cancelled in LosingFocus; Swing's requestFocusInWindow returns false).
     */
    fun focusUnlessRefused(focus: () -> Boolean): Boolean {
        refused = false
        val focused = focus()
        return focused && !refused
    }

    /** LosingFocus of [owner]. [args] is the LosingFocusEventArgs (borrowed). */
    fun onLosingFocus(owner: WComponent, args: ComPtr) {
        val verifier = owner.inputVerifier ?: return
        if (verifying) return
        val target = targetOf(owner, args) ?: return
        if (!target.verifyInputWhenFocusTarget) return
        verifying = true
        val allowed = try {
            verifier.shouldYieldFocus(owner, target) && (target.inputVerifier?.verifyTarget(target) ?: true)
        } finally {
            verifying = false
        }
        if (!allowed) {
            refused = true
            cancel(args)
        }
    }

    /**
     * If focus moves from [owner] (itself or a template part) to a WinUI4K component outside [owner], the destination.
     * null for moves between parts inside [owner] (including ones bubbling up from descendants), or when there is no destination or it is not a WinUI4K component
     * (dialogs, menus, etc.).
     */
    private fun targetOf(owner: WComponent, args: ComPtr): WComponent? {
        val old = args.getPtrOrNull(XamlInterop.ILosingFocusEventArgs_get_OldFocusedElement) ?: return null
        try {
            if (!isOwnFocusPart(owner, old)) return null
        } finally {
            old.release()
        }
        val new = args.getPtrOrNull(XamlInterop.ILosingFocusEventArgs_get_NewFocusedElement) ?: return null
        try {
            if (isOwnFocusPart(owner, new)) return null
            return WrapperLookup.nearestWrapper(listOf(generateSequence(owner) { it.parent }.last()), new)
        } finally {
            new.release()
        }
    }

    private fun cancel(args: ComPtr) {
        Ffi.backend.withScope { scope ->
            val out = scope.allocate(1, 1)
            args.call(XamlInterop.ILosingFocusEventArgs_TryCancel, out)
        }
    }
}
