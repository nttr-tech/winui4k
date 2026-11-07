# CAD サンプルのソースを生成するスクリプト

RibbonSpace (MIT License) の CAD デモ `samples/RibbonSpace.Demo` から、このサンプルの Kotlin ソースを
生成するためのスクリプト。生成したファイルはコミット済みなので、通常は実行しなくてよい
(RibbonSpace 側のデモを取り込み直すときだけ使う)。`<demo>` は RibbonSpace のチェックアウトの
`samples/RibbonSpace.Demo`、`<cad>` はこのサンプルの `src/main/kotlin/com/appkitbox/winui4k/sample/ribbon/cad`。

| 生成するファイル | 手順 |
|---|---|
| `CadIcons.kt` | `python -I gen_cad_icons.py <demo>/Cad/CadIcons.cs <cad>/CadIcons.kt` |
| `CadArt.kt` | `dotnet run --project cadart_dump -p:RibbonSpaceDemo=<demo> -- art.json` で CadArt の文字列を JSON に書き出し、`python -I gen_cad_art.py art.json <cad>/CadArt.kt` |
| `CadRibbonTabs.kt` | `python -I gen_cad_ribbon.py <demo>/Pages/CadPage.xaml <cad>/CadIcons.kt <cad>/CadArt.kt <cad>/CadRibbonTabs.kt` (上の 2 つを先に生成する) |

- `cad_ja.py` / `cad_tips_ja.py` は、ラベルとスクリーン ヒントの英語から日本語への対応表。
- `gen_cad_ribbon.py` は、画面の文字を `Messages.get("ribbon.…")` にし、英語 (XAML のまま) と日本語を
  `src/main/resources/.../cad/messages.properties` と `messages_ja.properties` の `# --- generated` の行から後ろに書く
  (その行より前の手書きの文字列は残す)。
- 生成後は `.\gradlew spotlessApply` で整形する。
