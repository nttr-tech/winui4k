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
  マージする必要がある (microsoft-ui-xaml Issue #12115 と同じ問題)。WTable は最初の生成時に
  `WinUiUtilities.ensureTabularControlsResources()` で自動的にマージする。
- **Chart: 系列の追加 (WChart.addSeries) が E_INVALIDARG「An axis cannot be shared across different data dimensions or physical layouts.」で失敗する**
  軸を共有する系列どうしの向きがそろっていない。BarSeries の既定の向きは横 (Horizontal) なので、
  縦向きの LineSeries / AreaSeries と軸を共有するときは `orientation = BarOrientation.VERTICAL` にする (WChartTest 参照)。
- **Chart: DateTimeAxis のラベルが 1 区切りぶん (1 週間・1 日など) ずれて、データ点の位置と合わない**
  Chart は DateTime を UTC で扱う。範囲 (Minimum / Maximum) やデータをローカル時刻の 0 時の絶対時刻
  (Instant / ZonedDateTime など) で渡すと UTC では前日になり、目盛りの区切りがずれる。日付を表示したいだけなら
  LocalDate / LocalDateTime で渡す (WinUI4K は UTC の日時として渡すので、指定した日付がそのまま表示される)。
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
  Boolean などは "(group)" と表示される。WTable.groupBy はそれ以外のキーを toString() の文字列にして渡す。
- **ウィンドウの要素の子ではない Popup を IsLightDismissEnabled で開くと、ウィンドウがクリックに一切反応しなくなる**
  `WPopup.show(owner)` の Popup は XamlRoot だけを設定した親のない Popup で、IsLightDismissEnabled=true にすると
  外側のクリックで閉じないまま、ウィンドウへのポインタ入力をすべて奪う (タブもボタンも反応しない)。
  リボンの検索結果は起動時の自動フォーカスで開いてしまい、ウィンドウ全体が操作不能に見えた。
  外側のクリックで閉じたいポップアップはライト ディスミスを使わず、先にウィンドウ全体を覆う透明な Popup
  (`RibbonDismissLayer`) を開き、その層が押されたら閉じる。
- **閉じたフライアウトの中身を別の場所へ移すと E_INVALIDARG (0x80070057) で落ちる**
  外側のクリックで閉じたフライアウトの中身は、VisualTreeHelper.GetParent では親が取れなくなっても、
  論理上の親 (FrameworkElement.Parent。例: 中身の StackPanel の Children) には残っている。そのまま別の親に入れると
  「既に別の要素の子」として失敗するため、`Xaml.detach` はビジュアル ツリーの親が無ければ論理上の親から外す。
- **前のフライアウトが閉じきる前に開いたフライアウトは表示されず、その中身は `Xaml.detach` で外せない**
  Hide の後の Closed は非同期に届き、その前に別のフライアウトの ShowAt を呼んでも表示されない (例外も出ない)。
  表示されなかった中身はライブ ツリーに載らず、VisualTreeHelper.GetParent も FrameworkElement.Parent も null を返すため
  `Xaml.detach` は何もしないが、Panel の子としては残っているので別の親に入れると E_INVALIDARG (0x80070057) になる。
  折りたたんだグループのポップアップは中身を入れた親を覚えておき、戻すときはそこから外す (`RibbonGroupView.restoreFromPopup`)。
  CI (GitHub Actions) ではポップアップを 1 回開いて閉じるだけのテストでも同じ例外になった (表示されなかった理由は未確認)。
- **ウィンドウの下端の要素から BottomEdgeAlignedLeft で開いたメニューが表示されない**
  画面の下端近くにあるウィンドウでは、下向きに開いたメニューがモニターの外に置かれて見えないことがある
  (例外も出ない)。ステータス バーのドロップダウンは TopEdgeAlignedLeft で上に開く (`WRibbonBar.dropDownPlacement`)。
- **既定のフォントの SemiBold で「Ω」(U+03A9) が「и」のような字形になる**
  日本語環境の既定の UI フォントでは、FontWeight=SemiBold (600) の TextBlock の Ω がキリル文字のような別の字形で
  描かれる (Normal では正しく Ω になる)。リボンの文字のアイコン (`RibbonIcon.text`) は RibbonSpace と同じく SemiBold で
  描くため、Ω のような記号はパスのアイコンにする (Word のサンプルの [記号と特殊文字])。
- **ウィンドウを開いたときの自動のフォーカスでも FocusState が Pointer になることがある**
  PowerPoint のサンプル (シンプル リボンで始める) では、起動直後に検索ボックスが受け取る自動のフォーカスの
  FocusState が Pointer (1) で、「クリックで入ったときだけ結果を出す」判定が誤って結果を開いていた。
  クリックかどうかは FocusState ではなく、ボックスの PointerPressed (handledEventsToo) を受けたかで見分ける
  (`WRibbonSearchBox`)。
- **InkCanvas: マウスやタッチで線が描けない**
  InkCanvas の既定の入力機器はペンだけである。`canvas.inkPresenter.inputDeviceTypes = setOf(CoreInputDeviceType.PEN, CoreInputDeviceType.MOUSE, CoreInputDeviceType.TOUCH)` のように設定する。
- **InkCanvas: `activateCustomDrying` が E_ILLEGAL_METHOD_CALL (0x8000000E) で失敗する**
  線を表示・操作した後 (モデルに線がある状態でキャンバスを作った、選択や保存を呼んだなど) は WinUI が拒否する。
  モデルが空のうちに、キャンバスをウィンドウに載せる前に呼ぶ。
- **InkPointerEvent の `pointerPoint` が null になる**
  Windows App SDK 2.5 実験版はインクの入力のイベントを UI スレッドへ遅れて届けるため、引数が読めないことがある (WinUI の制約)。
  線の内容は StrokesCollected (`addStrokesCollectedListener`) とモデルから得る。
- **InkToolbar: カスタム ペンの描画属性が使われない / `getToolButton` が null を返す**
  ボタンは読み込み後に作られるので `addLoadedListener` の中で取得する。カスタムのペン・ツール・トグルは
  WinUI の仕様で `getToolButton` / `getToggleButton` では得られないので、追加したラッパーをそのまま使う (`getItems`)。
  カスタム ペンは `WInkToolbar` に追加した `WInkToolbarCustomPenButton` の `customPen` に設定すると反映される
  (2.5.4-experimental の InkToolbar の不具合を WInkToolbar が補っている)。
- **ウィンドウを開くと検索ボックスにフォーカスが入り、F10 / Alt の KeyTip が出ず、打ったキーが検索に入る**
  ウィンドウがアクティブになると WinUI はタブ順で最初のフォーカス可能な要素にフォーカスを与える。
  `WRibbonTitleBar` ではそれが検索ボックスだった。クリック・Tab・`focusSearch` のいずれでもない最初のフォーカスは、
  選択中のリボン タブのヘッダーへ渡す (`WRibbonSearchBox`、f3f7f7d)。
  GotFocus のハンドラの中でフォーカスを移しても効かないため、`WinUiUtilities.invokeLater` で投函して移す。
  なお E2E の共有テスト ウィンドウはアクティブにならないので、WinUI 自身の初期フォーカスはテストでは再現できない
  (テストはその割り当てと同じことをフォーカス呼び出しで行う)。
- **Chart: LineSeries の X 軸に LinearAxis を設定すると E_INVALIDARG (0x80070057) "The axis scale is incompatible with the series data slot"**
  折れ線の X のデータ スロットは LinearAxis を受け付けない (`series.xAxis = LinearAxis()` の時点で失敗する)。
  X 軸には CategoryAxis か DateTimeAxis を使う (WChartTest で確認)。
- **Chart: DateTimeAxis で数秒おきの点が 1 本の縦線に潰れる**
  `DateTimeIntervalType` は Day / Week / Month / Year しかなく (Auto を除く)、目盛の最小単位が日である。
  そのため秒単位の時系列は WChart では表示できない (縦線に潰れるのは実使用での報告で、描画は自動テストしていない)。
  秒単位の時系列は `WWebView` で Web のチャートを表示する。
- **`Windows.UI.ViewManagement.UISettings` (WSystemColors) などの OS の WinRT 型が CO_E_NOTINITIALIZED (0x800401F0) で失敗する**
  これらの型は呼び出したスレッドで COM が初期化されている必要がある。UI スレッド (RoInitialize 済み) では動くが、
  素の JVM スレッドから呼ぶと 0x800401F0 になる (実使用での報告)。
  `WSystemColors.accentColor` / `accentPalette()` は UI スレッドから呼ぶ。
  ただし E2E (テスト用の JVM スレッド) では素のスレッドからでも成功した。プロセス内のどこかのスレッドが
  MTA を初期化していると (ReleasePump の cleaner スレッドが `RoInitialize(MTA)` する)、未初期化のスレッドは暗黙の MTA に入るためと考えられるが、
  失敗する条件は再現できておらず未確認である。
- **WWebView: ページの `chrome.webview.postMessage` で送った値が引用符付き (JSON) で届く**
  `addWebMessageReceivedListener` のリスナーには、ページが何を送っても常に WebMessageAsJson (メッセージの JSON テキスト) が渡る。
  `postMessage({a: 1})` は `{"a":1}`、`postMessage("hi")` は JSON の文字列リテラル `"hi"` (二重引用符付き) で届き、
  引用符は外されない (TryGetWebMessageAsString は使っていない)。
  文字列を受けたいときは受け側でパースして引用符を外す (WWebViewTest の `"echo:Hello"` 参照)。
- **WLayoutPanel の中に入れた WLayoutPanel の幅が 0 になる (内側の preferredSize は正しいのに actualWidth が 0)**
  WLayoutPanel は子に Width / Height を直接設定するため、子の DesiredSize はその値に縛られる (計測前は 0)。
  外側のパネルのレイアウトが内側のパネルのレイアウトより先に走ると、内側の preferredSize は子をまだ計測し直していない
  DesiredSize (前回割り当てた幅、初回は 0) から計算され、外側はその幅を内側に割り当てる。内側が後で子を計測しても
  外側へは伝わらないため、内側は 0 (ラベルの文字が伸びた後も前の幅) のまま残っていた。
  また Canvas の DesiredSize は常に 0x0 で、入れ子の WLayoutPanel を計測し直すために Width を一旦外す処理が加わると、
  Width を戻した後も DesiredSize / ActualWidth が 0 のまま残ることがあった (MigLayout の入れ子で確認。WinUI 側の理由は未確認)。
  `WLayoutPanel.preferredSize` は未計測の子をその場で計測してから集計し、レイアウト マネージャーを持つ子の
  WLayoutPanel は計測しない (WLayoutPanelTest / MigLayoutManagerTest)。
- **WTable のセルなどで `WRibbonTheme.setThemeBrush` を使うと、スクロールするほどパレット変更が重くなり、捨てたセルが解放されない**
  以前の setThemeBrush はパレット変更のリスナーをアプリ全体のリストに登録したまま外さなかった。仮想化された表の
  レンダラーは表示し直すたびに新しいコンポーネントを作るため、セル 1 個ごとにリスナーが 1 つずつ残り、
  表示から外れたセルもリストから参照され続けていた。現在は対象がビジュアル ツリーにある間 (Loaded から Unloaded まで)
  だけ登録し、再表示のときは外れていた間のパレット変更も反映する (RibbonResourcesTest)。
  別の親へ移すと新しい場所の Loaded が古い場所の Unloaded より先に届くことがある (WinUI の既知の動作。
  テストの移動では順序までは確認していない) ため、Unloaded では
  FrameworkElement.IsLoaded を見て、まだツリーにあれば登録を外さない。同じようにアプリ全体のイベントに
  コンポーネントごとのリスナーを登録する処理は、仮想化されたセルから呼ばれても溜まらないようにする。
- **初めて開いたリボン タブの最初の大きいボタンの KeyTip が、ボタンの下ではなくリボンの途中に出る**
  KeyTip の吹き出しは `ActualOffset` の合計でボタンの位置を求めるが、KeyTip で初めて開いたタブは項目が実体化された
  直後でレイアウトが終わっておらず、最初の項目の `ActualOffset` / `ActualWidth` が古い値のまま (ずれは 900 DIP 超になった)
  だった。2 回目以降はレイアウト済みなので正しく出る。`RibbonKeyTipController.render` が位置を読む前に
  `keyTipRoot.updateLayout()` で同期的にレイアウトを終わらせる (WRibbonTest)。
- **Backstage のナビゲーションで ↓ / ↑ キーを押すとアプリが落ちる (FocusManager.TryMoveFocus が E_UNEXPECTED (0x8000FFFF))**
  デスクトップ アプリの WinUI では、オプションなしの `TryMoveFocus(direction)` は「FindNextElementOptions の SearchRoot に
  読み込み済みの DependencyObject を渡すこと」というエラーで必ず失敗する。さらにオプション付きの TryMoveFocus は
  Next / Previous を受け付けず E_INVALIDARG (0x80070057) になるため、方向 (Up / Down / Left / Right) で移動する。
  親のない Popup の中身は XamlRoot.Content の下にないので、SearchRoot には候補を含む要素 (Popup の子など) を渡す
  (`Xaml.moveFocusVertically`、WRibbonTest)。
  キーのイベント ハンドラーが失敗の HRESULT を返すと XAML は未処理のエラーとしてプロセスごと終了させるため、
  `XamlElement` のキーのハンドラーは例外を stderr に出すだけで XAML へは返さない。
- **`WLabel.foreground = null` で文字が消える (既定の色に戻らない)**
  Brush 型のプロパティに null を put すると「ローカル値 = null のブラシ」になり、親から継承する TextBlock.Foreground や
  スタイルが設定する Control.Foreground / Background が効かなくなる (文字は描かれない)。既定 (テーマ) の値に戻すには
  DependencyObject.ClearValue で `XxxProperty` のローカル値を消す (`WComponent.clearValue`、WLabelTest / WChartTest)。
  Border.Background・SwipeItem.Background・チャートの軸のブラシなど既定値が null でスタイルも設定しないプロパティは、
  null の put でも既定に戻る。
- **WContentDialog や ScrollViewer に入れた WLayoutPanel が 2 行ほどに切れる / スクロールしない**
  WLayoutPanel の土台の Canvas は常に DesiredSize 0x0 を報告するため、内容の DesiredSize で大きさを決めるネイティブの親
  (ContentDialog・ScrollViewer・縦の StackPanel など) は最小の大きさしか与えず、ActualHeight が 0 でないので自分の
  preferredSize を使う自己サイズ化も働かなかった。現在は LayoutInformation.GetAvailableSize (親が measure で提示した大きさ) と
  GetLayoutSlot (実際に arrange された枠) で親の振る舞いを判定し、提示が無制限 (ScrollViewer の縦方向など) なら
  preferredSize を Width / Height に設定し、提示が枠より大きい (ContentDialog の幅・高さなど) なら preferredSize を
  提示の大きさで頭打ちにして MinWidth / MinHeight に設定する (FrameworkElement は DesiredSize を Min* まで引き上げる)。
  ウィンドウのように提示どおりの枠に引き伸ばす親では何も設定しないので、ウィンドウを縮めればレイアウトも縮む (WLayoutPanelTest)。
  なお ContentDialog のテンプレートの ScrollViewer は縦スクロールが無効 (VerticalScrollBarVisibility=Disabled) で、
  ネイティブのコンテンツでもダイアログ自体はスクロールしない。ウィンドウより高くなりうるフォームは WScrollPane に入れて
  `content` に設定する。
- **WContentDialog の幅が 548 DIP より広がらない (`maxWidth` を設定しても変わらない)**
  ContentDialog のテンプレートはダイアログ本体 (BackgroundElement) の MaxWidth にテーマ リソース `ContentDialogMaxWidth` (548) を使うため、
  ContentDialog 自身の FrameworkElement.MaxWidth (`WComponent.maxWidth`) では広がらない。`WContentDialog.dialogMaxWidth` は
  ダイアログ自身の Resources に `ContentDialogMaxWidth` を入れる。テンプレートは最初の表示のときにこのリソースを読むので、
  `show` の前に設定する (WContentDialogTest)。
- **MigLayout で折り返す WLabel (TextWrapping.WRAP) が 1 行分の高さになり、2 行目以降が切れる**
  MigLayout のアダプターが幅の指定 (`getPreferredHeight(wHint)`) を無視し、無限の幅で測った 1 行の高さを返していた。
  現在はコンテンツ バイアスを横 (`getContentBias() = HORIZONTAL`) にして、MigLayout が決めた幅で
  `WComponent.preferredHeight(forWidth)` を測る (希望の幅より狭いときだけ UIElement.Measure を呼び、結果はキャッシュする)。
  Canvas の子は WLayoutPanel が設定した Width / Height に DesiredSize が縛られるため、測る間だけ外して戻す。
  折り返した高さは配置で幅が決まった後に分かるので、希望の大きさが変わったらパネルをもう一度レイアウトする。
  折り返す WLabel の最小幅は 0 (列とともに狭まって折り返す) だが、WBorder に入れた場合などは `"w 300!"` や
  `"growx, wmin 0"` で幅を制約する。なお TextBlock の ActualHeight は Height で切られていても文字全体の高さを返すため、
  切れているかどうかはパネルや親の高さで確かめる (MigLayoutManagerTest)。
- **WTextField に表示した Windows のパスの `\` が `¥` になる**
  日本語環境の既定の UI フォント (Yu Gothic UI など) は U+005C (バックスラッシュ) を円記号の字形で描く。
  パスを表示する欄は `WTextField.fontFamily` (`WLabel` / `WPasswordField` も同様) に `"Consolas"` や `"Segoe UI"` など
  バックスラッシュの字形を持つフォントを指定する。
- **ファイル / フォルダーのピッカーを任意のフォルダーで開きたい (SuggestedStartLocation は既知のフォルダーしか選べない)**
  `PickerLocationId` はドキュメントやダウンロードなどの既知のフォルダーだけである。任意のフォルダーは
  WinAppSDK 2.x の IFolderPicker2 / IFileOpenPicker2 / IFileSavePicker2 のパス指定を使う:
  `suggestedFolder` (WFolderPicker / WFileOpenPicker / WFileSavePicker) は毎回そのフォルダーで開き、
  `suggestedStartFolder` (3 つとも) は Windows がそのピッカーの前回のフォルダーを覚えていないときだけ使われる
  (以後は前回選んだフォルダーが優先)。どちらも絶対パスで指定し、`suggestedStartLocation` より優先される。
  優先順位は WinAppSDK のドキュメントに基づき、ダイアログを開く動作は自動テストしていない (値の設定と読み戻しのみ WStoragePickersTest で確認)。
- **`addKeyboardAccelerator` を付けたテキスト ボックスや表に、ホバーやフォーカスで「Enter」などのツールチップが出る**
  WinUI は既定 (KeyboardAcceleratorPlacementMode=Auto) で、要素の KeyboardAccelerators のキーをツールチップとして表示する。
  Enter で確定する欄のように、ショートカットを独立したコマンドとして見せる必要がない要素では
  `keyboardAcceleratorPlacementMode = KeyboardAcceleratorPlacementMode.HIDDEN` にする (WComponentTest)。
