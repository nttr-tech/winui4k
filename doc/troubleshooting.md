# トラブルシューティング

- **`MddBootstrapInitialize2 failed` (HRESULT=0x80670016 など)**
  Windows App SDK 2.5 実験版 (2.5.4-experimental) ランタイムが未インストール、またはメジャーバージョン不一致である。
  実験版のランタイムは https://aka.ms/windowsappsdk の安定版とは別パッケージ (`Microsoft.WindowsAppRuntime.2-experimentalF`) なので、
  https://aka.ms/windowsappsdk/2.5/2.5.4-experimental/windowsappruntimeinstall-x64.exe (x86 / arm64 は末尾を置き換える) のインストーラを実行する
  (`.\gradlew :winui4k:downloadInstallers` で winui4k/installer/ にも取得できる)。
  別バージョンを使う場合は `internal/winui/Bootstrap.kt` の `WINAPPSDK_VERSION_TAG` / `WINAPPSDK_MIN_VERSION` を変更する
  (2.0 以降は major のみで解決され、minor は minVersion で指定する。安定版はバージョンタグが空文字列、
  実験版・プレビュー版はリリースごとのタグ (2.5.4-experimental は `experimentalF`) を指定する)。
- **`REGDB_E_CLASSNOTREG (0x80040154)` が RoGetActivationFactory で出る**
  ブートストラップが成功していない状態で WinUI 型を解決しようとしている。
  上と同じくランタイムの導入状況を確認する。
- **ウィンドウは出るがコントロールが表示されない**
  `XamlControlsResources` の適用に失敗している。
  コンソールの HRESULT を確認する。
- **Arm64 Windows**
  `fetchBootstrap` は `os.arch` を見て `win-arm64` の DLL を展開するが、JDK も Arm64 版である必要がある。
  未検証である。
  なお JNA バックエンドは x64 専用のため、Arm64 では Panama を使う。
- **`--enable-native-access` の警告/エラー**
  `gradlew run` 経由なら自動付与される。
  jar を直接実行する場合は `java --enable-native-access=ALL-UNNAMED ...` を付ける。
- **TabView: 表示後に TabItems へ Append しても画面にタブが増えない (Append は成功し Size も増える)**
  TabView は内部 ListView の Loaded 時に TabItems プロパティの実体を ListView.Items へ差し替える
  (microsoft-ui-xaml `TabView::OnListViewLoaded` 末尾の `TabItems(lvItems)`)。
  そのため Loaded 前に取得した IVector をキャッシュして操作すると、差し替え前の孤立した
  コレクションを更新するだけで表示に反映されない。get_TabItems は毎回取得し直すこと
  (WTabView の `withTabItemVector`)。同様に実体を差し替えるコレクションプロパティは他の
  コントロールにもありうるため、IVector のキャッシュは原則避ける。
- **Clipboard.setStorageItems に渡したパスと getStorageItems で戻るパスが一致しない**
  StorageFile 経由で戻るパスは常にロング形式に正規化される。一方 Java の
  `File.createTempFile().absolutePath` は環境によって 8.3 短縮形式 (例: `RUNNER~1`) を含む
  (GitHub Actions の Windows ランナーで発生)。パスを比較する場合は
  `toPath().toRealPath()` で短縮名を展開してから比較すること (ClipboardTest 参照)。
- **TableView: 表示やグループ化の直後に「Cannot find a Resource with the Name/Key TabularSurface...」(または `SortIndicatorForeground`) で落ちる (終了コード 0xC000027B = STATUS_STOWED_EXCEPTION)**
  TableView の既定スタイルが参照するテーマリソースは `XamlControlsResources` に含まれず、
  `TabularControlsResources` (App.xaml の `<tabular:TabularControlsResources />` 相当) を Application.Resources に
  マージする必要がある (microsoft-ui-xaml Issue #12115 と同じ問題)。WTableView は最初の生成時に
  `WinUiUtilities.ensureTabularControlsResources()` で自動的にマージする。
- **TableView: TableViewSource.From に Kotlin 実装のコレクションを渡すと Tabular DLL 内でアクセス違反になる**
  TableViewSource (と TableView) はアプリから渡されたコレクションを弱参照で追跡するため、
  コレクションが IWeakReferenceSource を実装していないと null を参照して落ちる。
  Kotlin 実装の COM オブジェクトを ItemsSource に渡すときは `KComObject.enableWeakReferences()` を呼ぶ
  (TableRowCollection / TableRowItem 参照)。
- **TableView: グループの見出しにキーの文字列と件数が表示されない (見出しの帯と展開ボタンだけになる)**
  既定の見出しテンプレートは TableViewGroupInfo の KeyText / ItemCountText に `{Binding}` しているが、
  WinUI のバインディングはソースの型情報が IsBindable でないとプロパティを解決しない (PropertyInfoPropertyAccess)。
  2.5.4-experimental の Tabular DLL の型情報 (XamlControlsTabularXamlMetaDataProvider) では TableViewGroupInfo に
  [Bindable] が付いていないため、アプリの IXamlMetadataProvider で IsBindable だけ true を返すラッパーに
  差し替えている (`internal/winui/BindableXamlTypes.kt`)。また、Tabular の型はアプリのプロバイダが
  `XamlControlsXamlMetaDataProvider` で解決できないため、`XamlControlsTabularXamlMetaDataProvider` へも問い合わせる。
- **TableView: グループの見出しが "(group)" になる**
  TableView がキーを見出しの文字列にできるのは String / Int32 / Int64 / UInt32 / Double (と IStringable) だけで、
  Boolean などは "(group)" と表示される。WTableView.groupBy はそれ以外のキーを toString() の文字列にして渡す。
