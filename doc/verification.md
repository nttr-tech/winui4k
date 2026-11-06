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
  - ComboBox のドロップダウンを開くと (プログラムから IsDropDownOpen = true にしたときも)、WinUI はフォーカスを Popup の中の
    項目 (ComboBoxItem) へ移す (`ComboBox_Partial.cpp` の `ComboBox::OnOpen` の SetFocusedItem(forceFocus))。Popup の子の
    ビジュアル ツリーの親 (VisualTreeHelper.GetParent) をたどってもコンボボックスに着かないので、以前は開くと focusLost、
    閉じると focusGained が届いていた (JComboBox は開いている間もフォーカスを持つ)。Popup.Child の論理ツリーの親
    (FrameworkElement.Parent) は Popup になる (`Popup.cpp` の AddLogicalChild) ので、フォーカスの判定では論理ツリーの親が Popup の
    要素から Popup へ移ってたどり、テンプレートの中の Popup の部品もコントロールの部品とみなす (`WFocusEventTest`)。
    マウスの判定 (処理済みの押下を子のコントロールのものとみなすか) では、Popup の中はこれまでどおりコンポーネントの外として扱う
    (JComboBox のポップアップも別のコンポーネント)
  - Flyout (FlyoutBase.ShowAt で出す DatePicker / TimePicker の選択の画面や WFlyout) の Popup はコントロールのツリーに属さず、
    配置先 (FlyoutBase の m_tpPlacementTarget、`FlyoutBase_partial.cpp` の SetPlacementTarget) も Popup には付かないので、
    どのコントロールのものかを公開 API で引けない。そのため Flyout がフォーカスを取ると focusLost、閉じて戻ると focusGained が届く
    (未対応。E2E では未確認)。AutoSuggestBox の候補の一覧はテンプレートの中の Popup なので、項目にフォーカスが移っても
    ComboBox と同じに部品として扱われる (E2E では未確認)
  - GotFocus / LostFocus は相手の要素を渡さない (RoutedEventArgs だけ) ため、WFocusEvent.oppositeComponent は null。
    なお WinUI の IsTabStop は Tab キーの移動先を決めるだけで、UIElement.Focus (プログラムからのフォーカス) は止めない。そのため WComponent.requestFocusInWindow は
    IsTabStop を見ずに Focus の結果を返し (NumberBox などでも true)、isFocusable = false にしたコンポーネントだけを、設定した値を覚えて止める
