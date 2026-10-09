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
