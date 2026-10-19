# 検証状況

Windows 11 x64 + Windows App SDK 2.5.4-experimental (実験版) ランタイムで動作検証済みである
(起動 → ウィンドウ表示 → ボタンクリック → クローズ → 正常終了 exit code 0)。

実行以外の検証:

- Kotlin 2.4.0 + JDK 25 + Gradle 9.6.1 でのコンパイル成功
- 全 IID / vtable スロットの winmd からの機械抽出 (推測値ゼロ)
- pinterface IID 計算の既知値照合
- GUID ネイティブレイアウトの往復テスト
- COM 機構のループバックテスト: `KComObject` が構築した vtable を `ComPtr` 経由で呼び戻し、QueryInterface の同一性、E_NOINTERFACE、メソッドディスパッチ、引数マーシャリング (int / out ポインタ)、参照カウント、IInspectable プロローグがすべて期待通りに動作
- JNA バックエンドは `.\gradlew :winui4k-sample-gallery:runJna` で JDK 8 実機起動を確認

## WinUI 3 実体との相互作用で判明した注意点

いずれも対応済みである。

- `Application.Resources` は init callback 中は触れない (E_UNEXPECTED)。
  `OnLaunched` 以降で `MergedDictionaries.Append` する
- テンプレート非使用のアンパッケージアプリは `ResourceManagerRequested` でカスタム `ResourceManager` を渡さないと、`XamlControlsResources` の生成が「Cannot locate resource from 'ms-appx:///Microsoft.UI.Xaml/Themes/themeresources.xaml'」で失敗する
- `ResourceManagerRequested` のハンドラ型は `TypedEventHandler<Object, ResourceManagerRequestedEventArgs>` である
  (第 1 型引数は Application ではなく Object。IID 計算では `cinterface(IInspectable)`)
- 終了時は `RoUninitialize` を呼ぶ (呼ばないと JVM シャットダウンと COM の遅延解放が競合して abort する)
- TableView (2.5.4-experimental) の既定スタイルが参照するテーマリソースは `TabularControlsResources` を
  Application.Resources にマージしないと解決できない (グループ見出しや並べ替えの矢印の描画で XAML 例外になりプロセスが終了する)
- アプリの IXamlMetadataProvider は Tabular の型を `XamlControlsXamlMetaDataProvider` では解決できないため、
  `XamlControlsTabularXamlMetaDataProvider` にも問い合わせる。さらに TableViewGroupInfo の型情報には IsBindable が無く、
  既定のグループ見出しテンプレートの `{Binding KeyText}` が空になるため、IsBindable だけ補ったラッパーを返す
- TableViewSource.From に渡すコレクションと行アイテムは IWeakReferenceSource を実装している必要がある
  (弱参照で追跡されるため。実装しないと Tabular DLL 内でアクセス違反)
- Chart (2.5.4-experimental。実装は Microsoft.UI.Xaml.Controls.Charts.dll) の型も `XamlControlsXamlMetaDataProvider` では
  解決できないため、`XamlControlsChartsXamlMetaDataProvider` にも問い合わせる。既定スタイルとテーマリソースは
  公式サンプル (microsoft-ui-xaml の Samples/ChartApp の App.xaml) と同じく `XamlChartsResources` をマージする
- Charts.Samples.ItemsSource には Kotlin 実装の `IObservableVector<Object>` (要素は PropertyValue で box した
  Double / String / DateTime) を渡せ、VectorChanged を発火すると表示中のチャートが描き直される
  (Charts DLL は Double / String / DateTime / Object の IVector・IObservableVector と IBindable* の IID を持つ)。
  系列の XValues / YValues に設定した Samples は Chart.Data に入れなくても描画される (Data は XAML で Samples を宣言するための入れ物)
- Chart の軸の整合性は Chart.Series への追加時に検証される。向きの違う系列 (既定の横向きの BarSeries と LineSeries など) で
  同じ軸を共有すると E_INVALIDARG「An axis cannot be shared across different data dimensions or physical layouts.」になる
- Chart は DateTime を UTC で扱う (DateTimeAxis の目盛りの区切りとラベルの日付が UTC 基準)。日本時間の 0 時
  (= 前日 15 時 UTC) で範囲やデータを渡すと、ラベルが 1 区切りぶん (週単位なら 1 週間、日単位なら 1 日) 前にずれる。
  WinUI4K は LocalDate / LocalDateTime をタイムゾーンを付けずに UTC の日時として渡す
