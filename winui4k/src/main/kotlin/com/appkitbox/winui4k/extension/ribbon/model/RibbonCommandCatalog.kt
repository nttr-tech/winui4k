package com.appkitbox.winui4k.extension.ribbon.model

import java.util.EventObject
import java.util.Locale

/**
 * Metadata of a command registered in a [RibbonCommandCatalog] (equivalent to the name / icon / enabled etc. of
 * Swing's Action). Changing the enabled or checked state is reflected in every item that references the same id.
 */
class RibbonCommandDescriptor @JvmOverloads constructor(
    /** Unique command id (e.g. "home.paste", "PASTE"). Case-insensitive. */
    val id: String,
    label: String,
    command: RibbonCommand? = null,
) : RibbonObservable() {
    /** Display label. */
    var label: String by observable(label)

    /** Icon. */
    var icon: RibbonIcon? by observable(null)

    /** Description used for the ScreenTip and command search. */
    var description: String? by observable(null)

    /** Shortcut (e.g. "Ctrl+B"). */
    var shortcut: String? by observable(null)

    /** Category used in the customization screen ("File", "Home", "Popular Commands", etc.). */
    var category: String? by observable(null)

    /** Search keywords and aliases. */
    val keywords: RibbonList<String> = RibbonList()

    /** Enabled state reflected in every item that references this id. */
    var isEnabled: Boolean by observable(true)

    /** Checked state reflected in every toggle item that references this id (null means not a toggle). */
    var isChecked: Boolean? by observable(null)

    /** The command itself. */
    var command: RibbonCommand? by observable(command)

    override fun toString(): String = "RibbonCommandDescriptor($id, $label)"
}

/** Registration or removal of a command ([RibbonCommandCatalog.addCommandsChangedListener]). */
class RibbonCommandsChangedEvent(
    source: RibbonCommandCatalog,
    /** Id of the registered or removed command. */
    val id: String,
) : EventObject(source) {
    /** The catalog where the change happened. */
    override fun getSource(): RibbonCommandCatalog = super.getSource() as RibbonCommandCatalog
}

/** Receives registrations and removals of commands. */
fun interface RibbonCommandsChangedListener {
    /** Called when a command is registered, replaced or removed. */
    fun commandsChanged(event: RibbonCommandsChangedEvent)
}

/** A change in the state of a registered command ([RibbonCommandCatalog.addCommandStateListener]). */
class RibbonCommandStateEvent(
    source: RibbonCommandCatalog,
    /** Id of the command whose state changed. */
    val id: String,
    /** The property that changed (`isEnabled`, `isChecked`, `label`, etc.). */
    val propertyName: String,
) : EventObject(source) {
    /** The catalog where the change happened. */
    override fun getSource(): RibbonCommandCatalog = super.getSource() as RibbonCommandCatalog
}

/** Receives changes in the state of registered commands. */
fun interface RibbonCommandStateListener {
    /** Called when a property of a registered command changes. */
    fun commandStateChanged(event: RibbonCommandStateEvent)
}

/** Execution of a command through the catalog ([RibbonCommandCatalog.addCommandExecutedListener]). */
class RibbonCommandExecutedEvent(
    source: RibbonCommandCatalog,
    /** Id of the executed command. */
    val id: String,
    /** Argument of the execution. */
    val parameter: Any?,
) : EventObject(source) {
    /** The catalog that executed it. */
    override fun getSource(): RibbonCommandCatalog = super.getSource() as RibbonCommandCatalog
}

/** Receives executions of commands through the catalog. */
fun interface RibbonCommandExecutedListener {
    /** Called when a command is executed with [RibbonCommandCatalog.execute]. */
    fun commandExecuted(event: RibbonCommandExecutedEvent)
}

/**
 * A registry of commands looked up by id, shared by the ribbon, toolbars, menus, search and shortcuts.
 * Items reference commands with [RibbonItemModel.commandId], and states (enabled, checked) are applied per id.
 * Ids are case-insensitive. Registration order is preserved, and registering again with the same id replaces the
 * command in place. Use it on the UI thread.
 */
open class RibbonCommandCatalog {
    private val descriptors = LinkedHashMap<String, RibbonCommandDescriptor>()
    private val commandsChangedListeners = mutableListOf<RibbonCommandsChangedListener>()
    private val stateListeners = mutableListOf<RibbonCommandStateListener>()
    private val executedListeners = mutableListOf<RibbonCommandExecutedListener>()

    /** Relays property changes of the registrations to state change events. */
    private val descriptorListener = RibbonPropertyChangeListener { event ->
        val descriptor = event.source as RibbonCommandDescriptor
        fireStateChanged(RibbonCommandStateEvent(this, descriptor.id, event.propertyName))
    }

