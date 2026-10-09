# プロジェクト概要

- WinUI4KはKotlin/JavaからWinUIを使えるようにするライブラリ
  - JavaのFFI(Panama/JNA/JNR)でWinRTを呼び出し
  - 開発にはJDK 25(x64)とWindows 11が必要
  - リンター実行と一部テストはfoojay resolverでJDK 21/8/9/22を自動取得

# よく使うコマンド

```powershell
.\gradlew run                                  # Gallery アプリを起動 (動作確認の基本手段)
.\gradlew build                                # 全モジュールのビルド + check (テスト + detekt)
.\gradlew :winui4k:test                        # コアの E2E テスト (実際に WinUI を起動する。Windows + WinAppSDK ランタイム必須)
.\gradlew :winui4k-extension-ribbon:test       # リボンの E2E テスト
.\gradlew :winui4k:test --tests "WButtonTest"  # 単一テストクラスの実行 (Kotest はワイルドカード不可。単純クラス名か完全修飾名で指定)
.\gradlew testOnAllJavaVersions                # 全モジュールの E2E テストを JDK 8 / 9 / 22 / 25 で実行
.\gradlew spotlessApply                        # ktlint によるフォーマット
.\gradlew detekt                               # 静的解析 (check に含まれる)
.\gradlew detektFile -PdetektFile=<path>       # 1 ファイルだけ高速に detekt 検査 (hooks 用)
.\gradlew :winui4k-sample-gallery:runJna       # JDK 8 + JNA で Gallery 起動 (Java 8 互換の確認)
```

- フォーマットは Spotless + ktlint、静的解析は detekt という役割分担。ルールの詳細と detekt 指摘への対応方針は `.claude/skills` の kotlin-lint-rules スキルを参照。
- detekt 1.23 は JDK 25 で動かないため、Gradle プラグインではなく JDK 21 別プロセスの CLI 実行になっている (buildSrc の `winui4k.kotlin-common`)。
- リポジトリ全体を LF で統一(Spotless が強制)。

# アーキテクチャ

詳細は`doc/architecture.md`を参照

- **1 技術スタック = 1 パッケージのレイヤ構成**。依存は一方向:
  `com.appkitbox.winui4k` (公開 API、`W*` クラス) → `internal.winui` (ABI 定数の `*Interop`、Dispatcher、Bootstrap) → `internal.winrt` (HSTRING、KComObject、Activation) → `internal.com` (ComPtr、Guid、checkHr) → `internal.ffi.api` (バックエンド非依存の FFI SPI)
- **FFI バックエンドは別モジュール** (`winui4k-ffi-panama` / `-jna` / `-jnr`) で ServiceLoader により実行時選択。コア (`winui4k`) は Java 8 ターゲットで JDK 依存を持たない。`java.lang.foreign` への参照は panama モジュールだけ、`com.sun.jna` は jna モジュールだけが持つ。
- **リボンは拡張モジュール** (`winui4k-extension-ribbon`、パッケージ `extension.ribbon` / `extension.ribbon.model`)。コアの `internal` API (`XamlInterop` など) を使うため、コアを `-Xfriend-paths` で friend モジュールとしてコンパイルしている (`winui4k-extension-ribbon/build.gradle.kts`)。
- **IID / vtable スロットは手書き禁止**。すべて `tools/dump_winmd.py` で winmd から機械抽出した値を `internal/winui/*Interop.kt` に置く。新しいコントロールの追加手順は add-winui-component スキルを参照。
- **COM 参照のライフタイム**: W* ラッパーは GC 到達不能になると `ReleasePump` 経由で UI スレッド上で `Release` される (`doc/memory_management.md`)。UI スレッドは 1 本前提。
- 共通ビルド設定は `buildSrc` の convention plugin (`winui4k.kotlin-common` / `-library` / `-application`)、バージョンは `gradle/libs.versions.toml` で一元管理。

# 注意事項

- テストは実際に WinUI ウィンドウを起動する E2E (`winui4k/src/test` と `winui4k-extension-ribbon/src/test`、Kotest + `UiTestHarness`)。`UiTestHarness` は winui4k の testFixtures (`winui4k/src/testFixtures`) にあり、E2E の設定は buildSrc の `winui4k.ui-test` で共有する。ヘッドレス環境では動かない。
- WinUI 実体との相互作用で判明した落とし穴 (`Application.Resources` に触れるタイミング、`RoUninitialize` 省略時の abort 等) は `doc/verification.md` と `doc/troubleshooting.md` に記録されている。新たに判明したものも同様に記録すること。

# 方針

- レビュー時に把握しやすいように小さめの粒度でコミットを作成