- マウスのリスナー (WComponent.addMouseListener など) のために、ポインターのイベントの届き方を WinUI のソースと E2E テストの調査で確かめた
  (2.5.4-experimental)
  - E2E テストではポインターの入力を作れない。共有ウィンドウの InputSiteWindowClass・その親 (DesktopChildSiteBridge)・トップレベルの
    ウィンドウのどれに WM_MOUSEMOVE / WM_LBUTTONDOWN / WM_LBUTTONUP を PostMessageW / SendMessageW しても、PointerEntered /
    PointerPressed / PointerMoved は 1 つも届かない (キーの WM_KEYDOWN は届く)。SendInput や InjectSyntheticPointerInput は
    本物のカーソルを動かし、テスト中にユーザーが使っている別のウィンドウを操作してしまうので使わない。そのため `WMouseEventTest` は
    ネイティブのイベントから読んだ値の代わり (`TestMouse`) を MouseInput.dispatch に渡して確かめている
  - 実際の入力では確かめていないネイティブの経路: MouseInput.onNative / nativeSample (XamlPointerEvent.pointState の
    GetCurrentPoint と PointerPointProperties のボタン・ホイールの読み取り、PointerPoint.Timestamp、PointerId / FrameId による
    同じイベントの見分け)、Handled の読み取り、handledByContent が OriginalSource を読む部分、NativePointerTarget
    (UIElement.CapturePointer / ReleasePointerCapture と Handled = true の書き込み)、Xaml.addHandlerHandledToo で登録した
    処理済みのイベントが本当に届くこと、PointerEntered / PointerExited の購読、RasterizationScale の読み取り。
    実際の要素で確かめているのは、handledByContent が OriginalSource に使う分類 (isHandledInContent。パネルの中のボタンと
    その部品は子のもの、パネルの中の文字・自身・自身の部品は自分のもの、ツリーの外と Popup の中は外)、PointerUpdateKind から
    ボタンの番号への写し、修飾キーの写し (`WMouseEventTest`)
  - パネルなどが処理されていない押下でポインターを捕捉しても (MouseInput の onPressed)、タッチの ScrollViewer のパンは止まらない
    (WinUI のソースで確認。実機のタッチでは未確認)。DirectManipulation の接触はヒットテストした要素とその祖先の ManipulationMode
    で決まり (`InputServices.cpp` の ProcessDirectManipulationPointerHitTest と InitializeDirectManipulationForPointerId)、
    押下の後の登録 (`PointerInputProcessor.cpp` の XCP_POINTERDOWN での RegisterDMViewportsOnPointerDown) もヒットテストの要素を
    渡し、捕捉は見ない。パンが始まると WinUI は WM_POINTERCAPTURECHANGED を受けて捕捉を解き PointerCaptureLost を送る
    (`PointerInputProcessor.cpp` の ProcessPointerInput の XCP_POINTERCAPTURECHANGED → ReleasePointerCaptureById) ので、
    MouseInput は押した状態を捨てる (mouseReleased / mouseClicked は届かない)。そのため捕捉をマウスのポインターに限らない
  - PointerPressed / Released / Moved / WheelChanged は子から親へ伝わる。ButtonBase は左ボタンの押下と離すのを自分で処理済みにし、
    押下で自分にポインターを捕捉する (`ButtonBase_Partial.cpp` の OnPointerPressed / OnPointerReleased)。そのため MouseInput は
    処理済みのものも受け取り (AddHandler の handledEventsToo)、処理済みの押下では捕捉し直さない
  - コントロール自身の処理 (OnPointerPressed などの仮想関数) は、同じ要素に付けたハンドラより先に呼ばれる (`eventmgr.cpp` の
    CEventManager::Raise は RaiseControlEvents の後に登録されたハンドラを呼ぶ)。リスナーの consume() でボタンのクリックは止められない
  - PointerEntered / PointerExited は親へ伝わらず、入った・出た要素のそれぞれに直接送られる (`PointerInputProcessor.cpp` の
    ProcessPointerEnterLeave。子に入っても親の Exited は来ない)。Swing は子にマウスのリスナーがあると親に mouseExited を送るが、
    WinUI4K は WinUI と同じく、そのコンポーネントの境界をまたぐときだけ知らせる
- ItemListener (WToggleButton.addItemListener) と WButtonGroup のために、RadioButton の通知の順序を WinUI のソースと E2E テストで
  確かめた (2.5.4-experimental。`WRadioButtonTest`・`WButtonGroupTest`)
  - 同じ GroupName (無ければ同じ親) のラジオボタンを選ぶと、WinUI は外れた方の Unchecked を先に、選んだ方の Checked を後に送る。
    `RadioButton_Partial.cpp` の `RadioButton::OnChecked` が、Checked を発火する (`RadioButtonGenerated::OnChecked`) 前に
    UpdateRadioButtonGroup でほかのボタンの IsChecked を false にし、その Unchecked はその場で届く。外れた方の通知の時点で、
    選んだ方の IsChecked はもう true になっている。親が無い (ツリーに載る前の親も無い) ボタンでは UpdateRadioButtonGroup は何もしない
  - WButtonGroup は、外れた方の Unchecked が届いたとき、ほかのメンバーがもう選ばれていればそれを選択にする (選び直さない)。
    ほかに選ばれたメンバーが無ければ、外そうとしただけなので選択に戻し、外した通知も戻した通知も ItemListener とアクションに配らない
    (Swing の ButtonGroup では状態が変わらない)
  - 以前の WButtonGroup はラジオボタンごとに別の GroupName を付けていた (名前を作るプロパティが読むたびに新しい名前を返していた) ので、
    グループの中で WinUI の排他も矢印キーのフォーカスの移動 (`RadioButton::FocusNextElementInGroup` は同じ GroupName の
    ボタンにだけ移る) も効かなかった。いまはグループで 1 つの名前を付ける
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
- WSlider の ChangeListener とモデル、WRatingControl の ChangeListener のために、RangeBase と RatingControl の通知の届き方を
  E2E テストと WinUI のソースで確かめた (`WSliderTest` / `WRatingControlTest`)
  - RangeBase (Slider / ProgressBar) は Value の設定で切り詰める前の値を覚えていて (core/core/elements/RangeBase.cpp の
    m_uncoercedValue)、Minimum / Maximum で範囲を広げるとその値へ戻す (0..100 で 150 を入れて 100 になった後、Maximum を 200 にすると 150)。
    Swing のモデルは戻さないので、WSlider は範囲を書くたびにモデルの値を Value に書き直す。Minimum / Maximum の変更で Value が
    動かなければ ValueChanged は届かない
  - Slider は押下を SliderContainer (テンプレートの部品) やつまみ (Thumb) で処理済みにする (Slider_Partial.cpp の OnPointerPressed)。
    スライダー自身でドラッグの始まりと終わりを知るには、処理済みのものも受け取る AddHandler で PointerPressed / PointerReleased /
    PointerCaptureLost を購読する。押下の処理で値を先に動かす (MoveThumbToPoint) ので、値の通知が押下の通知より先に届く
    (ソースを読んだだけ。ポインターの入力は E2E テストから送れない (TestMouse 参照) ので、実際のドラッグでは確かめていない)
  - RatingControl.ValueChanged はユーザーの入力 (RatingControl.cpp の SetRatingTo) でだけ届き、Value の設定では届かない。
    最大の星でさらに増やすキーを押したときなど、値が変わらなくても届く。WRatingControl は設定でも自分で知らせ、同じ値の通知は捨てる
