# WinUI for Kotlin (winui4k) — WinUI 3 を Kotlin/JVM + Panama (FFM API) で直接叩く

ネイティブブリッジ DLL も C# も使わず、**純粋な JVM プロセスから WinUI 3 のウィンドウを表示する**
デモプロジェクトです。Java 25 の FFM API (Panama) で WinRT の COM ABI
(`RoGetActivationFactory`、HSTRING、vtable 呼び出し、upcall による COM オブジェクト実装、
COM 集約による `Application` サブクラス合成) を直接扱います。

表示されるのは「テキストフィールド + ボタン」だけのウィンドウです。
ボタンを押すとテキストフィールドの内容を読み取り、ボタンのラベルが `Hello, <入力>!` に変わり、
コンソールにも出力されます。API は Swing 風に薄くラップしてあります。

```kotlin
WinUiUtilities.invokeLater {
    val frame = WFrame(title = "WinUI for Kotlin Demo")
    val nameField = WTextField(placeholder = "お名前をどうぞ")
    val greetButton = WButton("Greet")

    greetButton.addActionListener {
        greetButton.text = "Hello, ${nameField.text.ifBlank { "world" }}!"
    }

    frame.add(nameField)
    frame.add(greetButton)
    frame.isVisible = true
}
```

## 動作要件

- **Windows 11 x64**(Windows 10 1809 以降でも動く想定。Arm64 は後述)
- **JDK 25**(FFM API を使用。`java --version` で 25 以上であることを確認)
- **Windows App SDK 2.2 ランタイム**(下記手順でインストール)
- インターネット接続(初回ビルド時に Gradle と NuGet パッケージを取得)

Visual Studio・C++ ビルドツール・.NET SDK は**不要**です。

## セットアップ手順