- X 値 (XValues) を省略した系列は、要素の番号 1, 2, 3, ... (1 始まり) が X になる
- XAML のバインディングは、ソースが `IMap<String, Object>` を実装していればパス名をキーとして Lookup / Insert する
  (MapPropertyAccess)。キーが存在するか (HasKey) で接続の可否を決め、`IObservableMap.MapChanged` は変化したキーと
  一致するものだけを反映する (Reset は無視される)。WTable の行アイテムはこれを使い、キー "c<列>" でモデルの値を返す
- `UIElement.AddHandler(XxxEvent, handler, handledEventsToo)` の handler (Object) には、デリゲートをそのまま渡さず
  `IReference<デリゲート>` に包んで渡す (C++/WinRT の `box_value` と同じ。IID は `pinterface({IReference};delegate({デリゲートの IID}))`)。
  get_Value でデリゲートを返すだけの実装で、子が処理済みにしたポインタ イベント (ボタンの上のクリック) も届く
  (`XamlElement.onPointerHandledToo`。リボンのクリックによる KeyTip の解除に使う)
- Ink (InkCanvas / InkToolbar、2.5.4-experimental) は次の挙動に合わせて実装している
  (いずれも 2.5.4-experimental の Microsoft.UI.Xaml.Controls.Ink* の実装 = microsoft-ui-xaml の
  `winui3/release/2.5.4-experimental` タグの `controls/dev/InkCanvas` / `InkToolbar` と実機で確認)
  - InkCanvas の既定の入力機器はペンだけ (`InputDeviceTypes = Pen`)。マウスやタッチで描くには設定が要る
  - InkStrokeContainer は末尾への追加 (AddStroke / AddStrokes) しかできず、Clear や DeleteSelected で一度外した
    InkStroke を同じコンテナへ追加し直すと黙って失敗する (AddStroke はインクのスレッドへの非同期の投函で、
    エラーは呼び出し元へ返らない)。並べ直すときは InkStroke.Clone した複製を追加する。個別の削除は
    対象だけを Selected にして DeleteSelected を呼ぶ
  - OS 側の InkStrokeContainer に一度でも触れる (Clear / AddStroke / CanPasteFromClipboard など) と、
    以後の ActivateCustomDrying は E_ILLEGAL_METHOD_CALL になる。custom drying 中は StrokeContainer の操作が失敗する
  - InkSynchronizer.BeginDry は乾かす線が無いと E_UNEXPECTED
  - InkPresenter の設定 (InputDeviceTypes / IsInputEnabled / InputProcessingConfiguration など) は、WinUI がインクの
    スレッドへ投函するだけで結果を見ない。投函した処理が失敗すると、その IAsyncAction を UI スレッドが解放した瞬間に
    XAML の未処理エラー (Application.UnhandledException) になり、Handled にしなければプロセスが落ちる (0xC000027B)。
    winui4k は Handled にして UI スレッドの未捕捉例外ハンドラーへ渡す (`doc/troubleshooting.md`)
  - OS の InkPresenter.SetPredefinedConfiguration は、プレゼンター自身の初期化より先に投函が届いたときしか受け付けず
    (遅れると E_ILLEGAL_METHOD_CALL)、どちらが先かはタイミング次第 (2 コアの CI や JNA では負けやすい) なので
    ラッパーを提供しない
  - MoveSelected は点の列ではなく InkStroke.PointTransform に平行移動を加える。LoadAsync は既存の線を置き換える
  - 鉛筆 (InkDrawingAttributes.CreateForPencil) は PenTip / PenTipTransform / DrawAsHighlighter の設定を、
    既定値の再設定も含めて E_INVALIDARG で拒否する
  - InkToolbar のボタンは読み込み (テンプレート適用) 後に作られる。GetToolButton の CustomPen / CustomTool と
    GetToggleButton の Custom は常に null (複数置けるため)。単独で作ったペンのボタンの Palette は null
  - InkToolbar はカスタム ペンのボタンを選んでも InkToolbarCustomPen を呼ばず、InkToolbarCustomPen.CreateInkDrawingAttributes も
    オーバーライドした CreateInkDrawingAttributesCore を仮想呼び出ししない (WinUI の不具合)。WInkToolbar が描画属性を置き換えて補う
  - InkStrokeInput / InkUnprocessedInput のイベント引数 (Windows.UI.Core.PointerEventArgs) は、インクのスレッドから
    UI スレッドへ遅れて届くため、CurrentPoint の読み取りが CO_E_NOT_SUPPORTED で失敗することがある
    (WinUI 自身のサンプル InkBugBash も投げ縄には使っていない)。InkPointerEvent は読めなければ null を返す
  - InkToolbarStencilButton.Ruler / Protractor は設定されず常に null (定規・分度器の表示は InkToolbar が InkPresenter 経由で行う)