- WComponent.addComponentListener のために、位置と表示の変わり方を E2E テストで確かめた (`WComponentEventTest`)
  - Canvas の子の Canvas.Left / Top を替えても、WinUI はレイアウトをせずに ActualOffset だけを替える
    (microsoft-ui-xaml の dxaml/xcp/core/core/elements/uielement.cpp の Canvas_Left / Canvas_Top → OnActualOffsetChanged)。
    FrameworkElement.LayoutUpdated は来ないので、componentMoved は LayoutUpdated に加えて Canvas.Left / Top の変更のコールバックでも位置を比べる
  - Visibility を Collapsed にしても、その要素の SizeChanged は来ない (Swing の setVisible(false) と同じく componentResized は来ない)
- 閉じるまで呼び出し元を止めるダイアログ (Swing の JOptionPane.showConfirmDialog などの形) のために、UI スレッドで入れ子の
  メッセージ ループ (`internal.winui.ModalLoop`) を回したときの挙動を E2E テストで確かめた (2.5.4-experimental。`ModalLoopTest` は
  Panama、`ModalLoopProcessTest` は子のプロセスを Panama・JNA・JNR のそれぞれで起動)
  - ループは WinUI 自身のループ (`FrameworkApplication_Partial.cpp` の RunDesktopWindowMessageLoop) と同じ
    GetMessageW → ContentPreTranslateMessage → TranslateMessage → DispatchMessageW で回す。ContentPreTranslateMessage は
    Microsoft.UI.Windowing.Core.dll が公開している (2.5.1 のランタイムの DLL の公開シンボルで確認)
  - ループの間も DispatcherQueue (`invokeLater`)・GC 由来の Release (ReleasePump)・キー入力・描画は動く。ボタンの Click・
    メニュー項目の Click・キーボード アクセラレーターの Invoked の中から ContentDialog を開いて待ち、Esc (結果は None) と
    Enter (既定のボタン) で閉じると、呼び出し元の続きが動く
  - SizeChanged と Loaded のハンドラの中でループを回すと、ループの間は XAML のレイアウトが進まない (後から載せた要素が
    Loaded にならず、ループを抜けると Loaded になる)。SizeChanged と LayoutUpdated は CLayoutManager::UpdateLayout の中で
    m_isInUpdateLayout を立てたまま呼ばれ (`LayoutManager.cpp`。立っている間の UpdateLayout は何もせずに戻る)、Loaded も
    フレームの処理 (UIThreadScheduler の Ticking) の中で呼ばれるため。この中で開いたダイアログは現れないまま止まるので、
    ModalLoop は `addSizeChangedListener` / `addLoadedListener` のリスナーの中からの呼び出しを IllegalStateException で断る
  - 入れ子 (深さ 2) にできる。ダイアログを待つ間に FileOpenPicker の OS のダイアログを待っても、UI スレッドは内側のループで
    invokeLater を処理し、OS のダイアログを閉じると内側・外側の順に戻る。外側の条件が先に成り立っても、内側が戻るまで外側は戻らない
  - ContentDialog を開いたまま持ち主のウィンドウを閉じる (WM_CLOSE) と、ウィンドウは閉じるが ContentDialog.Closed は届かない。
    持ち主の要素の IsLoaded は false になるので、待つ側はそれ (かウィンドウの Closed) でも待ちを終えること
  - ループの中で Application.Exit (`WinUiUtilities.exit`) を呼んだときと、ただ 1 つのウィンドウを閉じた (自動終了) ときは、
    ループが WM_QUIT を受け取る。ModalLoop は同じ終了コードで PostQuitMessage し直して false を返し、WinUI のループも終わって
    プロセスは終了コード 0 で終わる (3 つのバックエンドとも)
  - ループの中で別のスレッドから System.exit を呼んでも、プロセスはその終了コードで終わる (止まらない)。ただし Panama と JNR では
    終了の途中で JVM が落ちて hs_err を残すことがある (2026-10-20 に各 4 回: Panama 4/4、JNR 3/4、JNA 0/4)。ループを回さずに
    呼んでも同じ割合で落ちる (Panama 4/4、JNR 4/4、JNA 0/4) ので、ModalLoop が原因ではない (doc/troubleshooting.md の upcall の件)
