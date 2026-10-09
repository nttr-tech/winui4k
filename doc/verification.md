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
- XAML のバインディングは、ソースが `IMap<String, Object>` を実装していればパス名をキーとして Lookup / Insert する
  (MapPropertyAccess)。キーが存在するか (HasKey) で接続の可否を決め、`IObservableMap.MapChanged` は変化したキーと
  一致するものだけを反映する (Reset は無視される)。WTableView の行アイテムはこれを使い、キー "c<列>" でモデルの値を返す
