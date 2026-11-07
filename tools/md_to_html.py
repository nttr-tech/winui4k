"""Turn a Markdown document under doc/ into a single HTML page with a table of contents on the left (links to images and samples keep the same relative paths as in the Markdown).

Usage (from the repository root; run pip install markdown-it-py the first time only):
    python tools/md_to_html.py                      # doc/swing_to_winui4k.md and doc/swing_to_winui4k_issues.md
    python tools/md_to_html.py doc/foo.md doc/bar.md

The output is the .html with the same name next to the Markdown. Links between Markdown files converted together (foo.md#x) are rewritten to .html.
A `<a id="x"></a>` before a heading becomes the heading's id, and the Markdown "table of contents" section is omitted in HTML because it duplicates the table of contents on the left.
"""
import html
import os
import re
import sys

from markdown_it import MarkdownIt

DEFAULT_SOURCES = ["doc/swing_to_winui4k.md", "doc/swing_to_winui4k_issues.md"]

CSS = """
:root {
  --bg: #ffffff; --fg: #1f2328; --muted: #59636e; --line: #d1d9e0; --soft: #f6f8fa;
  --side-bg: #f6f8fa; --accent: #0969da; --accent-soft: #ddf4ff; --code-bg: #f6f8fa; --mark: #fff8c5;
}
@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
    --bg: #0d1117; --fg: #e6edf3; --muted: #9198a1; --line: #3d444d; --soft: #151b23;
    --side-bg: #010409; --accent: #4493f8; --accent-soft: #121d2f; --code-bg: #151b23; --mark: #3b2e00;
  }
}
* { box-sizing: border-box; }
html { scroll-padding-top: 16px; }
body {
  margin: 0; background: var(--bg); color: var(--fg);
  font: 15px/1.75 "Segoe UI", "Yu Gothic UI", "Meiryo", system-ui, sans-serif;
}
a { color: var(--accent); text-decoration: none; }
a:hover { text-decoration: underline; }
#side {
  position: fixed; top: 0; left: 0; bottom: 0; width: 320px; overflow-y: auto;
  background: var(--side-bg); border-right: 1px solid var(--line); padding: 16px 12px 40px;
}
#side .title { font-weight: 600; font-size: 15px; margin: 0 4px 4px; line-height: 1.4; }
#side .other { font-size: 13px; margin: 0 4px 12px; display: block; }
#filter {
  width: 100%; padding: 6px 10px; margin-bottom: 10px; border: 1px solid var(--line); border-radius: 6px;
  background: var(--bg); color: var(--fg); font: inherit; font-size: 13px;
}
#toc, #toc ul { list-style: none; margin: 0; padding: 0; }
#toc ul { padding-left: 14px; border-left: 1px solid var(--line); margin: 2px 0 6px 10px; }
#toc a { display: block; padding: 2px 8px; border-radius: 6px; color: var(--fg); font-size: 13.5px; line-height: 1.5; }
#toc ul a { color: var(--muted); font-size: 13px; }
#toc a:hover { background: var(--soft); text-decoration: none; }
#toc a.current { background: var(--accent-soft); color: var(--accent); font-weight: 600; }
#toc details > summary { list-style: none; cursor: pointer; display: flex; align-items: center; }
#toc details > summary::-webkit-details-marker { display: none; }
#toc details > summary::before {
  content: "▸"; width: 14px; flex: none; color: var(--muted); font-size: 11px; transition: transform .15s;
}
#toc details[open] > summary::before { transform: rotate(90deg); }
#toc details > summary > a { flex: 1; }
#toc .leaf { padding-left: 14px; }
#toc .hidden { display: none; }
main { margin-left: 320px; padding: 24px 48px 120px; }
article { max-width: 1040px; margin: 0 auto; }
h1 { font-size: 30px; line-height: 1.3; border-bottom: 1px solid var(--line); padding-bottom: 10px; }
h2 { font-size: 23px; line-height: 1.35; margin-top: 56px; border-bottom: 1px solid var(--line); padding-bottom: 6px; }
h3 { font-size: 18px; margin-top: 36px; }
h4 { font-size: 16px; margin-top: 28px; }
h2 .anchor, h3 .anchor, h4 .anchor { visibility: hidden; margin-left: 6px; color: var(--muted); font-weight: 400; }
h2:hover .anchor, h3:hover .anchor, h4:hover .anchor { visibility: visible; }
code { font: 13px/1.6 "Cascadia Mono", "Consolas", monospace; background: var(--code-bg); padding: 1px 5px; border-radius: 4px; }
pre { background: var(--code-bg); border: 1px solid var(--line); border-radius: 8px; padding: 14px 16px; overflow-x: auto; }
pre code { padding: 0; background: none; font-size: 13px; }
pre code.hljs { background: none; padding: 0; }
.table { overflow-x: auto; margin: 14px 0; }
table { border-collapse: collapse; width: 100%; }
th, td { border: 1px solid var(--line); padding: 6px 12px; vertical-align: top; }
th:first-child, td:first-child { min-width: 7.5em; }
td code { overflow-wrap: break-word; }
th { background: var(--soft); text-align: left; }
tr:nth-child(2n) td { background: color-mix(in srgb, var(--soft) 50%, transparent); }
td img, p img { max-width: 100%; height: auto; border: 1px solid var(--line); border-radius: 6px; cursor: zoom-in; background: #fff; }
blockquote { margin: 0; padding: 0 16px; color: var(--muted); border-left: 4px solid var(--line); }
#lightbox {
  position: fixed; inset: 0; background: rgba(0,0,0,.75); display: none; align-items: center; justify-content: center;
  z-index: 20; cursor: zoom-out; padding: 24px;
}
#lightbox.open { display: flex; }
#lightbox img { max-width: 100%; max-height: 100%; border-radius: 6px; background: #fff; }
#top {
  position: fixed; right: 20px; bottom: 20px; width: 40px; height: 40px; border-radius: 20px; border: 1px solid var(--line);
  background: var(--bg); color: var(--fg); font-size: 18px; cursor: pointer; display: none;
}
#menu { display: none; }
@media (max-width: 900px) {
  #side { transform: translateX(-100%); transition: transform .2s; z-index: 10; width: min(320px, 85vw); }
  #side.open { transform: none; box-shadow: 0 0 24px rgba(0,0,0,.3); }
  main { margin-left: 0; padding: 56px 16px 120px; }
  #menu {
    display: block; position: fixed; top: 10px; left: 10px; z-index: 11; padding: 6px 12px; border-radius: 6px;
    border: 1px solid var(--line); background: var(--bg); color: var(--fg); font: inherit; font-size: 14px;
  }
}
"""