    /** Registered commands (in registration order). */
    val commands: List<RibbonCommandDescriptor> get() = descriptors.values.toList()

    /** Registers a command (if the same id exists, replaces it while keeping its position). */
    fun register(descriptor: RibbonCommandDescriptor): RibbonCommandDescriptor {
        val key = keyOf(descriptor.id)
        descriptors[key]?.removePropertyChangeListener(descriptorListener)
        descriptors[key] = descriptor
        descriptor.addPropertyChangeListener(descriptorListener)
        fireCommandsChanged(descriptor.id)
        return descriptor
    }

    /** Registers a command. */
    @JvmOverloads
    @Suppress("LongParameterList") // The parameters map 1:1 to the main properties of RibbonCommandDescriptor (same shape as RibbonSpace)
    fun register(
        id: String,
        label: String,
        command: RibbonCommand,
        icon: RibbonIcon? = null,
        shortcut: String? = null,
        description: String? = null,
        category: String? = null,
    ): RibbonCommandDescriptor = register(
        RibbonCommandDescriptor(id, label, command).also {
            it.icon = icon
            it.shortcut = shortcut
            it.description = description
            it.category = category
        },
    )

    /** Removes a command. Returns false if it is not registered. */
    fun unregister(id: String): Boolean {
        val removed = descriptors.remove(keyOf(id)) ?: return false
        removed.removePropertyChangeListener(descriptorListener)
        fireCommandsChanged(id)
        return true
    }

    /** Finds a command by id. */
    fun find(id: String?): RibbonCommandDescriptor? = if (id == null) null else descriptors[keyOf(id)]

    /** The command for the id (null if none). */
    fun resolve(id: String?): RibbonCommand? = find(id)?.command

    /** Whether the command can execute (false if unregistered, disabled or without a command). */
    @JvmOverloads
    fun canExecute(id: String, parameter: Any? = null): Boolean {
        val descriptor = find(id) ?: return false
        val command = descriptor.command ?: return false
        return descriptor.isEnabled && command.canExecute(parameter)
    }

    /** Executes a command by id. Returns false if it is unregistered or cannot execute. */
    @JvmOverloads
    fun execute(id: String, parameter: Any? = null): Boolean {
        if (!canExecute(id, parameter)) return false
        val descriptor = find(id)!!
        descriptor.command!!.execute(parameter)
        val event = RibbonCommandExecutedEvent(this, descriptor.id, parameter)
        for (listener in executedListeners.toList()) listener.commandExecuted(event)
        return true
    }

    /** Sets the enabled state of a command (does nothing if unregistered). */
    fun setEnabled(id: String, enabled: Boolean) {
        find(id)?.isEnabled = enabled
    }

    /** Sets the checked state of a command (does nothing if unregistered). */
    fun setChecked(id: String, isChecked: Boolean?) {
        find(id)?.isChecked = isChecked
    }

    /** Subscribes to registrations and removals of commands. */
    fun addCommandsChangedListener(listener: RibbonCommandsChangedListener) {
        commandsChangedListeners += listener
    }

    /** Removes a listener registered with [addCommandsChangedListener]. */
    fun removeCommandsChangedListener(listener: RibbonCommandsChangedListener) {
        commandsChangedListeners -= listener
    }

    /** Subscribes to changes in the state of registered commands. */
    fun addCommandStateListener(listener: RibbonCommandStateListener) {
        stateListeners += listener
    }

    /** Removes a listener registered with [addCommandStateListener]. */
    fun removeCommandStateListener(listener: RibbonCommandStateListener) {
        stateListeners -= listener
    }

    /** Subscribes to executions of commands through the catalog. */
    fun addCommandExecutedListener(listener: RibbonCommandExecutedListener) {
        executedListeners += listener
    }

    /** Removes a listener registered with [addCommandExecutedListener]. */
    fun removeCommandExecutedListener(listener: RibbonCommandExecutedListener) {
        executedListeners -= listener
    }

    private fun fireCommandsChanged(id: String) {
        val event = RibbonCommandsChangedEvent(this, id)
        for (listener in commandsChangedListeners.toList()) listener.commandsChanged(event)
    }

    private fun fireStateChanged(event: RibbonCommandStateEvent) {
        for (listener in stateListeners.toList()) listener.commandStateChanged(event)
    }

    private companion object {
        /** Case-insensitive lookup key for an id (equivalent to .NET's OrdinalIgnoreCase). */
        fun keyOf(id: String): String = id.uppercase(Locale.ROOT)
    }
}
