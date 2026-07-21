package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.Hstring
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS

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
        WinRt.composeDefault(Abi.CLS_XamlUICommand, Abi.IID_IXamlUICommandFactory),
    ) {
        if (label.isNotEmpty()) this.label = label
    }

    /** The IXamlUICommand view holding Label / ExecuteRequested, etc. (also used by StandardUICommand). */
    private val xamlUICommand: ComPtr by lazy {
        inspectable.queryInterface(Abi.IID_IXamlUICommand)
    }

    /** The ICommand view required by put_Command (XamlUICommand implements ICommand). */
    private val icommand: ComPtr by lazy { inspectable.queryInterface(Abi.IID_ICommand) }

    override val commandPtr: MemorySegment
        get() = icommand.ptr

    /** ExecuteRequested event tokens registered via addExecuteListener. */
    private val executeTokens = ListenerTokens<(String?) -> Unit>()

    /** The label shown on the control (XamlUICommand.Label). */
    var label: String
        get() = xamlUICommand.getString(Abi.IXamlUICommand_get_Label)
        set(value) = Hstring.use(value) { h -> xamlUICommand.call(Abi.IXamlUICommand_put_Label, h) }

    /** The icon shown on the control (XamlUICommand.IconSource). Creates and passes a SymbolIconSource. */
    var icon: Symbol? = null
        set(value) {
            field = value
            if (value == null) {
                xamlUICommand.call(Abi.IXamlUICommand_put_IconSource, MemorySegment.NULL)
                return
            }
            val iconSource = value.createIconSource()
            xamlUICommand.call(Abi.IXamlUICommand_put_IconSource, iconSource.ptr)
            iconSource.release()
        }

    /** The description shown in the tooltip (XamlUICommand.Description). */
    var description: String
        get() = xamlUICommand.getString(Abi.IXamlUICommand_get_Description)
        set(value) = Hstring.use(value) { h ->
            xamlUICommand.call(Abi.IXamlUICommand_put_Description, h)
        }

    /** The access key (XamlUICommand.AccessKey). */
    var accessKey: String
        get() = xamlUICommand.getString(Abi.IXamlUICommand_get_AccessKey)
        set(value) = Hstring.use(value) { h ->
            xamlUICommand.call(Abi.IXamlUICommand_put_AccessKey, h)
        }

    /**
     * Adds a keyboard shortcut (XamlUICommand.KeyboardAccelerators).
     * While a control with this command is visible, pressing it fires ExecuteRequested.
     */
    fun addKeyboardAccelerator(key: VirtualKey, vararg modifiers: VirtualKeyModifier) {
        val accelerator = createKeyboardAccelerator(key, modifiers)
        val accelerators = xamlUICommand.getPtr(Abi.IXamlUICommand_get_KeyboardAccelerators)
        accelerators.call(Abi.IVector_Append, accelerator.ptr)
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
            Abi.IID_XamlUICommandExecuteRequestedHandler,
            Abi.IXamlUICommand_add_ExecuteRequested,
        ) { _, args ->
            val boxed = ComPtr(args).getPtrOrNull(Abi.IExecuteRequestedEventArgs_get_Parameter)
            val parameter = boxed?.let {
                try {
                    WinRt.unboxString(it)
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
        xamlUICommand.removeEventHandler(Abi.IXamlUICommand_remove_ExecuteRequested, token)
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
        get() = StandardUICommandKind.of(inspectable.getInt(Abi.IStandardUICommand_get_Kind))

    private companion object {
        /** IStandardUICommandFactory.CreateInstanceWithKind(kind, outer, out inner, out instance). */
        fun createWithKind(kind: StandardUICommandKind): ComPtr = Arena.ofConfined().use { a ->
            val factory =
                WinRt.factory(Abi.CLS_StandardUICommand, Abi.IID_IStandardUICommandFactory)
            val inner = a.allocate(ADDRESS)
            val instance = a.allocate(ADDRESS)
            factory.call(
                Abi.IStandardUICommandFactory_CreateInstanceWithKind,
                kind.native, MemorySegment.NULL, inner, instance,
            )
            factory.release()
            ComPtr(instance.get(ADDRESS, 0))
        }
    }
}
