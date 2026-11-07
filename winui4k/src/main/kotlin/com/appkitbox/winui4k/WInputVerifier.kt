package com.appkitbox.winui4k

/**
 * javax.swing.InputVerifier-like: checks the input before focus leaves a component, and keeps focus there if it is not valid
 * (set it to [WComponent.inputVerifier]).
 *
 * When focus is about to move from the component it is set on to another component (clicking with the mouse, the Tab key, [WComponent.requestFocus] /
 * [WComponent.transferFocus], etc.), [shouldYieldFocus] is called on the UI thread, and if false, the focus move is cancelled
 * (TryCancel of WinUI's UIElement.LosingFocus). If [WComponent.verifyInputWhenFocusTarget] of the destination component is false
 * (a cancel button, etc.), no check is done. As in Swing, if the destination also has a [WComponent.inputVerifier], the move is also cancelled when its [verifyTarget]
 * is false.
 *
 * Not checked: moves between parts inside a component (the input field and buttons of a [WSpinner], etc.), moves to places that are not WinUI4K components
 * (dialogs ([WOptionPane] / [WContentDialog]), menus and Flyouts) (Swing does not check moves to another window either),
 * and when the window becomes inactive.
 *
 * While it is called, WinUI is in the middle of moving focus, so focus cannot be moved inside it (showing a dialog,
 * [WComponent.requestFocus], etc.). To show a notice, defer it with [WinUiUtilities.invokeLater]
 * (the Swing example's way of showing a JOptionPane inside verify cannot be used).
 */
abstract class WInputVerifier {
    /** InputVerifier.verify-like: true if the input of [input] is valid. Swing's rule is that it has no side effects (such as showing notices). */
    abstract fun verify(input: WComponent): Boolean

    /**
     * InputVerifier.shouldYieldFocus(source, target)-like: true if focus may be passed from [source] (the component this verifier is set on) to [target].
     * The default is the result of [verify]. Side effects such as showing notices are done here (as in Swing).
     */
    open fun shouldYieldFocus(source: WComponent, target: WComponent): Boolean = verify(source)

    /**
     * InputVerifier.verifyTarget-like: true if this component ([input]) may receive focus. Called when focus moves from another
     * component to [input], after [shouldYieldFocus] of the component it moves from returns true. The default is true.
     */
    open fun verifyTarget(input: WComponent): Boolean = true
}