SCRIPT = """
(() => {
  const side = document.getElementById('side');
  const links = [...document.querySelectorAll('#toc a')];
  const byId = new Map(links.map(a => [decodeURIComponent(a.hash.slice(1)), a]));
  // Highlight the section being read in the table of contents and open its chapter
  const heads = [...document.querySelectorAll('article h2[id], article h3[id], article h4[id]')].filter(h => byId.has(h.id));
  let current = null;
  const mark = () => {
    let id = null;
    for (const h of heads) { if (h.getBoundingClientRect().top < 120) id = h.id; else break; }
    const a = id && byId.get(id);
    if (a === current) return;
    if (current) current.classList.remove('current');
    current = a || null;
    if (!current) return;
    current.classList.add('current');
    if (!filter.value) for (let d = current.closest('details'); d; d = d.parentElement.closest('details')) d.open = true;
    const r = current.getBoundingClientRect(), s = side.getBoundingClientRect();
    if (r.top < s.top + 60 || r.bottom > s.bottom - 20) current.scrollIntoView({ block: 'center' });
  };
  // Filter the table of contents (show entries whose chapter or section name contains the text)
  const filter = document.getElementById('filter');
  filter.addEventListener('input', () => {
    const q = filter.value.trim().toLowerCase();
    for (const li of document.querySelectorAll('#toc > li')) {
      const head = li.querySelector('a').textContent.toLowerCase();
      let any = !q || head.includes(q);
      for (const sub of li.querySelectorAll('ul li')) {
        const hit = !q || head.includes(q) || sub.textContent.toLowerCase().includes(q);
        sub.classList.toggle('hidden', !hit);
        any = any || hit;
      }
      li.classList.toggle('hidden', !any);
      for (const dd of li.querySelectorAll('details')) dd.open = !!q && any;
    }
  });
  const top = document.getElementById('top');
  let ticking = false;
  addEventListener('scroll', () => {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(() => { mark(); top.style.display = scrollY > 600 ? 'block' : 'none'; ticking = false; });
  });
  top.addEventListener('click', () => scrollTo({ top: 0 }));
  // On narrow screens, make the table of contents a drawer
  document.getElementById('menu').addEventListener('click', () => side.classList.toggle('open'));
  side.addEventListener('click', e => { if (e.target.closest('a') && innerWidth <= 900) side.classList.remove('open'); });
  // Clicking an image shows it enlarged
  const box = document.getElementById('lightbox'), boxImg = box.querySelector('img');
  document.querySelector('article').addEventListener('click', e => {
    const img = e.target.closest('img');
    if (!img || img.closest('a')) return;
    boxImg.src = img.src; boxImg.alt = img.alt; box.classList.add('open');
  });
  box.addEventListener('click', () => box.classList.remove('open'));
  addEventListener('keydown', e => { if (e.key === 'Escape') box.classList.remove('open'); });
  if (window.hljs) hljs.highlightAll();
  mark();
})();
"""

HLJS = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0"


def strip_tags(s):
    return html.unescape(re.sub(r"<[^>]+>", "", s)).strip()


