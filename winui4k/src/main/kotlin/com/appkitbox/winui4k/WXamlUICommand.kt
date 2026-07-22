package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.com.lifetime.ComLifetime
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.PropertyValues
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winrt.removeEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop

/**
 * Equivalent to Swing's Action (implemented on the WinUI side): Microsoft.UI.Xaml.Input.XamlUICommand.
 *
 * Unlike [WCommand], this is an object on the XAML side with [label] / [icon] / [description].
 * Setting it as the command on a WAppBarButton or similar automatically fills in the label and
 * icon on a control that doesn't already have them, and wires up [addKeyboardAccelerator]'s shortcut.
 * Execution is received via the ExecuteRequested event ([addExecuteListener]).
 */
open class WXamlUICommand internal constructor(
    /** The default interface pointer (IXamlUICommand / IStandardUICommand). */
    internal val inspectable: ComPtr,
) : WCommandBase() {
    constructor(label: String = "") : this(
        Activation.composeDefault(XamlInterop.CLS_XamlUICommand, XamlInterop.IID_IXamlUICommandFactory),
    ) {
        if (label.isNotEmpty()) this.label = label
    }

    /** The record of COM references this wrapper owns (the same mechanism as WComponent). */
    private val lifetime = ComLifetime.adopt(this, inspectable)

    /** Ties ownership of [ptr] to this wrapper's lifetime. */
    private fun own(ptr: ComPtr): ComPtr = lifetime.own(ptr)

    /** The IXamlUICommand view holding Label / ExecuteRequested, etc. (also used by StandardUICommand). */
    private val xamlUICommand: ComPtr by lazy {
        own(inspectable.queryInterface(XamlInterop.IID_IXamlUICommand))
    }

    /** The ICommand view required by put_Command (XamlUICommand implements ICommand). */
    private val icommand: ComPtr by lazy { own(inspectable.queryInterface(XamlInterop.IID_ICommand)) }

    override val commandPtr: Ptr
        get() = icommand.ptr

    /** ExecuteRequested event tokens registered via addExecuteListener. */
    private val executeTokens = ListenerTokens<(String?) -> Unit>()

    /** The label shown on the control (XamlUICommand.Label). */
    var label: String
        get() = xamlUICommand.getString(XamlInterop.IXamlUICommand_get_Label)
        set(value) = Hstring.use(value) { h -> xamlUICommand.call(XamlInterop.IXamlUICommand_put_Label, h) }

    /** The icon shown on the control (XamlUICommand.IconSource). Creates and passes a SymbolIconSource. */
    var icon: Symbol? = null
        set(value) {
            field = value
            if (value == null) {
                xamlUICommand.call(XamlInterop.IXamlUICommand_put_IconSource, null)
                return
            }
            val iconSource = value.createIconSource()
            xamlUICommand.call(XamlInterop.IXamlUICommand_put_IconSource, iconSource.ptr)
            iconSource.release()
        }

    /** The description shown in the tooltip (XamlUICommand.Description). */
    var description: String
        get() = xamlUICommand.getString(XamlInterop.IXamlUICommand_get_Description)
        set(value) = Hstring.use(value) { h ->
            xamlUICommand.call(XamlInterop.IXamlUICommand_put_Description, h)
        }

    /** The access key (XamlUICommand.AccessKey). */
    var accessKey: String
        get() = xamlUICommand.getString(XamlInterop.IXamlUICommand_get_AccessKey)
        set(value) = Hstring.use(value) { h ->
            xamlUICommand.call(XamlInterop.IXamlUICommand_put_AccessKey, h)
        }

    /**
     * Adds a keyboard shortcut (XamlUICommand.KeyboardAccelerators).
     * While a control with this command is visible, pressing it fires ExecuteRequested.
     */
    fun addKeyboardAccelerator(key: VirtualKey, vararg modifiers: VirtualKeyModifier) {
        val accelerator = createKeyboardAccelerator(key, modifiers)
        val accelerators = xamlUICommand.getPtr(XamlInterop.IXamlUICommand_get_KeyboardAccelerators)
        accelerators.call(FoundationInterop.IVector_Append, accelerator.ptr)
        accelerators.release()
        accelerator.release()
    }

    /**
     * ActionListener-like: subscribes to the command's execution (XamlUICommand.ExecuteRequested).
     * The listener receives the control's commandParameter (null if unset).
     */
    fun addExecuteListener(listener: (parameter: String?) -> Unit) {
        val token = xamlUICommand.addEventHandler(
            "WinUI4K.ExecuteRequestedHandler",
            XamlInterop.IID_XamlUICommandExecuteRequestedHandler,
            XamlInterop.IXamlUICommand_add_ExecuteRequested,
        ) { _, args ->
            val boxed = ComPtr(args).getPtrOrNull(XamlInterop.IExecuteRequestedEventArgs_get_Parameter)
            val parameter = boxed?.let {
                try {
                    PropertyValues.unboxString(it)
                } finally {
                    it.release()
                }
            }
            listener(parameter)
        }
        executeTokens.add(listener, token)
    }

    /** Unsubscribes a listener registered via [addExecuteListener]. */
    fun removeExecuteListener(listener: (parameter: String?) -> Unit) {
        val token = executeTokens.remove(listener) ?: return
        xamlUICommand.removeEventHandler(XamlInterop.IXamlUICommand_remove_ExecuteRequested, token)
    }
}

/**
 * Microsoft.UI.Xaml.Input.StandardUICommandKind (the kind of predefined command).
 * Values extracted from the winmd.
 */
enum class StandardUICommandKind(internal val native: Int) {
    NONE(0),
    CUT(1),
    COPY(2),
    PASTE(3),
    SELECT_ALL(4),
    DELETE(5),
    SHARE(6),
    SAVE(7),
    OPEN(8),
    CLOSE(9),
    PAUSE(10),
    PLAY(11),
    STOP(12),
    FORWARD(13),
    BACKWARD(14),
    UNDO(15),
    REDO(16),
    ;

    internal companion object {
        fun of(native: Int): StandardUICommandKind = entries.first { it.native == native }
    }
}

/**
 * Equivalent to a predefined Action: Microsoft.UI.Xaml.Input.StandardUICommand (a XamlUICommand subclass).
 * Comes with an OS-standard label, icon, shortcut (e.g. Ctrl+C), and description already set based on [kind].
 */
class WStandardUICommand(kind: StandardUICommandKind) : WXamlUICommand(createWithKind(kind)) {
    /** The predefined command's kind (StandardUICommand.Kind). */
    val kind: StandardUICommandKind
        get() = StandardUICommandKind.of(inspectable.getInt(XamlInterop.IStandardUICommand_get_Kind))

    private companion object {
        /** IStandardUICommandFactory.CreateInstanceWithKind(kind, outer, out inner, out instance). */
        fun createWithKind(kind: StandardUICommandKind): ComPtr = Ffi.backend.withScope { scope ->
            val factory =
                Activation.factory(XamlInterop.CLS_StandardUICommand, XamlInterop.IID_IStandardUICommandFactory)
            val inner = scope.allocate(8)
            val instance = scope.allocate(8)
            factory.call(
                XamlInterop.IStandardUICommandFactory_CreateInstanceWithKind,
                kind.native, null, inner, instance,
            )
            factory.release()
            ComPtr(Ffi.backend.memory.getPtr(instance, 0))
        }
    }
}
