package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.ffi.Hstring
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi
import jp.hisano.winui4k.winui.Async
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT

/**
 * The taskbar's right-click menu: Windows.UI.StartScreen's JumpList.
 * [load] fetches the current jump list, [add] appends items, and [save] applies the changes.
 *
 * Only usable by an app with a package identity. Under an unpackaged run (launching
 * java.exe directly), [isSupported] is false, so check it first.
 */
class WJumpList private constructor(private val jumpList: ComPtr) {
    companion object {
        /** Whether jump lists are usable in this environment (JumpList.IsSupported). */
        val isSupported: Boolean
            get() {
                val statics = WinRt.factory(Abi.CLS_JumpList, Abi.IID_IJumpListStatics)
                return try {
                    statics.getBool(Abi.IJumpListStatics_IsSupported)
                } finally {
                    statics.release()
                }
            }

        /** Loads the current jump list (awaits JumpList.LoadCurrentAsync's completion). */
        fun load(): WJumpList {
            val statics = WinRt.factory(Abi.CLS_JumpList, Abi.IID_IJumpListStatics)
            val operation = statics.getPtr(Abi.IJumpListStatics_LoadCurrentAsync)
            statics.release()
            val result = Async.awaitResult(
                operation, Abi.IID_AsyncOperationCompletedHandler_JumpList, "JumpList.LoadCurrentAsync",
            )
            operation.release()
            return WJumpList(result)
        }
    }

    /** The display kind of the system-managed groups ("Frequent" / "Recent"). */
    var systemGroupKind: JumpListSystemGroupKind
        get() = JumpListSystemGroupKind.of(jumpList.getInt(Abi.IJumpList_get_SystemGroupKind))
        set(value) = jumpList.call(Abi.IJumpList_put_SystemGroupKind, value.native)

    /** The list of custom items (a snapshot of JumpList.Items). */
    val items: List<WJumpListItem>
        get() {
            val vector = jumpList.getPtr(Abi.IJumpList_get_Items)
            try {
                return Arena.ofConfined().use { a ->
                    val size = a.allocate(JAVA_INT)
                    vector.call(Abi.IVector_get_Size, size)
                    (0 until size.get(JAVA_INT, 0)).map { i ->
                        val out = a.allocate(ADDRESS)
                        vector.call(Abi.IVector_GetAt, i, out)
                        WJumpListItem(ComPtr(out.get(ADDRESS, 0)))
                    }
                }
            } finally {
                vector.release()
            }
        }

    /** Appends a custom item to the end. */
    fun add(item: WJumpListItem) {
        val vector = jumpList.getPtr(Abi.IJumpList_get_Items)
        vector.call(Abi.IVector_Append, item.item.ptr)
        vector.release()
    }

    /** Removes all custom items. */
    fun removeAll() {
        val vector = jumpList.getPtr(Abi.IJumpList_get_Items)
        vector.call(Abi.IVector_Clear)
        vector.release()
    }

    /** Applies the changes to the taskbar (awaits JumpList.SaveAsync's completion). */
    fun save() {
        val action = jumpList.getPtr(Abi.IJumpList_SaveAsync)
        Async.await(action, "JumpList.SaveAsync")
        action.release()
    }
}

/**
 * One jump list item (JumpListItem). Clicking it launches the app with [arguments] as its
 * command-line arguments.
 */
class WJumpListItem internal constructor(internal val item: ComPtr) {
    companion object {
        /** Creates an item with launch arguments (JumpListItem.CreateWithArguments). */
        fun of(arguments: String, displayName: String): WJumpListItem {
            val statics = WinRt.factory(Abi.CLS_JumpListItem, Abi.IID_IJumpListItemStatics)
            val item = Hstring.use(arguments) { a ->
                Hstring.use(displayName) { d ->
                    statics.getPtr(Abi.IJumpListItemStatics_CreateWithArguments, a, d)
                }
            }
            statics.release()
            return WJumpListItem(item)
        }

        /** Creates a separator item (JumpListItem.CreateSeparator). */
        fun separator(): WJumpListItem {
            val statics = WinRt.factory(Abi.CLS_JumpListItem, Abi.IID_IJumpListItemStatics)
            val item = statics.getPtr(Abi.IJumpListItemStatics_CreateSeparator)
            statics.release()
            return WJumpListItem(item)
        }
    }

    /** Whether this is a separator (JumpListItem.Kind == Separator). */
    val isSeparator: Boolean
        get() = item.getInt(Abi.IJumpListItem_get_Kind) == 1 // JumpListItemKind.Separator

    /** The launch arguments passed to the app when clicked (fixed at creation time). */
    val arguments: String
        get() = item.getString(Abi.IJumpListItem_get_Arguments)

    /** The name shown in the menu. */
    var displayName: String
        get() = item.getString(Abi.IJumpListItem_get_DisplayName)
        set(value) = Hstring.use(value) { h -> item.call(Abi.IJumpListItem_put_DisplayName, h) }

    /** The description shown in the tooltip. */
    var description: String
        get() = item.getString(Abi.IJumpListItem_get_Description)
        set(value) = Hstring.use(value) { h -> item.call(Abi.IJumpListItem_put_Description, h) }

    /** The heading of the group this item belongs to (the system's "Tasks" group if empty). */
    var groupName: String
        get() = item.getString(Abi.IJumpListItem_get_GroupName)
        set(value) = Hstring.use(value) { h -> item.call(Abi.IJumpListItem_put_GroupName, h) }
}

/** The display kind of the system-managed groups (JumpListSystemGroupKind). Values extracted from the winmd. */
enum class JumpListSystemGroupKind(internal val native: Int) {
    /** Don't show it. */
    NONE(0),

    /** Frequent. */
    FREQUENT(1),

    /** Recent. */
    RECENT(2),

    ;

    internal companion object {
        fun of(native: Int) = entries.first { it.native == native }
    }
}
