package com.appkitbox.winui4k.extension.ribbon.model

/**
 * A command executed by a ribbon item (equivalent to .NET's ICommand, or the execution part of Swing's Action).
 *
 * Implementing only [execute] lets you write it as a lambda (`RibbonCommand { p -> ... }`). To change whether it
 * can execute dynamically, also implement [canExecute] and [addCanExecuteChangedListener], which notifies when that
 * changes (usually you use [RibbonRelayCommand]). The view shows items whose command cannot execute as disabled.
 */
fun interface RibbonCommand {
    /** Executes the command. [parameter] is the per-item argument (checked state, selected value, color, string, etc.). */
    fun execute(parameter: Any?)

    /** Whether the command can execute with [parameter] (always true by default). */
    fun canExecute(parameter: Any?): Boolean = true

    /** Subscribes to changes in whether the command can execute (does nothing by default = it never changes). */
    fun addCanExecuteChangedListener(listener: Runnable) {
        // Default implementation for commands whose executability never changes
    }

    /** Removes a listener registered with [addCanExecuteChangedListener]. */
    fun removeCanExecuteChangedListener(listener: Runnable) {
        // Default implementation for commands whose executability never changes
    }
}