def convert(source, sources):
    md = MarkdownIt("commonmark", {"html": True}).enable("table").enable("strikethrough")
    text = open(source, encoding="utf-8").read()
    body = md.render(text)

    # A paragraph with only <a id="x"></a> + a heading -> a heading with the id
    body = re.sub(r'<p><a id="([^"]+)"></a></p>\s*<h([1-6])>', r'<h\2 id="\1">', body)
    # Omit the Markdown "table of contents" section because it duplicates the table of contents on the left
    body = re.sub("<h2>\u76ee\u6b21</h2>.*?(?=<h2)", "", body, count=1, flags=re.S)

    # Give ids to headings without one
    counter = [0]

    def add_id(m):
        counter[0] += 1
        return f'<h{m.group(1)} id="sec-{counter[0]}">'

    body = re.sub(r"<h([2-4])>", add_id, body)
    # Add a link to itself (#) to each heading
    body = re.sub(
        r'<h([2-4]) id="([^"]+)">(.*?)</h\1>',
        lambda m: f'<h{m.group(1)} id="{m.group(2)}">{m.group(3)}<a class="anchor" href="#{m.group(2)}">#</a></h{m.group(1)}>',
        body,
    )

    # Links to Markdown files converted together point to .html
    names = {os.path.basename(s) for s in sources}

    def relink(m):
        name, frag = m.group(1), m.group(2) or ""
        return f'href="{name[:-3]}.html{frag}"' if name in names else m.group(0)

    body = re.sub(r'href="([\w.-]+\.md)(#[^"]*)?"', relink, body)
    # Scroll inside a table only when it does not fit horizontally
    body = body.replace("<table>", '<div class="table"><table>').replace("</table>", "</table></div>")
    # Allow code in tables to wrap after . ( , (never in the middle of a name)
    body = re.sub(
        r"<td>(.*?)</td>",
        lambda m: "<td>" + re.sub(
            r"<code>(.*?)</code>", lambda c: "<code>" + re.sub(r"([.(,])(?=\w)", r"\1<wbr>", c.group(1)) + "</code>", m.group(1)
        ) + "</td>",
        body,
        flags=re.S,
    )

    title_match = re.search(r"<h1[^>]*>(.*?)</h1>", body, re.S)
    title = strip_tags(title_match.group(1)) if title_match else os.path.basename(source)

    # Table of contents on the left: h2 are chapters, h3 are sections. Documents with few sections (whose items are h4) also include h4
    deepest = 4 if len(re.findall(r"<h3 ", body)) < 30 else 3
    items = []
    for m in re.finditer(rf'<h([2-{deepest}]) id="([^"]+)">(.*?)<a class="anchor"', body, re.S):
        items.append((int(m.group(1)), m.group(2), strip_tags(m.group(3))))

    def build(start, level):
        """Return the <li> list of the level headings in items[start:] and the position where they end."""
        out, i = [], start
        while i < len(items) and items[i][0] >= level:
            lv, hid, label = items[i]
            link = f'<a href="#{hid}">{html.escape(label)}</a>'
            children, i = build(i + 1, lv + 1)
            if children:
                out.append(f"<li><details><summary>{link}</summary><ul>{''.join(children)}</ul></details></li>")
            else:
                out.append(f'<li class="leaf">{link}</li>')
        return out, i

    toc, _ = build(0, 2)

    others = [s for s in sources if s != source]
    other_links = "".join(
        f'<a class="other" href="{os.path.basename(o)[:-3]}.html">→ {html.escape(first_title(o))}</a>' for o in others
    )
    page = f"""<!doctype html>
<html lang="ja">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)}</title>
<link rel="stylesheet" href="{HLJS}/styles/github.min.css" media="(prefers-color-scheme: light)">
<link rel="stylesheet" href="{HLJS}/styles/github-dark.min.css" media="(prefers-color-scheme: dark)">
<style>{CSS}</style>
</head>
<body>
<!-- Generated from {os.path.basename(source)} by tools/md_to_html.py. To change it, edit the Markdown and regenerate -->
<button id="menu" type="button">☰ \u76ee\u6b21</button>
<nav id="side">
<div class="title">{html.escape(title)}</div>
{other_links}
<input id="filter" type="search" placeholder="\u76ee\u6b21\u3092\u7d5e\u308a\u8fbc\u3080 (\u4f8b: JTable\u3001\u6ce8\u610f\u70b9)">
<ul id="toc">{"".join(toc)}</ul>
</nav>
<main><article>
{body}
</article></main>
<div id="lightbox"><img alt=""></div>
<button id="top" type="button" title="\u5148\u982d\u3078">↑</button>
<script src="{HLJS}/highlight.min.js"></script>
<script src="{HLJS}/languages/kotlin.min.js"></script>
<script src="{HLJS}/languages/powershell.min.js"></script>
<script>{SCRIPT}</script>
</body>
</html>
"""
    out = source[:-3] + ".html"
    open(out, "w", encoding="utf-8", newline="\n").write(page)
    print(f"{source} -> {out} ({len(items)} headings)")


def first_title(path):
    for line in open(path, encoding="utf-8"):
        if line.startswith("# "):
            return line[2:].strip()
    return os.path.basename(path)


def main(argv):
    sources = [s.replace("\\", "/") for s in (argv or DEFAULT_SOURCES)]
    sources = [s for s in sources if os.path.exists(s)]
    for source in sources:
        convert(source, sources)


if __name__ == "__main__":
    main(sys.argv[1:])