- 既定ボタン (WFrame.defaultButton) は、ボタンに修飾キーなしの Enter の KeyboardAccelerator (ScopeOwner なし) を入れて作る。
  グローバルなアクセラレータは KeyDown がどの要素にも処理されなかったときだけ動き
  (`components/ContentRoot/KeyboardInputProcessor.cpp`)、Invoked を処理しなければボタンの UI オートメーションの Invoke
  (= クリック) になる。E2E テスト (`WDefaultButtonTest`。Enter は共有ウィンドウの InputSiteWindowClass へ PostMessageW で送る)
  で次の動きを確かめた (2.5.4-experimental)
  - CheckBox にフォーカスがあるとき: 既定ボタンが 1 回押される (CheckBox は Enter を受け付けない。`CheckBox_Partial.cpp` の `SetAcceptsReturn(false)`)
  - 既定ボタン自身にフォーカスがあるとき: 既定ボタンが 1 回だけ押される (ボタンが KeyDown を処理済みにするのでアクセラレータは動かない)
  - 既定ボタンでない Button にフォーカスがあるとき: フォーカスのあるボタンが押され、既定ボタンは押されない
    (ButtonBase は Enter の KeyDown を処理済みにし、KeyUp でクリックする。`ButtonBaseKeyProcess.h`)
  - WinUI の 1 行の TextBox にフォーカスがあるとき: アクセラレータでは既定ボタンは押されない (TextBox は Enter をテキスト エンジンへ
    渡して処理済みにする。`TextBoxBase.cpp` の `CTextBoxBase::OnKeyDown`)。ただし処理済みにされるのは要素に付けた KeyDown の
    ハンドラが呼ばれた後で、普通のハンドラにも handledEventsToo のハンドラにも Handled = false で届く (`WTextFieldActionTest` の実測)
  - そこで WTextField は KeyDown を `UIElement.AddHandler(KeyDownEvent, handler, handledEventsToo = true)` で購読し、
    1 行 (AcceptsReturn = false) で修飾キーなしの Enter なら、ActionListener があればそれを呼び、無ければ同じ XamlRoot の
    既定ボタンを押して、どちらでも Handled = true にする (JTextField と同じ動き。アクセラレータとの 2 重押しはない)。
    複数行 (AcceptsReturn = true) は改行が入るだけ。AcceptsReturn = false で TextWrapping = Wrap の TextBox は Enter を
    テキスト エンジンへ渡さない (`ShouldProcessKeyMessage`) ためアクセラレータでも押されるが、WTextField が先に処理済みにするので 1 回だけ押される
  - IME の変換中に確定のために押した Enter で ActionListener が呼ばれないかは未確認 (PostMessageW の Enter では IME を通らない)
  - 無効な既定ボタンは押されず、アクセラレータを外した後も押されない
