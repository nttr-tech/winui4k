# 第I部 概要

## 第1章 [WinUI4K](https://github.com/nttr-tech/winui4k) とは

WinUI は、Microsoft が Windows 11 世代の標準として推進する UI フレームワークです。
Windows App SDK の一部として OS 本体から切り離して配布され、Windows 10 バージョン 1809 以降で動作します。

WinUI が公式に想定する開発言語は C++ と C# であり、Java や Kotlin から実用的に扱う手段はこれまでありませんでした。
[WinUI4K](https://github.com/nttr-tech/winui4k) は、JVM の FFI で WinUI を直接呼び出すことでこの空白を埋める Kotlin ライブラリです。
ブリッジ DLL も C# も Visual Studio も使わずに、Kotlin や Java だけで WinUI アプリを書けます。

この章では、[WinUI4K](https://github.com/nttr-tech/winui4k) が解決する課題、設計思想、類似技術との比較、採用判断のポイントを説明します。

### 1.1 解決する課題

JVM から Windows のネイティブ UI を書く手段は、これまで事実上ありませんでした。

- Swing の Windows ルックアンドフィールは、OS のテーマを見た目で再現したものです。コントロールの実体は Java 側にあり、本物の OS コントロールではありません。
- SWT は OS のウィジェットを直接ラップしますが、対象は Win32 世代のコントロールで、WinUI には対応していません。
- JavaFX と Compose for Desktop は独自の描画エンジンによる自前描画方式で、OS の見た目に似ていますが実物ではありません。

再現方式や自前描画方式には、次の問題があります。

- **見た目の追従**：Windows 11 標準のデザイン言語 Fluent Design は OS のアップデートで更新されるため、再現方式は追従作業が続きます。
- **アクセシビリティ**：スクリーンリーダーなどの支援技術は OS の UI オートメーション情報を読みます。自前描画方式で同等の対応を得るには、ツールキット側が支援技術との接続を再実装する必要があり、対応の質はその完成度に依存します。支援技術対応は公共案件や企業案件で調達要件になることがあります。
- **入力と表示の細部**：IME、タッチ、DPI スケーリングなどで OS 標準との差異が出ます。

このため、JVM 資産を持つ現場が Windows のネイティブ UI を必要とすると、C# への全面的な書き換えか、Electron のようなブラウザエンジン同梱方式への移行しか選択肢がありませんでした。
前者は既存のコードとスキルセットを手放すことになり、後者はネイティブ UI をあきらめたうえで、配布サイズとメモリ使用量の増加を受け入れることになります。

[WinUI4K](https://github.com/nttr-tech/winui4k) はこの問題を、JVM の FFI (Java から他言語の関数を直接呼び出す仕組み) で WinUI を直接呼び出すことで解決します。
これが可能なのは、WinUI のオブジェクトがプロセス内では COM (言語をまたいでオブジェクトを呼び出すために Windows がバイナリレベルで定めた規約) のオブジェクトとして存在するためです。
COM は特定の言語に依存しないバイナリ規約なので、規約どおりの呼び出しを FFI で組み立てれば、C# や C++ を経由する必要はありません。
オブジェクト生成、メソッド呼び出し、イベントハンドラの登録、`Application` のサブクラス合成まで、必要な操作はすべて JVM の FFI だけで実装されています (詳細は第V部)。

結果として、Kotlin と Java のコードだけで 60 超の WinUI コントロールを使えます。
動作要件は Java 8 以降で、アプリは通常の JAR として配布できます。

### 1.2 設計思想

[WinUI4K](https://github.com/nttr-tech/winui4k) の設計は次の 4 つの方針に基づいています。

**ブリッジ DLL を持たない。**
JVM とネイティブの間を翻訳する独自ビルドの DLL を作らず、FFI で COM の呼び出し規約を直接組み立てます。
このため開発に Visual Studio や C++ ビルドツールチェーンが不要で、x86 / x64 / ARM64 のアーキテクチャごとに DLL をビルド・署名・同梱する保守作業も発生しません。
なお、Windows App SDK の初期化に必要な Microsoft 公式のブートストラップ DLL は JAR に内蔵していますが、これは Microsoft が配布する初期化用 DLL であり、独自ビルドのブリッジではありません。

**Swing 風の命令的 API を提供する。**
XAML は書かず、画面はコードで組み立てます。

```kotlin
WinUiUtilities.invokeLater {
    val frame = WFrame(title = "WinUI4K")
    val nameField = WTextField(placeholder = "Name")
    val greetButton = WButton("Greet")

    greetButton.addActionListener {
        greetButton.text = "Hello, ${nameField.text.ifBlank { "world" }}!"
    }

    frame.add(nameField)
    frame.add(greetButton)
    frame.isVisible = true
}
```

`invokeLater`、`addActionListener`、`frame.add` という語彙は Swing を踏襲しています。
主対象が Swing や JavaFX の既存資産を持つ現場であるため、`JFrame` → `WFrame`、`JButton` → `WButton` のような対応関係で移植でき、新しい UI パラダイムの学習を前提にしません。

**OS 標準コントロールをそのまま使う。**
`W*` クラスは WinUI コントロールの薄いラッパーで、描画の再実装を持ちません。
見た目、アクセシビリティ、IME、DPI スケーリングは OS 実装の品質がそのまま得られ、OS 側で Fluent Design が更新されればアプリの見た目も再ビルドなしで追従します。

**推測に頼らない。**
COM 呼び出しに必要な識別子 (IID) と関数テーブル上の位置 (vtable スロット) は、すべて Windows の型情報ファイル winmd から機械抽出した値で、手書きの推測値を含みません。
テストは実際に WinUI ウィンドウを起動する E2E 方式で、CI では JDK 8 / 9 / 22 / 25 で実行しています。

このほか、動作要件を Java 8 まで下げていることも設計上の選択です。
FFI バックエンドは差し替え式で、Java 22 以降では標準の Panama を、それより古い環境では JNA または JNR を使います (第7章)。

### 1.3 類似技術との比較

| 技術 | UI の実体 | 開発言語 | 対応 OS | 配布物の特徴 |
|---|---|---|---|---|
| 従来の WinUI 開発 | ネイティブ (WinUI) | C# / C++ と XAML | Windows | .NET ランタイム + Windows App SDK ランタイム |
| [WinUI4K](https://github.com/nttr-tech/winui4k) | ネイティブ (WinUI) | Kotlin / Java | Windows | JVM + Windows App SDK ランタイム |
| SWT | ネイティブ (Win32 / GTK 等) | JVM 言語 | Windows / macOS / Linux | JVM + OS 別ネイティブライブラリ |
| Swing | 自前描画 (OS 風テーマで再現) | JVM 言語 | Windows / macOS / Linux | JVM のみ |
| JavaFX | 自前描画 | JVM 言語 | Windows / macOS / Linux | JVM + JavaFX ランタイム |
| Compose for Desktop | 自前描画 (Skia) | Kotlin | Windows / macOS / Linux | JVM + Skia |
| Electron | ブラウザ描画 (Chromium) | JavaScript / TypeScript | Windows / macOS / Linux | Chromium + Node.js 同梱 |

それぞれとの使い分けは次のとおりです。

- **従来の WinUI 開発 (C#)**：チームが .NET のスキルセットを持つなら C# で書くのが正攻法です。[WinUI4K](https://github.com/nttr-tech/winui4k) の対象は JVM の資産とスキルセットを持つ現場です。
- **Compose for Desktop / JavaFX**：クロスプラットフォーム対応が必須なら、Windows 専用の [WinUI4K](https://github.com/nttr-tech/winui4k) は候補から外れ、これらの自前描画方式が妥当です。自前描画は「OS の実物ではない」という弱点と「複数 OS で見た目を統一できる」という利点が表裏一体で、要件次第で評価が変わります。
- **SWT**：OS ウィジェットをラップしてネイティブの見た目を得るという設計思想は [WinUI4K](https://github.com/nttr-tech/winui4k) と同じで、[WinUI4K](https://github.com/nttr-tech/winui4k) はその WinUI 版に相当します。SWT は Win32 世代のコントロールを OS 別ネイティブライブラリでラップし、[WinUI4K](https://github.com/nttr-tech/winui4k) は WinUI 世代を独自ネイティブコードなしの FFI でラップします。Fluent Design が要件なら SWT では届きません。
- **Electron**：Web のスキルセットと資産がある現場では合理的な選択です。ブラウザエンジン同梱による配布サイズとメモリ使用量を避けたい場合や、OS 標準コントロールの見た目と支援技術対応が要件の場合は [WinUI4K](https://github.com/nttr-tech/winui4k) が優位です。

まとめると、[WinUI4K](https://github.com/nttr-tech/winui4k) が適するのは「JVM 資産がある」「Windows 専用でよい」「ネイティブの見た目とアクセシビリティが必要」の 3 条件がそろう場合です。

### 1.4 採用判断のポイント

**動作要件。**
実行環境は Windows 11 (Windows 10 バージョン 1809 以降でも動く想定)、Java 8 以降です。
WinUI の実行基盤である Windows App SDK ランタイムが必要ですが、未導入の環境ではアプリ起動時に自動でセットアップされます (第2章)。
リポジトリのビルドには JDK 25 が必要ですが、これは開発側の要件で、ライブラリの動作要件は Java 8 のままです。

**パフォーマンス特性。**
ブラウザエンジンを同梱しないため、配布サイズとメモリ使用量のベースラインは Electron 方式より小さくなります。
描画とレイアウト計算は OS 側の WinUI ランタイムが担い、JVM 側の仕事はアプリケーションロジックと FFI 呼び出しに限られます。
一方、JVM プロセスの起動と Windows App SDK の初期化が起動時間に乗ります。

**ライセンス。**
Apache License 2.0 で、商用か非商用かを問わず利用できます。
依存する Windows App SDK ランタイムは Microsoft が配布し、インストーラーの同梱と再頒布が認められています。

**メンテナンス状況。**
[WinUI4K](https://github.com/nttr-tech/winui4k) は NTTレゾナントテクノロジーが、自社サービス Remote TestKit の PC クライアントに活かすことを一つの目的として開発しているライブラリです。
設計上の割り切りと既知の制約はドキュメントに明文化されており、不具合報告と機能要望は GitHub の Issue で受け付けています。

**設計に影響する制約。**

- **Windows 専用**：WinUI 自体の制約で、覆せません。クロスプラットフォーム要件が出た時点で作り直しになります。
- **COM 参照の解放が GC 連動**：`W*` ラッパーが GC で回収されたときにネイティブ側の参照が解放されるため、解放タイミングは非決定的です。UI 要素の生成と破棄を高頻度に繰り返し、かつネイティブ資源の解放時期を厳密に制御したい用途では、この前提を踏まえた設計が必要です (第6章)。
- **言語境界をまたぐ循環参照は自動回収されない**：ネイティブ → Kotlin のイベントハンドラ → ネイティブという循環は JVM の GC から見えません。不要になったイベントリスナーは remove 系メソッドで明示的に解除する運用が前提です (第6章、第8章)。
- **UI スレッドは 1 本**：`W*` API は UI スレッド上でのみ使う契約です (第5章)。
- **エラー処理は HRESULT の例外化のみ**：COM の呼び出し結果コードが例外に変換されるだけで、それ以上の型付きエラー体系はありません (第14章)。
- **ラップ済み API は WinUI の一部**：60 超のコントロールで実用アプリは組めますが、WinUI の全 API がラップされているわけではありません。未ラップの機能は内部レイヤを直接呼んで補えます (第12章)。

## 第2章 クイックスタート

この章では、依存の追加から「ボタンを押すと反応するアプリ」が動くまでを説明します。
つまずきやすい初期エラーと、動作確認に使える同梱サンプルもここでまとめます。

### 2.1 環境要件と依存の追加

実行環境の要件は次の 2 つです。

- **OS**：Windows 11 x64 (Windows 10 バージョン 1809 以降でも動く想定)
- **Java**：Java 8 以降。JDK のアーキテクチャは OS に合わせます (x64 の Windows なら x64 の JDK)

WinUI の実行基盤である Windows App SDK ランタイムも必要ですが、未導入ならアプリの初回起動時に自動セットアップされるため (2.4 節)、事前準備は不要です。
Visual Studio、C++ ビルドツール、.NET SDK は使いません。

ライブラリはモジュール分割されています。
本体の `winui4k` に加えて、FFI バックエンド (ネイティブ呼び出しの実装) を 1 つ以上クラスパスに置く構成です。

| モジュール | 内容 | 対応環境 |
|---|---|---|
| `winui4k` | 本体 (必須) | Java 8 以降 |
| `winui4k-ffi-panama` | Panama (`java.lang.foreign`) バックエンド。既定 | Java 22 以降 |
| `winui4k-ffi-jna` | JNA バックエンド | Java 8 以降 (x64 のみ) |
| `winui4k-ffi-jnr` | JNR バックエンド | Java 8 以降 (x86 / x64 / ARM64) |
| `winui4k-extension-coroutines` | `Dispatchers.WinUi` (任意) | - |
| `winui4k-extension-miglayout` | MigLayout アダプタ (任意) | - |
| `winui4k-all` | 上記すべての一括参照 | - |

迷ったら、全部入りの `winui4k-all` を使うのが簡単です。

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.appkitbox.winui4k:winui4k-all:0.1.0") // バージョンは最新版に読み替えてください
}
```

配布サイズを絞りたい場合は、本体と必要なバックエンドだけを選びます。

```kotlin
dependencies {
    implementation("com.appkitbox.winui4k:winui4k:0.1.0")
    implementation("com.appkitbox.winui4k:winui4k-ffi-panama:0.1.0") // Java 22 以降の場合
    // implementation("com.appkitbox.winui4k:winui4k-ffi-jna:0.1.0") // Java 8〜21 の場合
}
```

バックエンドが複数あるときは優先度 (Panama > JNA > JNR) で自動選択されるため、通常は選択を意識する必要はありません。
明示的に切り替えたい場合の方法は第7章で説明します。

### 2.2 ウィンドウを 1 枚出す最小コード

最小のアプリは次のとおりです。

```kotlin
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WinUiUtilities

fun main() {
    WinUiUtilities.invokeLater {
        val frame = WFrame(title = "Hello WinUI4K")
        frame.isVisible = true
    }
}
```

実行すると、Fluent Design のネイティブウィンドウが 1 枚開きます。
ウィンドウを閉じるとアプリも終了します。

`WinUiUtilities.invokeLater` は Swing の `SwingUtilities.invokeLater` に相当し、渡したブロックを UI スレッドで実行します。
WinUI が未起動であれば、初回呼び出し時に起動処理 (ブートストラップ DLL の展開、Windows App SDK の初期化、メッセージループの開始) も自動で行われます。
`W*` クラスの操作はすべて UI スレッド上で行う契約なので、UI の構築は必ず `invokeLater` のブロック内に書きます (スレッドモデルの詳細は第5章)。

なお、Panama バックエンドを使って jar を直接実行する場合は、ネイティブアクセスの許可オプションが必要です。

```powershell
java --enable-native-access=ALL-UNNAMED -jar app.jar
```

### 2.3 ボタンとイベントハンドラを持つ最小アプリ

次は、テキスト入力とボタンを置き、クリックに反応させます。

```kotlin
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WTextField
import com.appkitbox.winui4k.WinUiUtilities

fun main() {
    WinUiUtilities.invokeLater {
        val frame = WFrame(title = "WinUI4K")
        val nameField = WTextField(placeholder = "Name")
        val greetButton = WButton("Greet")

        greetButton.addActionListener {
            greetButton.text = "Hello, ${nameField.text.ifBlank { "world" }}!"
        }

        frame.add(nameField)
        frame.add(greetButton)
        frame.isVisible = true
    }
}
```

構成要素は Swing と同じ考え方です。

- コントロールはコンストラクタで生成し、`frame.add` で追加します。追加した順に縦に並びます (レイアウトの制御は第9章)。
- イベントは `addActionListener` のようなリスナー登録で受け取ります。リスナーは UI スレッド上で呼び出されるため、そのまま UI を更新できます。
- リスナー内で時間のかかる処理を行うと UI がフリーズします。重い処理はワーカースレッドに逃がし、結果の反映だけを `invokeLater` で UI スレッドに戻します (第5章、第10章)。

### 2.4 Windows App SDK ランタイムの自動セットアップ

WinUI アプリの実行には Windows App SDK 2.2 ランタイムが必要です。
[WinUI4K](https://github.com/nttr-tech/winui4k) は、開発者とエンドユーザーの双方がこれを意識しなくて済むよう、起動時に 2 段階の自動セットアップを行います。

**ブートストラップ DLL の展開。**
Windows App SDK の初期化に必要なブートストラップ DLL (`Microsoft.WindowsAppRuntime.Bootstrap.dll`) は、x86 / x64 / ARM64 の 3 アーキテクチャ分が winui4k の JAR に内蔵されています。
初回の API 呼び出し時に、実行中の PC のアーキテクチャに合った DLL が一時ディレクトリへ自動展開され、プロセス終了時に削除されます。
アプリ側で DLL を用意したり配置したりする必要はありません。

**ランタイムのインストール。**
ランタイム本体が未インストールの場合は、次の順で対応します。

1. カレントディレクトリ (または `winui4k.installer.dir` で指定したディレクトリ) に `WindowsAppRuntimeInstall-x64.exe` などのインストーラーがあれば、サイレントインストールを実行してそのままアプリを起動します。
2. インストーラーが見つからない場合は、Microsoft のダイアログが表示され、ユーザーにランタイムのダウンロードを促します。

エンドユーザーへの配布時は、アーキテクチャに合ったインストーラーを同梱しておくと、ユーザー操作なしでセットアップが完了します。
インストーラーは次のコマンドで取得できます (x86 / x64 / ARM64 の 3 種類、各約 104 MB が `winui4k/installer/` にダウンロードされます)。

```powershell
.\gradlew :winui4k:downloadInstallers
```

手動でインストールする場合は https://aka.ms/windowsappsdk から `WindowsAppRuntimeInstall-x64.exe` を実行してください。
配布形態ごとの詳しい構成は第15章で扱います。

### 2.5 初期トラブル

最初の起動でつまずきやすいポイントをまとめます。
いずれも症状はコンソールに HRESULT (COM のエラーコード) 付きで出力されるため、まずコンソール出力を確認してください。

- **`MddBootstrapInitialize2 failed` (HRESULT=0x80670016 など)**：Windows App SDK 2.2 ランタイムが未インストールか、メジャーバージョンが不一致です。https://aka.ms/windowsappsdk から 2.2 系のランタイムをインストールしてください。
- **`REGDB_E_CLASSNOTREG` (0x80040154) が出る**：ブートストラップが成功していない状態で WinUI の型を解決しようとしています。上と同じく、ランタイムの導入状況を確認してください。
- **ウィンドウは出るがコントロールが表示されない**：コントロールの既定スタイル (`XamlControlsResources`) の適用に失敗しています。コンソールの HRESULT を確認してください。
- **`--enable-native-access` の警告またはエラーが出る**：Panama バックエンド使用時、jar 直接実行では `java --enable-native-access=ALL-UNNAMED` の付与が必要です (2.2 節)。Gradle の `run` タスク経由なら自動付与されます。
- **Java 8〜21 で起動しない**：Panama バックエンドは Java 22 以降専用です。JNA (x64) または JNR をクラスパスに追加してください。
- **ARM64 Windows で起動しない**：JDK 自体が ARM64 版である必要があります。また JNA バックエンドは x64 専用のため、ARM64 では Panama または JNR を使います。

このほか、実行環境全般の注意として、E2E テストを含め WinUI は実際のデスクトップセッションを必要とします。
ヘッドレスの CI 環境やセッションのないサービスプロセスでは動作しません (CI での扱いは第13章)。

### 2.6 同梱サンプルアプリの歩き方

リポジトリには動作確認と実装例を兼ねたサンプルアプリが同梱されています。
リポジトリをクローンすれば、JDK 25 だけで起動できます。

```powershell
git clone https://github.com/nttr-tech/winui4k.git
cd winui4k
.\gradlew run
```

| サンプル | 内容 | 起動コマンド |
|---|---|---|
| Gallery | 60 超のコントロールをカテゴリ別に一覧できるデモ (WinUI 3 Gallery 風) | `.\gradlew run` |
| Filer | Fluent Design のファイラー。タブ、表示切り替え、ブレッドクラム、サイドバー、フィルター | `.\gradlew :winui4k-sample-filer:run` |
| Notes | シンプルなメモ帳アプリ | `.\gradlew :winui4k-sample-notes:run` |
| Form with MigLayout | MigLayout を使った入力フォーム | `.\gradlew :winui4k-sample-form-with-miglayout:run` |

それぞれの位置づけは次のとおりです。

- **Gallery** は「どんなコントロールがあるか」「どう見えるか」を確かめるカタログです。本書はコンポーネントカタログを持たないため、コントロール探しは Gallery を使ってください。使いたいコントロールを見つけたら、対応するソースコードが実装例になります。
- **Filer と Notes** は、複数のコントロールを組み合わせた実用寄りの構成例です。画面構成、イベント処理、テーマ対応の実例として読めます。
- **Form with MigLayout** は、レイアウトライブラリ MigLayout との組み合わせ例です (第9章)。

Java 8 環境での動作確認には、JDK 8 + JNA / JNR で Gallery を起動する専用タスクがあります。

```powershell
.\gradlew :winui4k-sample-gallery:runJna   # JDK 8 + JNA
.\gradlew :winui4k-sample-gallery:runJnr   # JDK 8 + JNR
```

必要な JDK は Gradle の foojay resolver が自動取得するため、JDK 8 を手動で用意する必要はありません。

# 第II部 コアコンセプト

## 第3章 アーキテクチャ概観

この章では、[WinUI4K](https://github.com/nttr-tech/winui4k) の全体構造を押さえます。
レイヤ構成と依存方向、モジュール分割の意図、そして本書全体で使う用語をここで定義します。
以降の章はすべてこの章の語彙を前提に書かれています。

### 3.1 レイヤ構成と依存方向

[WinUI4K](https://github.com/nttr-tech/winui4k) のコアモジュール (`winui4k`) は、技術スタック 1 層 = 1 パッケージの原則で構成されています。
依存は上から下への一方向で、逆方向はありません。

| レイヤ | パッケージ | 役割 |
|---|---|---|
| 公開 API | `com.appkitbox.winui4k` | `WinUiUtilities` と `W*` クラス (`WFrame` / `WButton` / ...) |
| WinUI | `internal.winui` | ABI 定数の `*Interop` オブジェクト、`Dispatcher`、Windows App SDK の `Bootstrap` |
| WinRT | `internal.winrt` | `Hstring`、`KComObject` (Kotlin 実装を COM オブジェクトとして見せる)、`Activation`、`Async`、`Pinterface` |
| COM | `internal.com` | `ComPtr` (vtable 呼び出し)、`Guid`、`checkHr` (HRESULT の例外化)、`lifetime` (参照の自動解放) |
| FFI SPI | `internal.ffi.api` | バックエンド非依存の FFI 語彙 (`Ptr` / `CallDescriptor` / `FfiBackend`) |

この分け方には 2 つの意図があります。

- **公開範囲の最小化**：公開 API はルートパッケージだけで、他はすべて `internal` 配下です。利用者が学ぶべき面積を `W*` クラス群に限定し、内部は互換性を約束せずに作り替えられる余地を残しています。
- **知識の局所化**：COM の一般規約 (参照カウント、QueryInterface) は `internal.com` に、WinRT 固有の拡張 (HSTRING、アクティベーション) は `internal.winrt` に、WinUI 固有の値 (IID、vtable スロット) は `internal.winui` に閉じています。たとえば「Button の Click イベントのスロット番号」を知っているのは `internal.winui` の `XamlInterop` だけです。

各レイヤの実装は第V部で読み解きます。

### 3.2 モジュール分割

モジュール構成は 2.1 節の表のとおりで、分割の理由は次の 3 つです。

**コアを Java 8 ターゲットに保つため。**
`winui4k` 本体は `-Xjdk-release=8` でコンパイルされ、Java 9 以降の API へのコンパイル時参照を持ちません。
JDK バージョンに依存するコード (Panama の `java.lang.foreign` は Java 22 以降) は本体に置けないため、FFI バックエンドを別モジュールに切り出し、実行時に ServiceLoader で発見します。
`java.lang.foreign` への参照は `winui4k-ffi-panama` だけが、`com.sun.jna` への参照は `winui4k-ffi-jna` だけが持ちます。

**任意の依存を選択制にするため。**
`kotlinx-coroutines` と MigLayout はどちらも外部ライブラリへの依存を持ち込むため、拡張モジュール (`winui4k-extension-coroutines` / `-miglayout`) として分離されています。
使わないアプリはこれらを配布物に含めずに済みます。

**選択の手間を省く入口を用意するため。**
`winui4k-all` は上記すべてを一括参照する集約モジュールです。
依存を 1 行で済ませたい場合はこれを、配布サイズを絞る場合は個別選択を使います。

### 3.3 用語集

本書で繰り返し使う用語をまとめます。

| 用語 | 意味 |
|---|---|
| COM | 言語をまたいでオブジェクトを呼び出すために Windows がバイナリレベルで定めた規約 |
| WinRT | COM の発展形。共通基底の IInspectable、文字列型 HSTRING、winmd 形式のメタデータを加えたもの。参照管理の規約は COM と同じ |
| インターフェースポインタ | COM オブジェクトへの参照。vtable へのポインタを先頭に持つ構造体へのポインタ |
| vtable | 関数ポインタの配列。`ポインタ → vtable → vtable[スロット番号]` とたどるとメソッドの実体に届く |
| IID | インターフェースを識別する GUID |
| QueryInterface (QI) | 同じオブジェクトの別インターフェースを IID 指定で問い合わせる操作 |
| HSTRING | WinRT の文字列型 |
| HRESULT | COM の呼び出し結果コード。負値が失敗 |
| アパートメント | COM のスレッド規約。STA のオブジェクトは特定スレッドに束縛され、MTA のオブジェクトは任意スレッドから呼べる |
| downcall / upcall | JVM からネイティブ関数を呼ぶこと / ネイティブから JVM のコードを呼び返すこと |
| RCW / CCW | ネイティブ COM オブジェクトを JVM から使うためのラッパー (`W*` クラス) / Kotlin 実装をネイティブへ COM オブジェクトとして見せるためのラッパー (`KComObject`) |
| winmd | Windows の型情報ファイル。IID や vtable 上のメソッド順の一次情報 |
| DIP | device-independent pixel。WinUI の論理ピクセル。`W*` API のレイアウト座標はすべてこの単位 |

## 第4章 アプリケーションのライフサイクル

[WinUI4K](https://github.com/nttr-tech/winui4k) アプリの一生は「起動 → メッセージループ → 終了」の 3 段階です。
利用者のコードから見えるのは `invokeLater` に渡すブロックだけですが、その裏で何が起きているかを知っておくと、初期化タイミングの制約 (テーマリソース、ウィンドウ生成) と終了時の注意 (第14章のアンチパターン) が理解できます。

### 4.1 起動シーケンス

`WinUiUtilities.invokeLater` の初回呼び出しが起動のトリガーです。
このとき専用の UI スレッド (スレッド名 `WinUI4K-UI`、非デーモン) が起動し、次の順で初期化が進みます。

1. **DPI 宣言**：`SetProcessDpiAwarenessContext` で Per-Monitor v2 の DPI 対応をプロセスに宣言します。java.exe は DPI 対応のマニフェストを持たないため、コードで宣言する必要があります (9.4 節)。
2. **Windows App SDK のブートストラップ**：JAR 内蔵のブートストラップ DLL を一時ディレクトリへ展開し、`MddBootstrapInitialize2` で Windows App SDK 2.2 ランタイムをプロセスに結び付けます。失敗した場合はインストーラーの自動実行を試みます (2.4 節)。
3. **COM への参加**：`RoInitialize(RO_INIT_SINGLETHREADED)` を呼びます。このスレッドが STA となり、以後すべての XAML オブジェクトがこのスレッドに束縛されます (第5章)。
4. **`Application.Start`**：コールバックを渡して呼び出します。この呼び出しはメッセージループとしてブロックし、アプリ終了まで戻りません。
5. **`Application` サブクラスの合成**：コールバック内で、COM 集約により `Application` の「サブクラス」を合成します。C# の `class App : Application` に相当するものを、Kotlin 実装の COM オブジェクトとネイティブ側の基底実装の合体で作ります (仕組みは 16.5 節)。合わせて、テーマリソースの解決に必要な `ResourceManagerRequested` ハンドラを登録します (11.3 節)。
6. **`OnLaunched`**：XAML ランタイムが合成したサブクラスの `OnLaunched` を呼び返してきます。ここで UI スレッドの `DispatcherQueue` を捕捉し、コントロールの既定スタイル (`XamlControlsResources`) を `Application.Resources` に追加します。`Application.Resources` はこの時点より前には触れないという WinUI 側の制約があるためです。
7. **ユーザーコードの実行**：最後に、`invokeLater` に渡されたブロックが実行されます。

利用者にとっての要点は 1 つです。
UI の構築はすべて `invokeLater` のブロック内、つまり手順 7 以降で行われるため、初期化順序を意識する必要はありません。

### 4.2 メッセージループとイベントディスパッチ

`Application.Start` がブロックしている間、UI スレッドは WinUI のメッセージループを回し続けます。
このループに仕事を届ける経路は 2 つあります。

**invokeLater による投函。**
`WinUiUtilities.invokeLater` は、渡されたブロックを FIFO キューに積み、UI スレッドの `DispatcherQueue` に対して `TryEnqueue` を呼びます。
`TryEnqueue` に渡すハンドラは Kotlin 実装の COM オブジェクト (16.4 節) で、メッセージループがそれを呼び返すとキューからブロックを取り出して実行します。
どのスレッドから呼んでも安全で、実行順は投函順です。

**イベントの upcall。**
ボタンのクリックなど WinUI 側で発生したイベントは、リスナー登録時に渡したデリゲート (これも Kotlin 実装の COM オブジェクト) をメッセージループが呼び返す形で届きます。
つまりリスナーは常に UI スレッド上で実行され、そのまま UI を更新できます (第8章)。

どちらの経路も、実行されるのは「現在処理中のメッセージが終わった後」です。
UI スレッド上の処理が長引けば、後続の描画も入力もイベントもすべて止まります。
これが「重い処理をワーカースレッドへ逃がす」原則 (第5章、第10章) の理由です。

### 4.3 終了シーケンス

既定では、最後の `WFrame` が閉じられるとアプリは終了します (`WinUiUtilities.exitOnLastWindowClosed`、既定 true)。
最後のウィンドウを閉じてもループを継続したい場合 (常駐アプリなど) は、これを false にして、任意のタイミングで `WinUiUtilities.exit()` を呼びます。
`exit()` はどのスレッドからでも呼べます。

内部の終了処理は次の順で進みます。

1. `Application.Exit` によりメッセージループが終わり、`Application.Start` が戻ります。
2. `ReleasePump.shutdown()` で、GC 連動の COM 参照解放 (第6章) を停止します。以後の解放要求は破棄され、未解放分はプロセス終了に任せます。
3. `RoUninitialize` で COM アパートメントを閉じ、`MddBootstrapShutdown` で Windows App SDK を切り離します。

手順 2 と 3 の順序は重要です。
`RoUninitialize` の後に `Release` を呼ぶとクラッシュするため、先に解放経路を閉じます (「未解放分を追いかけない」のは CsWinRT と同じ割り切りです。第18章)。
また `RoUninitialize` 自体を省くと、JVM シャットダウン中にネイティブスレッドからの呼び返しがアタッチ失敗を起こし、JVM ごと abort します。
`System.exit` などでこの経路を迂回しないでください (第14章)。

なお、メッセージループは JVM プロセス内で一度しか起動できません。
終了後に `invokeLater` を呼ぶと `IllegalStateException` になります。
この制約はテストの設計にも影響します (第13章)。

## 第5章 スレッドモデル

「UI フリーズ」と「別スレッドから触ってクラッシュ」は GUI 開発の二大定番事故です。
この章では、それを避けるための [WinUI4K](https://github.com/nttr-tech/winui4k) のスレッド契約と、契約を守るための道具を説明します。

### 5.1 UI スレッド 1 本の契約と COM アパートメント

[WinUI4K](https://github.com/nttr-tech/winui4k) のスレッド契約は 1 文で言えます。
**`W*` API は UI スレッド上でのみ使う。**

この契約は [WinUI4K](https://github.com/nttr-tech/winui4k) が便宜的に課したものではなく、COM のアパートメント規約に由来します。
起動時に UI スレッドは `RoInitialize(RO_INIT_SINGLETHREADED)` で STA として初期化され (4.1 節)、XAML のオブジェクトはすべてこのスレッドに束縛されます。
別のスレッドから XAML オブジェクトのメソッドを呼ぶことは COM の規約違反で、例外で済むとは限らず、クラッシュや不定動作を招きます。

Swing の EDT や JavaFX の Application Thread と同じ発想ですが、違反時の帰結がより厳しい (JVM の外で壊れる) と考えてください。
現在のスレッドが UI スレッドかどうかは `WinUiUtilities.isDispatchThread` で判定でき、自作コンポーネントの入口でのアサーションに使えます。

なお、UI スレッドは [WinUI4K](https://github.com/nttr-tech/winui4k) が起動する専用スレッド (`WinUI4K-UI`) であり、`main` スレッドではありません。
`main` は `invokeLater` を呼んだ後、自由に使えます (非デーモンの UI スレッドが生きている限りプロセスは終了しません)。

### 5.2 invokeLater と Dispatcher

UI スレッドへ処理を届ける基本 API は 2 つです。

- **`WinUiUtilities.invokeLater(block)`**：ブロックを UI スレッドのメッセージループへ投函します。どのスレッドからでも呼べ、実行順は投函順です。Swing の `SwingUtilities.invokeLater` に相当します。
- **`WinUiUtilities.schedule(delayMillis, block)`**：指定ミリ秒後にブロックを UI スレッドで一度だけ実行します。戻り値の `AutoCloseable` を `close()` すると、発火前ならキャンセルできます。`javax.swing.Timer` のワンショット相当で、実体はネイティブの `DispatcherQueueTimer` です。

Swing の `invokeAndWait` に相当する同期版はありません。
結果が必要な場合は、コルーチン統合 (5.4 節) を使うか、`CountDownLatch` などで自前で待ち合わせます。
ただし UI スレッド上から待ち合わせるとデッドロックするため、待つ側は必ずワーカースレッドであることを確認してください。

### 5.3 ワーカースレッドからの安全な UI 更新

重い処理の定石は「ワーカーで計算し、結果の反映だけを UI スレッドへ戻す」です。

```kotlin
loadButton.addActionListener {
    statusLabel.text = "Loading..."           // ここは UI スレッド
    Thread {
        val result = fetchFromServer()        // 重い処理はワーカーで
        WinUiUtilities.invokeLater {
            statusLabel.text = result         // 反映は UI スレッドへ戻す
        }
    }.start()
}
```

守るべきルールは 2 つだけです。

- ワーカースレッドから `W*` オブジェクトに触らない。読み取り (プロパティの get) も契約違反です。UI の状態が必要なら、ワーカー起動前に UI スレッドで読み出して値を渡します。
- UI スレッドをブロックしない。`Thread.join` や `Future.get` での待機も含みます。

結果が届く前に画面の状態が変わる場合 (検索条件の変更、ページ遷移) は、古い結果の反映を抑止する仕組みが必要です。
同梱の Filer サンプルは、フォルダ移動のたびに世代カウンタを進め、`invokeLater` で戻ってきた結果が古い世代なら捨てる方式で対処しています。
実装の参考にしてください。

### 5.4 コルーチン統合

`winui4k-extension-coroutines` を依存に加えると、`Dispatchers.WinUi` が使えます。
kotlinx-coroutines-swing の `Dispatchers.Swing` の WinUI 版です。

```kotlin
import com.appkitbox.winui4k.extension.coroutines.WinUi

loadButton.addActionListener {
    scope.launch(Dispatchers.WinUi) {
        statusLabel.text = "Loading..."
        val result = withContext(Dispatchers.IO) { fetchFromServer() }
        statusLabel.text = result             // WinUi へ自動的に戻る
    }
}
```

要点は次のとおりです。

- **`Dispatchers.Main` としても登録されます。**`MainDispatcherFactory` の ServiceLoader 登録により、このモジュールがクラスパスにあれば `Dispatchers.Main` は `Dispatchers.WinUi` に解決されます。Android や Swing 向けに書かれた `Dispatchers.Main` 前提のコードがそのまま動きます。
- **`delay` はネイティブタイマーで動きます。**遅延再開は `DispatcherQueueTimer` によるワンショットタイマーで実装されており、スレッドをブロックしません。
- **`Dispatchers.WinUi.immediate` があります。**すでに UI スレッド上なら再ディスパッチせずその場で実行する版で、`MainCoroutineDispatcher` の標準的な使い分けに従います。

5.3 節の Thread 方式と比べると、キャンセル (`Job.cancel`) と例外伝播が構造化されるぶん、画面遷移で結果を破棄する類の制御が簡潔になります。
新規コードではこちらを推奨します。

## 第6章 COM 参照のライフタイム管理

[WinUI4K](https://github.com/nttr-tech/winui4k) のアプリでは、前提の異なる 2 つの寿命管理が同居します。
ネイティブ側の WinUI オブジェクトは COM の参照カウントで、Kotlin 側の `W*` ラッパーは JVM のトレース型 GC で寿命が決まります。
この章では、利用者として知っておくべき橋渡しの仕組みと運用上の規約を説明します。
実装の詳細と CsWinRT との比較は第18章で扱います。

### 6.1 参照カウントとトレース型 GC のミスマッチ

COM のオブジェクトは「自分を参照している者の数」を自分で数えており、`AddRef` で増え、`Release` で減り、0 になった瞬間に自分自身を解放します。
利用者全員が「使い終えたら `Release` を呼ぶ」規約を守ることが前提です。

一方 JVM の GC は参照の数を数えず、ルートからの到達可能性で生存を判定します。
この 2 つをつなごうとすると、次のミスマッチに直面します。

- **解放通知の不在**：GC には「このオブジェクトへの最後の参照が消えた瞬間」を検出する仕組みがなく、`Release` を呼ぶべきタイミングが分かりません。
- **解放スレッドの制約**：XAML オブジェクトの `Release` は UI スレッドから呼ぶ必要がありますが (第5章)、GC の後始末は専用スレッドで走ります。
- **境界をまたぐ循環**：ネイティブと Kotlin が互いを参照し合う循環は、どちらの仕組みも単独では回収できません。

### 6.2 GC 連動の自動解放

最初の 2 つのミスマッチを、[WinUI4K](https://github.com/nttr-tech/winui4k) は次の設計で解決しています。

**ラッパー 1 個に参照カウント 1 つぶんを対応付ける。**
`W*` ラッパーの生成時に確保した COM 参照を、GC がラッパーの到達不能を検出したときに `Release` で返します。
つまり GC の到達可能性判定を、参照カウントを減らすトリガーとして流用します。
検出には Java 9 以降は `java.lang.ref.Cleaner` を、Java 8 では `PhantomReference` による同等の自作機構を使います (第18章)。

**`Release` は UI スレッドへ集約する。**
クリーナースレッドは `Release` を直接呼ばず、解放タスクを `ReleasePump` 経由で UI スレッドのメッセージループへ送ります。
これでアパートメントの制約を常に満たせるうえ、UI スレッド上で実行中の処理の最中にオブジェクトが横から解放されることも構造的に起きません。

利用者にとって重要なのは、ラッパーとコントロール本体の寿命が別物であることです。
ラッパーの回収で返るのは「ラッパーが所有していたカウント 1 つぶん」だけで、ビジュアルツリーがコントロールに持っている参照はそのまま残ります。
画面に表示中のコントロールは、ラッパーが GC されても消えません。
逆に、どこにも追加しなかったコントロールは、ラッパーの回収と同時にネイティブ側でも破棄されます。

要するに、通常の利用では何も管理する必要がありません。
Swing と同じ感覚で `W*` オブジェクトを作って捨てれば、ネイティブ側も追従して解放されます。

### 6.3 自動解放の対象外

次の 2 種類は自動解放の対象外で、参照を保持し続けます。

- **ウィンドウ・Shell 系のラッパー**：`WFrame`、`WAppWindow`、`WAppNotification`、`WJumpList` など。個数が少なく寿命がアプリと近いため、自動解放の複雑さに見合わないという判断です。
- **共有インフラ**：vtable、upcall スタブ、キャッシュ済み HSTRING、ファクトリの statics。プロセス生存期間ぶん保持します。

どちらも、通常のアプリで問題になる量ではありません。

### 6.4 言語境界をまたぐ循環参照とイベントリスナー解除の運用

3 つ目のミスマッチ (境界をまたぐ循環) は、[WinUI4K](https://github.com/nttr-tech/winui4k) では解決していません。
具体的に問題になるのは次の形です。

ネイティブのコントロールがイベントハンドラ (Kotlin 実装の COM オブジェクト) への参照を持ち、そのハンドラが Kotlin 側のオブジェクトを捕捉し、その Kotlin オブジェクトが元のコントロールのラッパーを保持する。
この輪の中では、ネイティブ側の参照カウントと Kotlin 側の到達可能性が互いを支え合い、どちらも解放されません。

CsWinRT はこれを .NET GC 自体の拡張点 (`IReferenceTracker` 統合) で解いていますが、JVM の GC に相当する拡張点はなく、ライブラリのコードでは実現できません (18.4 節)。

回避は利用側の規約になります。
といっても特別なことではなく、通常のイベント購読の作法そのままです。

- 不要になったリスナーは remove 系メソッドで解除する (第8章)。
- ラッパーへの参照を捕捉したリスナーを、コントロールより長寿命のオブジェクトに登録したまま放置しない。

画面を作っては捨てる構成 (ページ遷移など) では、ページの破棄時にリスナーをまとめて解除する後始末処理を設けるのが定石です。

### 6.5 チューニング

寿命管理に関わるシステムプロパティは 3 つあります。

| プロパティ | 値 | 意味 |
|---|---|---|
| `winui4k.lifetime` | `cleaner` / `phantom` | 後始末機構の明示指定。既定は Java バージョンで自動選択 |
| `winui4k.gcThreshold` | 参照数 (整数) | 生存中のネイティブ参照数が閾値を超えるたびに `System.gc()` を要請。既定は無効 |
| `winui4k.ffi` | `panama` / `jna` / `jnr` | FFI バックエンドの明示指定 (第7章) |

`winui4k.gcThreshold` は、GC がネイティブ側のメモリ量を観測できない問題 (.NET の `GC.AddMemoryPressure` に相当する API が JVM にない) への緩和策です。
参照の「数」しか見ないためオプトインの保険という位置づけで、大きな UI を短い周期で作り直すアプリで検討してください (`-XX:+ExplicitGCInvokesConcurrent` の併用を推奨します)。

このプロパティは自動解放の動作検証にも使えます。
`-Dwinui4k.gcThreshold=200` のような低い閾値で Gallery を起動し、カテゴリページを何往復か切り替えると、GC 連動の解放経路が強制的に多発します。
UI が操作でき、標準エラーに解放失敗が出ず、終了コード 0 で終われば、生成・解放・終了の全経路が通っています。

## 第7章 FFI バックエンド

[WinUI4K](https://github.com/nttr-tech/winui4k) のネイティブ呼び出しは、差し替え式の FFI バックエンドが担います。
この章では 3 実装の特徴と選択の仕組み、Java 8 で動かすときの注意を説明します。

### 7.1 3 実装の特徴と対応環境

| バックエンド | 実装基盤 | Java | アーキテクチャ | 優先度 |
|---|---|---|---|---|
| `winui4k-ffi-panama` | Panama (`java.lang.foreign`、FFM API) | 22 以降 | JDK が対応するもの (x64 / ARM64) | 100 (既定) |
| `winui4k-ffi-jna` | JNA | 8 以降 | x64 のみ | 50 |
| `winui4k-ffi-jnr` | JNR (jffi = libffi) | 8 以降 | x86 / x64 / ARM64 | 40 |

3 実装は同じ SPI (`internal.ffi.api` の `FfiBackend`) を実装しており、機能面は等価です。
違いは対応環境と呼び出しコストにあります。

- **Panama** は Java 22 で正式化された標準 API で、追加のネイティブライブラリを持ち込みません。Java 22 以降ではこれが既定です。
- **JNA** が x64 専用なのは、構造体の値渡しを Windows x64 の呼び出し規約で手動変換しているためです。ARM64 では使えません。
- **JNR** は低レベル層の libffi が呼び出し規約の変換を担うため、x86 / x64 / ARM64 のすべてで動きます。Java 8〜21 の ARM64 環境では唯一の選択肢です。

### 7.2 ServiceLoader による実行時選択と明示指定

バックエンドは実行時クラスパスに 1 つ以上置き、初回のネイティブ呼び出し時に次の優先順で確定します。

1. `WinUiUtilities.setFfiBackend(...)` による明示指定 (最初の FFI 使用前にのみ呼べます)
2. システムプロパティ `-Dwinui4k.ffi=panama|jna|jnr`
3. ServiceLoader が発見したもののうち、利用可能 (`isAvailable`) かつ優先度最大のもの

通常は 3 の自動選択で問題ありません。
Java 22 以降なら Panama、それ未満なら JNA (x64) または JNR が選ばれます。
Java 8 のクラスパスに Panama モジュールが混ざっていても、Java 22 バイトコードのロード失敗を検出してスキップするため、`winui4k-all` を全バージョン共通の依存にできます。

明示指定が役立つのは、動作差の切り分け (Panama で起きる事象が JNA でも起きるか) と、特定バックエンドの検証です。

### 7.3 Java 8 で動かすときの注意

- **バックエンドの選択**:x64 なら JNA と JNR のどちらでも動きます (既定は優先度により JNA)。ARM64 では JNR 一択です。
- **`--enable-native-access` は不要**:このオプションは Java 22 以降の Panama 向けで、Java 8〜21 では指定しません。
- **fat JAR を作る場合**:FFI バックエンドは ServiceLoader で発見されるため、`META-INF/services` の同名ファイルを結合する設定 (Shadow プラグインなら `mergeServiceFiles()`) が必須です。結合しないと一部バックエンドが発見されなくなります。
- **動作確認の手段**:リポジトリには JDK 8 + JNA / JNR で Gallery を起動する `runJna` / `runJnr` タスクがあり、必要な JDK は foojay resolver が自動取得します (2.6 節)。自アプリでも、サポート対象の最小 Java バージョンでの起動確認を CI に組み込むことを推奨します (第13章)。

なお、寿命管理の下回りも Java バージョンで切り替わりますが (Java 8 は `PhantomReference` 方式)、これは自動で行われ、利用者が意識する必要はありません (第18章)。

# 第III部 実践ガイド

## 第8章 イベント処理

### 8.1 リスナーの登録と解除

イベント API の形は Swing を踏襲しています。
`addXxxListener` で登録し、`removeXxxListener` で解除します。
リスナーの型は Kotlin の関数型で、専用のリスナーインターフェースは定義されていません。

```kotlin
val onClick: () -> Unit = { println("clicked") }
button.addActionListener(onClick)
// ...
button.removeActionListener(onClick)
```

- リスナーは常に UI スレッド上で呼び出されるため、そのまま UI を更新できます (第5章)。
- 同じリスナーを複数回 add でき、remove は 1 回につき 1 件 (後に登録したもの) を解除します。
- remove の照合は**参照等価**です。`removeActionListener { ... }` のようにその場でラムダを書いても、登録時と別のインスタンスなので解除できません。解除する予定のリスナーは、上の例のように変数に保持して同じ参照を渡します。

コントロール固有のイベント (`WButton` の ActionListener、`WTextField` の TextChangedListener など) に加えて、基底の `WComponent` には全コントロール共通のリスナーがあります (`addSizeChangedListener`、`addLoadedListener`、`addActualThemeChangedListener`)。

### 8.2 デリゲートの正体

`addActionListener` の内部では、WinRT のイベント購読プロトコルがそのまま動いています。
仕組みを知っておくと、リスナー管理の規約 (8.3 節) が「なぜ必要か」まで理解できます。

1. Kotlin のラムダを包む COM オブジェクト (デリゲート) を `KComObject` で構築します。これは `Invoke` メソッド 1 つを持つ WinRT デリゲート (`RoutedEventHandler` や `TypedEventHandler`) の実装で、ネイティブから呼び返せる本物の COM オブジェクトです (16.4 節)。
2. コントロールの `add_Click` のようなイベント登録メソッドを呼びます。戻り値として `EventRegistrationToken` (64 ビット整数) が返り、これが購読の解除キーになります。
3. `W*` クラスは「リスナー → トークン」の対応表を保持します。`removeActionListener` はこの表からトークンを引き、`remove_Click` に渡して購読を解除します。

登録が済んだ時点で、デリゲートへの参照はコントロールのイベントテーブルが保持しています。
つまり **Kotlin のラムダの生存は、ネイティブ側の購読が握っています**。
リスナーを解除すればネイティブ側の参照が外れ、ラムダも GC 可能に戻ります。

### 8.3 リークを避けるリスナー管理のパターン

第6章で述べたとおり、言語境界をまたぐ循環参照は自動回収されません。
イベントリスナーはその循環の典型的な構成要素です。

```
ネイティブのコントロール ──(イベントテーブル)──> デリゲート (Kotlin のラムダ)
        ↑                                            │ 捕捉
        └──(ラッパー経由の COM 参照)── Kotlin オブジェクト <┘
```

実務での指針は次のとおりです。

- **コントロールと同寿命のリスナーは解除不要です。**ボタンとそのクリックリスナーのように、リスナーが捕捉しているのが自分の属する画面だけなら、画面ごと捨てれば循環も丸ごと不要になります。この形の循環はトレース型 GC では回収できないため厳密にはリークですが、対象は少量で、問題になるのは画面の生成と破棄を大量に繰り返す場合に限られます。
- **寿命の異なるオブジェクトをまたぐリスナーは必ず解除します。**アプリ全体で 1 つのモデルに画面側のリスナーを登録する、長寿命のコントロールに短寿命のダイアログを捕捉するリスナーを登録する、といった形が危険です。remove 系メソッドで明示的に解除してください。
- **後始末を 1 箇所に集めます。**ページやダイアログの破棄時に呼ぶ `dispose` 相当の処理を設け、登録したリスナーをそこでまとめて解除するのが定石です。登録と解除をペアで書くことで、解除漏れをレビューで見つけやすくなります。

## 第9章 レイアウト

[WinUI4K](https://github.com/nttr-tech/winui4k) の画面配置には 3 つの手段があります。
WinUI のパネルをそのまま使う方法 (9.1 節)、Swing 風のレイアウトマネージャ (9.2 節)、MigLayout 拡張 (9.3 節) です。
どれを使うかの目安は、単純な縦横並びなら WinUI パネル、Swing からの移植や自作レイアウトなら レイアウトマネージャ、フォーム系の複雑な配置なら MigLayout です。

### 9.1 WinUI パネルによる配置の考え方

WinUI のレイアウトは「親パネルが子を配置する」方式で、パネルの種類が配置規則を決めます。
[WinUI4K](https://github.com/nttr-tech/winui4k) は主要なパネルをラップしています。

**WPanel (StackPanel)。**
子を縦または横に一列に並べます。
`WFrame` の既定のコンテンツ領域 (`contentPane`) はこの `WPanel` で、`frame.add` した順に縦に並ぶのはそのためです。

```kotlin
val row = WPanel(spacing = 8.0, orientation = Orientation.HORIZONTAL)
row.add(WLabel("Name"))
row.add(WTextField())
frame.add(row)
```

**WGrid (Grid)。**
行と列を定義し、セル位置を指定して子を置きます。
行列のサイズは `GridLength` で指定し、`AUTO` (内容に合わせる)、`pixel(値)` (固定)、`star(重み)` (残り領域を重みで分配) の 3 種類があります。

```kotlin
val grid = WGrid(rowSpacing = 8.0, columnSpacing = 8.0)
grid.addColumn(GridLength.AUTO)          // ラベル列は内容幅
grid.addColumn(GridLength.star())        // 入力列が残りを取る
grid.addRow(GridLength.AUTO)
grid.add(WLabel("Name"), row = 0, column = 0)
grid.add(WTextField(), row = 0, column = 1)
```

このほか `WCanvas` (絶対座標)、`WRelativePanel`、`WVariableSizedWrapGrid` などもあります。
ルートの構成を丸ごと変えたい場合は `frame.setContentPane(component)` で `contentPane` 自体を差し替えます。
Gallery のように「タイトルバー行 + コンテンツ行」の `WGrid` をルートに据える構成が実例です。

### 9.2 Swing 風レイアウトマネージャ

Swing の `LayoutManager` に相当する仕組みとして、`WLayoutManager` インターフェースと、その実行環境である `WLayoutPanel` があります。

```kotlin
interface WLayoutManager {
    fun addLayoutComponent(component: WComponent, constraints: Any?)
    fun removeLayoutComponent(component: WComponent)
    fun preferredLayoutSize(parent: WLayoutPanel): WSize
    fun layoutContainer(parent: WLayoutPanel)
}
```

`WLayoutPanel` の土台は WinUI の Canvas (子を自動配置しないパネル) で、計測と配置の計算はすべて Kotlin 側で行い、結果を子の座標とサイズとして適用します。
レイアウトの流れは次のとおりです。

1. WinUI の同期レイアウト (`UpdateLayout`) で各子の希望サイズ (DesiredSize) を確定させる
2. レイアウト対象の領域を決める (ユーザーの明示サイズ > 親からの割り当て > 実測サイズ > `preferredLayoutSize` の優先順)
3. `layoutContainer` がその領域内で各子の位置とサイズを計算し、`setBounds` で適用する

標準実装として `WBorderLayout` (NORTH / SOUTH / EAST / WEST / CENTER の 5 領域) が付属します。

```kotlin
val panel = WLayoutPanel(WBorderLayout())
panel.add(toolbar, WBorderLayout.Constraint.NORTH)
panel.add(content, WBorderLayout.Constraint.CENTER)
panel.add(statusBar, WBorderLayout.Constraint.SOUTH)
```

子の増減やサイズ変化を自分で起こした後は `revalidate()` で再レイアウトを要求します (同一 UI ターン内の複数回要求は 1 回にまとめられます)。
独自のレイアウトが必要なら、`WBorderLayout` の実装 (100 行程度) を雛形に `WLayoutManager` を実装してください。

### 9.3 MigLayout 拡張による配置

`winui4k-extension-miglayout` は、レイアウトライブラリ MigLayout の制約文字列で `W*` コントロールを配置するアダプタです。
実体は `WLayoutManager` 実装の `MigLayoutManager` で、グリッド計算は miglayout-core (5.3 系、Java 8 互換の最終系列) にそのまま委譲します。

```kotlin
val form = WLayoutPanel(MigLayoutManager("wrap 2", "[right][grow, fill]"))
form.add(WLabel("Name"))
form.add(WTextField())
form.add(WLabel("Mail"))
form.add(WTextField())
form.add(WButton("Submit"), "skip, right")
```

制約の文法は本家 MigLayout と同じです (レイアウト制約、列制約、行制約、コンポーネント制約)。
Swing や SWT で MigLayout を使ってきたフォームは、制約文字列をほぼそのまま持ち込めます。
同梱の Form with MigLayout サンプル (2.6 節) が実例です。

WinUI 固有の注意が 1 つあります。
WinUI には「最小サイズ」の計測がないため、このアダプタはコンポーネントの最小サイズを希望サイズと同じ値として扱います。
ウィンドウ縮小時に希望サイズより縮めたいコンポーネントには、`"width 0::"` のように制約で明示してください。

### 9.4 DPI スケーリングと座標系

[WinUI4K](https://github.com/nttr-tech/winui4k) は起動時に Per-Monitor v2 の DPI 対応をプロセスに宣言します (4.1 節)。
モニタごとのスケーリング (100% / 150% / 200% など) への追従は WinUI が行い、アプリ側の対応は不要です。

座標の単位は 2 種類だけ覚えてください。

- **レイアウト系はすべて DIP (論理ピクセル、Double)**：`W*` コンポーネントの幅・高さ・マージン、`WSize` / `WInsets`、レイアウトマネージャの計算はすべてこの単位です。150% スケールのモニタでは 1 DIP = 1.5 物理ピクセルに描画されますが、コード上は意識しません。
- **`WAppWindow` の位置とサイズは物理ピクセル (整数)**:ウィンドウ自体の画面上の位置 (`position: WPoint`) とサイズ (`size: WDimension`) は OS のウィンドウ管理の単位、つまり物理ピクセルです。

「ウィンドウを 800×600 にしたのにコントロールの見かけが合わない」と感じたら、この 2 つの単位の混在を疑ってください。

## 第10章 非同期処理と UI 更新

### 10.1 通信や重い処理中の UI 更新パターン

基本形は第5章で示した「ワーカーで計算し、`invokeLater` (またはコルーチン) で反映する」です。
実務ではこれに 2 つの要素が加わります。

**進捗の表示。**
処理中であることを示すには、開始時に UI を更新してからワーカーを起動し、完了時に戻します。
`WProgressRing` や `WProgressBar` (不確定モード) を出す、ボタンを `isEnabled = false` にする、が定番です。

```kotlin
searchButton.addActionListener {
    searchButton.isEnabled = false
    progressRing.isActive = true
    scope.launch(Dispatchers.WinUi) {
        try {
            val results = withContext(Dispatchers.IO) { search(queryField.text) }
            resultList.setItems(results)
        } finally {
            progressRing.isActive = false
            searchButton.isEnabled = true
        }
    }
}
```

**古い結果の破棄。**
結果が届く前に条件が変わる操作 (逐次検索、フォルダ移動) では、後勝ちの制御が必要です。
コルーチンなら直前の `Job` を `cancel` してから新しい処理を起動するのが簡潔です。
生スレッドで組む場合は、Filer サンプルの世代カウンタ方式 (操作のたびにカウンタを進め、反映時に世代が古ければ捨てる) が参考になります。

### 10.2 WinRT 非同期 API との付き合い方

WinRT の API には `IAsyncOperation<T>` / `IAsyncAction` を返す非同期メソッドが多数あります。
[WinUI4K](https://github.com/nttr-tech/winui4k) はこれらを内部の `Async` 機構で処理し、**公開 API には原則として非同期型を出しません**。
利用者から見える形は 2 通りです。

- **完了までブロックする同期 API**:`WJumpList.load()` や `save()` は、内部で非同期 API の完了を待ってから戻ります。待機には `CoWaitForMultipleObjects` を使い、UI スレッド上で待っても着信 COM 呼び出しをディスパッチしてデッドロックを避けます (タイムアウト 10 秒)。この方式は短時間で終わる操作にだけ使われています。
- **コールバックを受け取る API**:`WWebView.executeScript(script) { result -> ... }` のように、完了時の処理を引数で渡します。スクリプト実行のように完了までに UI のメッセージ処理そのものが必要な操作は、ブロックすると構造的にデッドロックするためです。コールバックは UI スレッドで呼ばれます。

自分でラップ範囲を広げる場合 (第21章) も、この使い分けに従ってください。
「短時間・UI 非依存の完了待ちは同期化、UI の進行が必要な操作はコールバック」が判断基準です。

## 第11章 テーマと外観

### 11.1 Fluent Design と既定スタイル

[WinUI4K](https://github.com/nttr-tech/winui4k) のコントロールが最初から Fluent Design の見た目で表示されるのは、起動時に `XamlControlsResources` (全コントロールの既定スタイル集) が `Application.Resources` へ自動追加されるためです (4.1 節)。
アプリ側の作業はありません。

そのうえで、外観に関わる主な API は次のとおりです。

- **アクセントカラー**:`WButton` の `isAccent = true` で、OS のアクセントカラーを使う強調ボタン (`AccentButtonStyle`) になります。ダイアログの既定アクションなどに使います。
- **背景素材 (backdrop)**:`WFrame.systemBackdrop` で、ウィンドウ背景に Windows 11 の素材を適用できます。`MICA` (デスクトップ壁紙を淡く透過)、`MICA_ALT`、`ACRYLIC` (すりガラス)、`NONE` (既定) の 4 択です。Gallery / Filer は `MICA` を使っています。
- **タイトルバーへの拡張**:`WFrame.extendsContentIntoTitleBar = true` でコンテンツ領域をタイトルバーまで広げ、`setTitleBar(component)` でドラッグ領域を指定します。Gallery のように検索ボックスをタイトルバーに埋め込む構成が作れます。

### 11.2 ダークモード対応

テーマは 3 値の `ElementTheme` (`DEFAULT` / `LIGHT` / `DARK`) で制御します。

- **`WComponent.requestedTheme`**:要素とその配下のテーマを指定します。`DEFAULT` は OS 設定への追従です。ルート要素 (contentPane や `setContentPane` したコンポーネント) に設定すれば、実質的にアプリ全体のテーマ切り替えになります。
- **`WComponent.actualTheme`**:実際に解決されたテーマ (`LIGHT` か `DARK`) を返します。`requestedTheme = DEFAULT` のときに、いま OS がどちらかを知るのに使います。
- **`addActualThemeChangedListener`**:OS 設定の変更などでテーマが切り替わった瞬間に通知されます。

標準コントロールの配色は、テーマ切り替えに自動で追従します。
アプリ側で対応が必要なのは、**自分でハードコードした色**だけです。
Gallery は「ダーク/ライトで値を出し分ける色プロパティを定義し、`addActualThemeChangedListener` で表示中のページを塗り直す」方式で対応しており、独自配色を持つアプリの実装例になります。
設定画面でテーマを選ばせる UI (Light / Dark / Use system setting) も Gallery の SettingsPage が実例です。

### 11.3 リソース解決の仕組み

この節は、テーマ関連のエラーに出会ったときのための背景知識です。

WinUI のスタイルとテーマは XAML リソースシステムの上に載っており、既定テーマリソースは通常、アプリパッケージ内のリソースファイル `resources.pri` から解決されます。
ところが [WinUI4K](https://github.com/nttr-tech/winui4k) のアプリはアンパッケージ (MSIX を使わない構成) で、実行ファイルは java.exe です。
その隣に `resources.pri` は存在しないため、素の状態では XAML が既定テーマリソース (`ms-appx:///Microsoft.UI.Xaml/Themes/themeresources.xaml`) を見つけられず、`XamlControlsResources` の生成が失敗します。

[WinUI4K](https://github.com/nttr-tech/winui4k) はこれを、公式の拡張点である `Application.ResourceManagerRequested` イベントで解決しています。
起動時にこのイベントへ、Windows App SDK ランタイムパッケージ側の `resources.pri` を読む MRT Core の `ResourceManager` を返すハンドラを登録します (4.1 節)。
「Cannot locate resource from 'ms-appx:...'」系のエラーを見たら、この初期化が走る前に XAML リソースへ触れる経路がないかを疑ってください。

同じ理由で、`Application.Resources` へのアクセスは `OnLaunched` 以降でないと失敗します (E_UNEXPECTED)。
利用者のコードは常に `OnLaunched` 後に実行されるため通常は意識不要ですが、ライブラリ内部を拡張する場合 (第21章) は制約として効いてきます。

## 第12章 OS 統合と発展的な利用

### 12.1 シェル統合

Windows のシェル (タスクバー、通知センター) と連携する API が 3 系統あります。

**トースト通知 (WAppNotification)。**
通知の内容は `WAppNotification` のビルダー API で組み立て、`WAppNotificationManager` で表示します。

```kotlin
WAppNotificationManager.register()
WAppNotificationManager.addNotificationInvokedListener { args ->
    // 通知クリック時。args は addArgument で埋め込んだ値
}
val notification = WAppNotification("ビルドが完了しました")
    .addText("winui4k: BUILD SUCCESSFUL")
    .addButton("ログを開く", "action=openLog")
WAppNotificationManager.show(notification)
```

通知のクリックはアプリの再起動を伴うことがあるため、`register()` と Invoked リスナーの登録は起動時に済ませておきます。
対応可否は `WAppNotificationManager.isSupported` で、ユーザーが通知を無効化しているかは `setting` で確認できます。

**バッジ (WBadgeNotification)。**
タスクバーアイコンへの数値バッジ (`setCount`) と記号バッジ (`setGlyph`) です。未読件数の表示などに使います。

**ジャンプリスト (WJumpList)。**
タスクバーアイコンの右クリックメニューです。
`WJumpList.load()` で現在の内容を取得し、`WJumpListItem` を追加して `save()` で反映します。
**ジャンプリストはパッケージ ID を持つアプリ専用**で、アンパッケージ実行 (通常の `java -jar`) では `isSupported` が false になります。利用前に必ず確認してください。

### 12.2 WebView2 の組み込み

`WWebView` は、Microsoft Edge ベースのブラウザコントロール WebView2 のラッパーです。
Web ベースの画面やドキュメント表示をネイティブアプリに埋め込めます。
必要な WebView2 ランタイムは Windows 11 に標準搭載されており、SDK 側の DLL は winui4k の JAR に同梱されているため、追加のセットアップはありません。

```kotlin
val webView = WWebView("https://example.com")
webView.addNavigationCompletedListener { success, status ->
    if (!success) statusLabel.text = "読み込み失敗: $status"
}
frame.setContentPane(webView)
```

押さえておくべき性質は次のとおりです。

- **初期化は非同期です。**ブラウザプロセスの起動には時間がかかるため、`source` の設定や `navigateToString` は初期化完了後に順次適用されます。CoreWebView2 が必要な操作 (`documentTitle` など) を初期化前に使いたい場合は、`ensureCoreWebView2()` で開始し `addCoreWebView2InitializedListener` で完了を待ちます。
- **JavaScript 連携**:`executeScript(script) { result -> ... }` でスクリプトを実行し、`postWebMessageAsJson` / `addWebMessageReceivedListener` でページとの双方向メッセージングができます。
- **ナビゲーション制御**:`addNavigationStartingListener` で false を返すと遷移をキャンセルできます。外部リンクをブロックする用途などに使います。
- **ユーザーデータフォルダ**:WebView2 はプロファイルデータの書き込み先を必要とします。未指定なら書き込み可能な場所 (LOCALAPPDATA 配下) が自動設定されるため、通常は意識不要です。

### 12.3 未ラップの WinUI API を呼ぶ

`W*` クラスがラップしているのは WinUI の一部です。
使いたい機能が未ラップだった場合の選択肢を、推奨順に挙げます。

**リポジトリをフォークしてラッパーを追加する。**
これが本線です。
[WinUI4K](https://github.com/nttr-tech/winui4k) の内部は「winmd から ABI 定数を抽出し、`*Interop` に定数を置き、`ComPtr` でスロットを呼ぶ」という機械的な手順で拡張できるよう設計されており、第17章と 21.3 節にその手順があります。
既存コントロールへのプロパティ 1 つの追加なら、変更は数十行で済みます。
汎用性のある追加は本家への PR を検討してください (21.4 節)。

**内部レイヤを直接呼ぶ。**
内部レイヤ (`ComPtr`、`*Interop` など) の可視性は Kotlin の `internal` で、通常の Kotlin コードからは見えません。
ただし `internal` は JVM バイトコード上は public であるため、Java から書いたコード (または `@Suppress` などの回避手段) では技術的に呼び出せます。
フォークを避けたい一時的な検証には使えますが、内部レイヤは互換性を約束しない領域です。恒久的なコードには前項のフォークを選んでください。

**Issue で要望する。**
実装する余力がない場合は、GitHub の Issue でラップ要望を出せます (21.4 節)。

# 第IV部 品質と配布

## 第13章 テスト

### 13.1 テスト戦略

[WinUI4K](https://github.com/nttr-tech/winui4k) 自体のテストは、実際に WinUI ウィンドウを起動してコントロールを操作する E2E 方式です。
モックやスタブで WinUI を代替しません。
FFI・COM ABI・WinUI 実体という 3 層の境界を越えるライブラリでは、境界のどこか 1 つでも仮定が間違っていれば全体が壊れるため、「本物で検証する」以外に信頼できる方法がないからです。

[WinUI4K](https://github.com/nttr-tech/winui4k) を使うアプリのテストも、同じ道具立てで書けます。
ただし E2E は実デスクトップセッションを要求し、実行も相応に遅いので、アプリ側では次の分担を推奨します。

- **ビジネスロジックは UI から分離し、通常のユニットテストで検証する。**これが大部分を占めるべきです。
- **UI の結線 (イベント → 状態 → 表示) は E2E で薄く検証する。**次節の UiTestHarness 方式が使えます。

### 13.2 UiTestHarness による E2E テストの書き方

E2E テストで最初にぶつかる制約は、第4章で述べた「メッセージループは JVM プロセス内で一度しか起動できない」です。
テストごとにウィンドウを開閉することはできません。

winui4k のテスト基盤 `UiTestHarness` (winui4k/src/test) は、これを次の設計で解決しています。

- 共有の `WFrame` を 1 つだけ遅延生成し、全テストクラスで使い回す。ウィンドウは `activate = false` で表示し、テスト実行中に PC を使っている人からフォーカスを奪わない。
- テスト対象のコントロールは、この共有フレームへの `attach` / `detach` で出し入れする。テンプレート適用後でないと発火しないイベント (TextChanged など) を試すときは、`attachAndAwaitLoaded` で Loaded を待ってから操作する。
- `W*` API は UI スレッド契約 (第5章) なので、テストコードからの操作は `onUiThread { }` / 結果を返す `onUiThreadGet { }` で UI スレッドへ送る。
- 全テスト終了後に一度だけ、共有ウィンドウを閉じてメッセージループの終了を待ち合わせる (終了処理を待たずにプロセスが落ちると、COM の遅延解放と衝突してクラッシュするため)。

テストフレームワークは Kotest (FunSpec スタイル) です。
実際のテストは次のような形になります。

```kotlin
class WButtonTest : FunSpec() {
    init {
        test("コンストラクタで渡したラベルが text から取得できる") {
            onUiThreadGet { WButton("実行する").text } shouldBe "実行する"
        }
    }
}
```

「UI スレッドで生成・操作した結果を取り出し、テストスレッドでアサートする」が基本形です。
実行は次のコマンドで行います。

```powershell
.\gradlew :winui4k:test                        # 全テスト
.\gradlew :winui4k:test --tests "WButtonTest"  # 単一クラス (ワイルドカード不可。単純名か完全修飾名で指定)
```

### 13.3 CI での実行

E2E テストは実デスクトップセッションを必要とし、ヘッドレス環境では動きません。
幸い、GitHub Actions の Windows ランナー (`windows-latest`) は対話セッション相当の環境を持つため、仮想ディスプレイなどの追加工作なしで WinUI を起動できます。
winui4k 自身の CI (`.github/workflows/build.yml`) が実例で、構成は次のとおりです。

1. JDK 25 をセットアップする (テスト用の JDK 8 / 9 / 22 は Gradle の foojay resolver が自動取得)
2. `.\gradlew :winui4k:downloadInstallers` で Windows App SDK ランタイムのインストーラーを取得し、`--quiet --force` でインストールする
3. `.\gradlew build testOnAllJavaVersions` を実行する

`testOnAllJavaVersions` は、同じテストを JDK 8 / 9 / 22 / 25 のツールチェインで順に実行するタスクです。
4 バージョンなのは、FFI と寿命管理の実行時切り替え (第7章、第18章) の境界が Java 9 と 22 にあるためで、各境界の両側を踏んでいます。
JDK 8 / 9 では JNA、22 以降では Panama が自動選択され、バックエンドの組み合わせも同時に検証されます。

ARM64 は `windows-11-arm` ランナーの別ジョブで検証しています (JDK 8 / 9 の Windows ARM64 ビルドが存在しないため JDK 25 のみ。JDK も Temurin ではなく Microsoft Build of OpenJDK を使うなどの差異があります)。
自アプリの CI を組む場合も、この build.yml が出発点として使えます。

## 第14章 デバッグとトラブルシューティング

### 14.1 HRESULT と IRestrictedErrorInfo による診断

[WinUI4K](https://github.com/nttr-tech/winui4k) のエラー処理は 1 種類だけです。
COM 呼び出しが失敗 (HRESULT が負値) すると、`WindowsRuntimeException` が投げられます。

```
WindowsRuntimeException: XamlControlsResources.Append failed: HRESULT=0x802B000A (詳細メッセージ)
```

読み方は次のとおりです。

- **どの操作が失敗したか**:メッセージ先頭の操作名。内部のどの COM 呼び出しかを示します。
- **HRESULT コード**:`0x` 始まりの 32 ビット値。Microsoft のドキュメントやエラーコード検索でそのまま調べられる、Windows 共通のエラーコードです。頻出値は 2.5 節にまとめてあります。
- **詳細メッセージ**:取得できた場合のみ付きます。WinRT には HRESULT に加えて人間可読のエラー詳細を運ぶ仕組み (`IRestrictedErrorInfo`) があり、[WinUI4K](https://github.com/nttr-tech/winui4k) は例外の生成時にこれを問い合わせて、XAML ランタイムが残したエラー説明 (「どのリソースが見つからないか」など) をメッセージへ連結します。

例外の `hresult` プロパティからコードをプログラムで参照することもできます。
それ以上の型付きエラー体系 (例外クラスの階層) はありません。
「どの HRESULT なら回復可能か」の判断はアプリ側の責務です。

### 14.2 症状別トラブル索引

起動時のトラブルは 2.5 節にまとまっています。
ここでは実行中の症状を扱います。

| 症状 | 原因の見当 | 対処 |
|---|---|---|
| UI が固まる (フリーズ) | UI スレッド上での長時間処理またはブロッキング待機 | 重い処理をワーカーへ (第5章、第10章)。スレッドダンプで `WinUI4K-UI` スレッドの滞留箇所を特定する |
| 突然のクラッシュ、JVM の abort | ワーカースレッドから `W*` API に触れている | すべての UI 操作を `invokeLater` 経由に。怪しい箇所に `check(WinUiUtilities.isDispatchThread)` を仕込んで特定する |
| 終了時にクラッシュ・abort する | `System.exit` などで正規の終了経路 (4.3 節) を迂回している | `WinUiUtilities.exit()` で終了する |
| コントロールが描画されない | 既定スタイル (`XamlControlsResources`) の適用失敗 | コンソールの HRESULT を確認 (2.5 節、11.3 節) |
| TabView: `TabItems` に追加してもタブが増えない | 表示前に取得した IVector をキャッシュしている。TabView は表示時にコレクション実体を差し替えるため、古い実体への追加は画面に反映されない | コレクションは操作のたびに取得し直す。ラップ実装では IVector のキャッシュを原則避ける |
| ダークモード切替で一部の色だけ残る | ハードコードした色は自動追従しない | `addActualThemeChangedListener` で塗り直す (11.2 節) |
| メモリ使用量が単調増加する | リスナー解除漏れによる循環参照 (第6章、第8章)、またはネイティブ参照の解放待ち | まず remove 漏れを疑う。GC 頻度依存なら `-Dwinui4k.gcThreshold` で切り分ける (6.5 節) |

### 14.3 アンチパターン集

レビューで指摘される定番を先回りしてまとめます。
いずれも「なぜ駄目か」は既出の章にあります。

- **UI スレッドでのブロッキング処理**:リスナー内での通信、ファイル I/O、`Thread.sleep`、`Future.get`。UI 全体が止まります (第5章)。
- **ワーカースレッドからの `W*` 操作**:書き込みだけでなく読み取りも契約違反です。例外にならずに壊れることがあるため、「たまたま動いた」を安全と誤認しがちです (第5章)。
- **その場ラムダでの removeListener**:参照等価で照合されるため解除できません。解除予定のリスナーは変数に保持します (8.1 節)。
- **寿命をまたぐリスナーの解除忘れ**:長寿命オブジェクトに短寿命の画面を捕捉させると、言語境界をまたぐ循環で残り続けます (6.4 節、8.3 節)。
- **コレクションプロパティの実体キャッシュ**:TabView の例のように、WinUI 側が実体を差し替えるコレクションがあります。毎回取得し直すのが安全です (14.2 節)。
- **`System.exit` による即時終了**:解放停止 → `RoUninitialize` の終了シーケンスが飛び、シャットダウン中のクラッシュを招きます。`WinUiUtilities.exit()` を使います (4.3 節)。
- **UI 要素の生成を `invokeLater` の外で行う**:`W*` オブジェクトはコンストラクタの時点で COM 呼び出しを行うため、生成も UI スレッド上で行う必要があります (第5章)。

## 第15章 パッケージングと配布

[WinUI4K](https://github.com/nttr-tech/winui4k) アプリの配布形態は 2 つあります。
JVM のある環境向けの **JAR 配布**と、エンドユーザー向けの **jpackage による JRE 同梱インストーラー**です。
どちらの場合も、Windows App SDK ランタイムのセットアップ (2.4 節) を考慮に入れます。

### 15.1 jpackage によるアプリ配布

エンドユーザーに JVM の準備を求めない配布は、JDK 標準の jpackage で行います。
Gallery サンプルの `packageExe` タスクが実装例で、要点は次のとおりです。

- **入力は fat JAR 1 つに絞る**:アプリと全依存を 1 つにまとめた fat JAR を作り、それだけを置いたディレクトリを jpackage の `--input` に渡します (`--input` はディレクトリ全体を同梱するため)。fat JAR の作成では、FFI バックエンド 3 つが同名の `META-INF/services` ファイルを持つため、**サービスファイルの結合 (Shadow プラグインの `mergeServiceFiles()`) が必須**です。
- **`--java-options --enable-native-access=ALL-UNNAMED` を付ける**:同梱 JRE は Java 22 以降になるため Panama バックエンドが選ばれ、このオプションが必要です (2.2 節)。
- **`--win-upgrade-uuid` を固定する**:上書きインストールを正しく動かすための ID です。Gallery はアプリ ID から決定的に導出して、ビルドごとに変わらないようにしています。
- **`--type exe` には WiX Toolset が必要です**:Gallery のビルドは WiX 3.14 を自動ダウンロードして使います。

これで「ダブルクリックでインストールできる .exe、JVM 不要」が得られます。
Gallery では `.\gradlew :winui4k-sample-gallery:packageExe` で実際に生成できるので、自アプリの雛形にはこのタスク定義をコピーするのが早道です。

JAR 配布の場合は、fat JAR を作って `java --enable-native-access=ALL-UNNAMED -jar app.jar` (Java 22 以降) または `java -jar app.jar` (Java 8〜21、JNA / JNR 使用時) で起動する案内を添えます。

### 15.2 ランタイムインストーラーの同梱

Windows App SDK ランタイムが未導入の環境では、既定では Microsoft のダウンロード誘導ダイアログが出ます (2.4 節)。
エンドユーザーにこの操作をさせたくない場合は、ランタイムのインストーラーを配布物に同梱します。

1. `.\gradlew :winui4k:downloadInstallers` で 3 アーキテクチャ分のインストーラーを取得する (各約 104 MB)
2. 配布対象アーキテクチャの `WindowsAppRuntimeInstall-<arch>.exe` を、アプリの作業ディレクトリ (または `winui4k.installer.dir` で指定するディレクトリ) に置く

これだけで、初回起動時にサイレントインストールが走り、ユーザー操作なしでアプリが立ち上がります。
Windows App SDK のライセンスはインストーラーの再頒布を認めているため、同梱に法的な障害はありません (1.4 節)。
なお、ランタイムはマシン単位のインストールなので、2 回目以降の起動ではこの処理は走りません。

### 15.3 配布サイズと起動時間

配布サイズの内訳は、おおよそ次の 3 つです。

- **アプリ本体 (fat JAR)**:winui4k とバックエンド、アプリコードで数十 MB 未満。JAR に内蔵されたブートストラップ DLL (3 アーキテクチャ分) を含みます。
- **同梱 JRE (jpackage の場合)**:数十 MB 程度。jlink によるモジュール絞り込みで削減の余地があります。
- **ランタイムインストーラー (同梱する場合)**:約 104 MB。最大の要素ですが、同梱せずダイアログ誘導に任せる選択もできます。

Chromium と Node.js を必ず同梱する Electron 方式と比べると、ベースラインは小さく、かつ「何を同梱するか」の選択肢がある構成です。
描画エンジンを同梱しないぶん、実行時のメモリ使用量のベースラインも小さくなります (1.4 節)。

起動時間は「JVM の起動 + Windows App SDK の初期化 + XAML の初期化」の合計で、C# 製 WinUI アプリより JVM 起動のぶん不利です。
体感を改善する常套手段は、`invokeLater` に渡す初期化ブロックを最小にして最初のウィンドウを早く出し、残りの初期化を表示後に回すことです。

# 第V部 内部実装

第V部は、[WinUI4K](https://github.com/nttr-tech/winui4k) が「ブリッジ DLL なしで WinUI を動かす」仕組みそのものの解説です。
デバッグで内部にステップインするとき、フォークして保守を引き取るとき (1.4 節)、ラップ範囲を広げるとき (第21章) の地図として使ってください。
COM の基礎用語は 3.3 節と第6章を前提にします。

## 第16章 COM ABI を JVM から直接扱う仕組み

WinUI のオブジェクトはプロセス内では COM オブジェクトであり、その呼び出し規約はバイナリレベルで固定されています。
この章では、その規約を JVM の FFI だけで満たすための 6 つの部品を、下から順に見ていきます。

### 16.1 vtable 呼び出しと ComPtr

すべての基礎は「インターフェースポインタから関数ポインタへの 2 段の間接参照」です。
`ComPtr` (internal/com) はこれをカプセル化した最小単位で、生アドレスを包み、次の手順でメソッドを呼びます。

1. ポインタの指す先頭 8 バイトを読む → vtable のアドレス
2. `vtable + スロット番号 × 8` を読む → 目的のメソッドの関数ポインタ
3. その関数ポインタを FFI の downcall で呼ぶ。第 1 引数は常に自分自身のポインタ (C++ の this に相当)

呼び出しのシグネチャは `CallDescriptor` (戻り値と引数の型の並び。`internal.ffi.api` のバックエンド非依存の語彙) で表現します。
`ComPtr.call(slot, args...)` は引数の Kotlin 型 (Ptr / Int / Long / Double / 構造体) からディスクリプタを推論し、戻り値の HRESULT を検査します。
このほか、COM 頻出の「出力ポインタで結果を受け取る」パターン用の `getPtr` / `getInt` / `getBool` などと、IUnknown の 3 メソッド (`queryInterface` / `addRef` / `release`。スロット 0 / 1 / 2) を備えます。

呼び出しごとにディスクリプタを組み直すのは無駄なので、スカラー引数のみの呼び出しは引数型の並びを整数にエンコードし、downcall ハンドルをキャッシュして再利用します。
`ComPtr` 自体は寿命を管理しない点が重要で、解放の責務は第18章の機構が担います。

### 16.2 HSTRING と文字列変換

WinRT の文字列型 HSTRING と Java の String の変換は `Hstring` (internal/winrt) が担います。
実体は combase.dll の `WindowsCreateString` / `WindowsGetStringRawBuffer` / `WindowsDeleteString` の呼び出しで、UTF-16 バッファを介して相互変換します。

工夫は 2 つあります。

- **意図的リークのキャッシュ**:`RoGetActivationFactory` に渡すランタイムクラス名 (`"Microsoft.UI.Xaml.Controls.Button"` など) は少数の固定文字列が繰り返し使われるため、`ofCached` が HSTRING をプロセス生存期間ぶんキャッシュします。第6章の「共有インフラは解放しない」の一例です。
- **所有権移譲時の複製**:キャッシュ済み HSTRING を「呼び出し先が解放する」規約の出力先へ渡すと、キャッシュがダングリングします。そうした経路 (upcall での `GetRuntimeClassName` 応答など) では `WindowsDuplicateString` で複製してから渡します。COM の所有権規約 (第6章) が文字列にもそのまま適用される例です。

### 16.3 RoGetActivationFactory とオブジェクト生成

WinRT のオブジェクト生成は「クラス名からファクトリを取得し、ファクトリに作らせる」の 2 段階です。
`Activation` (internal/winrt) がこれを 3 つの形に整理しています。

- **`factory(runtimeClass, iid)`**:`RoGetActivationFactory` でアクティベーションファクトリを取得します。static メソッドの呼び出し (`Application.Start` など) にも、このファクトリがそのまま使われます。
- **`activate(runtimeClass, iid)`**:既定コンストラクタ相当。ファクトリの `ActivateInstance` でインスタンスを作り、目的のインターフェースへ QI して返します。中間で得たポインタ (ファクトリと IInspectable) は内部で確実に解放されます。この「中間参照の解放漏れ」が手書きで最も起きやすいリークで、2 引数版の存在理由です。
- **`composeDefault(runtimeClass, factoryIid)`**:継承可能 (composable) なクラス用。ファクトリの `CreateInstance(outer, &inner, &instance)` を outer = null で呼びます。outer に実体を渡すと COM 集約になります (16.5 節)。

どの W* クラスがどの形を使うかは、winmd に記録されたクラスの種別 (activatable / composable) で決まります (第17章)。

### 16.4 upcall による COM オブジェクト実装

ここまでは JVM からネイティブを呼ぶ方向 (downcall) でした。
イベントハンドラや `Application` のオーバーライドのように、**ネイティブから呼び返される Kotlin 実装の COM オブジェクト** (CCW) を作るのが `KComObject` (internal/winrt) です。

COM オブジェクトの見た目は「vtable へのポインタを先頭に持つメモリブロック」なので、原理的には次を組み立てれば本物になります。

1. Kotlin のラムダを FFI の upcall スタブ (ネイティブから呼べる関数ポインタ) に変換する
2. スタブの関数ポインタを並べた vtable をネイティブメモリに書く
3. vtable へのポインタを先頭に持つオブジェクト本体を確保する

`KComObject` はこれに 3 つの実装上の工夫を加えています。

- **vtable の共有**:upcall スタブと vtable はメソッドシグネチャの並び (形状) ごとに 1 個だけ作り、全インスタンスで共有します。イベントハンドラは大量に生成されるため、インスタンスごとにスタブを作ると際限なくネイティブ資源を消費するからです。オブジェクト本体は `{vtable ポインタ, インスタンスキー}` の 16 バイトだけで、共有スタブは呼び返されるとキーでグローバル登録表を引き、そのインスタンスの Kotlin 実装 (ラムダ) に処理を委譲します。
- **規約どおりのプロローグ**:vtable の先頭には IUnknown (QueryInterface / AddRef / Release)、必要なら続けて IInspectable (GetIids / GetRuntimeClassName / GetTrustLevel) の実装が自動で入ります。参照カウントは `AtomicInteger` (生成時 1) で自前実装し、カウントが生きている間は登録表がラムダを GC から守り、ネイティブからの最後の `Release` で登録を外して GC 可能に戻します。「カウントが生きている間はマネージ側を守る」という不変条件は、.NET ランタイムが CCW に課しているものと同じです。
- **例外の遮断**:upcall の中で Kotlin の例外がネイティブ側へ突き抜けると JVM ごとクラッシュします。このためディスパッチ処理はすべての例外を捕捉し、標準エラーへ出力したうえで HRESULT (E_FAIL) に変換して返します。イベントリスナー内の例外がアプリを落とさないのはこの遮断のおかげですが、裏を返せば**リスナー内の例外は握りつぶされて標準エラーに出るだけ**なので、デバッグ時はコンソールを確認してください。

### 16.5 COM 集約による Application サブクラス合成

WinUI アプリは `Application` のサブクラスを要求します。
C# なら `class App : Application` と書くところですが、JVM のクラスがネイティブのクラスを直接継承することはできません。
WinRT はこの「言語をまたぐ継承」を **COM 集約**という仕組みで実現しており、[WinUI4K](https://github.com/nttr-tech/winui4k) はそれを FFI で組み立てます。

考え方は「継承を、外側 (outer) と内側 (inner) の 2 オブジェクトの合体で表す」です。

1. Kotlin 側で outer を作ります。`KComObject` で `IApplicationOverrides` (仮想メソッド `OnLaunched` の実装) と `IXamlMetadataProvider` (XAML の型解決。実装は WinUI 付属のプロバイダー実体へ全転送) を実装した COM オブジェクトです。これが「サブクラスで上書きした部分」に相当します。
2. `IApplicationFactory.CreateInstance(outer, &inner, &app)` を呼びます。ネイティブ側が基底クラスの実装 (inner) を作り、outer と結び付けて、合成されたオブジェクト (app) を返します。
3. 以後、XAML ランタイムから見ると app は 1 つの `Application` サブクラスです。仮想メソッドの呼び出しは outer (Kotlin 実装) へ、outer が知らないインターフェースの QueryInterface は inner (基底実装) へ委譲されます。この転送は `KComObject` の QI 実装が担います。

`OnLaunched` が Kotlin 側へ upcall されてくる (4.1 節) のは、この合成の結果です。
オーバーライドと基底委譲という継承の 2 要素を、COM の規約だけで再現していることになります。

### 16.6 ジェネリック実体 IID の SHA-1 計算

`IVector<UIElement>` のようなジェネリックインターフェースの実体には、型引数の組ごとに固有の IID があります。
ところがこれらの IID は winmd に記録されていません。
WinRT の仕様では、ジェネリック定義の IID と型引数から**署名文字列**を組み立て、それを名前ベース UUID (RFC 4122 version 5、SHA-1) にかけた値を IID とする、と定められています。

`Pinterface` (internal/winrt) がこの計算を実装しています。
WinRT 固有の名前空間 GUID をソルトとして、署名文字列 (例: `pinterface({ジェネリック定義のIID};rc(Microsoft.UI.Xaml.UIElement;{既定インターフェースのIID}))`) の SHA-1 ハッシュから UUID を導出します。
イベントの `TypedEventHandler<T1, T2>` の実体 IID もこの計算で得ています。

計算誤りは「なぜか QI が E_NOINTERFACE を返す」という追いにくい不具合になるため、実装は公開されている既知値 (`IIterable<String>` = `e2fcc7c1-3bfc-5a0b-b2b0-72e769d1cb7e`) との一致で検証済みです。
「推測に頼らない」という設計方針 (1.2 節) の、winmd に値がない領域での実現方法と言えます。

## 第17章 ABI 定数の機械抽出

### 17.1 winmd と tools/dump_winmd.py

COM 呼び出しには、インターフェースごとの IID と、メソッドごとの vtable スロット番号が必要です。
これらの一次情報は winmd (Windows の型情報ファイル、ECMA-335 形式のメタデータ) にあります。
WinUI の winmd は NuGet パッケージ (Microsoft.WindowsAppSDK.WinUI) に、OS 側の型 (Windows.Foundation など) の winmd は Windows SDK に含まれます。

同梱の `tools/dump_winmd.py` は、winmd から必要な値を抽出するツールです。
依存は PE/.NET メタデータの読み取りライブラリ dnfile だけです。

```bash
pip install dnfile
python tools/dump_winmd.py Microsoft.UI.Xaml.winmd Microsoft.UI.Xaml.Controls.IButton
```

型の種類に応じて次を出力します。

- **インターフェース**:IID と、`vtbl[6+i]: メソッド名(引数) -> 戻り値` の形式のスロット一覧。WinRT のインターフェースはスロット 0〜2 が IUnknown、3〜5 が IInspectable で固定なので、固有メソッドは 6 から winmd の宣言順に並びます。
- **デリゲート**:IID と `Invoke` (スロット 3)。
- **enum**:名前と数値の一覧。`W*` API の enum 定数の出所です。
- **構造体**:フィールドの順序と型。FFI のメモリレイアウト定義に使います。
- **クラス**:基底、既定インターフェース、ファクトリの種類 (activatable / composable / statics)。16.3 節のどの生成手段を使うかがここで決まります。

### 17.2 推測値ゼロの原則と検証方法

抽出した値は `internal/winui` の `*Interop` オブジェクトに定数として置きます。
ファイルは winmd のソース単位で分かれています (`XamlInterop` / `WindowingInterop` / `FoundationInterop` / `NotificationInterop` / `WebView2Interop`)。

```kotlin
const val IID_IUIElement = "c3c01020-320c-5cf6-9d24-d396bbfa4d8b"
const val IUIElement_get_Opacity = 9
```

原則は 1 つだけです。
**IID とスロット番号を、記憶や推測から手書きしない。必ず dump_winmd.py の出力から転記する。**

理由は、間違いの現れ方が最悪だからです。
IID の誤りは E_NOINTERFACE でまだ分かりやすいものの、スロット番号の誤りは「隣のメソッドが呼ばれる」ことを意味します。
シグネチャがたまたま互換なら、エラーにならずに別の動作をする、再現条件の不明な不具合になります。

検証は 2 段構えです。
定数の正しさ自体は、winmd から再抽出して突き合わせれば機械的に確認できます (出所が機械抽出なので、照合も機械的にできます)。
そのうえで、値が実際に正しく機能することは E2E テスト (第13章) が実機の WinUI で担保します。
winmd に値がないジェネリック実体 IID の扱いは 16.6 節のとおりです。

## 第18章 ライフタイム管理の実装と CsWinRT との比較

第6章で利用者視点の振る舞いを説明した自動解放機構を、この章では実装として読み解きます。
設計の下敷きは Microsoft 公式の C# 向け相互運用ランタイム CsWinRT で、同じ問題を JVM の道具でどう解いたか、何が再現できなかったかを対比します。

### 18.1 ComLifetime と 3 状態 CAS、premature finalization と fence

**ComLifetime。**
`W*` ラッパー 1 個につき 1 個作られる所有記録で、CsWinRT の `IObjectReference` に相当します。
生成時の既定インターフェースを `adopt` で預かり、後から QI した各ビュー (同一オブジェクトへの別インターフェースのポインタ。それぞれが参照カウント 1 つぶんの所有) を `own` で追記します。
ラッパーが所有する全カウントを 1 箇所に束ね、解放を一括で行うためです。

**State の分離。**
解放に必要な情報 (生ポインタのリストと状態フラグ) は、`ComLifetime` からさらに `State` という別オブジェクトへ分離され、GC への登録にはそれだけが渡されます。
後始末処理にラッパー本体を捕捉させると、登録自体が本体への強参照となり、永遠に到達不能にならないからです。
この種の実装で最も間違えやすい点で、CsWinRT ではなく Java の `Cleaner` の API 設計に由来する制約です。

**3 状態 CAS。**
解放へ至る経路は GC 経由と明示的な `close()` の 2 つがあり、同時に走りえます。
`State` は `AtomicInteger` の CAS で NOT_DISPOSED → DISPOSE_PENDING → DISPOSE_COMPLETED を遷移させ、遷移に成功した側だけが解放タスクを発行することで、`Release` の二重実行を防ぎます。
この 3 状態の設計は CsWinRT からの移植です。

**premature finalization と fence。**
トレース型 GC には、メソッドの実行中でも「このオブジェクトのフィールドをもう読まない」と JIT が判断した時点でオブジェクトを到達不能とみなせる、という直感に反する性質があります。
ポインタを読み出してネイティブ呼び出しを実行している最中に後始末が走る premature finalization の危険があり、対策として `Reference.reachabilityFence` (Java 8 では `synchronized (obj) {}` イディオム) で生存を保証します。
ただし [WinUI4K](https://github.com/nttr-tech/winui4k) で fence が実際に必要なのは所有権登録の完了保証 (`ComLifetime.own`) だけです。
`Release` が UI スレッドのメッセージループへ集約される (次項) 結果、UI スレッド上で実行中の W* 呼び出しと解放が同時に走ることは構造的にないからです。

**ReleasePump。**
解放タスクを UI スレッドへ送る投函口です。
本体は com 層にありますが、投函の実装 (DispatcherQueue) は winui 層にあるため、レイヤの依存方向 (winui → com) を守るべく、起動時に winui 層が投函手段を注入します。
UI スレッドの捕捉前に届いたタスクは控えに溜め、捕捉時にまとめて流し込みます。

### 18.2 Cleaner / PhantomReference の切り替えと MethodHandle による実行時解決

「到達不能になったら後始末を実行する」ための API は Java のバージョンで異なるため、下回りは実行時に切り替えます (`-Dwinui4k.lifetime` で明示指定も可能)。

| 実行環境 | 後始末 | fence | FFI |
|---|---|---|---|
| Java 8 | `PhantomReference` + `ReferenceQueue` + 自前デーモンスレッド | `synchronized` イディオム | JNA / JNR |
| Java 9〜21 | `java.lang.ref.Cleaner` | `Reference.reachabilityFence` | JNA / JNR |
| Java 22 以上 | 同上 | 同上 | Panama |

境界が 9 と 22 の 2 箇所にあるのは、寿命管理層の境界 (Java 9 の `Cleaner` / `reachabilityFence` 導入) と FFI 層の境界 (Java 22 の FFM 正式化) が独立しているためです。

Java 8 向けの自作実装は、`PhantomReference` を `ReferenceQueue` に結び付け、デーモンスレッドがキューを待ち受けるという、Cleaner 自身の内部と同じ構造です。
罠が 1 つあり、PhantomReference そのものが GC されないよう、生きている登録は集合で強参照し、後始末が済んだら外します。

興味深いのは、コアモジュールが Java 8 ターゲット (`-Xjdk-release=8`) なのに Java 9 の Cleaner を使えている点です。
これは `Class.forName` と `MethodHandle` による実行時解決で実現しています。
同じ制約を持つ FFI 層は別モジュール分離 (ServiceLoader) を選んでおり、使い分けの基準は API 面の広さです。
FFM のように型が大量に登場する API は別モジュールでの直接参照が読みやすく、Cleaner のようにメソッド数個で済む API は実行時解決のほうが配布物を増やさずに済みます。

もう 1 つの JVM 固有の設計点は、クリーナースレッド (`WinUI4K-Cleaner`) の起動時に `RoInitialize(MTA)` を呼んで COM に参加させることです。
後始末は解放タスクを投函するだけですが、投函 (`DispatcherQueue.TryEnqueue`) 自体が COM 呼び出しであり、アパートメント未初期化のスレッドからの呼び出しを避けるためです。
DispatcherQueue は agile (スレッドを問わない) なので、MTA のスレッドから安全に呼べます。

### 18.3 CsWinRT の解決機構との対応表

COM の参照カウントとトレース型 GC のミスマッチは 4 つに整理でき (第6章)、CsWinRT と [WinUI4K](https://github.com/nttr-tech/winui4k) の解き方は次のように対応します。

| ミスマッチ | CsWinRT | [WinUI4K](https://github.com/nttr-tech/winui4k) |
|---|---|---|
| ① 解放通知の不在 | ファイナライザと明示 `Dispose` の 2 経路 + 3 状態 CAS | `Cleaner` / `PhantomReference` と `close()` の 2 経路 + 3 状態 CAS (移植) |
| ② ネイティブメモリの不可視 | `GC.AddMemoryPressure` (ラッパーあたり固定値の申告) | `NativeMemoryGovernor` (参照数の閾値で `System.gc()` を要請。オプトイン) |
| ③ 境界をまたぐ循環 | `IReferenceTracker` と .NET GC の統合で自動回収 | **解決せず**。リスナー解除の運用規約 (6.4 節) |
| ④ 解放スレッドの制約 | 生成時のコンテキストへマーシャルして `Release` | `ReleasePump` で UI スレッドへ集約 |

④ の違いは前提の違いから来ています。
CsWinRT は複数アパートメントの一般ケースを扱うためコンテキストの記録とマーシャルが要りますが、[WinUI4K](https://github.com/nttr-tech/winui4k) は「UI スレッド 1 本」を前提に置いているため (第5章)、全解放を単一スレッドへ集約するだけで済みます。
前提を狭めて実装を単純化した例です。
プロセス終了時に未解放分を追いかけない割り切り (4.3 節) は、CsWinRT と共通です。

### 18.4 JVM で再現できないもの

対応表の ③ が、JVM で原理的に再現できない唯一の項目です。

CsWinRT の循環回収は、.NET GC のマークフェーズ中にランタイムと XAML の参照トラッカーが相互にオブジェクトグラフを照会する、という **GC 自体の拡張点**の上に成り立っています。
JVM の GC にはマークフェーズへ外部の参照グラフを注入する拡張点がなく、ライブラリのコードでは実現できません。
JVMTI エージェントで全ヒープを走査すれば理論上は可能ですが、GC のたびに stop-the-world の走査を行うコストは実用に耐えません。

② にも差があります。
`GC.AddMemoryPressure` に相当する「ネイティブ割り当て量を GC に申告する」汎用 API は JVM に存在せず、`NativeMemoryGovernor` は参照の個数しか見ない近似にとどまります。

この 2 つの制約が、第6章で述べた利用側の規約 (リスナーは明示解除する、必要なら `gcThreshold` を使う) の根拠です。
言い換えると、[WinUI4K](https://github.com/nttr-tech/winui4k) の寿命管理は「CsWinRT の設計から、言語ランタイムの改造が必要な部分を除いたすべて」を JVM 上に再現したものです。

## 第19章 コードリーディングガイド

### 19.1 リポジトリの歩き方

「何を知りたいか」から入口ファイルを引ける対応表です。
パスはコアモジュール `winui4k/src/main/kotlin/com/appkitbox/winui4k/` からの相対です。

| 知りたいこと | 入口 | 本書の対応章 |
|---|---|---|
| 起動と終了の全体 | `WinUiUtilities.kt` (メッセージループ、`Application` 合成) | 第4章 |
| UI スレッドへの投函 | `internal/winui/Dispatcher.kt` | 第5章 |
| イベント購読の配管 | `internal/winrt/Events.kt` と `Events.kt` (ルート) | 第8章 |
| Windows App SDK の初期化 | `internal/winui/Bootstrap.kt` | 2.4 節 |
| vtable 呼び出し | `internal/com/ComPtr.kt` | 16.1 節 |
| HRESULT の例外化と診断 | `internal/com/Hresult.kt` | 14.1 節 |
| CCW (upcall) | `internal/winrt/KComObject.kt` | 16.4 節 |
| オブジェクト生成 | `internal/winrt/Activation.kt` | 16.3 節 |
| 参照の自動解放 | `internal/com/lifetime/` 一式 | 第18章 |
| FFI の抽象と実装 | `internal/ffi/api/`、別モジュール `winui4k-ffi-*` | 第7章 |
| ABI 定数 | `internal/winui/*Interop.kt` | 第17章 |
| E2E テスト基盤 | `winui4k/src/test/.../UiTestHarness.kt` | 第13章 |

読み進め方としては、まず `W*` クラスを 1 つ選んで下へ潜るのが効率的です。
たとえば `WButton` から始めると、生成 (`Activation`) → プロパティ (`ComPtr` + `XamlInterop` の定数) → イベント (`Events` + `KComObject`) → 解放 (`ComLifetime`) の順に、全レイヤを一筆書きで通過できます。
`W*` クラス自体は「Interop の定数を使って ComPtr を呼ぶ」だけの薄い層なので、下のレイヤさえ分かればどのクラスも同じ形に見えてきます。

### 19.2 ビルド基盤

ビルドの共通設定は `buildSrc` の convention plugin に集約されています。

- **`winui4k.kotlin-common`**:全モジュールの土台。ビルドは常に JDK 25 ツールチェインで行い、`-Xjdk-release` でターゲットのバイトコードと API 面 (コアは Java 8、panama モジュールは Java 22) を保証します。Spotless + ktlint (フォーマット) と detekt (静的解析) の設定もここです。
- **`winui4k.kotlin-library`**:公開ライブラリ用。Maven Central への公開設定 (POM、ソース JAR、署名) を持ちます。
- **`winui4k.kotlin-application`**:サンプルアプリ用。`run` タスクへの `--enable-native-access` 付与や、Java 8 ターゲットのアプリの実行時クラスパスに Java 22 ターゲットの panama モジュールを載せるための属性調整を行います。
- **`winui4k.fat-jar`**:Shadow プラグインによる fat JAR 生成。FFI バックエンドの `META-INF/services` 結合 (15.1 節) はここで設定されています。

特殊なのは detekt の実行方法です。
detekt 1.23 は JDK 25 上で動かないため、Gradle プラグインのインプロセス実行ではなく、JDK 21 の別プロセスで detekt-cli を起動する方式をとっています (JDK 21 は foojay resolver が自動取得)。
バージョンは `gradle/libs.versions.toml` で一元管理され、テスト用の複数 JDK も含めて、開発者が手動で用意するのは JDK 25 だけで済むようになっています (21.1 節)。

# 第VI部 プロジェクト情報

## 第20章 バージョンと互換性

### 20.1 動作環境マトリクス

| 項目 | 対応範囲 | 備考 |
|---|---|---|
| OS | Windows 11 (Windows 10 バージョン 1809 以降でも動く想定) | WinUI 自体の要件 |
| Windows App SDK ランタイム | 2.2 系 | 未導入なら起動時に自動セットアップ (2.4 節) |
| Java (x64) | 8 以降 | 8〜21 は JNA / JNR、22 以降は Panama (第7章) |
| Java (ARM64) | 8 以降 | 8〜21 は JNR のみ、22 以降は Panama / JNR。JDK 自体が ARM64 版であること |
| Java (x86) | 8 以降 | JNR のみ |

CI で常時検証しているのは、x64 の JDK 8 / 9 / 22 / 25 と、ARM64 の JDK 25 です (13.3 節)。
それ以外の組み合わせは、設計上は上表のとおり動く想定ですが、検証頻度が下がります。
本番採用時は、自アプリの CI で対象の組み合わせを直接回すことを推奨します。

依存する Windows App SDK のバージョンは、ライブラリ側に定数として埋め込まれています (現在は 2.2 系)。
ランタイムのメジャーバージョンが一致しないと起動に失敗するため (2.5 節)、[WinUI4K](https://github.com/nttr-tech/winui4k) のバージョンを上げる際は、同梱するランタイムインストーラー (15.2 節) も対応する版に揃えてください。

### 20.2 バージョニングポリシーと変更履歴の読み方

現在のバージョンは 0.x 系です。
セマンティックバージョニングの慣例どおり、0.x の間は公開 API の互換性を保証せず、マイナーバージョン間でも破壊的変更がありえます。
1.4 節で述べた「試作段階のライブラリ」という位置づけが、バージョン番号にも表れていると理解してください。

リリースの成果物は 2 系統あります。

- **Maven Central**:`com.appkitbox.winui4k` グループの各モジュール。通常の依存解決はこちらです。スナップショット版は Central の snapshots リポジトリに公開されることがあります。
- **GitHub Releases**:バージョンタグ (`v*.*.*`) ごとに、全部入り JAR (`winui4k-*-all.jar`) と Gallery のインストーラーが添付されます。変更内容もここに記録されるため、**変更履歴の一次情報は GitHub Releases のリリースノート**です。

アップグレード時の確認手順は、リリースノートで破壊的変更の有無を見る → 動作環境マトリクス (依存する Windows App SDK 版を含む) の変化を見る → 自アプリの E2E テスト (第13章) を通す、の順が実務的です。

### 20.3 ロードマップ

[WinUI4K](https://github.com/nttr-tech/winui4k) は、NTTレゾナントテクノロジーが自社サービスの PC クライアントへの適用を一つの目的として試作したライブラリです (1.4 節)。
確約されたロードマップとして読まないでほしい前提のうえで、リポジトリに明文化されている方向性は次のとおりです。

- **ラップ範囲の拡充**:現在の 60 超のコントロールは WinUI の一部で、需要に応じて広げていきます。追加手順が定型化されている (21.3 節) のはこのためです。
- **バインディングの自動生成**:現在は winmd から抽出した値を人が `*Interop` に転記していますが、この工程は機械化できる形になっています。`tools/dump_winmd.py` の発展形として、winmd からラッパーコードを自動生成する仕組みが、ライブラリとして育てる際の次の一歩と位置づけられています。

方向性に影響する要望や不具合は、GitHub の Issue へ (21.4 節)。
フォークして自社保守する選択肢の評価材料は、第V部が提供しています。

## 第21章 コントリビューション

### 21.1 開発環境のセットアップ

必要なのは JDK 25 (x64) と Windows 11 だけです。
Visual Studio、C++ ビルドツール、.NET SDK は使いません。

```powershell
git clone https://github.com/nttr-tech/winui4k.git
cd winui4k
.\gradlew run          # Gallery が起動すれば環境構築は完了
```

detekt 用の JDK 21 やマルチバージョンテスト用の JDK 8 / 9 / 22 は、Gradle の foojay resolver が必要時に自動取得します。
開発中によく使うコマンドは次のとおりです。

```powershell
.\gradlew run                                  # Gallery を起動 (動作確認の基本手段)
.\gradlew build                                # 全モジュールのビルド + テスト + detekt
.\gradlew :winui4k:test --tests "WButtonTest"  # 単一テストクラスの実行
.\gradlew :winui4k:testOnAllJavaVersions       # JDK 8 / 9 / 22 / 25 でテスト
.\gradlew spotlessApply                        # ktlint によるフォーマット
.\gradlew detekt                               # 静的解析
.\gradlew :winui4k-sample-gallery:runJna       # JDK 8 + JNA で Gallery (Java 8 互換の確認)
```

テストは実際に WinUI ウィンドウを起動する E2E です (第13章)。
リモートデスクトップ越しでも動きますが、ヘッドレス環境では動きません。

### 21.2 コーディング規約

規約は 2 つのツールに分担させています。

- **フォーマットと命名は Spotless + ktlint**:`spotlessApply` で自動適用されます。手で整える必要はなく、スタイル論争も発生しません。リポジトリ全体の改行コードは LF に統一されています。
- **それ以外の静的解析は detekt**:`build` に含まれ、CI でも検査されます。

detekt の指摘への対応方針には順序があります。

1. まずコードの修正を検討する
2. 意図的なコードであれば、最小スコープに理由を添えて `@Suppress` する (このプロジェクトの標準手法。「ツールを黙らせるため」という理由は不可)
3. ルール自体がコードベースの性質に合わない場合に限り、設定ファイル (`config/detekt/detekt.yml`) の変更を提案する

baseline ファイル (既存違反の一括免除) は使いません。
例外はすべて `@Suppress` としてコード上に可視化する方針です。

このほかの慣例として、`W*` クラスの KDoc 1 行目には対応する Swing クラスを記します (`WButton` なら「JButton 相当」)。

### 21.3 新しいコントロールの追加手順

コントロールの追加は定型化されており、手順は 6 ステップです。
背景となる仕組みは第16章と第17章にあります。

1. **winmd を用意する**:WinUI の型なら NuGet の Microsoft.WindowsAppSDK.WinUI から `Microsoft.UI.Xaml.winmd` を、OS 側の型なら Windows SDK の contract winmd を使います。
2. **ABI 値を抽出する**:`tools/dump_winmd.py` で対象クラス → 既定・基底インターフェース → 関連する enum / デリゲート / 構造体の順に IID とスロットをダンプします (17.1 節)。
3. **`*Interop.kt` に定数を追加する**:winmd のソースに対応するファイルへ、抽出値を転記します。IControl など共通インターフェースの定数は既存のものを再利用します。
4. **`W*` クラスを実装する**:派生元に応じて基底 (`WButtonBase` / `WControl` / `WContainer` / `WComponent` など) を選び、winmd 上の種別に応じた生成手段 (composable なら `composeDefault`、activatable なら `activate`) を使います。長期保持する QI 結果は `own(...)` で包みます (第18章)。
5. **Gallery にデモページを追加する**:ナビゲーションへの登録と、カテゴリ別ページへの実装例の追加です。デモページがそのまま利用者向けの実装例になります (2.6 節)。
6. **検証する**:コンパイル → `.\gradlew run` での目視確認 → E2E テストの追加、と進めます。

守るべき鉄則は 2 つです。
**IID / vtable スロット / enum 値は必ず winmd から機械抽出し、手書き・記憶・推測で書かない** (17.2 節)。
そして、コアモジュールでは `java.lang.foreign` を import しない (FFI バックエンド非依存の維持。3.2 節)。

### 21.4 Issue / PR の作法

不具合報告、機能要望、質問はいずれも GitHub の Issue で受け付けています。
テンプレートは用意されていないため、次を書くと調査が速く進みます。

- **不具合報告**:再現手順 (できれば最小コード)、期待した結果と実際の結果、コンソール出力 (**HRESULT を含む例外メッセージは必ず全文**)、環境情報 (OS のバージョン、Java のバージョンとアーキテクチャ、FFI バックエンド、Windows App SDK ランタイムの版)。表示系の不具合ではスクリーンショットと、ディスプレイのスケーリング設定・テーマ (ライト / ダーク) も添えてください。
- **機能要望**:実現したいことと、対応する WinUI の API 名 (分かる場合)。

プルリクエストの要件は次のとおりです。

- `.\gradlew build` が通ること (テストと detekt を含む)
- 挙動の追加・変更には E2E テストを添えること (13.2 節)
- コントロールの追加は 21.3 節の手順と鉄則に従うこと

本書を含むドキュメントの誤りの指摘や改善提案も、同じく Issue で歓迎します。
ドキュメントはリポジトリの一部 (`doc/`) なので、修正の PR を直接送ることもできます。
