#!/usr/bin/env python3
"""Dump WinRT interface IIDs, vtable method order and class activation info from a .winmd.

Usage: dump_winmd.py <file.winmd> <FullTypeName> [<FullTypeName> ...]
"""
import sys, struct, uuid
import dnfile

def read_compressed_uint(b, pos):
    v = b[pos]
    if v & 0x80 == 0:
        return v, pos + 1
    if v & 0xC0 == 0x80:
        return ((v & 0x3F) << 8) | b[pos+1], pos + 2
    return ((v & 0x1F) << 24) | (b[pos+1] << 16) | (b[pos+2] << 8) | b[pos+3], pos + 4

def read_ser_string(b, pos):
    if b[pos] == 0xFF:
        return None, pos + 1
    ln, pos = read_compressed_uint(b, pos)
    return b[pos:pos+ln].decode('utf-8'), pos + ln

class Winmd:
    def __init__(self, path):
        self.pe = dnfile.dnPE(path)
        md = self.pe.net.mdtables
        self.typedefs = list(md.TypeDef)
        self.methods = list(md.MethodDef)
        self.customattrs = list(md.CustomAttribute)
        self.interfaceimpls = list(md.InterfaceImpl) if md.InterfaceImpl else []
        self.fields = list(md.Field) if md.Field else []
        self.constants = list(md.Constant) if md.Constant else []
        self.field_rid = {id(f): i + 1 for i, f in enumerate(self.fields)}
        self.by_name = {}
        for i, td in enumerate(self.typedefs):
            self.by_name[f"{td.TypeNamespace}.{td.TypeName}"] = (i, td)

    def _attr_type_name(self, ca):
        # ca.Type is a coded index -> MemberRef (ctor); its Class -> TypeRef
        try:
            memberref = ca.Type.row
            cls = memberref.Class.row
            return f"{cls.TypeNamespace}.{cls.TypeName}"
        except Exception:
            return None

    def attrs_for_typedef(self, td_index):
        """CustomAttribute rows whose parent is TypeDef #td_index (1-based rid = td_index+1)."""
        out = []
        for ca in self.customattrs:
            p = ca.Parent
            try:
                if p.table and p.table.name == 'TypeDef' and p.row_index == td_index + 1:
                    out.append(ca)
            except Exception:
                pass
        return out

    def attrs_for_interfaceimpl(self, ii_rid):
        out = []
        for ca in self.customattrs:
            p = ca.Parent
            try:
                if p.table and p.table.name == 'InterfaceImpl' and p.row_index == ii_rid:
                    out.append(ca)
            except Exception:
                pass
        return out

    def guid_of(self, td_index):
        for ca in self.attrs_for_typedef(td_index):
            if self._attr_type_name(ca) == 'Windows.Foundation.Metadata.GuidAttribute':
                blob = ca.Value.value_bytes()  # includes 0x0001 prolog
                a, b, c = struct.unpack_from('<IHH', blob, 2)
                d = blob[10:18]
                return f"{a:08x}-{b:04x}-{c:04x}-{d[0]:02x}{d[1]:02x}-" + ''.join(f"{x:02x}" for x in d[2:])
        return None

    def enum_values(self, td_index):
        """(name, value) pairs of an enum TypeDef's constant fields (value__ excluded)."""
        td = self.typedefs[td_index]
        rids = set()
        for f in td.FieldList:
            rid = self.field_rid.get(id(f.row))
            if rid is not None:
                rids.add(rid)
        out = []
        for c in self.constants:
            p = c.Parent
            try:
                if not (p.table and p.table.name == 'Field' and p.row_index in rids):
                    continue
            except Exception:
                continue
            frow = self.fields[p.row_index - 1]
            val = struct.unpack_from('<i', c.Value.value_bytes())[0]
            out.append((frow.Name, val))
        return out

    def methods_of(self, td_index):
        td = self.typedefs[td_index]
        return [m.row.Name for m in td.MethodList]

    def class_info(self, td_index):
        info = {'activatable': [], 'composable': [], 'statics': [], 'default_iface': None}
        for ca in self.attrs_for_typedef(td_index):
            an = self._attr_type_name(ca)
            blob = ca.Value.value_bytes()
            if an == 'Windows.Foundation.Metadata.ActivatableAttribute':
                # ctor variants: (uint32 version) or (System.Type factory, uint32) etc.
                s, _ = read_ser_string(blob, 2)
                # heuristics: if first fixed arg is Type it's a SerString; plain uint32 won't decode
                if s and s[0].isalpha():
                    info['activatable'].append(s)
                else:
                    info['activatable'].append('<default IActivationFactory>')
            elif an == 'Windows.Foundation.Metadata.ComposableAttribute':
                s, _ = read_ser_string(blob, 2)
                info['composable'].append(s)
            elif an == 'Windows.Foundation.Metadata.StaticAttribute':
                s, _ = read_ser_string(blob, 2)
                info['statics'].append(s)
        # default interface: InterfaceImpl rows for this class with DefaultAttribute
        for rid, ii in enumerate(self.interfaceimpls, start=1):
            try:
                if ii.Class.row_index != td_index + 1:
                    continue
            except Exception:
                continue
            for ca in self.attrs_for_interfaceimpl(rid):
                if self._attr_type_name(ca) == 'Windows.Foundation.Metadata.DefaultAttribute':
                    intf = ii.Interface.row
                    tn = getattr(intf, 'TypeName', None)
                    ns = getattr(intf, 'TypeNamespace', '')
                    info['default_iface'] = f"{ns}.{tn}" if tn else '<typespec>'
        return info

def main():
    path = sys.argv[1]
    names = sys.argv[2:]
    w = Winmd(path)
    for name in names:
        if name not in w.by_name:
            print(f"=== {name}: NOT FOUND")
            continue
        idx, td = w.by_name[name]
        flags = td.Flags
        is_interface = bool(flags.tdInterface)
        print(f"=== {name}")
        g = w.guid_of(idx)
        if g:
            print(f"    guid: {g}")
        if is_interface:
            for i, m in enumerate(w.methods_of(idx)):
                print(f"    vtbl[{6+i}]: {m}")
        else:
            ext = td.Extends
            try:
                base = f"{ext.row.TypeNamespace}.{ext.row.TypeName}"
            except Exception:
                base = "?"
            if base == 'System.MulticastDelegate':
                print("    (delegate) Invoke at vtbl[3]")
            elif base == 'System.Enum':
                for n, v in w.enum_values(idx):
                    print(f"    {n} = {v}")
            else:
                ci = w.class_info(idx)
                print(f"    base: {base}")
                print(f"    default_iface: {ci['default_iface']}")
                for a in ci['activatable']: print(f"    activatable factory: {a}")
                for c in ci['composable']: print(f"    composable factory: {c}")
                for s in ci['statics']: print(f"    statics: {s}")
    w.pe.close()

if __name__ == '__main__':
    main()
