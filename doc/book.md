# WinUI4K 解説書 構成案

コンポーネントカタログと API リファレンスを除く、WinUI4K の全事項を扱う書籍の章立て。
「導入 → 概念理解 → 実践 → 品質と配布 → 内部実装 → プロジェクト情報」の順に、読者の習熟フローに沿って並べる。
対象読者はシニアエンジニアとし、言語やツールの基礎解説は置かない。

## 第I部 導入

### 第1章 WinUI4K とは

採用判断に必要な材料を最初にまとめて提供する章。

- 1.1 解決する課題：JVM から Windows ネイティブ UI を使う手段の空白
- 1.2 設計思想：ブリッジ DLL 不要、Swing 風の命令的 API、OS 標準コントロールのラップ方式
- 1.3 類似技術との比較：C# + WinUI、Swing / JavaFX、Compose for Desktop、Electron
- 1.4 採用判断のポイント：動作要件 (Java 8 以降 / Windows)、ライセンス、アクセシビリティ、既知の制約 (Windows 専用、GC 連動解放、言語境界をまたぐ循環参照)

### 第2章 クイックスタート

コピペで「動いた」体験に最短で到達させる章。初期トラブルを先回りして書く。

- 2.1 環境要件と依存の追加：モジュール構成と選び方 (winui4k-all / コア + FFI バックエンド個別)
- 2.2 ウィンドウを 1 枚出す最小コード
- 2.3 ボタンとイベントハンドラを持つ最小アプリ
- 2.4 Windows App SDK ランタイムの自動セットアップ：ブートストラップ DLL、インストーラーの自動実行
- 2.5 初期トラブル：JDK アーキテクチャの不一致、ランタイム未導入、リモートデスクトップや仮想環境での注意
- 2.6 同梱サンプルアプリの歩き方：Gallery、Filer、Notes、MigLayout フォーム

## 第II部 コアコンセプト

### 第3章 アーキテクチャ概観

以降のすべての章の前提となるメンタルモデルを作る章。

- 3.1 レイヤ構成と依存方向：公開 API → winui → winrt → com → ffi.api
- 3.2 モジュール分割：コア、FFI バックエンド 3 実装、拡張 (coroutines / miglayout)
- 3.3 用語集：COM、WinRT、HSTRING、vtable、IID、RCW / CCW、アパートメント、upcall

### 第4章 アプリケーションのライフサイクル

- 4.1 起動シーケンス：MddBootstrapInitialize → RoInitialize → Application.Start → OnLaunched
- 4.2 メッセージループとイベントディスパッチ
- 4.3 終了シーケンス：RoUninitialize と MddBootstrapShutdown、省略時に abort する理由

### 第5章 スレッドモデル

- 5.1 UI スレッド 1 本の契約と COM アパートメント
- 5.2 invokeLater と Dispatcher
- 5.3 ワーカースレッドからの安全な UI 更新
- 5.4 コルーチン統合：Dispatchers.WinUi、DispatcherQueueTimer による delay

### 第6章 COM 参照のライフタイム管理

利用者視点の寿命管理ルールを扱う章 (実装の詳細は第 18 章)。

- 6.1 参照カウントとトレース型 GC のミスマッチ
- 6.2 GC 連動の自動解放：ラッパー 1 個 = 参照カウント 1 つ、ReleasePump による UI スレッドへの集約
- 6.3 自動解放の対象外：ウィンドウ・Shell 系ラッパーの扱い
- 6.4 言語境界をまたぐ循環参照とイベントリスナー解除の運用
- 6.5 チューニング：winui4k.gcThreshold、winui4k.lifetime、Java バージョン別の後始末機構

### 第7章 FFI バックエンド

- 7.1 3 実装の特徴と対応環境：Panama (Java 22+)、JNA (Java 8+ / x64)、JNR (Java 8+ / x86・x64・arm64)
- 7.2 ServiceLoader による実行時選択と優先度、winui4k.ffi による明示指定
- 7.3 Java 8 で動かすときの注意

## 第III部 実践ガイド

### 第8章 イベント処理

