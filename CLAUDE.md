# プロジェクト概要

winui4k は、ブリッジ DLL・C#・Visual Studio なしで WinUI 3 アプリを書ける Kotlin ライブラリ。
Java の FFI (Panama / JNA / JNR) で WinRT の COM ABI (`RoGetActivationFactory`、HSTRING、vtable 呼び出し、upcall、COM 集約) を直接扱う。
開発には JDK 25 (x64) と Windows 11 が必要 (ビルドは JDK 25、detekt 実行と一部テストは foojay resolver が JDK 21/8/9/22 を自動取得する)。

# よく使うコマンド

```powershell
.\gradlew run                                  # Gallery アプリを起動 (動作確認の基本手段)
.\gradlew build                                # 全モジュールのビルド + check (テスト + detekt)
.\gradlew :winui4k:test                        # コアの E2E テスト (実際に WinUI を起動する。Windows + WinAppSDK ランタイム必須)
.\gradlew :winui4k:test --tests "*WButtonTest" # 単一テストクラスの実行
.\gradlew :winui4k:testOnAllJavaVersions       # JDK 8 / 9 / 22 / 25 でテスト実行
.\gradlew spotlessApply                        # ktlint によるフォーマット
.\gradlew detekt                               # 静的解析 (check に含まれる)
.\gradlew detektFile -PdetektFile=<path>       # 1 ファイルだけ高速に detekt 検査 (hooks 用)
.\gradlew :winui4k-sample-gallery:runJna       # JDK 8 + JNA で Gallery 起動 (Java 8 互換の確認)
```

- フォーマットは Spotless + ktlint、静的解析は detekt という役割分担。ルールの詳細と detekt 指摘への対応方針は `.claude/skills` の kotlin-lint-rules スキルを参照。
- detekt 1.23 は JDK 25 で動かないため、Gradle プラグインではなく JDK 21 別プロセスの CLI 実行になっている (buildSrc の `winui4k.kotlin-common`)。
- リポジトリ全体を LF で統一している (Spotless が強制)。

# アーキテクチャ

詳細は `doc/architecture.md` (必読)。要点:

- **1 技術スタック = 1 パッケージのレイヤ構成**。依存は一方向:
  `com.appkitbox.winui4k` (公開 API、`W*` クラス) → `internal.winui` (ABI 定数の `*Interop`、Dispatcher、Bootstrap) → `internal.winrt` (HSTRING、KComObject、Activation) → `internal.com` (ComPtr、Guid、checkHr) → `internal.ffi.api` (バックエンド非依存の FFI SPI)
- **FFI バックエンドは別モジュール** (`winui4k-ffi-panama` / `-jna` / `-jnr`) で ServiceLoader により実行時選択。コア (`winui4k`) は Java 8 ターゲットで JDK 依存を持たない。`java.lang.foreign` への参照は panama モジュールだけ、`com.sun.jna` は jna モジュールだけが持つ。
- **IID / vtable スロットは手書き禁止**。すべて `tools/dump_winmd.py` で winmd から機械抽出した値を `internal/winui/*Interop.kt` に置く。新しいコントロールの追加手順は add-winui-component スキルを参照。
- **COM 参照のライフタイム**: W* ラッパーは GC 到達不能になると `ReleasePump` 経由で UI スレッド上で `Release` される (`doc/memory_management.md`)。UI スレッドは 1 本前提。
- 共通ビルド設定は `buildSrc` の convention plugin (`winui4k.kotlin-common` / `-library` / `-application`)、バージョンは `gradle/libs.versions.toml` で一元管理。

# 注意事項

- テストは実際に WinUI ウィンドウを起動する E2E (`winui4k/src/test`、Kotest + `UiTestHarness`)。ヘッドレス環境では動かない。
- WinUI 実体との相互作用で判明した落とし穴 (`Application.Resources` に触れるタイミング、`RoUninitialize` 省略時の abort 等) は `doc/verification.md` と `doc/troubleshooting.md` に記録されている。新たに判明したものも同様に記録すること。