- フォーカスのリスナー (WComponent.addFocusListener) のために、GotFocus / LostFocus の届き方を E2E テストの調査で確かめた
  (2.5.4-experimental。`WFocusEventTest`)
  - GotFocus / LostFocus は子孫から親へ伝わる (パネルにも、中の入力欄やボタンのフォーカスが OriginalSource = その子として届く)。
    WinUI4K は OriginalSource がそのコンポーネント自身か、そのテンプレートの部品のときだけ知らせる
  - TextBox / Button / ComboBox / Slider / ToggleSwitch / CheckBox / PasswordBox / CalendarDatePicker / RatingControl は
    コントロール自身がフォーカスを受け取る (OriginalSource = コントロール)。NumberBox / AutoSuggestBox / DatePicker / TimePicker は
    自身の IsTabStop が false で、UIElement.Focus はテンプレートの中の部品 (NumberBox は "InputBox" の TextBox、AutoSuggestBox は
    "TextBox"、DatePicker / TimePicker は "FlyoutButton") へフォーカスを渡す。部品の FrameworkElement.Parent はコントロールに
    ならず、内容 (Content / Children) の Parent はコントロールになるので、これでテンプレートの部品と内容を見分ける
  - ウィンドウで最初にフォーカスを移したとき、LostFocus を挟まずに同じ要素の GotFocus が 2 回届くことがある。
    WinUI4K は状態が変わったときだけ知らせる
  - GotFocus / LostFocus は相手の要素を渡さない (RoutedEventArgs だけ) ため、WFocusEvent.oppositeComponent は null。
    なお WinUI の IsTabStop は Tab キーの移動先を決めるだけで、UIElement.Focus (プログラムからのフォーカス) は止めない。そのため WComponent.requestFocusInWindow は
    IsTabStop を見ずに Focus の結果を返し (NumberBox などでも true)、isFocusable = false にしたコンポーネントだけを、設定した値を覚えて止める
- マウスのリスナー (WComponent.addMouseListener など) のために、ポインターのイベントの届き方を WinUI のソースと E2E テストの調査で確かめた
  (2.5.4-experimental)
  - E2E テストではポインターの入力を作れない。共有ウィンドウの InputSiteWindowClass・その親 (DesktopChildSiteBridge)・トップレベルの
    ウィンドウのどれに WM_MOUSEMOVE / WM_LBUTTONDOWN / WM_LBUTTONUP を PostMessageW / SendMessageW しても、PointerEntered /
    PointerPressed / PointerMoved は 1 つも届かない (キーの WM_KEYDOWN は届く)。SendInput や InjectSyntheticPointerInput は
    本物のカーソルを動かし、テスト中にユーザーが使っている別のウィンドウを操作してしまうので使わない。そのため `WMouseEventTest` は
    ネイティブのイベントから読んだ値の代わり (`TestMouse`) を MouseInput.dispatch に渡して確かめ、ネイティブのイベントからの読み取り
    (MouseInput.nativeSample) と捕捉の呼び出しは実際の入力では未確認
  - PointerPressed / Released / Moved / WheelChanged は子から親へ伝わる。ButtonBase は左ボタンの押下と離すのを自分で処理済みにし、
    押下で自分にポインターを捕捉する (`ButtonBase_Partial.cpp` の OnPointerPressed / OnPointerReleased)。そのため MouseInput は
    処理済みのものも受け取り (AddHandler の handledEventsToo)、処理済みの押下では捕捉し直さない
  - コントロール自身の処理 (OnPointerPressed などの仮想関数) は、同じ要素に付けたハンドラより先に呼ばれる (`eventmgr.cpp` の
    CEventManager::Raise は RaiseControlEvents の後に登録されたハンドラを呼ぶ)。リスナーの consume() でボタンのクリックは止められない
  - PointerEntered / PointerExited は親へ伝わらず、入った・出た要素のそれぞれに直接送られる (`PointerInputProcessor.cpp` の
    ProcessPointerEnterLeave。子に入っても親の Exited は来ない)。Swing は子にマウスのリスナーがあると親に mouseExited を送るが、
    WinUI4K は WinUI と同じく、そのコンポーネントの境界をまたぐときだけ知らせる