- 日付と時刻のピッカーの未選択 (null) と ChangeListener のために、DatePicker / TimePicker / CalendarDatePicker / CalendarView の
  通知の届き方を E2E テストと WinUI のソースで確かめた (`WDatePickerTest` / `WTimePickerTest` / `WCalendarDatePickerTest` / `WCalendarViewTest`)
  - DatePicker の既定の Date は未選択を表す番兵値 0 (1601-01-01 UTC)、TimePicker の既定の Time は番兵値 -1 (TimeSpan)。
    SelectedDate / SelectedTime を null にすると Date / Time も番兵値になる (DatePicker_Partial.cpp / TimePicker_Partial.cpp の
    OnPropertyChanged2)。WinUI4K はこの番兵値を null として返す
  - DatePicker はテンプレートを適用する (OnApplyTemplate が終わる) までは Date を MinYear / MaxYear に寄せず、DateChanged /
    SelectedDateChanged も送らない (OnPropertyChanged2 の m_isInitializing)。適用の途中で範囲外の Date を寄せる (RefreshSetup の
    put_Date) ときも m_isInitializing のままなので通知は無い。WDatePicker の ChangeListener は setter の後と Loaded で値を比べて知らせる。
    適用後は範囲外の Date を寄せてから寄せた値で 1 回だけ通知し、MinYear / MaxYear の変更で Date が動いたときも通知する
  - TimePicker は載る前でも Time の変更で TimeChanged / SelectedTimeChanged を送り、MinuteIncrement の刻みに切り捨てる
    (TimePicker_Partial.cpp の OnTimeChanged と CheckAndCoerceTime)。00:00 は番兵値と区別される
  - CalendarView.SelectedDates (IVector<DateTime>) の Append / Clear は、載る前でもそれぞれ SelectedDatesChanged を 1 回送る
