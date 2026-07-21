---
name: add-winui-component
description: WinUI 3 コントロールの Kotlin ラッパー (W* クラス) を winui4k に追加する。winmd から ABI 値 (IID / vtable スロット / enum 値) を機械的に抽出し、Abi.kt → swing パッケージ → GalleryApp の順に実装する。Use when adding a Kotlin wrapper for a WinUI 3 control (CheckBox, Slider, ToggleSwitch, ComboBox, ProgressBar, ...), when the user asks to 「コンポーネントを追加」「コントロールに対応」「ラッパーを実装」, or when extending an existing W* class with more WinUI APIs.
argument-hint: [WinUIコントロール名 (例: CheckBox)]
---

# WinUI コンポーネントの Kotlin ラッパー追加

Microsoft.UI.Xaml.Controls 配下のコントロールを、Swing 風の `W*` クラスとして
`src/main/kotlin/jp/hisano/winui4k/swing/` に追加する手順。対象: $ARGUMENTS

## 鉄則

- ABI 値 (IID / vtable スロット / enum 値) は**必ず** `tools/dump_winmd.py` で winmd から抽出する。
  記憶・推測・手書きの値は禁止 (Abi.kt 冒頭の方針)。C# ドキュメントの値もそのまま信用しない。
- 命名は Swing 風にする: WButton (JButton 風)、WLabel (JLabel 風)、WTextField (JTextField 風)。
  KDoc の 1 行目に「J〇〇 風: WinUI 3 の 〇〇。」と対応を書く。
- KDoc・コメントは日本語。既存ファイルのコメント密度とスタイルに合わせる。

## 手順

### 1. winmd を用意する

`build/winmd/metadata/Microsoft.UI.Xaml.winmd` があれば再利用する。無ければ取得する
(バージョンは Abi.kt ヘッダー記載の Microsoft.WindowsAppSDK.WinUI と一致させること):

```bash
mkdir -p build/winmd && cd build/winmd
curl -sL -o winui.nupkg "https://api.nuget.org/v3-flatcontainer/microsoft.windowsappsdk.winui/2.2.1/microsoft.windowsappsdk.winui.2.2.1.nupkg"
python -c "import zipfile; zipfile.ZipFile('winui.nupkg').extract('metadata/Microsoft.UI.Xaml.winmd')"
```

### 2. ABI 値を抽出する

```bash
python tools/dump_winmd.py build/winmd/metadata/Microsoft.UI.Xaml.winmd \
  Microsoft.UI.Xaml.Controls.CheckBox \
  Microsoft.UI.Xaml.Controls.ICheckBox
```

1. まず**クラス本体**をダンプし、`default_iface` / `activatable factory` /
   `composable factory` / `statics` を確認する
2. 次に既定インターフェースと、使う機能が宣言されている基底インターフェース
   (Primitives.IToggleButton など) をダンプし、guid と vtbl スロットを得る
3. enum はそのまま型名を渡すと `Name = value` 形式で出る
4. delegate (イベントハンドラ型) は `Invoke at vtbl[3]` と出る。guid も控える
5. 添付プロパティ (Canvas.Left, Grid.Row, ...) を使うなら `IXxxStatics` もダンプする。
   Get/Set の引数型 (UIElement か FrameworkElement か、値の型) はメソッドシグネチャ
   バイト列で確認する (下記スニペット参照)

継承ツリーの探索が必要なら、クラスの `base:` をたどって親クラスも順にダンプする。

構造体 (GridLength, Color など) のフィールド順は dump_winmd.py では出ない。
同じ dnfile でフィールドを直接列挙する
(シグネチャ末尾バイト: 0x02=bool, 0x05=u8, 0x08=i32, 0x0d=f64, 0x11=enum):

```bash
python - <<'EOF'
import dnfile
pe = dnfile.dnPE('build/winmd/metadata/Microsoft.UI.Xaml.winmd')
for td in pe.net.mdtables.TypeDef:
    if f"{td.TypeNamespace}.{td.TypeName}" == "Microsoft.UI.Xaml.GridLength":
        for f in td.FieldList:
            print(f.row.Name, f.row.Signature.value_bytes().hex())
EOF
```

Windows.UI.Color など OS 側の型は Microsoft.UI.Xaml.winmd に無い。
NuGet の `microsoft.windows.sdk.contracts` から
`ref/netstandard2.0/Windows.Foundation.UniversalApiContract.winmd` を展開して同様に調べる。

### 3. Abi.kt に定数を追加する

`src/main/kotlin/jp/hisano/winui4k/winui/Abi.kt` の既存セクションの形式に合わせる:

- `// ---- Microsoft.UI.Xaml.Controls.CheckBox ----` のセクションコメント
- `CLS_CheckBox` / `IID_ICheckBoxFactory` / `IID_ICheckBox` / スロット定数
  (`ICheckBox_put_XXX = n` の形式、末尾コメントにシグネチャの要点)
- IControl / IContentControl / IButtonBase / IUIElement / IFrameworkElement など
  **定義済みの共通インターフェースは再定義せず再利用する**。追加前に Abi.kt を検索すること

### 4. ラッパークラスを作成する

`src/main/kotlin/jp/hisano/winui4k/swing/W<SwingName>.kt` を新規作成する。

- 基底クラス: Control 派生なら `WControl`、Panel 派生 (StackPanel / Grid / Canvas /
  RelativePanel / ...) なら `WContainer` (Children の add / removeAll を継承)、
  それ以外の FrameworkElement 直系なら `WComponent`
- インスタンス生成 (手順 2 のクラス情報で分岐):
  - `composable factory` あり → `WinRt.composeDefault(Abi.CLS_X, Abi.IID_IXFactory)`
    (戻り値は既定インターフェースのポインタ)
  - `activatable factory: <default IActivationFactory>` →
    `WinRt.activate(Abi.CLS_X).queryInterface(Abi.IID_IX)`
- プロパティ・イベント・enum・構造体・ICommand などの実装パターンは
  [references/patterns.md](references/patterns.md) を必ず読んで踏襲する

### 5. GalleryApp にデモページを追加する

`src/main/kotlin/jp/hisano/winui4k/gallery/GalleryApp.kt`:

- `buildNavigationPane()` の `pages` にページ名 (WinUI のコントロール名) を追加する
- `build<Name>Page()` を実装し、追加した API を一通り操作できるデモを
  `buildExample("見出し", body)` 単位で並べる (既存の Button ページの構成に合わせる)

### 6. 検証する

```bash
./gradlew compileKotlin   # まずコンパイル
./gradlew run             # ウィンドウを開き、追加したデモを目視確認 (ユーザーが閉じると終了する)
```

実行時に COM 呼び出しが失敗すると `HRESULT` 例外、Kotlin 側コールバックの例外は
`[winui4k] exception escaped from COM upcall:` として stderr に出る。
スロット番号のずれが典型的な原因なので、失敗したら手順 2 のダンプ結果と突き合わせる。
