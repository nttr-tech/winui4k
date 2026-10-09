# リボン (WRibbon)

Office / AutoCAD 風のリボンと、その周辺のバー (ツール バー・ステータス バー・メニュー バー・タイトル バー・検索・
アプリケーション メニュー) を提供する。[RibbonSpace.WinUI](https://github.com/wieslawsoltes/RibbonSpace) (C#) を
Pure Kotlin で再実装したもので、ブリッジ DLL も C# も使わない。RibbonSpace は MIT License で、その著作権表示と
ライセンス文は `THIRD-PARTY-NOTICES.md` と jar の `META-INF/LICENSE-RibbonSpace.txt` に収めている。

## 構成: モデルとビュー

Swing の MVC と同じく、データと状態はモデル (`com.appkitbox.winui4k.ribbon` パッケージ) が持ち、
ビュー (`com.appkitbox.winui4k` の `WRibbon*`) はモデルを購読して表示する。

- **モデル**: `RibbonModel` を根に、`RibbonTabModel` → `RibbonGroupModel` → 項目 (`RibbonItemModel` の派生) の木。
  プロパティは `RibbonObservable` で変更を通知し、子の並びは `RibbonList` (追加・削除・移動を通知するリスト) で持つ。
  WinUI に依存しないので、テストやコマンドの組み立てはウィンドウなしでできる。
- **ビュー**: `WRibbon(model)` がモデルを描画する。モデルを変えると表示が追従し、ユーザーの操作 (トグル・選択・入力・
  タブの切り替え・QAT の追加など) はモデルへ書き戻される。同じ項目のモデルを複数の場所 (QAT・カスタム グループ・
  ツール バー) に置くと、どこで操作しても同じ状態を共有する。
- **イベント**: `add...Listener` / `remove...Listener` (Swing 流)。Java からはラムダで登録できる。

```kotlin
val model = RibbonModel().apply {
    tabs.add(RibbonTabModel("home", "ホーム").also { tab ->
        tab.groups.add(RibbonGroupModel("clipboard", "クリップボード").also { group ->
            group.items.add(RibbonButtonModel("paste", "貼り付け", RibbonIcons.PASTE).also { it.size = RibbonItemSize.LARGE })
            group.items.add(RibbonToggleButtonModel("bold", "太字", RibbonIcons.BOLD))
        })
    })
}
val ribbon = WRibbon(model)
ribbon.addItemInvokedListener { e -> println("実行: ${e.item.id}") }
frame.setContentPane(ribbon)
```

Java からも同じモデルを組み立てられる (`winui4k-sample-gallery-in-java` の `RibbonPages.java`)。

## 主なクラス

| 分類 | クラス |
|---|---|
| 構造 | `RibbonModel`、`RibbonTabModel`、`RibbonGroupModel`、`RibbonContextualGroupModel` (コンテキスト タブ)、`RibbonBackstageModel` / `RibbonBackstageItemModel` |
| ボタン | `RibbonButtonModel`、`RibbonToggleButtonModel` (`groupName` でラジオ)、`RibbonDropDownButtonModel`、`RibbonSplitButtonModel`、`RibbonCheckBoxModel` |
| 入力 | `RibbonComboBoxModel` (+ `RibbonFontComboBoxModel` / `RibbonFontSizeComboBoxModel`)、`RibbonSpinnerModel`、`RibbonTextBoxModel`、`RibbonSliderModel` |
| 選択 | `RibbonGalleryModel` / `RibbonGalleryItemModel` (分類・絞り込み・ライブ プレビュー)、`RibbonColorPickerModel`、`RibbonGridPickerModel` (表の挿入)、`RibbonSegmentedModel` |
| 配置 | `RibbonButtonGroupModel`、`RibbonRowModel`、`RibbonSeparatorModel`、`RibbonLabelModel`、`RibbonCustomItemModel` (任意の `WComponent`) |
| メニュー | `RibbonMenuItemModel`、`RibbonMenuSeparatorModel`、`RibbonMenuHeaderModel` |
| 補助 | `RibbonScreenTip`、`RibbonIcon` / `RibbonIcons`、`RibbonKeyGesture`、`RibbonSizeDefinition` |
| コマンド | `RibbonCommand`、`RibbonRelayCommand`、`RibbonAsyncCommand`、`RibbonCommandCatalog` (`commandId` で項目とコマンドをつなぐ) |
| 状態 | `RibbonState` / `RibbonStateSerializer` (QAT・カスタマイズ・最小化・浮動パネルの保存と復元)、`RibbonModelMerger` (MDI の子のリボンを親へ統合) |
| バー | `RibbonToolBarModel`、`RibbonStatusBarModel`、`RibbonZoomModel`、`RibbonMenuBarModel`、`RibbonApplicationMenuModel` |
| 文字列 | `RibbonStrings` (英・独・西・仏・波・日。既定は OS の表示言語。`RibbonStrings.current` で切り替え) |

ビュー側:

| クラス | 役割 |
|---|---|
| `WRibbon` | リボン本体 (タブ・グループ・QAT・バックステージ・KeyTip・カスタマイズ ダイアログ・状態の保存) |
| `WRibbonTitleBar` | Office 風のタイトル バー (QAT・題名・検索を載せ、`attachToWindow` でウィンドウのタイトル バーにする) |
| `WRibbonSearchBox` / `WRibbonCommandPalette` | リボンのコマンド検索 (Alt+Q) と、幅が狭いときのコマンド パレット |
| `WRibbonToolBar` / `WRibbonStatusBar` / `WRibbonContextualToolBar` / `WRibbonMenuBar` | リボンの外のバー (同じ項目モデルを使う) |
| `WRibbonApplicationMenu` | AutoCAD 風のアプリケーション メニュー (最近使ったファイル・サブコマンド・検索) |
| `WRibbonScreenTipService` | 任意のコントロールに ScreenTip を付ける |
| `WRibbonTheme` | パレット (Word / Excel / … / CAD)・装飾の色使い・面の見た目 (Office / CAD)・ライト / ダーク |

## 機能

- **表示モード**: クラシック (大・中・小のボタン) とシンプル (1 行。入りきらない項目はオーバーフロー メニュー)。
- **表示の状態**: 常に表示 / タブのみ (クリックで一時的に開く) / 全画面。最小化の動作 (`RibbonMinimizeBehavior`) は AutoCAD の循環にも対応。
- **縮小**: 幅が足りないとグループを大 → 中 → 小 → 折りたたみ (ポップアップ) の順に縮める。順序は `reductionOrder`・
  `RibbonSizeDefinition`、戦略は `RibbonReductionStrategy` (全体 / グループ単位)。
- **KeyTip**: Alt (または F10) で表示。タブ → 項目 → メニューの階層をたどり、Esc で 1 段戻る。未指定の KeyTip は自動で割り当てる。
- **QAT**: リボンの上 / 下。項目の右クリックで追加・削除、`quickAccessCandidates` はカスタマイズ メニューに出る候補。
- **コンテキスト タブ**: `RibbonContextualGroupModel` の表示で色付きのタブが現れる (`activation` で表示時に選択)。
- **バックステージ**: [ファイル] で全面に開く画面。項目ごとに任意の `WComponent` を内容にできる。
- **カスタマイズ**: ユーザー設定ダイアログ (リボン / QAT)、カスタム タブ・グループ、名前の変更・並べ替え・非表示・リセット。
- **浮動パネル・展開パネル** (AutoCAD): グループのタイトルのドラッグで浮動、`slideOutItems` はタイトルの矢印で開く展開パネル (ピン留め可)。
- **検索**: リボンの全項目 (メニューの中も) をラベル・KeyTip・ショートカット・説明から検索して実行する。
- **テーマ**: `WRibbonTheme` のパレット・装飾・面の見た目は、アプリの XAML からも `{ThemeResource Ribbon...Brush}` で参照できる。
- **右から左**: `WComponent.flowDirection = FlowDirection.RIGHT_TO_LEFT` でタブの ← / → キーとタイトル バーのキャプション
  ボタンの余白が反転する。
- **UI の言語**: リボンが自分で出す文字列 (スクリーン ヒント・メニュー・ユーザー設定など) は `RibbonStrings.current` に従い、
  切り替えると表示中のリボンも追従する。

## 拡張と操作の API

- **モデルの差し替え**: `ribbon.model = 別のモデル` で、タブ・QAT・タブ行の項目を新しいモデルで作り直す
  (古いモデルの購読は外れる)。
- **項目の表示の差し替え**: `ribbon.itemFactory` (`RibbonItemFactory`) が項目のモデルに対してコンポーネントを返すと、
  既定の表示の代わりにそれを置く (null を返した項目は既定の表示)。
- **スレッド**: モデルの変更の通知は `RibbonNotifications.dispatcher` を通して配る。リボンのビューを最初に作ったときに (未設定なら) UI スレッドへ
  配る口が入るので、バックグラウンドのスレッドでモデルを変えても、ビューとリスナーは UI スレッドで通知を受け取る。
- **コードからの操作** (テストやスクリプトから UI を動かす): `performClick(item)`、`openDropDown` / `closeDropDown`、
  `openGroupPopup` / `closeGroupPopup` (折りたたんだグループ)、`openSlideOut` / `setSlideOutPinned` (展開パネル)、
  `openDialogLauncher`、`groupState`、`overflowItems`、`pickGalleryItem` / `scrollGalleryRows`、`floatGroup` /
  `returnGroupToRibbon`、`showKeyTips` / `processKeyTipInput` / `currentKeyTips`。
- **検索の対象外**: 項目の `isSearchable = false` でコマンド検索に出さない。
- **QAT**: `model.showQuickAccessCustomizeButton` でカスタマイズ ボタンの表示、`ribbon.defaultQuickAccessPosition` で
  最初のロード時点の位置 (リセットの戻り先) を得る。
- **コンボボックスの確定**: `RibbonComboBoxModel.addCommitListener` は確定した項目と文字列の両方を知らせる。
- **単独で置けるコンポーネント**: `WRibbonIcon` (リボンのアイコン (グリフ・パス・多色のレイヤー付き線画) を任意の
  場所に描く)、`WRibbonColorPalette` (色の選択のパレットだけを置く。`showThemeColors` でテーマの色の欄を隠せる)。
- **テーマの値**: `WRibbonTheme.getBrushColor(scope, key)` (要素のテーマでのブラシの色)、
  `setThemeBrush(target, key) { ... }` (テーマが変わるたびに色を当て直す)、`getCornerRadius(key)`。
- **メニューのアイコン**: メニュー項目は単色のアイコンしか持てないため、線画・多色のアイコンは
  `WRibbonTheme.menuIconConverter` で近いグリフなどに変えて出す (CAD のサンプル)。

## WinUI の制約による RibbonSpace との違い

- winui4k は Size / Point / Rect などの浮動小数点の構造体を値で受け渡す呼び出しを使わない。そのため
  メニューを任意の位置に出すときは 1px の目印の Popup に添えて出し、要素の大きさは DesiredSize / ActualOffset から得る。
- ウィンドウの要素の子ではない Popup のライト ディスミスは入力を奪うため、外側のクリックで閉じるポップアップは
  ウィンドウ全体を覆う透明な層 (`RibbonDismissLayer`) で閉じる (`doc/troubleshooting.md`)。
- XAML のマークアップで組み立てる API (RibbonSpace の `<Ribbon>` 要素など) は持たず、モデルをコードで組み立てる。
- 独自の AutomationPeer (RibbonSpace のタブ見出しの TabItem 役割やグループのピアなど) は持たない。ピアを差し替えるには
  コントロールとピアの両方を COM 集約で合成して overrides を実装する必要があるため。代わりに AutomationProperties の
  名前・AutomationId (タブは `RibbonTab_{id}`、項目は id、アプリケーション メニューは `AppMenu_{id}`)・ヘルプ (説明)・
  アクセス キー (KeyTip) を付け、検索で強調した候補は RaiseNotificationEvent で読み上げさせる。

## サンプル

| サンプル | 内容 | 起動 |
|---|---|---|
| Gallery の [Ribbon] ページ | Word 風・AutoCAD 風のリボンと、表示・テーマ・UI の言語・状態の Options | `.\gradlew run` |
| `winui4k-sample-ribbon-word` | Word 風 (スタイル ギャラリー・書式を文書に反映・表 / 図のコンテキスト タブ・バックステージ・検索付きタイトル バー) | `.\gradlew :winui4k-sample-ribbon-word:run` |
| `winui4k-sample-ribbon-excel` | Excel 風 (コマンド ID で MVVM のビュー モデルにつなぐ・数式バー・シート・グラフ ツール) | `.\gradlew :winui4k-sample-ribbon-excel:run` |
| `winui4k-sample-ribbon-powerpoint` | PowerPoint 風 (シンプル表示・テーマのライブ プレビュー・画面切り替えのギャラリー) | `.\gradlew :winui4k-sample-ribbon-powerpoint:run` |
| `winui4k-sample-ribbon-cad` | AutoCAD 風 (線画のアイコン・ワークスペース・テキスト エディタ / ハッチング作成のコンテキスト タブ・図面・ビュー キューブ・コマンド ライン) | `.\gradlew :winui4k-sample-ribbon-cad:run` |
| `winui4k-sample-ribbon-tools` | リボンを使わないバー (メニュー バー・ツールに追従するオプション バー・最後の選択に追従する分割ボタン・ステータス バー) | `.\gradlew :winui4k-sample-ribbon-tools:run` |

`winui4k-sample-ribbon-shell` はこれらのデモに共通の外枠 (タイトル バー・ステータス バーの配置) である。