- キーのリスナー (WComponent.addKeyListener) のために、キーのイベントの届き方を E2E テストの調査と WinUI のソースで確かめた
  (2.5.4-experimental。`WKeyEventTest`)
  - KeyDown / KeyUp / CharacterReceived は、フォーカスを持つ要素 (OriginalSource) から親へ伝わる。PreviewKeyDown / PreviewKeyUp は
    親から子へ伝わり、KeyDown より先に届く。Button は Enter の KeyDown を、要素に付けたハンドラより先に処理済みにする
    (ハンドラには Handled = true で届く)。そこで keyPressed / keyReleased は PreviewKeyDown / PreviewKeyUp (処理済みのものも
    受け取る) から作り、consume() で Handled を立てると Button の Enter のクリックは起きない (ButtonBase.OnKeyDown は処理済みなら
    何もしない)。WTextField の Enter の ActionListener は KeyDown で動くので、keyPressed で consume された Enter を覚えて無視する
  - 共有ウィンドウの InputSiteWindowClass へ PostMessageW した WM_KEYDOWN からも、WinUI のメッセージ ループの TranslateMessage が
    WM_CHAR を作り、CharacterReceived が届く。
    Enter の WM_CHAR は '\r' (13) なので、keyTyped の文字は AWT と同じ '\n' に直す
  - KeyDown が処理済みになった押下の文字は CharacterReceived として送られない。ただしウィンドウがアクティブなときだけ
    (`KeyboardInputProcessor.cpp` の m_fKeyDownHandled)。非アクティブ表示の共有ウィンドウでも、Button が処理済みにした Enter や
    PreviewKeyDown で処理済みにした Enter の文字は、実行によって届いたり届かなかったりした (TextBox で処理済みにした Enter の文字は
    届いた。どこで決まるかは未確認)。WinUI4K の keyTyped は、処理済みになった押下で来るかどうかを約束しない
  - WM_KEYDOWN と WM_KEYUP を続けて送ると、TranslateMessage の作る WM_CHAR が WM_KEYUP の前に処理されるか後かは実行によって変わる
    (テストは WM_KEYDOWN の後の keyTyped を待ってから WM_KEYUP を送る)
  - TextBox は CharacterReceived の文字を、要素に付けたハンドラより先に挿入する (`TextBoxBase.cpp` の CTextBoxBase::OnCharacterReceived)
    ため、keyTyped の consume() では入力を止められない
- WLabel にアイコンを並べるために、TextBlock を Grid に入れたときの大きさと文字の位置を E2E テストで確かめた
  (2.5.4-experimental。`WLabelTest`)
  - TextBlock は画像を子に持てない (Inlines の InlineUIContainer は RichTextBlock だけ) ため、WLabel のルートは TextBlock を 1 つ入れた
    Grid にした。アイコンの無いラベルの DesiredSize・TextTrimming での切り詰め・TextAlignment での最初の文字の位置は、TextBlock だけのときと同じ
  - TextBlock の ActualWidth は、Width を 300 にしても、Stretch で広げても文字の幅 (例: 14) を返す。TextAlignment.Right の文字は
    広げた幅の右端に描かれる (TextPointer.GetCharacterRect の x が 286) ので、揃えの確認には ActualWidth ではなく文字の位置を使う
- WTextField の DocumentListener 風のリスナーと選択の API のために、TextBox の通知の届き方を E2E テストで確かめた
  (2.5.4-experimental。`WTextFieldDocumentTest` / `WTextFieldEditTest`)
  - TextChanging は put_Text の中で同期的に呼ばれる (Loaded の前でも呼ばれる)。TextChanged と違い、ハンドラの中で Text を読むと
    変更後の文字列。プログラムからの setText と replaceSelection は、WinUI4K が Swing と同じ REMOVE / INSERT を自分で知らせ、
    書き換えの最中に届く TextChanging は無視する
  - Select や put_SelectedText の SelectionChanged は同期的には呼ばれず、後からまとめて届く (同じ位置で 2 回届くこともある)
  - put_SelectedText は入れた文字を選択したままにする (Swing の replaceSelection は入れた文字の後ろにキャレットを置くので、
    WinUI4K は置き換えた後で Select し直す)
  - Padding (10,5,6,6)・BorderThickness (1)・MinWidth (64) などの既定のスタイルの値は、ツリーに載る (Loaded) までは 0 のまま
- WTextField.columns のために、TextBox の Loaded と幅の決まり方を E2E テストで確かめた (2.5.4-experimental。`WTextFieldColumnsTest`)
  - パネルの Loaded が届いた時点で、中の TextBox の IsLoaded はまだ false のことがある (TextBox の Loaded は後から届く)。
    XamlRoot はもう持っているので、columns は XamlRoot があれば測り、TextBox の Loaded でも測り直す
  - WinUI4K は UIElement.Measure を呼ばない (float の Size の値渡しを避ける) ため、'm' の幅は見えない Popup に TextBlock を入れて
    表示中のウィンドウで開き、UpdateLayout の後の DesiredSize で測る (TextMetrics)。Yu Gothic UI 14 の 'm' は 12.67 DIP、
    'm' を 10 個並べると 120.67 DIP で、1 個の 10 倍 (126.7) より狭い。columns は Swing と同じく 1 個の幅 × 列数で決める
