import io
import re
import sys

src, dst = sys.argv[1], sys.argv[2]
text = io.open(src, encoding="utf-8").read()

REGION_JA = {
    "Home · Draw": "Home › Draw",
    "Home · Modify": "Home › Modify",
    "Home · Annotation": "Home › Annotation",
    "Home · Layers": "Home › Layers",
    "Insert · Block": "Insert › Block",
    "Home · Properties": "Home › Properties",
    "Home · Groups": "Home › Groups",
    "Home · Utilities": "Home › Utilities",
    "Home · Clipboard": "Home › Clipboard",
    "View": "View",
    "Insert": "Insert",
    "Output": "Output",
    "Manage": "Manage",
    "3D": "3D",
    "Status bar": "Status bar",
    "Application": "Application",
    "Showcase additions": "Other",
}


def snake(name):
    s = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name)
    s = re.sub(r"(?<=[A-Z])(?=[A-Z][a-z])", "_", s)
    return s.upper()


token = re.compile(
    r'#region\s+(?P<region>[^\r\n]+)|public static string (?P<name>\w+) \{ get; \} =\s*(?P<value>(?:"(?:[^"\\]|\\.)*"\s*\+?\s*)+);',
)
out = []
names = []
for m in token.finditer(text):
    if m.group("region"):
        title = m.group("region").strip()
        out.append("")
        out.append("    // ---- " + REGION_JA.get(title, title))
        continue
    name = m.group("name")
    parts = re.findall(r'"((?:[^"\\]|\\.)*)"', m.group("value"))
    kname = snake(name)
    names.append(kname)
    out.append("")
    if len(parts) == 1:
        out.append('    const val %s = "%s"' % (kname, parts[0].replace("$", "\\$")))
    else:
        out.append("    const val %s =" % kname)
        for i, p in enumerate(parts):
            sep = " +" if i < len(parts) - 1 else ""
            out.append('        "%s"%s' % (p.replace("$", "\\$"), sep))

header = '''package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.ribbon.RibbonIcon

/**
 * Line-art icons for the CAD commands (ported from CadIcons in the RibbonSpace demo. MIT License, THIRD-PARTY-NOTICES.md).
 *
 * Layered paths on a 32 x 32 design grid (`[viewbox=32;stroke=1.8;color=#E8A33D]M...` separated by `|`).
 * Layers without a color follow the theme's icon color; fixed colors are used for small accents (orange = result,
 * blue = grip, green = add / on, red = delete). Use [icon] to turn one into a [RibbonIcon].
 */
@Suppress("LargeClass") // A table that gathers the demo CAD icon definitions in one place
object CadIcons {
    /** Turns a layered path string into an icon whose path is in 32 units. */
    @JvmStatic
    fun icon(value: String): RibbonIcon = RibbonIcon.path(value, 32.0)
'''
body = "\n".join(out)
io.open(dst, "w", encoding="utf-8", newline="").write(header + body + "\n}\n")
print(len(names))
