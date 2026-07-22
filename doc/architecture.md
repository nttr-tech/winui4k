# アーキテクチャ

winui4k がネイティブブリッジ DLL なしで WinUI 3 を動かす仕組みを説明する。
Java の FFI (Panama または JNA) で WinRT の COM ABI (`RoGetActivationFactory`、HSTRING、vtable 呼び出し、upcall による COM オブジェクト実装、COM 集約による `Application` サブクラス合成) を直接扱う。

## レイヤ構成

技術スタック 1 層 = 1 パッケージで、`com.appkitbox.winui4k → internal.winui → internal.winrt → internal.com → internal.ffi.api` の一方向に依存する。
公開 API はルートパッケージ `com.appkitbox.winui4k` のみで、他は `internal` 配下にある。

| レイヤ | パッケージ | 役割 |
|---|---|---|
| FFI SPI | `internal/ffi/api` | バックエンド非依存の FFI 語彙 (`Ptr` / `CallDescriptor` / `StructType` / `FfiBackend`)。バックエンドは ServiceLoader で発見する |
| FFI 実装 | `internal/ffi/panama` (winui4k-ffi-panama)、`internal/ffi/jna` (winui4k-ffi-jna) | 別モジュールの FFI バックエンド。java.lang.foreign への参照は winui4k-ffi-panama、com.sun.jna への参照は winui4k-ffi-jna だけが持つ |
| Win32 | `internal/win32/Win32.kt` | DPI 宣言、`GetModuleFileNameW` |
| COM | `internal/com/` | `ComPtr` (`ptr → vtable → vtable[slot]` の呼び出し)、`Guid`、`checkHr` (HRESULT 例外 + IRestrictedErrorInfo 診断) |
| WinRT | `internal/winrt/` | `Hstring`、`KComObject` (upcall で vtable を構築し delegate、overrides、集約 outer になる)、`Activation`、`PropertyValues` (box 化)、`Pinterface` (`IVector<T>` 実体 IID の SHA-1 計算)、`Async` |
| WinUI | `internal/winui/` | `Abi` (IID / vtable スロット。すべて winmd から機械抽出)、`Dispatcher`、`WinAppSdkBootstrap`、`XamlStructs` |
| API | ルート (`com/appkitbox/winui4k/`) | `WinUiUtilities` と `W*` クラス (`WFrame` / `WButton` / ...) |

## FFI バックエンド

FFI バックエンドは別モジュールとして提供され、実行時クラスパスに 1 つ以上追加する。

- **winui4k-ffi-panama**：Panama (`java.lang.foreign`)。Java 22 以降。優先度 100 (既定)
- **winui4k-ffi-jna**：JNA。Java 8 以降の Windows x64 で動作する (構造体の値渡しを Windows x64 ABI で手動 lowering しているため Arm64 非対応。Arm64 は Panama を使う)。優先度 50 (Java 8 での既定)
- **winui4k-ffi-jnr**：JNR (低レベル層 jffi = libffi)。Java 8 以降の Windows x64 / Arm64 / x86 で動作する (ABI lowering は libffi が行う)。優先度 40

システムプロパティ `-Dwinui4k.ffi=panama|jna|jnr` または `WinUiUtilities.setFfiBackend(...)` で明示選択できる (未指定なら利用可能なもののうち優先度最大 = Panama)。
Java 8 での動作は `.\gradlew :winui4k-sample-gallery:runJna` / `runJnr` (JDK 8 + JNA / JNR で Gallery を起動) で確認できる。

## 起動シーケンス

1. `MddBootstrapInitialize2(0x00020000, L"", 2.2.0.0, OnNoMatch_ShowUI)` で Windows App SDK 2.x ランタイムをプロセスに結び付ける
   (2.0 以降 majorMinorVersion の minor 部は無視されるため、欲しい 2.2 は minVersion = PACKAGE_VERSION 2.2.0.0 で指定する)
2. `RoInitialize(RO_INIT_SINGLETHREADED)` を呼ぶ。このスレッド (JVM の main) が UI スレッドになる
3. `Application.Start(callback)` を呼ぶ。callback は Kotlin 実装の WinRT デリゲート (upcall スタブ) で、この呼び出しはメッセージループとしてブロックする
4. callback 内で COM 集約により `Application` の「サブクラス」を合成する。
   Kotlin 実装の outer (`IApplicationOverrides` + `IXamlMetadataProvider` を実装) を `IApplicationFactory.CreateInstance(outer, &inner, &app)` に渡し、未知の `QueryInterface` は inner (XAML 側の基底実装) へ転送する
5. `Application.ResourceManagerRequested` イベントに、ランタイムパッケージの `resources.pri` を読む MRT Core `ResourceManager` を返すハンドラを登録する。
   アンパッケージ (テンプレート非使用) アプリでは exe (= java.exe) の隣に `resources.pri` が無く、XAML が既定テーマリソース (`ms-appx:///Microsoft.UI.Xaml/Themes/themeresources.xaml`) を自力で見つけられないため、この公式拡張点で解決させる
6. XAML が outer の `OnLaunched` を upcall してくる。その冒頭で `Application.Resources.MergedDictionaries` に `XamlControlsResources` を追加する
   (これが無いとコントロールの既定スタイルが無く、Button 等が描画されない。`Application.Resources` はコア初期化完了後 = OnLaunched 以降でないと触れない)。
   続けて Window / StackPanel / TextBox / Button などを各コンポーザブルファクトリの `CreateInstance(null, ...)` で生成し、`Window.Activate()` する。
   `IXamlMetadataProvider` は `XamlControlsXamlMetaDataProvider` の実体へ全転送する
