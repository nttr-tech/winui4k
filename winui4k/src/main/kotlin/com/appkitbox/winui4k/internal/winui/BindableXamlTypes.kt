package com.appkitbox.winui4k.internal.winui

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.ArgKind
import com.appkitbox.winui4k.internal.ffi.api.CallDescriptor
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.Ptr
import com.appkitbox.winui4k.internal.ffi.api.ValueKind
import com.appkitbox.winui4k.internal.winrt.KComObject
import com.appkitbox.winui4k.internal.winrt.getString

/**
 * Supplements XAML type information (IXamlType) for types that serve as {Binding} sources but lack IsBindable.
 *
 * WinUI bindings do not resolve property names unless the source's type information is IsBindable
 * (PropertyInfoPropertyAccess). In the type information of the Tabular DLL in Windows App SDK 2.5.4-experimental,
 * TableViewGroupInfo (the DataContext of TableView group headers) lacks [Bindable], so `{Binding KeyText}` in the
 * default header template comes out empty. When the app's IXamlMetadataProvider returns these types, they are
 * replaced with a wrapper that returns true only for IsBindable and forwards everything else to the original type
 * information.
 */
internal object BindableXamlTypes {
    /** Types to supplement with IsBindable (full names). */
    private val TYPES = setOf("Microsoft.UI.Xaml.Controls.Tabular.TableViewGroupInfo")

    /** Full type name → replacement wrapper (cached for the process lifetime; XAML also keeps holding type information). */
    private val wrappers = HashMap<String, KComObject>()

    /**
     * If the type information in the out parameter [out] of IXamlMetadataProvider.GetXamlType is a target to
     * supplement, replaces it with a wrapper. The wrapper takes over the reference to the original type information
     * (the reference that was in out is not returned to the caller).
     */
    fun fixUp(out: Ptr) {
        val type = Ffi.backend.memory.getPtr(out, 0)
        if (type.isNull) return
        val xamlType = ComPtr(type)
        val name = xamlType.getString(IXamlType_get_FullName)
        if (name !in TYPES || xamlType.getBool(IXamlType_get_IsBindable)) return
        val wrapper = wrappers.getOrPut(name) { wrap(ComPtr(type).also { it.addRef() }) }
        xamlType.release()
        wrapper.addRef()
        Ffi.backend.memory.putPtr(out, 0, wrapper.primary)
    }

    /** An IXamlType that forwards to [inner] (IXamlType) and returns true only for get_IsBindable. */
    private fun wrap(inner: ComPtr): KComObject {
        fun forward(slot: Int, descriptor: CallDescriptor) = KComObject.Method(descriptor) { args ->
            inner.rawCall(slot, descriptor, *args.copyOfRange(1, args.size))
        }
        val methods = (FIRST_SLOT..LAST_SLOT).map { slot ->
            when (slot) {
                IXamlType_get_IsBindable -> KComObject.Method(DESC_OUT) { args ->
                    Ffi.backend.memory.putByte(args[1] as Ptr, 0, 1)
                    KComObject.S_OK
                }
                IXamlType_CreateFromString, IXamlType_GetMember, IXamlType_AddToVector -> forward(slot, DESC_PTR_OUT)
                IXamlType_AddToMap -> forward(slot, DESC_PTR_PTR_PTR)
                IXamlType_RunInitializer -> forward(slot, DESC_THIS)
                else -> forward(slot, DESC_OUT) // get_* / ActivateInstance (one out parameter)
            }
        }
        return KComObject("WinUI4K.BindableXamlType").addInterface(IID_IXamlType, methods)
    }

    // ---- Microsoft.UI.Xaml.Markup.IXamlType (extracted from Microsoft.UI.Xaml.winmd) ----
    private const val IID_IXamlType = "d24219df-7ec9-57f1-a27b-6af251d9c5bc"
    private const val FIRST_SLOT = 6 // get_BaseType
    private const val IXamlType_get_FullName = 8 // get_FullName(out HSTRING)
    private const val IXamlType_get_IsBindable = 14 // get_IsBindable(out boolean)
    private const val IXamlType_CreateFromString = 20 // CreateFromString(HSTRING, out Object)
    private const val IXamlType_GetMember = 21 // GetMember(HSTRING, out IXamlMember)
    private const val IXamlType_AddToVector = 22 // AddToVector(Object, Object)
    private const val IXamlType_AddToMap = 23 // AddToMap(Object, Object, Object)
    private const val IXamlType_RunInitializer = 24 // RunInitializer()
    private const val LAST_SLOT = IXamlType_RunInitializer

    private val DESC_THIS = CallDescriptor(ValueKind.I32, ArgKind.PTR)
    private val DESC_OUT = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR)
    private val DESC_PTR_OUT = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
    private val DESC_PTR_PTR_PTR = CallDescriptor(ValueKind.I32, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR, ArgKind.PTR)
}