- WTabView の JTabbedPane の形の API と、ナビゲーション系のリスナーのために、TabView・NavigationView などの動作を E2E テストと
  WinUI のソースで確かめた (`WTabViewTest` / `WNavigationViewTest` / `WBreadcrumbBarTest` / `WSemanticZoomTest`)
  - TabView.SelectedIndex の既定値は 0 (controls/dev/TabView/TabView.idl の MUX_DEFAULT_VALUE("0"))。読み込む前はタブが無くても 0 を返し、
    読み込み (内部の ListView の Loaded) で ListView の選択から読み直す。読み込んだ後の空の TabView の TabItems にタブを足しても選ばれない
    (SelectedIndex は -1 のまま)。選択中のタブを外すと、TabView は同じ位置 (末尾なら 1 つ前) の、押せて見えているタブを選ぶ
    (TabView.cpp の OnItemsChanged)。WTabView は読み込みの前後どちらでも Swing と同じ規則になるよう、足す・外すたびに SelectedIndex を書き直す
  - TabView のインデックスは TabItems の並び。ドラッグでの並べ替えは TabItems (読み込み後は ListView.Items) の中で動かすので、
    ラッパーの側で並びを持つとずれる。TabItemsChanged は SelectedIndex を ListView から読み直す前に送られる (OnItemsChanged の先頭) ので、
    選択の変化を知るのには使えない。TabDragCompleted は読み直した後に送られる (OnListViewDragItemsCompleted)
  - TabViewItem の閉じるボタン (テンプレートの部品 CloseButton) をオートメーションの Invoke で押すと TabCloseRequested が届く
  - NavigationViewItem のオートメーションのピアは Invoke を持たず、SelectionItem の Select でクリックと同じ ItemInvoked と選択が起きる
    (NavigationViewItemAutomationPeer.cpp の ChangeSelection)。ItemInvoked の InvokedItemContainer (INavigationViewItemInvokedEventArgs2) は
    子の項目のクリックでは子の項目を指す。項目のコンテナがまだ無いと、SelectionChanged は後で (DispatcherQueue の Low で) 送られる
    (NavigationView.cpp の ChangeSelection)
  - BreadcrumbBarItem のピアは Invoke を持ち、押すと ItemClicked が届く。VisualTreeHelper には DependencyObject のビューを渡すこと
    (コントロールの既定のインターフェースのポインタを渡すと Microsoft.UI.Xaml.dll の中でアクセス違反になる)
  - SemanticZoom.ViewChangeCompleted は ToggleActiveView と IsZoomedInViewActive の設定のどちらでも、切り替えの後に届く
- JDialog 風の `WContentDialog` と JOptionPane 風の `WOptionPane` のために、ContentDialog の閉じ方を E2E テストで確かめた
  (`WContentDialogTest`・`WOptionPaneTest`)
  - Esc は、CloseButtonText が空でなければ閉じるボタンを押したことになり、CloseButtonClick → Closing → Closed (結果は None) が
    閉じるボタンを押したときと同じ順で来る (`ContentDialog_Partial.cpp` の ProcessLayoutRootKey → ExecuteCloseAction。テンプレートの
    LayoutRoot の KeyDown で処理する)。Swing は Esc を CLOSED_OPTION、キャンセルのボタンを CANCEL_OPTION と区別するので、
    WOptionPane はダイアログ自身の PreviewKeyDown (トンネル) で Esc を先に受けて処理済みにし、Hide で閉じる。ウィンドウに
    載せていない ContentDialog はダイアログごと Popup に載る (EntireControlInPopup) ので、ダイアログは中の要素の祖先になり、
    PreviewKeyDown が届く
  - 同じウィンドウ (XamlRoot) にほかの ContentDialog が開いている間の ShowAsync は、非同期の処理を
    始めずにその場で E_ASYNC_OPERATION_NOT_STARTED (0x80000019、ERROR_CONTENTDIALOG_MULTIPLE_OPEN) を返す。
    WContentDialog はこれを IllegalStateException にする
  - 開いたまま持ち主のウィンドウを閉じると、XamlRoot.Content (ウィンドウの内容) の IsLoaded も false になる。showAndWait は
    開いたときの XamlRoot.Content の IsLoaded を待ちの条件に入れるので、持ち主がメニュー項目のように先に外れる要素でも
    ウィンドウが開いている限り待ち続ける
