import io
import json
import re
import sys

src, dst = sys.argv[1], sys.argv[2]
data = json.load(io.open(src, encoding="utf-8"))


def snake(name):
    s = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name)
    s = re.sub(r"(?<=[A-Z])(?=[A-Z][a-z])", "_", s)
    return s.upper()


def chunks(value, width=110):
    parts = []
    while value:
        parts.append(value[:width])
        value = value[width:]
    return parts


lines = []
for name in data:
    value = data[name].replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")
    parts = chunks(value)
    lines.append("")
    if len(parts) == 1:
        lines.append('    const val %s = "%s"' % (snake(name), parts[0]))
    else:
        lines.append("    const val %s =" % snake(name))
        for i, p in enumerate(parts):
            lines.append('        "%s"%s' % (p, " +" if i < len(parts) - 1 else ""))

header = '''package com.appkitbox.winui4k.sample.ribbon.cad

/**
 * Artwork for the CAD demo (strings obtained by running CadArt from the RibbonSpace demo. MIT License, THIRD-PARTY-NOTICES.md).
 *
 * Represents the pictures for the progressive ScreenTips (`*_HELP`, on a 160 grid), block samples (`BLOCK_*`), hatch
 * pattern samples (`HATCH_*`) and visual style samples (`STYLE_*`) as layered paths. Orange marks the result of the
 * operation, blue marks grips.
 */
@Suppress("LargeClass") // A table that gathers the demo artwork definitions in one place
object CadArt {'''
io.open(dst, "w", encoding="utf-8", newline="").write(header + "\n".join(lines) + "\n}\n")
print(len(data))
