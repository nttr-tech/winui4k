package com.appkitbox.winui4k

/**
 * Component-like (as a dialog parent): what can be passed as the parent (parentComponent) of [WOptionPane] / [WFileChooser] and the owner
 * (owner) of [WContentDialog]. Implemented by [WComponent] (opens on the window the component is placed in) and [WFrame]
 * (opens on that window).
 *
 * Difference from Swing: in Swing JFrame is also a Component, so `JOptionPane.showMessageDialog(frame, …)` can be written, but WinUI4K's
 * [WFrame] is not a [WComponent]. So this type, implemented by both, is used as the parent type so that the same form can be written
 * (Java's `showMessageDialog(null, …)` is not ambiguous either). Implementations outside WinUI4K are not intended
 * (they cannot be implemented in Kotlin, and one implemented in Java does not work when passed).
 */
sealed interface WDialogParent