- JMenuItem 風のメニュー項目 (WMenuFlyoutItem の doClick・accelerator) のために、MenuFlyoutItem の Invoke とアクセラレーターの届き方を
  WinUI のソースと E2E テストで確かめた (`WMenuFlyoutItemTest`)
  - UI オートメーションの Invoke (`Xaml.invoke`) は `MenuFlyoutItem::Invoke` (MenuFlyoutItem_Partial.cpp) を呼び、Click・Command・
    開いているメニューを閉じる、までをマウスで選んだときと同じに行う。ToggleMenuFlyoutItem は Invoke を上書きして先に IsChecked を
    反転する。無効な項目の Invoke は UIA_E_ELEMENTNOTENABLED なので、doClick は呼ぶ前に isEnabled を見て何もしない
  - Invoked のハンドラを付けないアクセラレーターは、メニューを閉じたままでも項目を Invoke する (Click が届く)。ContextFlyout の
    メニュー (UIElement::OnProcessKeyboardAcceleratorsImpl) とメニュー バー (MenuBarItem はテンプレートの ContentButton の
    ContextFlyout に項目を移す。MenuBarItem.cpp の PopulateContent) のどちらでも効き、フォーカスは別の要素にあってよい
- JCheckBoxMenuItem / JRadioButtonMenuItem 風の ItemListener と WButtonGroup のために、ToggleMenuFlyoutItem と RadioMenuFlyoutItem の
  チェックの変わり方を WinUI のソースと E2E テストで確かめた (`WSelectableMenuItemTest`)
  - どちらにも Checked のイベントが無いので、IsChecked の依存関係プロパティの変更のコールバックで受け取る。RadioMenuFlyoutItem は
    実装では ToggleMenuFlyoutItem から派生し (`DeriveFromToggleMenuFlyoutItemHelper_base`)、クリックで反転するのは基底の IsChecked だが、
    公開の IsChecked は自前の別のプロパティ (`RadioMenuFlyoutItem.cpp` の OnInternalIsCheckedChanged が同期する) なので、そちらを見る
  - doClick (Invoke) では IsChecked が変わってから Click が届くので、ItemListener が ActionListener より先に呼ばれる (Swing と同じ)。
    チェック済みのラジオの項目をクリックすると、基底の IsChecked を戻すだけで公開の IsChecked は変わらない (ItemListener は呼ばれない)
  - RadioMenuFlyoutItem の排他は GroupName をキーにした UI スレッド全体の表 (`s_selectionMap`) による。既定の "" の項目はアプリ全体で
    1 つのグループになるため、WRadioMenuFlyoutItem は名前の無い項目に一意の名前を付ける。名前だけで排他にしたときは、選んだ項目の
    SELECTED が外れた項目の DESELECTED より先に届く。WButtonGroup に入れたときは、外れた方の通知でグループが選択を移すので Swing と同じく
    DESELECTED が先になる
- JComboBox 風の WComboBox (編集できる ComboBox の選択と、モデルに無い項目の扱い) のために、ComboBox と Selector の SelectedItem の受け付け方を
  WinUI のソースで確かめた
  - 編集できる (IsEditable) ComboBox がモデルに無い SelectedItem (入力した文字列など) を受け付けるのは、テンプレートが適用されてからになる。
    `ComboBox::SetupEditableMode` (ComboBox_Partial.cpp:318。テンプレート適用時の :264 と、適用後に IsEditable を変えたときの :1318 から呼ばれる)
    が `SetAllowCustomValues(true)` を呼ぶ (:390) までは、Selector は項目に無い値を受け付けない。テンプレートを適用する前
    (ウィンドウに載せる前) に編集できる ComboBox へ項目外の SelectedItem を入れても、選択にならない
  - 項目に無い SelectedItem を受け付けられない状態で ItemsSource も無いと、Selector はその値を捨てずに `m_itemPendingSelection` に覚える
    (`Selector::OnSelectedItemChanged`、Selector_Partial.cpp:633 の中の :671)。覚えた値は、あとで ItemsSource が (null 以外に) 設定され、
    その項目に含まれていたときに `Selector::OnItemsSourceChanged` (:404 の中の :466-481) が選び直す。つまり項目外の値を一度
    ネイティブへ送ると、後から ItemsSource を設定したときに思わぬ選択が起きうる。WComboBox は項目外の値をネイティブへ送らず、選択はモデル側で持つ