1. **JDK 25 をインストールする**

   [Eclipse Temurin](https://adoptium.net/) などから JDK 25 (x64) を入手し、
   `JAVA_HOME` を設定するか PATH を通します。

   ```powershell
   java --version   # openjdk 25.x であること
   ```

2. **Windows App SDK 2.2 ランタイムをインストールする**

   https://aka.ms/windowsappsdk から **2.2 系の Runtime インストーラ**
   (`WindowsAppRuntimeInstall-x64.exe`) をダウンロードして実行します。

   このデモは「アンパッケージ (unpackaged) アプリ」として動くため、OS にインストール済みの
   ランタイムパッケージをブートストラップ API で動的に参照します。未インストールの場合、
   起動時にインストールを促すダイアログが出ます
   (`MddBootstrapInitializeOptions_OnNoMatch_ShowUI` を指定しているため)。

3. **実行する**

   ```powershell
   cd winui4k
   .\gradlew run
   ```

   初回は次が自動で行われます:
   - Gradle 9.6.1 本体と Kotlin 2.4.0 ツールチェーンの取得
   - `fetchBootstrap` タスクが NuGet から Microsoft.WindowsAppSDK.Foundation 2.1.0
     (Microsoft.WindowsAppSDK 2.2.0 メタパッケージの依存) の nupkg (約 6 MB) を
     ダウンロードし、`Microsoft.WindowsAppRuntime.Bootstrap.dll` だけを
     `build/native/<version>/` に展開(2 回目以降はスキップ)

   ウィンドウが表示されれば成功です。ウィンドウを閉じるとメッセージループが終了し、
   `gradlew run` も終了します。

## 仕組み(レイヤ構成)

技術スタック 1 層 = 1 パッケージで、`jp.hisano.winui4k → internal.winui → internal.winrt → internal.com → internal.ffi.api` の
一方向に依存します (公開 API はルートパッケージ `jp.hisano.winui4k` のみ、他は `internal` 配下)。

| レイヤ | パッケージ | 役割 |
|---|---|---|
| FFI SPI | `internal/ffi/api` | バックエンド非依存の FFI 語彙 (`Ptr` / `CallDescriptor` / `StructType` / `FfiBackend`)。将来の JNA バックエンドはこの SPI を実装する |
| FFI 実装 | `internal/ffi/panama` | Panama (`java.lang.foreign`) バックエンド。**java.lang.foreign への参照はここだけ** |
| Win32 | `internal/win32/Win32.kt` | DPI 宣言、`GetModuleFileNameW` |
| COM | `internal/com/` | `ComPtr` (`ptr → vtable → vtable[slot]` の呼び出し)、`Guid`、`checkHr` (HRESULT 例外 + IRestrictedErrorInfo 診断) |
| WinRT | `internal/winrt/` | `Hstring`、`KComObject` (upcall で vtable を構築し delegate・overrides・集約 outer になる)、`Activation`、`PropertyValues` (box 化)、`Pinterface` (`IVector<T>` 実体 IID の SHA-1 計算)、`Async` |
| WinUI | `internal/winui/` | `Abi` (IID / vtable スロット。**すべて winmd から機械抽出**)、`Dispatcher`、`WinAppSdkBootstrap`、`XamlStructs` |
| API | ルート (`jp/hisano/winui4k/`) | `WinUiUtilities` と `W*` クラス (`WFrame` / `WButton` / ...) |

FFI バックエンドはシステムプロパティ `-Dwinui4k.ffi=panama` または
`WinUiUtilities.setFfiBackend(...)` で選択できます (既定は Panama)。

起動シーケンスの要点:

1. `MddBootstrapInitialize2(0x00020000, L"", 2.2.0.0, OnNoMatch_ShowUI)` で
   Windows App SDK 2.x ランタイムをプロセスに結び付ける
   (2.0 以降 majorMinorVersion の minor 部は無視されるため、欲しい 2.2 は
   minVersion = PACKAGE_VERSION 2.2.0.0 で指定する)
2. `RoInitialize(RO_INIT_SINGLETHREADED)` — このスレッド (JVM の main) が UI スレッドになる
3. `Application.Start(callback)` — callback は Kotlin 実装の WinRT デリゲート
   (upcall スタブ)。この呼び出しはメッセージループとしてブロックする
4. callback 内で **COM 集約**により `Application` の「サブクラス」を合成する:
   Kotlin 実装の outer(`IApplicationOverrides` + `IXamlMetadataProvider` を実装)を
   `IApplicationFactory.CreateInstance(outer, &inner, &app)` に渡し、
   未知の `QueryInterface` は inner(XAML 側の基底実装)へ転送する
5. `Application.ResourceManagerRequested` イベントに、ランタイムパッケージの
   `resources.pri` を読む MRT Core `ResourceManager` を返すハンドラを登録する。
   アンパッケージ (テンプレート非使用) アプリでは exe (= java.exe) の隣に
   `resources.pri` が無く、XAML が既定テーマリソース
   (`ms-appx:///Microsoft.UI.Xaml/Themes/themeresources.xaml`) を自力で
   見つけられないため、この公式拡張点で解決させる
6. XAML が outer の `OnLaunched` を upcall してくる。その冒頭で
   `Application.Resources.MergedDictionaries` に `XamlControlsResources` を追加する
   (これが無いとコントロールの既定スタイルが無く、Button 等が描画されない。
   `Application.Resources` はコア初期化完了後 = OnLaunched 以降でないと触れない)。
   続けて Window / StackPanel / TextBox / Button を各コンポーザブルファクトリの
   `CreateInstance(null, ...)` で生成し、`Window.Activate()` する。
   `IXamlMetadataProvider` は `XamlControlsXamlMetaDataProvider` の実体へ全転送する
7. `Button.Click` には `RoutedEventHandler` デリゲート(これも Kotlin 実装 COM オブジェクト)
   を `add_Click` で登録する
8. 最後のウィンドウが閉じて `Start` が戻ったら `RoUninitialize` →
   `MddBootstrapShutdown`。`RoUninitialize` を省くと、JVM シャットダウン中に
   ネイティブスレッドからの upcall (遅延 Release) がアタッチ失敗で JVM ごと abort する

## ABI 定数の出所 (tools/dump_winmd.py)

`winui/Abi.kt` の IID・スロット番号は、手書きではなく
Microsoft.WindowsAppSDK.WinUI 2.2.1 の `metadata/Microsoft.UI.Xaml.winmd`、
WinAppSDK ランタイムの `Microsoft.Windows.ApplicationModel.Resources.winmd`、
Windows SDK の `Windows.Foundation.FoundationContract.winmd` から
同梱の `tools/dump_winmd.py` で抽出した値です。再現するには:

```bash
pip install dnfile
python tools/dump_winmd.py Microsoft.UI.Xaml.winmd \
    Microsoft.UI.Xaml.IWindow Microsoft.UI.Xaml.Controls.ITextBox
```

`IVector<Microsoft.UI.Xaml.UIElement>` のようなジェネリック実体の IID は winmd には無く、
WinRT 仕様の署名文字列から SHA-1 名前ベース UUID として計算します
(`WinRt.pinterfaceIid`)。実装は既知値 `IIterable<String> =
e2fcc7c1-3bfc-5a0b-b2b0-72e769d1cb7e` との一致で検証済みです。

## 検証状況について

**Windows 11 x64 + Windows App SDK 2.2 ランタイムで動作検証済み**です
(起動 → ウィンドウ表示 → ボタンクリック → クローズ → 正常終了 exit code 0)。

実行以外の検証:

- Kotlin 2.4.0 + JDK 25 + Gradle 9.6.1 でのコンパイル成功
- 全 IID / vtable スロットの winmd からの機械抽出(推測値ゼロ)
- pinterface IID 計算の既知値照合
- GUID ネイティブレイアウトの往復テスト
- **COM 機構のループバックテスト**: `KComObject` が構築した vtable を `ComPtr` 経由で
  呼び戻し、QueryInterface の同一性・E_NOINTERFACE・メソッドディスパッチ・引数マーシャリング
  (int / out ポインタ)・参照カウント・IInspectable プロローグがすべて期待通りに動作

WinUI 3 実体との相互作用で判明した注意点(いずれも対応済み):

- `Application.Resources` は init callback 中は触れない (E_UNEXPECTED)。
  `OnLaunched` 以降で `MergedDictionaries.Append` する
- テンプレート非使用のアンパッケージアプリは `ResourceManagerRequested` で
  カスタム `ResourceManager` を渡さないと `XamlControlsResources` の生成が
  「Cannot locate resource from 'ms-appx:///Microsoft.UI.Xaml/Themes/themeresources.xaml'」
  で失敗する
- `ResourceManagerRequested` のハンドラ型は
  `TypedEventHandler<Object, ResourceManagerRequestedEventArgs>`
  (第 1 型引数は Application ではなく Object。IID 計算では `cinterface(IInspectable)`)
- 終了時は `RoUninitialize` を呼ぶ (呼ばないと JVM シャットダウンと COM の遅延解放が
  競合して abort する)

## トラブルシューティング

- **`MddBootstrapInitialize2 failed` (HRESULT=0x80670016 など)** — Windows App SDK **2.2**
  ランタイムが未インストール、またはメジャーバージョン不一致です。手順 2 の
  インストーラを実行してください。別バージョンを使う場合は `Toolkit.kt` の
  `WINAPPSDK_MAJOR_MINOR` / `WINAPPSDK_MIN_VERSION` を変更します
  (2.0 以降は major のみで解決され、minor は minVersion で指定します)。
- **`REGDB_E_CLASSNOTREG (0x80040154)` が RoGetActivationFactory で出る** —
  ブートストラップが成功していない状態で WinUI 型を解決しようとしています。
  上と同じくランタイムの導入状況を確認してください。
- **ウィンドウは出るがコントロールが表示されない** — `XamlControlsResources` の適用に
  失敗しています。コンソールの HRESULT を確認してください。
- **Arm64 Windows** — `fetchBootstrap` は `os.arch` を見て `win-arm64` の DLL を展開しますが、
  JDK も Arm64 版である必要があります。未検証です。
- **`--enable-native-access` の警告/エラー** — `gradlew run` 経由なら自動付与されます。
  jar を直接実行する場合は `java --enable-native-access=ALL-UNNAMED ...` を付けてください。

## 既知の割り切り(デモとしての簡略化)

参照カウントは解放よりリークを選ぶ場面があり(vtable・upcall スタブ・キャッシュ済み HSTRING は
プロセス生存期間ぶん保持)、スレッドは UI スレッド 1 本の前提です(`invokeLater` 相当は未実装)。
エラー処理は HRESULT の例外化のみで、`DispatcherQueue` 統合、レイアウトマネージャ、
その他のコントロールはスコープ外です。ライブラリ化する際は winmd からの
バインディング自動生成(`tools/dump_winmd.py` の発展形)が次の一歩になります。