- 8.1 リスナーの登録と解除
- 8.2 デリゲートの正体：Kotlin 実装オブジェクトが COM オブジェクトとして登録される仕組み
- 8.3 リークを避けるリスナー管理のパターン

### 第9章 レイアウト

- 9.1 WinUI パネルによる配置の考え方
- 9.2 Swing 風レイアウトマネージャ：WLayoutManager / WLayoutPanel / WBorderLayout
- 9.3 MigLayout 拡張による配置
- 9.4 DPI スケーリングと座標系

### 第10章 非同期処理と UI 更新

- 10.1 通信や重い処理中の UI 更新パターン：プログレス表示、キャンセル
- 10.2 WinRT 非同期 API との付き合い方

### 第11章 テーマと外観

- 11.1 Fluent Design と既定スタイル：XamlControlsResources の役割
- 11.2 ダークモード対応
- 11.3 リソース解決の仕組み：resources.pri と ResourceManagerRequested

### 第12章 OS 統合と発展的な利用

- 12.1 シェル統合：AppWindow、通知、JumpList
- 12.2 WebView2 の組み込み
- 12.3 未ラップの WinUI API を呼ぶ：internal レイヤ直接利用の方法と自己責任の範囲

## 第IV部 品質と配布

### 第13章 テスト

- 13.1 テスト戦略：ロジックと UI の分離、実ウィンドウ起動を前提とした E2E
- 13.2 UiTestHarness による E2E テストの書き方
- 13.3 CI での実行：GitHub Actions、JDK 8 / 9 / 22 / 25 マトリクス、ARM 環境

### 第14章 デバッグとトラブルシューティング

- 14.1 HRESULT と IRestrictedErrorInfo による診断
- 14.2 症状別トラブル索引：起動しない、描画されない、フリーズする、プロセスが abort する
- 14.3 アンチパターン集：UI スレッド外からのアクセス、リスナーの放置、ブロッキング処理

### 第15章 パッケージングと配布

- 15.1 jpackage によるアプリ配布
- 15.2 ランタイムインストーラーの同梱
- 15.3 配布サイズと起動時間

## 第V部 内部実装

### 第16章 COM ABI を JVM から直接扱う仕組み

第 3〜5 章で概念として説明した内容の実装版にあたる章。

- 16.1 vtable 呼び出しと ComPtr
- 16.2 HSTRING と文字列変換
- 16.3 RoGetActivationFactory とオブジェクト生成
- 16.4 upcall による COM オブジェクト実装：KComObject
- 16.5 COM 集約による Application サブクラス合成
- 16.6 ジェネリック実体 IID の SHA-1 計算：pinterface

### 第17章 ABI 定数の機械抽出

- 17.1 winmd と tools/dump_winmd.py
- 17.2 推測値ゼロの原則と検証方法

### 第18章 ライフタイム管理の実装と CsWinRT との比較

第 6 章の利用者視点ルールの裏側を読む章。

- 18.1 ComLifetime と 3 状態 CAS、premature finalization と fence
- 18.2 Cleaner / PhantomReference の切り替えと MethodHandle による実行時解決
- 18.3 CsWinRT の解決機構との対応表
- 18.4 JVM で再現できないもの：IReferenceTracker の不在と割り切り

### 第19章 コードリーディングガイド

- 19.1 リポジトリの歩き方：どこを読めば何がわかるか
- 19.2 ビルド基盤：buildSrc の convention plugin、detekt の別プロセス実行、foojay resolver

## 第VI部 プロジェクト情報

### 第20章 バージョンと互換性

- 20.1 動作環境マトリクス：Java / Windows / Windows App SDK
- 20.2 バージョニングポリシーと変更履歴の読み方
- 20.3 ロードマップ：winmd からのバインディング自動生成など

### 第21章 コントリビューション

- 21.1 開発環境のセットアップ
- 21.2 コーディング規約：Spotless / ktlint / detekt の役割分担
- 21.3 新しいコントロールの追加手順
- 21.4 Issue / PR の作法：不具合報告に必要な環境情報 (OS、ディスプレイ構成、JDK)
