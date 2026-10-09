"""Generates Kotlin that builds the WinUI4K model from the ribbon (tabs) in CadPage.xaml of the RibbonSpace CAD demo."""
import html
import io
import re
import sys
import xml.etree.ElementTree as ET

src, icons_kt, art_kt, dst = sys.argv[1:5]

CAD_ICONS = set(re.findall(r"const val (\w+)", io.open(icons_kt, encoding="utf-8").read()))
CAD_ART = set(re.findall(r"const val (\w+)", io.open(art_kt, encoding="utf-8").read()))
missing = set()


def snake(name):
    s = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name)
    s = re.sub(r"(?<=[A-Z])(?=[A-Z][a-z])", "_", s)
    return s.upper()


raw = io.open(src, encoding="utf-8").read()
raw = re.sub(r'\sxmlns(:\w+)?="[^"]*"', "", raw)
raw = re.sub(r"\{cad:CadIcon Name=(\w+)\}", r"@\1", raw)
raw = re.sub(r"<(/?)(\w+):(\w+)", r"<\1\2_\3", raw)
raw = re.sub(r"\s(\w+):(\w+)=", r" \1_\2=", raw)
root = ET.fromstring(raw)


def kstr(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("$", "\\$") + '"'


def icon(value):
    if value is None:
        return "null"
    if value.startswith("@"):
        name = snake(value[1:])
        if name in CAD_ICONS:
            return "ic(CadIcons.%s)" % name
        if name in CAD_ART:
            return "art(CadArt.%s)" % name
        missing.add("icon:" + value)
        return "null"
    if len(value) == 1 and 0xE000 <= ord(value) <= 0xF8FF:
        return 'RibbonIcon.glyph("\\u%04X")' % ord(value)
    return "RibbonIcon.text(%s)" % kstr(value)


SIZES = {"Large": "RibbonItemSize.LARGE", "Medium": "RibbonItemSize.MEDIUM", "Small": "RibbonItemSize.SMALL"}


def size(el, default):
    return SIZES.get(el.get("Size"), default)


def screen_tip(el):
    """The ScreenTip attribute or a child <RibbonScreenTip>."""
    for child in el:
        if child.tag.endswith(".ScreenTip"):
            st = child[0]
            args = [kstr(st.get("Title")) if st.get("Title") else "null", kstr(html.unescape(st.get("Description", "")))]
            extra = []
            if st.get("ExtendedDescription"):
                extra.append("extendedDescription = %s" % kstr(st.get("ExtendedDescription")))
            if st.get("ExtendedImage"):
                extra.append("extendedImage = %s" % icon(st.get("ExtendedImage")))
            if st.get("HelpText"):
                extra.append("helpText = %s" % kstr(st.get("HelpText")))
            return "richTip(%s, %s)" % (", ".join(args), ", ".join(extra))
    if el.get("ScreenTip"):
        return "tip(%s)" % kstr(el.get("ScreenTip"))
    return None


def common(el, out):
    """The settings common to items (KeyTip, shortcut, ScreenTip, label visibility)."""
    sets = []
    if el.get("KeyTip"):
        sets.append("it.keyTip = %s" % kstr(el.get("KeyTip")))
    if el.get("Shortcut"):
        sets.append("it.shortcut = %s" % kstr(el.get("Shortcut")))
    tip = screen_tip(el)
    if tip:
        sets.append("it.screenTip = %s" % tip)
    if el.get("ShowLabel") == "False":
        sets.append("it.showLabel = false")
    if el.get("IsChecked") == "True":
        sets.append("it.isChecked = true")
    if el.get("GroupName"):
        sets.append("it.groupName = %s" % kstr(el.get("GroupName")))
    return out if not sets else out + ".also {\n" + "".join("    " + s + "\n" for s in sets) + "}"


def menu_entries(el, owner):
    """The items of a <MenuFlyout> (separators, radio items, toggles)."""
    entries = []
    menu = None
    for child in el:
        if child.tag == "MenuFlyout":
            menu = child
        elif child.tag.endswith(".Flyout"):
            for c in child:
                if c.tag == "MenuFlyout":
                    menu = c
    if menu is None:
        return entries
    for index, item in enumerate(menu):
        if item.tag == "MenuFlyoutSeparator":
            entries.append("sep()")
            continue
        label = item.get("Text")
        mid = "%s.%d" % (owner, index)
        if item.tag == "RadioMenuFlyoutItem":
            entries.append("radioItem(%s, %s, %s, %s)" % (kstr(mid), kstr(label), kstr(item.get("GroupName")), "true" if item.get("IsChecked") == "True" else "false"))
        else:
            entries.append("mi(%s, %s, %s)" % (kstr(mid), kstr(label), icon(item.get("Tag"))))
    return entries


def combo(el):
    cid = el.get("Id")
    special = {
        "layerCombo": "layerCombo()",
        "objectColor": "colorCombo(\"objectColor\")",
        "teColor": "colorCombo(\"teColor\")",
        "hcColor": "colorCombo(\"hcColor\")",
        "objectLinetype": "linetypeCombo(\"objectLinetype\")",
        "objectLineweight": "lineweightCombo(\"objectLineweight\")",
    }
    if cid in special:
        return special[cid]
    items = [s.text for s in el if s.tag == "x_String"]
    selected = el.get("Text") if el.get("Text") else (items[0] if items else None)
    args = [kstr(cid), kstr(el.get("Label")) if el.get("Label") else "null", icon(el.get("Icon")),
            el.get("InputWidth", "120") + ".0", "true" if el.get("IsEditable") == "True" else "false",
            kstr(selected) if selected else "null"] + [kstr(i) for i in items]
    return common(el, "combo(%s)" % ", ".join(args))


def item(el, depth=0):
    tag = el.tag
    iid = el.get("Id")
    label = el.get("Label")
    if tag == "RibbonButton":
        return common(el, "cmd(%s, %s, %s, %s)" % (kstr(iid or label), kstr(label), icon(el.get("Icon")), size(el, "RibbonItemSize.MEDIUM")))
    if tag == "RibbonToggleButton":
        return common(el, "tgl(%s, %s, %s, %s)" % (kstr(iid), kstr(label), icon(el.get("Icon")), size(el, "RibbonItemSize.SMALL")))
    if tag == "RibbonSplitButton":
        entries = menu_entries(el, iid)
        if not entries:
            entries = EXTRA_MENUS.get(iid, [])
        follow = "true" if el.get("FollowLastChoice") == "True" else "false"
        args = [kstr(iid), kstr(label), icon(el.get("Icon")), size(el, "RibbonItemSize.MEDIUM"), follow] + entries
        return common(el, "spl(%s)" % ",\n".join(args))
    if tag == "RibbonDropDownButton":
        entries = menu_entries(el, iid) or EXTRA_MENUS.get(iid, [])
        args = [kstr(iid), kstr(label), icon(el.get("Icon")), size(el, "RibbonItemSize.MEDIUM")] + entries
        return common(el, "ddn(%s)" % ",\n".join(args))
    if tag == "RibbonComboBox":
        return combo(el)
    if tag == "RibbonFontComboBox":
        return "fontCombo(%s, %s, %s.0)" % (kstr(iid), kstr(el.get("Text", "Arial")), el.get("InputWidth", "130"))
    if tag == "RibbonSlider":
        return "slider(%s, %s, %s, %s.0, %s)" % (kstr(iid), kstr(label), icon(el.get("Icon")), el.get("InputWidth", "120"), el.get("Value", "0") + ".0" if "." not in el.get("Value", "0") else el.get("Value"))
    if tag == "RibbonSpinner":
        value = el.get("Value", "0")
        return "spinner(%s, %s, %s.0, %s, %s)" % (kstr(iid), kstr(label), el.get("InputWidth", "70"), value if "." in value else value + ".0", kstr(el.get("Unit")) if el.get("Unit") else "null")
    if tag == "RibbonTextBox":
        return "textBox(%s, %s, %s.0)" % (kstr(iid), icon(el.get("Icon")), el.get("InputWidth", "140"))
    if tag == "RibbonStackPanel":
        vertical = "true" if el.get("Orientation") == "Vertical" else "false"
        children = [item(c, depth + 1) for c in el]
        return "stack(%s,\n%s)" % (vertical, ",\n".join(children))
    if tag == "RibbonButtonGroup":
        children = [item(c, depth + 1) for c in el]
        return "RibbonButtonGroupModel(\n%s)" % ",\n".join(children)
    if tag == "RibbonGallery":
        return gallery(el)
    if tag == "RibbonSeparator":
        return "RibbonSeparatorModel()"
    raise ValueError("unknown item: " + tag)


def gallery(el):
    gid = el.get("Id")
    items = []
    footer = []
    for child in el:
        if child.tag == "RibbonGalleryItem":
            items.append("gi(%s, %s, %s)" % (kstr(child.get("Label")), icon(child.get("Icon")), kstr(child.get("Category")) if child.get("Category") else "null"))
        elif child.tag.endswith("FooterItems"):
            for index, f in enumerate(child):
                footer.append("mi(%s, %s, %s)" % (kstr("%s.footer%d" % (gid, index)), kstr(f.get("Label")), icon(f.get("Icon"))))
    sets = []
    for attr, prop, conv in (("ItemWidth", "itemWidth", float), ("ItemHeight", "itemHeight", float), ("MinColumns", "minColumns", int),
                             ("MaxColumns", "maxColumns", int), ("DropDownColumns", "dropDownColumns", int), ("Rows", "rows", int),
                             ("MaxDropDownHeight", "maxDropDownHeight", float)):
        if el.get(attr):
            value = conv(el.get(attr))
            sets.append("g.%s = %s" % (prop, ("%s" % value) if conv is int else ("%s" % float(value))))
    if el.get("IsDropDownOnly") == "True":
        sets.append("g.isDropDownOnly = true")
    if el.get("ShowItemLabels") == "False":
        sets.append("g.showLabels = false")
    if el.get("KeyTip"):
        sets.append("g.keyTip = %s" % kstr(el.get("KeyTip")))
    tip = screen_tip(el)
    if tip:
        sets.append("g.screenTip = %s" % tip)
    for f in footer:
        sets.append("g.menuItems.add(%s)" % f)
    body = "gallery(%s, %s, %s, %s,\n%s)" % (kstr(gid), kstr(el.get("Label")), icon(el.get("Icon")), size(el, "RibbonItemSize.LARGE"), ",\n".join(items))
    if sets:
        body += ".also { g ->\n" + "".join("    " + s + "\n" for s in sets) + "}"
    return body



# Items of the drop-down and split buttons that have no menu in the XAML
EXTRA_MENUS = {
    "teColumns": ['mi("teColumns.0", "No Columns", null)', 'mi("teColumns.1", "Dynamic Columns", null)', 'mi("teColumns.2", "Static Columns", null)'],
    "teBullets": ['mi("teBullets.0", "Off", null)', 'mi("teBullets.1", "Numbered", null)', 'mi("teBullets.2", "Lettered", null)', 'mi("teBullets.3", "Bulleted", null)'],
    "teLineSpacing": ['radioItem("teLineSpacing.0", "1.0x", "lineSpacing", true)', 'radioItem("teLineSpacing.1", "1.5x", "lineSpacing", false)', 'radioItem("teLineSpacing.2", "2.0x", "lineSpacing", false)'],
    "hcMatch": ['mi("hcMatch.0", "Use Current Origin", null)', 'mi("hcMatch.1", "Use Source Hatch Origin", null)'],
    "minimizeBehavior": [],
}


def group(el):
    gid = el.get("Id")
    children = [c for c in el if not c.tag.startswith("RibbonGroup.")]
    slide = [c for c in el if c.tag == "RibbonGroup.SlideOutItems"]
    items = [item(c) for c in children]
    sets = []
    if el.get("Icon"):
        sets.append("g.icon = %s" % icon(el.get("Icon")))
    if el.get("ScreenTip"):
        sets.append("g.description = %s" % kstr(el.get("ScreenTip")))
    if el.get("ReductionOrder"):
        sets.append("g.reductionOrder = %s" % el.get("ReductionOrder"))
    if el.get("IsDialogLauncherVisible") == "True":
        sets.append("g.isDialogLauncherVisible = true")
    if el.get("ItemsLayout") == "Rows":
        sets.append("g.itemsLayout = RibbonGroupItemsLayout.ROWS")
    if el.get("RowCount"):
        sets.append("g.rowCount = %s" % el.get("RowCount"))
    for s in slide:
        for c in s:
            sets.append("g.slideOutItems.add(\n%s,\n)" % item(c))
    body = "group(%s, %s,\n%s)" % (kstr(gid), kstr(el.get("Header")), ",\n".join(items))
    if sets:
        body += ".also { g ->\n" + "".join("    " + s + "\n" for s in sets) + "}"
    return body


functions = []
tab_calls = []
for tab in root.iter("RibbonTab"):
    tid = tab.get("Id")
    fname = re.sub(r"[^A-Za-z0-9]", "", tid)
    fname = (fname[:-3] if fname.endswith("Tab") else fname) + "Tab"
    groups = [group(g) for g in tab if g.tag == "RibbonGroup"]
    sets = []
    if tab.get("IsTabVisible") == "False":
        sets.append("t.isVisible = false")
    if tab.get("ContextualGroupId"):
        sets.append("t.contextualGroupId = %s" % kstr(tab.get("ContextualGroupId")))
    body = "tab(%s, %s, %s,\n%s)" % (kstr(tid), kstr(tab.get("Header")), kstr(tab.get("KeyTip")) if tab.get("KeyTip") else "null", ",\n".join(groups))
    if sets:
        body += ".also { t ->\n" + "".join("    " + s + "\n" for s in sets) + "}"
    functions.append(
        "/** The [%s] tab. */\n@Suppress(\"LongMethod\", \"CyclomaticComplexMethod\") // Declarative tab assembly generated from the XAML of the RibbonSpace CAD demo\nprivate fun %s(): RibbonTabModel = %s\n" % (tab.get("Header"), fname, body)
    )
    tab_calls.append(fname + "()")

header = '''package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonGroupModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupItemsLayout
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSeparatorModel
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel
import com.appkitbox.winui4k.sample.ribbon.shell.group
import com.appkitbox.winui4k.sample.ribbon.shell.radioItem
import com.appkitbox.winui4k.sample.ribbon.shell.tab

// The ribbon tabs of the RibbonSpace CAD demo (samples/RibbonSpace.Demo/Pages/CadPage.xaml, MIT License),
// converted by tools/gen_cad_ribbon.py into code that builds the WinUI4K model.

/** The CAD ribbon tabs (the regular tabs, the 3D workspace tabs and the contextual tabs). */
internal fun cadTabs(): List<RibbonTabModel> = listOf(
%s
)

''' % "".join("    %s,\n" % c for c in tab_calls)
io.open(dst, "w", encoding="utf-8", newline="").write(header + "\n".join(functions))
for m in sorted(missing):
    print("MISSING:", m)