7. `Button.Click` には `RoutedEventHandler` デリゲート (これも Kotlin 実装 COM オブジェクト) を `add_Click` で登録する
8. 最後のウィンドウが閉じて `Start` が戻ったら `RoUninitialize` → `MddBootstrapShutdown` を呼ぶ。
   `RoUninitialize` を省くと、JVM シャットダウン中にネイティブスレッドからの upcall (遅延 Release) がアタッチ失敗で JVM ごと abort する

## COM 参照のライフタイム管理

設計の背景と CsWinRT との対応は `doc/cswinrt_vs_winui4k.md` を参照。

W* ラッパー (WComponent 派生と WFlyoutBase / WXamlUICommand / WSwipeItems / WSwipeItem / WTreeNode / WUniformGridLayout) は、
所有する COM 参照 (既定インターフェースと QI した各ビュー) を `ComLifetime` (internal/com/lifetime) に記録する。
ラッパーが GC で到達不能になると、後始末機構が解放タスクを `ReleasePump` へ投函し、UI スレッドのメッセージループ上で `Release` が実行される
(XAML オブジェクトは UI スレッドに束縛されるため。UI スレッドへの集約により、UI スレッド上の呼び出し中にポインタが解放される premature finalization も構造的に起きない)。
明示解放とクリーナーの競合は 3 状態 (NOT_DISPOSED / DISPOSE_PENDING / DISPOSE_COMPLETED) の CAS で解決し、`Release` は一度しか実行されない。
ビジュアルツリーがコントロール本体の参照を保持している限り、ラッパーを回収しても本体は生き続ける (COM の参照カウント 1 つぶんを返すだけ)。

後始末機構は Java バージョンごとに使える下回りが異なるため、FFI バックエンドと同様に実行時に切り替える (`Cleanup` が選択):

| 実行環境 | 後始末 | fence | FFI |
|---|---|---|---|
| Java 8 | `PhantomReference` + `ReferenceQueue` + 自前デーモンスレッド (phantom) | `synchronized` イディオム | JNA / JNR |
| Java 9〜21 | `java.lang.ref.Cleaner` (cleaner、MethodHandle で実行時解決) | `Reference.reachabilityFence` | JNA / JNR |
| Java 22 以上 | cleaner (同上) | `Reference.reachabilityFence` | Panama (FFM 正式化) |

システムプロパティ `-Dwinui4k.lifetime=cleaner|phantom` で明示選択できる。
クリーナースレッド (`WinUI4K-Cleaner`) は起動時に `RoInitialize(MTA)` で COM に参加する。
シャットダウン時は `RoUninitialize` の前に `ReleasePump.shutdown()` で以後の解放を止め、未解放分はプロセス終了に任せる (`RoUninitialize` 後の `Release` はクラッシュを招くため。CsWinRT と同じ割り切り)。

`-Dwinui4k.gcThreshold=<参照数>` (オプトイン) で、生存中のネイティブ参照数が閾値を超えるたびに `System.gc()` を要請する
(.NET の `GC.AddMemoryPressure` の近似。`-XX:+ExplicitGCInvokesConcurrent` の併用を推奨)。

## ABI 定数の出所 (tools/dump_winmd.py)

`winui/Abi.kt` の IID とスロット番号は手書きではない。
Microsoft.WindowsAppSDK.WinUI 2.2.1 の `metadata/Microsoft.UI.Xaml.winmd`、WinAppSDK ランタイムの `Microsoft.Windows.ApplicationModel.Resources.winmd`、Windows SDK の `Windows.Foundation.FoundationContract.winmd` から、同梱の `tools/dump_winmd.py` で抽出した値である。
再現するには:

```bash
pip install dnfile
python tools/dump_winmd.py Microsoft.UI.Xaml.winmd \
    Microsoft.UI.Xaml.IWindow Microsoft.UI.Xaml.Controls.ITextBox
```

`IVector<Microsoft.UI.Xaml.UIElement>` のようなジェネリック実体の IID は winmd には無く、WinRT 仕様の署名文字列から SHA-1 名前ベース UUID として計算する (`WinRt.pinterfaceIid`)。
実装は既知値 `IIterable<String> = e2fcc7c1-3bfc-5a0b-b2b0-72e769d1cb7e` との一致で検証済みである。

## 既知の割り切り

共有インフラ (vtable、upcall スタブ、キャッシュ済み HSTRING、ファクトリ statics) はプロセス生存期間ぶん保持し、UI スレッドは 1 本の前提である。
W* ラッパーの COM 参照は GC 連動で自動解放されるが (上記「COM 参照のライフタイム管理」)、
ウィンドウ・Shell 系のラッパー (WFrame / WAppWindow / WAppNotification / WJumpList など) は対象外で従来どおり保持し続ける。
言語境界をまたぐ循環参照 (ネイティブ → CCW → Kotlin → RCW → ネイティブ) は自動回収されない (JVM の GC に IReferenceTracker 相当の拡張点がないため)。
エラー処理は HRESULT の例外化のみである。
レイアウトマネージャは Swing 風の `WLayoutManager` / `WLayoutPanel` (Canvas を土台に Kotlin 側で計測・配置) を備え、実装例として `WBorderLayout` がある。
ライブラリとして育てる際は、winmd からのバインディング自動生成 (`tools/dump_winmd.py` の発展形) が次の一歩になる。
