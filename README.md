# WinUI for Kotlin & Java (winui4kプロジェクト)

ブリッジ DLL も C# も Visual Studio も使わずに WinUI を使ったアプリを作れる Kotlin ライブラリです。
Java の FFI (Panama / JNA / JNR) で WinRT ABI + COM + Win32 を直接呼び出すので軽量で安定しています。

[NTTレゾナントテクノロジー](https://nttr-tech.co.jp/)が提供しているインターネット経由でスマホを借りられるサービス「[Remote TestKit](https://appkitbox.com/)」のPCクライアント用に試作しました。

Apache Licenseですので商用・非商用を問わずにご自由に利用いただけます。

下のスクリーンショットは Microsoft 製の WinUI 3 Gallery ではありません。
**すべて Kotlin で書かれた** 同梱の Gallery アプリ ([winui4k-sample-gallery](winui4k-sample-gallery/)) です。

![winui4k Gallery](doc/images/gallery-table.png)

## 利用例

`SwingUtilities.invokeLater` と同じ感覚で書けます。

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

## 特徴

- **ブリッジ DLL なし**：`RoGetActivationFactory`、HSTRING、vtable 呼び出し、upcall による COM オブジェクト実装、COM 集約まで、WinRT の COM ABI を JVM の FFI だけで扱います
- **60 超のコントロール**：Button / TextBox から NavigationView、TeachingTip、AppNotification、AppWindow まで `W*` クラスとしてラップ済み。Gallery で全部試せます
- **Java 8 でも動く**：FFI バックエンドは差し替え式。Panama (Java 22 以降、既定)、JNA (Java 8 以降)、JNR (Java 8 以降) の 3 実装を同梱します
- **コルーチン対応**：`Dispatchers.WinUi` (winui4k-extension-coroutines) で UI スレッドへディスパッチし、`delay` は DispatcherQueueTimer にネイティブ対応します
- **推測値ゼロの ABI**：IID と vtable スロットはすべて winmd から機械抽出した値です ([doc/architecture.md](doc/architecture.md))

## 起動

必要なのは **JDK 25** (x64) だけです (Visual Studio、C++ ビルドツール、.NET SDK は不要)。
[Eclipse Temurin](https://adoptium.net/) などから入手してパスに設定してください。

```powershell
.\gradlew run
```

これで Gallery が起動します。
Java 8 + JNA での起動は `.\gradlew :winui4k-sample-gallery:runJna`、Java 8 + JNR での起動は `.\gradlew :winui4k-sample-gallery:runJnr` で確認できます。

動作環境は Windows 11 x64 です (Windows 10 1809 以降でも動く想定)。

## Windows App SDK ランタイムの自動セットアップ

winui4k はアプリ起動時に Windows App SDK ランタイムを自動でセットアップします。

### ブートストラップ DLL

Windows App SDK の初期化に必要なブートストラップ DLL (`Microsoft.WindowsAppRuntime.Bootstrap.dll`) は winui4k の JAR に内蔵されています (x86 / x64 / arm64 の 3 アーキテクチャ対応)。`WinUiUtilities` の初回呼び出し時に、実行中の PC アーキテクチャに合った DLL が一時ディレクトリへ自動展開され、プロセス終了時に削除されます。

### ランタイムのインストール

Windows App SDK 2.2 ランタイムが必要です。未インストールの場合は以下の順に対応します。

1. **インストーラーの自動実行**：カレントディレクトリ (または `winui4k.installer.dir` で指定したディレクトリ) に `WindowsAppRuntimeInstall-x64.exe` 等のインストーラーがあれば、`--quiet` オプションでサイレントインストールを実行し、アプリをそのまま起動します
2. **インストールダイアログの表示**：インストーラーが見つからない場合は、Microsoft のダイアログが表示され、ユーザーにランタイムのダウンロードを促します

インストーラーは以下のコマンドでダウンロードできます。

```powershell
.\gradlew :winui4k:downloadInstallers
```

`winui4k/installer/` に x86 / x64 / arm64 の 3 種類がダウンロードされます (各約 104 MB)。アプリ配布時にアーキテクチャに合ったインストーラーを同梱すれば、エンドユーザーの環境にランタイムが自動インストールされます。

手動でインストールする場合は https://aka.ms/windowsappsdk から `WindowsAppRuntimeInstall-x64.exe` を実行してください。

### システムプロパティ

| プロパティ | 説明 |
|---|---|
| `winui4k.bootstrap.dll` | ブートストラップ DLL のパスを明示指定する (JAR 内蔵 DLL の代わりに使用) |
| `winui4k.installer.dir` | ランタイムインストーラーの検索ディレクトリを指定する (既定はカレントディレクトリ)。絶対パス・相対パスの両方に対応 |
| `winui4k.ffi` | FFI バックエンドを指定する (`panama` / `jna` / `jnr`) |

## モジュール構成

| モジュール | 内容 |
|---|---|
| `winui4k` | 本体。公開 API (`W*` クラス) と COM / WinRT / WinUI の内部レイヤ |
| `winui4k-ffi-panama` | Panama (`java.lang.foreign`) FFI バックエンド。Java 22 以降 |
| `winui4k-ffi-jna` | JNA FFI バックエンド。Java 8 以降 (x64 のみ) |
| `winui4k-ffi-jnr` | JNR (jffi) FFI バックエンド。Java 8 以降 |
| `winui4k-extension-coroutines` | `Dispatchers.WinUi` (kotlinx-coroutines-swing の WinUI 版) |
| `winui4k-sample-gallery` | 全コントロールのデモアプリ (WinUI 3 Gallery 風) |

# 参考情報

- [WinUI 開発ドキュメント](https://learn.microsoft.com/ja-jp/windows/apps/winui/winui3/)
   - [Fluent Design System](https://fluent2.microsoft.design/)
   - [Windows アプリの設計の概要](https://learn.microsoft.com/ja-jp/windows/apps/design/)

- [WinUI リポジトリ](https://github.com/microsoft/microsoft-ui-xaml)
   - [WinUI Gallery リポジトリ](https://github.com/microsoft/WinUI-Gallery)
   - [Windows App SDK リポジトリ](https://github.com/microsoft/WindowsAppSDK)
   - [nuget](https://www.nuget.org/packages/Microsoft.WindowsAppSDK)
- WinUI追加コンポーネント
   - [TableView リポジトリ](https://github.com/w-ahmad/WinUI.TableView)
   - [Community Controls リポジトリ](https://github.com/CommunityToolkit/Windows)