- AppWindow.Closing は利用者の閉じる操作 (WM_CLOSE、タイトル バーの ×) では来るが、Window.Close() では来ない
  (WFrameTest で WM_CLOSE の 4 回だけ来て、dispose の Close では 1 回も来なかった)。Closing の Cancel を true にすると
  Window.Closed も来ずウィンドウは開いたまま、Closing の中で AppWindow.Hide を呼ぶと隠れて開いたままになる
  (WFrame の DO_NOTHING_ON_CLOSE / HIDE_ON_CLOSE)
- WScrollPane / WScrollView の JScrollPane の形の API のために、ScrollViewer と ScrollView の動作を E2E テストと WinUI のソースで確かめた
  (`WScrollPaneTest` / `WScrollPaneBarTest` / `WScrollViewTest`)
  - ScrollViewer.ChangeView を disableAnimation = true で呼んでも、HorizontalOffset / VerticalOffset はその場では変わらない
    (オフセットは ScrollContentPresenter に書かれ、再配置の後に読める。ScrollViewer_Partial.cpp の ChangeViewInternal)。
    続けて UpdateLayout を呼ぶと戻った時点で新しい位置を読める。ViewChanged はその後に非同期に届き、最後の通知の IsIntermediate は false
  - HorizontalScrollBarVisibility が Hidden なら中身は横に無制限で測られ (ExtentWidth = 中身の幅)、Disabled なら表示領域の幅で測られる
  - 見出し (TopHeader / LeftHeader / TopLeftHeader) があると、中身が左上寄せでないと測る段階で E_NOT_SUPPORTED になりプロセスが落ちる
    (ScrollContentPresenter_Partial.cpp の MeasureOverride、doc/troubleshooting.md)
  - UIElement.StartBringIntoView に TargetRect を渡すと、祖先の ScrollViewer は四角が見える最小の量だけスクロールする (Swing の scrollRectToVisible と同じ)。
    スクロールは呼んだ後に起きる
  - ScrollView.ScrollTo を AnimationMode = Disabled の ScrollingScrollOptions で呼んでも位置は InteractionTracker の非同期の更新で変わる
    (ScrollPresenter.cpp の TryUpdatePosition)。UpdateLayout では読み戻せない
- JSplitPane 風の WSplitPane (Grid の 3 つのセルを Kotlin から決める) のために、仕切りのマウス ポインターと配分の反映を E2E テストで確かめた
  (`WSplitPaneTest`)
  - UIElement.ProtectedCursor は protected (派生クラス向け) だが、派生していない Border でも IUIElementProtected を QI で取れ、
    InputSystemCursor.Create(SizeWestEast / SizeNorthSouth) を IInputCursor にして put_ProtectedCursor すると、get で読み戻せる
  - Grid の SizeChanged の中で ColumnDefinition.Width を変えても例外にならず、続くレイアウトで配置し直される
    (同じ UpdateLayout の中で済むかは確かめておらず、テストは validate を 2 回回してから位置を読む)
- JFileChooser 風の `WFileChooser` のために、Windows App SDK のピッカー (Microsoft.Windows.Storage.Pickers) を確かめた
  (`WStoragePickersTest`・`WFileChooserTest`)
  - Foundation 2.3.13-experimental の winmd には IFileOpenPicker2・IFileSavePicker2・IFolderPicker2 があり (2.1.0 には無い)、
    Title・SuggestedFolder (開く・フォルダー)・FileTypeChoices と InitialFileTypeIndex (開く)・PickMultipleFoldersAsync が使える。
    実行時のランタイムで QueryInterface でき、設定した値をそのまま読み戻せる。Title は OS のダイアログ (ウィンドウ クラス #32770) の
    タイトルになる
  - FileTypeChoices は追加した順を保ち、同じ表示名の Insert は位置を変えずに拡張子を置き換える。拡張子は ".txt" か "*" で、
    ".tar.gz" のように 2 文字目以降に "." を含むものは Append / Insert の時点で例外になる。開くピッカーでは FileTypeChoices が
    FileTypeFilter より優先される (WindowsAppSDK の PickerCommon.cpp の CaptureFilterSpecData)
  - ピッカーの OS のダイアログは持ち主のウィンドウにモーダルで、開いている間も UI スレッドの入れ子のループ (ModalLoop) は回り、
    ダイアログに WM_CLOSE を送るとキャンセル (結果は null) で完了する
