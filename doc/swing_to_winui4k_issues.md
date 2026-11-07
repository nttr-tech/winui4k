# Swing から WinUI4K に移すときに気になるところ

[swing_to_winui4k.md](swing_to_winui4k.md) (Swing のコードを WinUI4K でどう書くかを並べた文書) を作る中で気づいたことを、2 つの一覧にまとめます。

1. [画面写真でおかしく見えるところ](#display): 文書に載せた画面写真を見て、Swing と比べて「おかしい」と感じる表示です。原因ごとに分け、それぞれ影響の大きい順に並べています。
2. [Swing から移るときに不自然な API](#api): Swing のコードを WinUI4K に書き換えるときに引っかかる API です。種類ごとに分け、それぞれ重いもの (コンパイルが通るのに黙って違う動きになるもの、例外になるもの、よく使う API) を先に置いて ★ を付けています。

最後に、[まとめ: 優先して直したいもの](#summary) を表にしています。

2026-11-07 に、1 の一覧のうち WinUI4K の不具合と見られた表示 21 件を直しました ([1.1](#display-bug))。あわせて、2 の一覧のうち同じ修正で解消した API には「直した」と書き、残る不自然さだけを残しています。直した後に、文書のサンプルから不具合を避けるコードを外し、画像を撮り直しました。そのため、1.2 以降の画像も直した後の姿です。

**どう調べたか**

- 文書の各章のサンプル ([`winui4k-sample-swing-to-winui4k`](../winui4k-sample-swing-to-winui4k) の `src/main/java/com/appkitbox/winui4k/swing_to_winui4k/<部品>/<ユースケース>/`) を動かして撮った画像 (`doc/images/swing_to_winui4k/` の全部) を、1 枚ずつ見ました。
- おかしく見えたものは、サンプルを少し変えた試しのコードを動かして、どんなときに起きるかを確かめました。試しのコードとその画像はリポジトリに入れていないので、「今の画像には写っていない」と書いたものがあります。
- API は WinUI4K のソースを読み、多くは試しのコードをコンパイル・実行して確かめました。ソースの行番号は、不具合を直す前の 2026-11-07 の作業ツリー (コミット `41397d06`) のものです。直したファイルでは行がずれています。

<a id="display"></a>
## 1. 画面写真でおかしく見えるところ

<a id="display-bug"></a>
### 1.1 直した不具合

前の版でこの節にまとめていた、WinUI4K の不具合と見られる表示 21 件は、2026-11-07 にすべて直しました。どれも WinUI4K の E2E テスト (`winui4k/src/test`) で、直した後の動きを確かめています。文書のサンプルからは、不具合を避けるために書いていたコード (`setWidth`・`WinUiUtilities.schedule(500, ...)`・`WLayoutPanel` の代わりの `WPanel` など) を外し、画像も撮り直しました。

| 番号 | 何が起きていたか | 直したコミット | 文書の節 |
|---|---|---|---|
| <a id="d1"></a>表示 1 | 入れ子にした `WLayoutPanel` (`WBorder` の中・`WScrollPane` の中身も) が子の大きさにならず、ボタンが消えたり重なったりした | `804e7a31` | [1.9](swing_to_winui4k.md#button-command)、[14.2](swing_to_winui4k.md#panel-nest)、[14.3](swing_to_winui4k.md#panel-border)、[14.4](swing_to_winui4k.md#panel-background)、[15.2](swing_to_winui4k.md#scroll-basic)、[15.4](swing_to_winui4k.md#scroll-position) |
| <a id="d2"></a>表示 2 | 表示する前の `setText` が、`addTextChangedListener` のリスナーに届かなかった | `e354832a` | [2.4](swing_to_winui4k.md#textfield-change)、[3.13](swing_to_winui4k.md#list-filter)、[19.6](swing_to_winui4k.md#table-sort) |
| <a id="d3"></a>表示 3 | 起動した直後から最初の部品にキーボードのフォーカスの太い枠が出た。ラジオボタンは選ばれていないボタン、木は最初の行、ツール バーは「…」に付いた | `b9a6151b` | [0.2](swing_to_winui4k.md#basics-differences)、[1.2](swing_to_winui4k.md#button-click)、[6.2](swing_to_winui4k.md#radio-group)、[20.3](swing_to_winui4k.md#tree-selection)、[23.2](swing_to_winui4k.md#toolbar-basic)、[28.3](swing_to_winui4k.md#colorchooser-embed) |
| <a id="d4"></a>表示 4 | 表示した後に `WLabel` の文字を長くすると、前の幅のまま切れた (空のラベルは何も出なかった) | `355623db` | [4.2](swing_to_winui4k.md#label-basic) |
| <a id="d5"></a>表示 5 | 幅を指定しないスライダー・進捗バー・進捗リング・スピナーが縮んだり消えたりした | `9ffda622` | [9.2](swing_to_winui4k.md#spinner-basic)、[10.2](swing_to_winui4k.md#slider-basic)、[11.2](swing_to_winui4k.md#progress-basic)、[11.7](swing_to_winui4k.md#progress-winui) |
| <a id="d6"></a>表示 6 | 画像のアイコン (`WImageIcon`) を付けたボタン・ラベルの文字が切れ、アイコンが小さく切り抜かれた | `c906b813`・`4c99b6ce` | [1.4](swing_to_winui4k.md#button-icon)、[4.4](swing_to_winui4k.md#label-icon) |
| <a id="d7"></a>表示 7 | 表示する前・表示した直後の操作 (リストのスクロール・表の選択・仕切りの位置・スクロールの位置・ダイアログ・メニュー) が画面に写らなかった | `3c075d1f`・`a343108e`・`9092016b`・`0c0c992d`・`e652273a`・`e065f680` | [3.7](swing_to_winui4k.md#list-select)、[19.3](swing_to_winui4k.md#table-model)、[19.5](swing_to_winui4k.md#table-selection)、[17.4](swing_to_winui4k.md#split-listener)、[15.4](swing_to_winui4k.md#scroll-position)、[21.2](swing_to_winui4k.md#menu-basic)、[25.2](swing_to_winui4k.md#dialog-basic) |
| <a id="d8"></a>表示 8 | 表示前に `setDividerLocation(0.7)` と `setResizeWeight(0.7)` を指定した仕切りが、7 割ではなく約 85% の位置に来た | `9092016b` | [17.3](swing_to_winui4k.md#split-vertical) |
| <a id="d9"></a>表示 9 | チェックボックスの文字の最後の単語が 2 行目に回って消えた | `6698d303` | [1.3](swing_to_winui4k.md#button-enabled) |
| <a id="d10"></a>表示 10 | 入力欄の `setForeground` が、フォーカスを持った欄の文字の色にならなかった | `16accf8e` | [12.6](swing_to_winui4k.md#textarea-appearance)、[2.14](swing_to_winui4k.md#textfield-appearance) |
| <a id="d11"></a>表示 11 | `setWrapStyleWord(true)` にすると折り返さなくなった | `ec8a1ea7` | [12.3](swing_to_winui4k.md#textarea-wrap) |
| <a id="d12"></a>表示 12 | 幅を決めない表の列が中身の幅で細く、右に空きが残り、並べ替えの印が列名に重なった | `7178e9a3` | [19.2](swing_to_winui4k.md#table-basic)、[19.6](swing_to_winui4k.md#table-sort) |
| <a id="d13"></a>表示 13 | `getColumnClass` が `Number` の列が右に寄らなかった | `6814a8be` | [19.4](swing_to_winui4k.md#table-columns) |
| <a id="d14"></a>表示 14 | 引き伸ばした `WLabel` の文字が縦の中央ではなく上に寄り、ツール バーに載せた部品も上に寄った | `02fbb386` | [14.4](swing_to_winui4k.md#panel-background)、[17.2](swing_to_winui4k.md#split-basic)、[18.3](swing_to_winui4k.md#tabbed-select)、[23.2](swing_to_winui4k.md#toolbar-basic)、[23.5](swing_to_winui4k.md#toolbar-component) |
| <a id="d15"></a>表示 15 | `showInputDialog` の入力欄が初めの文字の幅しかなく、`WContentDialog` の入力欄が中央に寄った | `d8a92279` | [26.4](swing_to_winui4k.md#optionpane-input)、[25.2](swing_to_winui4k.md#dialog-basic)、[25.3](swing_to_winui4k.md#dialog-validate)、[25.5](swing_to_winui4k.md#dialog-nonblocking) |
| <a id="d16"></a>表示 16 | ツール バーで選んだトグルボタンが、上の細い帯と下にはみ出したアイコンになった | `5dbd50fb` | [23.4](swing_to_winui4k.md#toolbar-toggle) |
| <a id="d17"></a>表示 17 | `setPaintTicks(true)` の目盛りがトラックの上に重なって見えなかった | `699193f8` | [10.3](swing_to_winui4k.md#slider-ticks) |
| <a id="d18"></a>表示 18 | 小数の値「12.0」が「12」と出て、表示の書式を決める API が無かった (`WSpinner.NumberEditor` を足した) | `4b70ad55` | [9.4](swing_to_winui4k.md#spinner-decimal) |
| <a id="d19"></a>表示 19 | スクロールペインの見出しに背景が無く、下へスクロールした中身が透けた | `fec2f999` | [15.6](swing_to_winui4k.md#scroll-header) |
| <a id="d20"></a>表示 20 | 行の高さに引き伸ばしたチェックボックスの、四角と文字の高さがずれた | `707be25c` | [14.2](swing_to_winui4k.md#panel-nest) |
| <a id="d21"></a>表示 21 | 小さなウィンドウ (480×320) では色を選ぶダイアログが切れ、入力欄まで出せなかった (中身を縦にスクロールできるようにした) | `2142540d` | [28.2](swing_to_winui4k.md#colorchooser-dialog) |

直していないもの・直した後も残る違いは、次のとおりです。

- 表示 3 の一部だった、コンボ ボックスの枠の内側の青緑の縦の棒は出なくなりました。カラーピッカーの色の名前のツールチップ (「青」) は、ウィンドウに置いたピッカーでは出なくなりましたが、`WColorPicker.showDialog` のダイアログでは、開いたときに色の面にフォーカスが入って出ます ([28.2](swing_to_winui4k.md#colorchooser-dialog))。
- 表示 7 のうち、ツールチップ・ツール バーの「…」のメニュー (`setOpen(true)`)・`TeachingTip` (`show()`)・ボタンの `setFlyout` で付けたフライアウト (`doClick()`) は、表示した直後に呼んでも出ないままです。サンプルは `WinUiUtilities.schedule(500, ...)` で待っています ([API 41](#a-after-show-menu))。
- 表示した直後のスクロールバーの `getMaximum()` と `getViewSize()` は、最初のレイアウトが済むまで 0 です。末尾へスクロールするサンプル ([16.2](swing_to_winui4k.md#scrollbar-bottom)・[16.5](swing_to_winui4k.md#scrollbar-loadmore)) は待ってから動かしています ([API 43](#a-after-show-position))。
- [17.4](swing_to_winui4k.md#split-listener) のラベルの前の位置は、Swing の「(was 0)」に対して「(was -1)」です。

<a id="display-winui"></a>
### 1.2 WinUI の仕様や既定のスタイルによる、Swing との違い

<a id="d22"></a>
#### 表示 22. 行が高く、見える項目が少ない

| Swing | WinUI4K |
|---|---|
| ![JList BasicSwing](images/swing_to_winui4k/JList/BasicSwing.png) | ![JList BasicWinUI4K](images/swing_to_winui4k/JList/BasicWinUI4K.png) |
| ![JList MultipleSwing](images/swing_to_winui4k/JList/MultipleSwing.png) | ![JList MultipleWinUI4K](images/swing_to_winui4k/JList/MultipleWinUI4K.png) |
| ![JTable EditSwing](images/swing_to_winui4k/JTable/EditSwing.png) | ![JTable EditWinUI4K](images/swing_to_winui4k/JTable/EditWinUI4K.png) |

- 何が変か: 高さ 150 DIP のリストに 5 項目あるのに 4 項目しか見えず (Cherry が欠ける)、スクロールバーも見えないので、隠れた項目があることが分かりません (JList の Delete・DoubleClick・Listener・Move・Popup・Selected・Select も同じ)。Multiple では 6 項目のうち 4 つしか見えず、Swing の画像で選ばれている notes.txt は見えない行にあります。表は 1 行が約 41 DIP (見出しも同じくらい) で、160 DIP の表に 3 行半しか見えません。
- Swing では: リストの行は約 27 ピクセル、表の行は 16 ピクセルで、同じ高さに 5〜6 項目が入り、溢れるとスクロールバーが見えます。
- 原因の見立て: WinUI の仕様 (ListViewItem の既定の高さは 40、TableView の Standard の密度)。表は `setDensity(TableDensity.COMPACT)` で詰められます。Swing と同じ高さのまま移すと、見える行がおよそ半分になります。文書の画像では「5 項目並べた」例が 4 項目に見えるので、サンプルの高さを大きめにする手もあります。
- 文書の節: [3.2](swing_to_winui4k.md#list-basic)、[3.6](swing_to_winui4k.md#list-multiple)、[3.10](swing_to_winui4k.md#list-rows)、[19.2](swing_to_winui4k.md#table-basic)

<a id="d23"></a>
#### 表示 23. スクロールバーが見えない (ALWAYS でも)

| Swing | WinUI4K |
|---|---|
| ![JScrollPane PolicySwing](images/swing_to_winui4k/JScrollPane/PolicySwing.png) | ![JScrollPane PolicyWinUI4K](images/swing_to_winui4k/JScrollPane/PolicyWinUI4K.png) |
| ![JList RowsSwing](images/swing_to_winui4k/JList/RowsSwing.png) | ![JList RowsWinUI4K](images/swing_to_winui4k/JList/RowsWinUI4K.png) |

- 何が変か: `VERTICAL_SCROLLBAR_ALWAYS` と、はみ出しのある `HORIZONTAL_SCROLLBAR_AS_NEEDED` を指定しても、縦にも横にもバーが写っていません。文字が右端で切れているだけで、横にスクロールできることが画面から分かりません。リストも 8 項目のうち 5 項目だけが見え、バーがありません (ほかの JScrollPane・JScrollBar の画像も同じ)。
- Swing では: 縦と横のバーがはっきり出ます。
- 原因の見立て: WinUI の仕様。スクロールバーはマウスを重ねたときだけ太く出ます (ScrollBarVisibility.Visible でも同じ)。撮るときはマウスを使わないので写りません。Swing から移った人には「ALWAYS が効いていない」ように見えます。
- 文書の節: [15.3](swing_to_winui4k.md#scroll-policy)、[3.10](swing_to_winui4k.md#list-rows)

<a id="d24"></a>
#### 表示 24. チェックボックス・ラジオボタンの間が広く、不ぞろい

| Swing | WinUI4K |
|---|---|
| ![JCheckBox SelectAllSwing](images/swing_to_winui4k/JCheckBox/SelectAllSwing.png) | ![JCheckBox SelectAllWinUI4K](images/swing_to_winui4k/JCheckBox/SelectAllWinUI4K.png) |
| ![JRadioButton TwoGroupsSwing](images/swing_to_winui4k/JRadioButton/TwoGroupsSwing.png) | ![JRadioButton TwoGroupsWinUI4K](images/swing_to_winui4k/JRadioButton/TwoGroupsWinUI4K.png) |
![JCheckBox IndeterminateWinUI4K](images/swing_to_winui4k/JCheckBox/IndeterminateWinUI4K.png)

- 何が変か: 「Apples」と「Bananas」、「Medium」と「Large」の間などに大きな空白があり、短い文字の項目ほど右の余白が大きいです。Swing では 1 行に収まる 4 項目が 2 行になり、2 行目の「Cherries」「Blue」がぽつんと中央に置かれます (JRadioButton の Group も同じ)。
- Swing では: 文字の幅に合わせて詰めて並び、1 行に収まります。
- 原因の見立て: WinUI の仕様。CheckBox と RadioButton の既定のスタイルは MinWidth が 120 です (microsoft-ui-xaml の `CheckBox_themeresources.xaml:273`、`RadioButton_themeresources.xaml:194`)。
- 文書の節: [5.4](swing_to_winui4k.md#checkbox-selectall)、[5.5](swing_to_winui4k.md#checkbox-winui)、[6.2](swing_to_winui4k.md#radio-group)、[6.6](swing_to_winui4k.md#radio-twogroups)

<a id="d25"></a>
#### 表示 25. 同じ列数でも、入力欄が Swing よりかなり広い

| Swing | WinUI4K |
|---|---|
| ![JTextArea BasicSwing](images/swing_to_winui4k/JTextArea/BasicSwing.png) | ![JTextArea BasicWinUI4K](images/swing_to_winui4k/JTextArea/BasicWinUI4K.png) |
| ![JLabel LabelForSwing](images/swing_to_winui4k/JLabel/LabelForSwing.png) | ![JLabel LabelForWinUI4K](images/swing_to_winui4k/JLabel/LabelForWinUI4K.png) |

- 何が変か: `new WTextPane(5, 30)` の欄がウィンドウ (480 DIP) の幅いっぱい近くまで広く (画素で約 595。Swing の `JTextArea(5, 30)` は約 405)、[12.3](swing_to_winui4k.md#textarea-wrap) の 20 列の欄も 2 つが横に並ばず縦に積まれます。`new WTextField(15)` は Swing の約 1.5 倍 (約 310 と約 205) の幅で、「Email:」のラベルが 1 行目の右端に残り、その入力欄だけが 2 行目に回って、どのラベルの欄か分かりにくくなります。[2.4](swing_to_winui4k.md#textfield-change) の Register ボタンも次の行に回ります。
- Swing では: 30 列の欄はボタンと横に並び、「Name: [ ] Email: [ ]」は 1 行に収まります。
- 原因の見立て: WinUI の仕様と WinUI4K の測り方。既定の文字が 14 DIP (Swing は 12) と大きく、列の幅を 'm' の幅で測るので、Swing より 1.4〜1.5 倍広くなります。Swing から同じ列数で移すと、並びが崩れやすいです。
- 文書の節: [12.2](swing_to_winui4k.md#textarea-basic)、[12.3](swing_to_winui4k.md#textarea-wrap)、[4.6](swing_to_winui4k.md#label-labelfor)、[2.4](swing_to_winui4k.md#textfield-change)

<a id="d26"></a>
#### 表示 26. 選んだ項目の色が薄く、見落としやすい

| Swing | WinUI4K |
|---|---|
| ![JList ListenerSwing](images/swing_to_winui4k/JList/ListenerSwing.png) | ![JList ListenerWinUI4K](images/swing_to_winui4k/JList/ListenerWinUI4K.png) |
| ![JTree SelectionSwing](images/swing_to_winui4k/JTree/SelectionSwing.png) | ![JTree SelectionWinUI4K](images/swing_to_winui4k/JTree/SelectionWinUI4K.png) |

- 何が変か: 選んだ項目・節は、薄い灰色の背景と左端の細いアクセントの色の線だけで示され、一目では分かりません (JList の Multiple・Move、JTree の Model も同じ)。
- Swing では: 濃い色の塗り (リストは水色、木は青) で示されます。
- 原因の見立て: WinUI の仕様 (Fluent の選択の見た目)。表では、選んだ行の色を変える API もありません ([`WTable.kt:60-62`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTable.kt#L60-L62))。
- 文書の節: [3.5](swing_to_winui4k.md#list-listener)、[3.6](swing_to_winui4k.md#list-multiple)、[20.3](swing_to_winui4k.md#tree-selection)

<a id="d27"></a>
#### 表示 27. 入力欄の見た目 (下の線・消去ボタン・読み取り専用・選択の色)

| Swing | WinUI4K |
|---|---|
| ![JTextField ReadOnlySwing](images/swing_to_winui4k/JTextField/ReadOnlySwing.png) | ![JTextField ReadOnlyWinUI4K](images/swing_to_winui4k/JTextField/ReadOnlyWinUI4K.png) |
| ![JTextField SelectAllSwing](images/swing_to_winui4k/JTextField/SelectAllSwing.png) | ![JTextField SelectAllWinUI4K](images/swing_to_winui4k/JTextField/SelectAllWinUI4K.png) |
| ![JTextPane RtfSwing](images/swing_to_winui4k/JTextPane/RtfSwing.png) | ![JTextPane RtfWinUI4K](images/swing_to_winui4k/JTextPane/RtfWinUI4K.png) |

- 何が変か:
  - 入力欄の下の端に、アクセントの色の太い線が出ます。中身があるときは右端に × (消去) のボタンが出ます。
  - 読み取り専用の欄が、ふつうの欄と同じ見た目です。
  - 選んだ範囲が濃いアクセントの色の塗りで、文字が白になります (JTextField の Caret も同じ)。
  - 同じ `WTextPane` でも、フォーカスのある欄は白、無い欄はわずかに灰色で、並べると「下の欄は無効?」と見えます (JTextArea の Wrap も同じ)。
- Swing では: 下の線も消去ボタンも無く、読み取り専用の欄は背景がやや灰色、選んだ範囲は薄い水色で黒い文字、欄の背景はどれも白です。
- 原因の見立て: WinUI の仕様 (TextBox・RichEditBox の既定のスタイル。TextControlBackground と TextControlBackgroundFocused の違い)。不具合ではありません。
- 文書の節: [2.7](swing_to_winui4k.md#textfield-readonly)、[2.9](swing_to_winui4k.md#textfield-selectall)、[2.11](swing_to_winui4k.md#textfield-caret)、[13.6](swing_to_winui4k.md#textpane-rtf)

<a id="d28"></a>
#### 表示 28. ツールバーのボタンが右に寄り、ラベルが出ず、「…」が出る

| Swing | WinUI4K |
|---|---|
| ![JToolBar BasicSwing](images/swing_to_winui4k/JToolBar/BasicSwing.png) | ![JToolBar BasicWinUI4K](images/swing_to_winui4k/JToolBar/BasicWinUI4K.png) |
![JToolBar OverflowWinUI4K](images/swing_to_winui4k/JToolBar/OverflowWinUI4K.png)

- 何が変か: `WCommandBar` に足したボタンが、バーの左ではなく右に寄って並びます。ボタンの間が広く (1 つが 70 前後)、`WAppBarButton("New", ...)` のラベルは出ず、アイコンだけです。右端に「…」ボタンが出ます。バーの高さも 48 ほどで、Swing (約 30) よりかなり高いです。「…」を開いたメニューは、ウィンドウの左端から右端まで広がり、下の「Ready」のラベルも隠します。
- Swing では: `JToolBar` はボタンを左から詰めて並べ、アイコンだけの小さなボタンになります。
- 原因の見立て: WinUI の CommandBar の仕様 (PrimaryCommands は右寄せ、DefaultLabelPosition が Bottom のラベルはバーを開いたときだけ出る、ラベルを見せるために「…」が出る、オーバーフローのメニューはバーの幅になる)。ただ、Swing から移す人には「おかしい」と感じる見た目です。`toolBar.setHorizontalAlignment(HorizontalAlignment.LEFT)` は、`WBorderLayout.NORTH` に置いたバーでは効きませんでした。
- 文書の節: [23.2](swing_to_winui4k.md#toolbar-basic)、[23.6](swing_to_winui4k.md#toolbar-winui)
- API の一覧: [API 50](#a-missing-table) (左に寄せる方法)

<a id="d29"></a>
#### 表示 29. ボタンから開いたフライアウトの先頭に、フォーカスの枠が出る

| Swing | WinUI4K |
|---|---|
| ![JPopupMenu ButtonSwing](images/swing_to_winui4k/JPopupMenu/ButtonSwing.png) | ![JPopupMenu ButtonWinUI4K](images/swing_to_winui4k/JPopupMenu/ButtonWinUI4K.png) |

- 何が変か: `export.doClick()` で開いたフライアウトの先頭の項目 (Export as PDF) に、黒い太い角丸の枠が付いています。
- Swing では: Metal のメニューは、開いただけでは項目が選ばれた見た目になりません。
- 原因の見立て: WinUI の仕様。ボタンをプログラムから押して開くと、キーボードで開いたのと同じ扱いになり、最初の項目にフォーカスが入って枠が出ます。前の版の画像では、メニュー バーのメニューや `popup.show` で開いたメニューにも枠が付き、Keys では「Ctrl+S」のツールチップも浮いていましたが、表示した直後の操作を並べた後で行うように直した後 (`e065f680`) の画像では、どちらも出ていません。
- 文書の節: [22.5](swing_to_winui4k.md#popup-button)

<a id="d30"></a>
#### 表示 30. ツールチップが部品の上に出て、タイトル バーを隠す

| Swing | WinUI4K |
|---|---|
| ![JToolTip ContentSwing](images/swing_to_winui4k/JToolTip/ContentSwing.png) | ![JToolTip ContentWinUI4K](images/swing_to_winui4k/JToolTip/ContentWinUI4K.png) |
| ![JToolTip ShowSwing](images/swing_to_winui4k/JToolTip/ShowSwing.png) | ![JToolTip ShowWinUI4K](images/swing_to_winui4k/JToolTip/ShowWinUI4K.png) |

- 何が変か: 何も指定しないと、ヒントがボタンや入力欄の上に出て、ウィンドウのタイトル バーに重なります。Content ではウィンドウの外 (上) まではみ出しています。
- Swing では: マウスの位置の下に出ます (この画像では部品の左上にマウスがあることにしたので、部品のすぐ下)。
- 原因の見立て: WinUI の仕様 (ToolTip の既定の Placement は上)。ウィンドウの一番上に並んだ部品では、「隠れて読めない」と感じそうです。
- 文書の節: [29.2](swing_to_winui4k.md#tooltip-placement)、[29.3](swing_to_winui4k.md#tooltip-content)、[29.4](swing_to_winui4k.md#tooltip-show)

<a id="d31"></a>
#### 表示 31. 進捗バーに割合の文字が出ない・不定のバーが短い線に見える

| Swing | WinUI4K |
|---|---|
| ![JProgressBar StringSwing](images/swing_to_winui4k/JProgressBar/StringSwing.png) | ![JProgressBar StringWinUI4K](images/swing_to_winui4k/JProgressBar/StringWinUI4K.png) |
| ![JProgressBar IndeterminateSwing](images/swing_to_winui4k/JProgressBar/IndeterminateSwing.png) | ![JProgressBar IndeterminateWinUI4K](images/swing_to_winui4k/JProgressBar/IndeterminateWinUI4K.png) |

- 何が変か: `setStringPainted(true)` でも、バーの上に「65%」が出ません (サンプルは横に `WLabel` を置いて出している)。不定の進捗バーは、背景の線 (トラック) が無く短い線が 1 本あるだけで、止まった画像では何の部品か分かりにくいです。
- Swing では: バーの中央に「65%」が出ます。不定のバーは、枠の付いた帯の中を塊が行き来します。
- 原因の見立て: WinUI の仕様 (ProgressBar には文字を描く部分が無い。不定の ProgressBar はトラックを描かない)。WinUI4K は割合の文字を UI オートメーションに出すだけです ([`ProgressRange.kt:76-78`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/ProgressRange.kt#L76-L78))。
- 文書の節: [11.4](swing_to_winui4k.md#progress-string)、[11.3](swing_to_winui4k.md#progress-indeterminate)
- API の一覧: [API 15](#a-painted)

<a id="d32"></a>
#### 表示 32. 範囲の外の値が勝手に書き換わる

| Swing | WinUI4K |
|---|---|
| ![JSpinner RangeSwing](images/swing_to_winui4k/JSpinner/RangeSwing.png) | ![JSpinner RangeWinUI4K](images/swing_to_winui4k/JSpinner/RangeWinUI4K.png) |

- 何が変か: 「From」を 10 にすると、「To」の値が 5 から 10 に変わります (最小を 10 にしただけで、値は変えていない)。
- Swing では: 「To」は 5 のまま (最小の 10 より小さい値を持ち続ける) です。
- 原因の見立て: WinUI の仕様 (NumberBox が値を範囲に切り詰める)。WinUI4K の説明 ([`WSpinner.kt:52-54`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WSpinner.kt#L52-L54)) にも書かれています。見た目としてはこちらのほうが自然ですが、Swing と動きが違います。
- 文書の節: [9.5](swing_to_winui4k.md#spinner-range)

<a id="d33"></a>
#### 表示 33. 木にアイコンが無い

| Swing | WinUI4K |
|---|---|
| ![JTree BasicSwing](images/swing_to_winui4k/JTree/BasicSwing.png) | ![JTree BasicWinUI4K](images/swing_to_winui4k/JTree/BasicWinUI4K.png) |

- 何が変か: フォルダーとファイルの区別が、開け閉めの印 (シェブロン) があるかどうかでしか分かりません。葉の節には何も付きません。
- Swing では: 既定でフォルダーと紙のアイコンが付きます。
- 原因の見立て: WinUI の仕様 (TreeView は節のアイコンを描かない)。WinUI4K の `WDefaultTreeCellRenderer` も、アイコンの既定は null です ([`WTreeCellRenderer.kt:35-46`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTreeCellRenderer.kt#L35-L46))。既定のレンダラーに `Symbol.FOLDER` / `Symbol.DOCUMENT` を入れておく案があります。
- 文書の節: [20.2](swing_to_winui4k.md#tree-basic)、[20.6](swing_to_winui4k.md#tree-renderer)

<a id="d34"></a>
#### 表示 34. 書式のある文の、段落の間が空かない

| Swing | WinUI4K |
|---|---|
| ![JTextPane HtmlSwing](images/swing_to_winui4k/JTextPane/HtmlSwing.png) | ![JTextPane HtmlWinUI4K](images/swing_to_winui4k/JTextPane/HtmlWinUI4K.png) |

- 何が変か: 2 つの段落がすき間なく続き、1 つの段落の中の改行のように見えます。
- Swing では: HTML の `<p>` なので、段落の間に 1 行ほどの余白があります。
- 原因の見立て: WinUI の仕様 (RichTextBlock の Paragraph の既定の余白は 0)。`WRichTextBlock` には段落の余白を決める API がありません。
- 文書の節: [13.7](swing_to_winui4k.md#textpane-html)

<a id="d35"></a>
#### 表示 35. ニーモニックの下線が出ない

| Swing | WinUI4K |
|---|---|
| ![JButton MnemonicSwing](images/swing_to_winui4k/JButton/MnemonicSwing.png) | ![JButton MnemonicWinUI4K](images/swing_to_winui4k/JButton/MnemonicWinUI4K.png) |
| ![JTabbedPane StateSwing](images/swing_to_winui4k/JTabbedPane/StateSwing.png) | ![JTabbedPane StateWinUI4K](images/swing_to_winui4k/JTabbedPane/StateWinUI4K.png) |

- 何が変か: `setMnemonic('S')` のボタンや `setMnemonicAt(1, VK_P)` のタブに、下線がありません。Swing の下線と比べると「何も起きていない」ように見えます。
- Swing では: 文字に下線が引かれます。
- 原因の見立て: WinUI の仕様 (アクセス キーは、Alt を押したときに出るヒントで示す)。WinUI4K の設計どおりです。
- 文書の節: [1.6](swing_to_winui4k.md#button-mnemonic)、[18.5](swing_to_winui4k.md#tabbed-state)

<a id="d36"></a>
#### 表示 36. WRelativePanel で互いを参照すると、ウィンドウが真っ黒になる

| Swing | WinUI4K |
|---|---|
| ![Layout SpringSwing](images/swing_to_winui4k/Layout/SpringSwing.png) | ![Layout SpringWinUI4K](images/swing_to_winui4k/Layout/SpringWinUI4K.png) |

- 画像: 今の `SpringWinUI4K.png` は、サンプルを直した後のものです。
- 何が変か: `panel.placeRightOf(field, label)` と `panel.alignVerticalCenterWith(label, field)` を両方書くと、ウィンドウの中が真っ黒で何も出ませんでした (コンソールには `RelativePanel error: Circular dependency detected` の未処理の例外)。`panel.alignVerticalCenterWith(field, label)` (入力欄をラベルの縦の中央に揃える) にすると、入力欄の下半分が切れました。
- Swing では: `SpringLayout` は向きごとに別の制約なので、互いを参照しても、BASELINE を揃えても問題なく並びます。
- 原因の見立て: WinUI の仕様 (RelativePanel は部品の単位で依存を見るので、横と縦の別の向きでも互いを参照すると循環になる。揃える相手の高さを、揃える部品の使える高さにする)。ただ、WinUI4K は例外を UI スレッドに投げるだけで、`placeRightOf` などを呼んだ場所では分かりません。サンプルはラベルの上の余白で合わせる形にしました。
- 文書の節: [30.7](swing_to_winui4k.md#layout-spring)
- API の一覧: [API 49](#a-relativepanel)

<a id="d37"></a>
#### 表示 37. そのほかの WinUI との違い (小さいもの)

| 画像 | 何が変か | 原因の見立て | 文書の節 |
|---|---|---|---|
| ![Layout BorderWinUI4K](images/swing_to_winui4k/Layout/BorderWinUI4K.png) | 隙間 0 の `WBorderLayout` なのに、ボタンの間に 2〜3 DIP の隙間があるように見える (位置を測ると隙間は無い) | WinUI のボタンは角が丸く、縁の線が背景より明るい | [30.3](swing_to_winui4k.md#layout-border) |
| ![JSplitPane SplitViewWinUI4K](images/swing_to_winui4k/JSplitPane/SplitViewWinUI4K.png) | 開いたペインと中身の領域が同じ背景で、境目が分からない | SplitView のペインの既定の背景が Mica のウィンドウとほぼ同じ色 | [17.6](swing_to_winui4k.md#split-winui) |
| ![JPopupMenu CommandBarFlyoutWinUI4K](images/swing_to_winui4k/JPopupMenu/CommandBarFlyoutWinUI4K.png) | 下の段が開いているのに、上の段の右端に「…」が出る。`setPlacement` を指定しないとラベルの上に開いてタイトル バーを隠した (サンプルは下に開くよう直した) | 「…」は下の段を開け閉めするボタン。既定の Placement は上 | [22.6](swing_to_winui4k.md#popup-winui) |
| ![JDialog ValidateWinUI4K](images/swing_to_winui4k/JDialog/ValidateWinUI4K.png) | `setText("report.txt")` した入力欄のキャレットが先頭にある (Swing は末尾) | TextBox は Text を入れてもキャレットが 0 のまま。`setText` で末尾に動かすかは判断が要る | [25.3](swing_to_winui4k.md#dialog-validate) |
| ![JFrame TitleBarWinUI4K](images/swing_to_winui4k/JFrame/TitleBarWinUI4K.png) | 幅 520 で撮ったときは、`WTitleBar` の見出しと副題が出ず、検索の欄だけが出た (今の画像は幅 720) | TitleBar は中身が入りきらないと Compact になって見出しを隠す (microsoft-ui-xaml の `TitleBar.cpp:174-178`) | [24.7](swing_to_winui4k.md#frame-winui) |

<a id="display-sample"></a>
### 1.3 サンプルや撮り方によるもの

<a id="d38"></a>
#### 表示 38. ファイルのダイアログがとても大きく、左の一覧に撮った人のフォルダーが写る (ぼかした)

| Swing | WinUI4K |
|---|---|
| ![JFileChooser OpenSwing](images/swing_to_winui4k/JFileChooser/OpenSwing.png) | ![JFileChooser OpenWinUI4K](images/swing_to_winui4k/JFileChooser/OpenWinUI4K.png) |

- 何が変か: WinUI4K の画像は約 1780×1070 ピクセルで、Swing (約 750×535) に比べてとても大きく、並べると文字が小さく見えます。また、左のナビゲーション ウィンドウには「OneDrive - Personal」と、クイック アクセスに固定したフォルダーの名前 (撮った PC の利用者のもの) が出るので、`capture.ps1` がその部分をぼかして保存しています (JFileChooser の WinUI4K の 5 枚すべて)。
- Swing では: Metal の `JFileChooser` には左の一覧が無く、大きさも既定のままです。
- 原因の見立て: OS のダイアログの仕様。前に使ったときの大きさを覚えていて、WinUI4K からは大きさを指定できません。大きさは、撮る前に一度ダイアログを小さくして閉じておけばそろいます。
- 文書の節: [27.2](swing_to_winui4k.md#filechooser-open)、[27.3](swing_to_winui4k.md#filechooser-filter)、[27.4](swing_to_winui4k.md#filechooser-save)、[27.5](swing_to_winui4k.md#filechooser-folder)、[27.6](swing_to_winui4k.md#filechooser-multiple)

<a id="d39"></a>
#### 表示 39. 起動した直後に撮るので、ツールチップ・メニュー・ヒントが写らない

| Swing | WinUI4K |
|---|---|
| ![JButton ToolTipSwing](images/swing_to_winui4k/JButton/ToolTipSwing.png) | ![JButton ToolTipWinUI4K](images/swing_to_winui4k/JButton/ToolTipWinUI4K.png) |
| ![JButton ActionSwing](images/swing_to_winui4k/JButton/ActionSwing.png) | ![JButton ActionWinUI4K](images/swing_to_winui4k/JButton/ActionWinUI4K.png) |

- 何が変か: ToolTip の画像はただのボタンで、何の例か分かりません。Action は保存ボタンが無効になるだけで、同じ Action を使うメニューの項目は写りません (Swing 側のアイコンは無効のぼかしが強く、粗く見える)。Mnemonic の WinUI4K は Alt のヒントが出ないので、何も起きていないように見えます ([表示 35](#d35))。
- 原因の見立て: 起動した直後を撮る撮り方の限界。ほかの章のように、サンプルが起動時にヒントやメニューを開くか、キーを送って撮る形にするとよいです。
- 文書の節: [1.5](swing_to_winui4k.md#button-tooltip)、[1.8](swing_to_winui4k.md#button-action)、[1.6](swing_to_winui4k.md#button-mnemonic)

<a id="d40"></a>
#### 表示 40. ポップアップの角に黒い点・縁が残る

| Swing | WinUI4K |
|---|---|
| ![JToolTip PlacementSwing](images/swing_to_winui4k/JToolTip/PlacementSwing.png) | ![JToolTip PlacementWinUI4K](images/swing_to_winui4k/JToolTip/PlacementWinUI4K.png) |

- 何が変か: 角の丸いヒントの四隅に、小さな黒い点が見えます (JToolTip の Content・Show も同じ)。メニューの角の丸みの外側にも、うっすら黒っぽい縁が残ることがあります。
- 原因の見立て: 撮り方の都合で、実際の画面では出ません。ツールチップやメニューは透明な部分を持つ別のウィンドウで、PrintWindow で撮ると透明な部分が黒になります。真っ黒の画素を透明にして重ねましたが、角の半透明の画素が黒っぽく残りました。
- 文書の節: [29.2](swing_to_winui4k.md#tooltip-placement)、[21.2](swing_to_winui4k.md#menu-basic)

<a id="d41"></a>
#### 表示 41. Swing と WinUI4K でサンプルがそろっていない・余白が無い

| 画像 | 何が変か | 原因の見立て | 文書の節 |
|---|---|---|---|
| ![JTextField PasswordSwing](images/swing_to_winui4k/JTextField/PasswordSwing.png) ![JTextField PasswordWinUI4K](images/swing_to_winui4k/JTextField/PasswordWinUI4K.png) | WinUI4K だけ「Password」の見出しが付く。Swing は何の欄か分からない | WinUI4K 側だけ `setHeader` を呼んでいる。Swing 側にラベルを足すとそろう | [2.15](swing_to_winui4k.md#textfield-password) |
| ![JTextField PlaceholderSwing](images/swing_to_winui4k/JTextField/PlaceholderSwing.png) ![JTextField PlaceholderWinUI4K](images/swing_to_winui4k/JTextField/PlaceholderWinUI4K.png) | WinUI4K の欄の幅が案内文「e.g. John Smith」ぎりぎりで、右に余白がほとんど無い (Swing は 20 列の広い欄) | `new WTextField("...")` は案内文で、列数を同時に渡せない ([API 1](#a-placeholder)) | [2.8](swing_to_winui4k.md#textfield-placeholder) |
| ![JList RendererSwing](images/swing_to_winui4k/JList/RendererSwing.png) ![JList RendererWinUI4K](images/swing_to_winui4k/JList/RendererWinUI4K.png) | 見出しの文字の太さと、アイコンの形 (Swing は塗りつぶしの人、WinUI4K は線の人) がそろわない | Swing 側の png を `Symbol.CONTACT` に合わせる手もある | [3.9](swing_to_winui4k.md#list-renderer) |
| ![JList SelectedWinUI4K](images/swing_to_winui4k/JList/SelectedWinUI4K.png) | リストの下のラベルが、4 つ目の項目の真下に付いて、項目の続きに見える (Delete も同じ) | リストの高さを決め、`WFlowLayout` の縦の隙間だけで並べている。Swing は `JScrollPane` の枠がある | [3.4](swing_to_winui4k.md#list-selected) |
| ![JList ItemClickWinUI4K](images/swing_to_winui4k/JList/ItemClickWinUI4K.png) | 2 つのリストが間を置かずに並び、区別が無い。チェックボックスのほうは何も選ばれていない | 起動時に選択を 1 つ入れ、間を空けるとよい | [3.15](swing_to_winui4k.md#list-winui) |
| ![JFrame TitleBarWinUI4K](images/swing_to_winui4k/JFrame/TitleBarWinUI4K.png) | 本文のラベルがウィンドウの左端に余白なしで接している | `setContentPane(new WGrid())` にすると、既定の中身の余白 (24) が無くなる | [24.7](swing_to_winui4k.md#frame-winui) |
| ![JFrame MenuBarWinUI4K](images/swing_to_winui4k/JFrame/MenuBarWinUI4K.png) | 状態の行 (「Ready」) が左下の角に余白なしで置かれる | `BorderLayout` の仕様どおりで、Swing も同じ | [24.5](swing_to_winui4k.md#frame-menubar) |
| ![JOptionPane ConfirmSwing](images/swing_to_winui4k/JOptionPane/ConfirmSwing.png) ![JOptionPane ConfirmWinUI4K](images/swing_to_winui4k/JOptionPane/ConfirmWinUI4K.png) | コードの文字は英語なのに、ボタンが日本語 (Swing は「はい(Y)」「取消」、WinUI4K は「はい」「キャンセル」) | 両方とも既定のロケール (日本語) に従う。WinUI4K は「(Y)」を付けない | [26.3](swing_to_winui4k.md#optionpane-confirm) |

<a id="display-swing"></a>
### 1.4 Swing 側の画像で気になるもの

| 画像 | 何が気になるか | 原因の見立て | 文書の節 |
|---|---|---|---|
| ![JButton ClickSwing](images/swing_to_winui4k/JButton/ClickSwing.png) | Swing の画像は文字がすべて太字で、ボタンは立体のグラデーション。同じ文字列でも WinUI4K と幅が違う | Metal の既定 (「Dialog」の太字)。比べるときに「Swing は太字」と分かっていると誤解が無い | [「この文書の読み方」](swing_to_winui4k.md#reading) |
| ![JButton AppearanceSwing](images/swing_to_winui4k/JButton/AppearanceSwing.png) | 赤い Delete ボタンの右端に、細い別の色の帯が出る | 背景の色を変えた Metal のボタンの描き方 | [1.11](swing_to_winui4k.md#button-appearance) |
| ![JButton DefaultButtonSwing](images/swing_to_winui4k/JButton/DefaultButtonSwing.png) | 既定ボタンの OK は枠が少し濃いだけで、既定とは分かりにくい (WinUI4K はアクセントの色) | Metal の仕様 | [1.7](swing_to_winui4k.md#button-default) |
| ![JTextPane HtmlSwing](images/swing_to_winui4k/JTextPane/HtmlSwing.png) | JEditorPane の HTML だけ、明朝体 (セリフ体) になる | HTMLEditorKit の既定のスタイルシートが serif | [13.7](swing_to_winui4k.md#textpane-html) |
| ![JTree ModelSwing](images/swing_to_winui4k/JTree/ModelSwing.png) | 「New folder」で足した Folder 2 と、子の無い Personal が紙 (葉) のアイコンで出る (WinUI4K は開け閉めの印が出ない) | `new DefaultTreeModel(root)` は子の無い節を葉にする。`new DefaultTreeModel(root, true)` にすればフォルダーになる | [20.5](swing_to_winui4k.md#tree-model) |
| ![JDialog CloseOperationSwing](images/swing_to_winui4k/JDialog/CloseOperationSwing.png) | 閉じようとした後のメッセージが「Click Save or Discard to cl...」で切れる (WinUI4K は全文) | `pack()` で短い文字に合わせた大きさのまま、長い文字に替えた | [25.4](swing_to_winui4k.md#dialog-close) |
| ![JLabel WrapSwing](images/swing_to_winui4k/JLabel/WrapSwing.png) | HTML の `width: 250px` の折り返しが、WinUI4K の `setWidth(250)` より広い (画素で約 450 と約 370) | HTML の px の扱いの違い。異常ではない | [4.7](swing_to_winui4k.md#label-wrap) |

<a id="api"></a>
## 2. Swing から移るときに不自然な API

ソースへのリンクは `winui4k/src/main/kotlin/com/appkitbox/winui4k/` の下のファイルです。★ は重いもの (コンパイルが通るのに黙って違う動きになるもの、例外になるもの、よく使う API) です。

<a id="api-meaning"></a>
### 2.1 Swing と同じ名前で、意味や振る舞いが違う

<a id="a-placeholder"></a>
#### API 1. ★ `new WTextField("…")` / `new WTextPane("…")` の文字列が案内文になる

- Swing: `new JTextField("Tokyo")`・`new JTextField("12-345", 10)`・`new JTextArea("Editor")` は、初めの文字列を入れる。
- WinUI4K: `WTextField(placeholder: String)` ([`WTextField.kt:38`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextField.kt#L38))・`WTextField(columns: Int)` ([`WTextField.kt:42`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextField.kt#L42))・`WTextPane(placeholder: String)` ([`WTextPane.kt:43`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextPane.kt#L43))。`WTextPane(text, rows, columns)` ([`WTextPane.kt:53`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextPane.kt#L53)) だけは文字列が本文。`WTextField(String, int)` は無い。
- 何が不自然か: 同じ形のコンストラクターで意味が違い、コンパイルが通るので、欄が空のまま薄い文字が出ることでしか気づけません。`WTextPane` では、同じ `String` が引数 1 つなら案内文、3 つなら本文です。列数と文字列を一度に渡せないので、`new JTextField("12-345", 10)` を書き換えると幅が決まりません。
- 直す案: 1 つの `String` は本文にし (Swing と同じ)、案内文は `setPlaceholderText` だけにする (大きな版の変更で)。少なくとも `WTextField(String text, int columns)` を足す。
- 文書の節: [2.8](swing_to_winui4k.md#textfield-placeholder)、[2.17](swing_to_winui4k.md#textfield-notes)、[12.2](swing_to_winui4k.md#textarea-basic)、[29.4](swing_to_winui4k.md#tooltip-show)

<a id="a-alignment"></a>
#### API 2. ★ `setHorizontalAlignment` / `setVerticalAlignment` が文字の揃えではない

- Swing: `JTextField.setHorizontalAlignment(JTextField.RIGHT)`・`JLabel.setHorizontalAlignment(SwingConstants.RIGHT)`・`JLabel.setVerticalAlignment(SwingConstants.TOP)` は、部品の中の文字の揃え。
- WinUI4K: 同じ名前の `horizontalAlignment` ([`WComponent.kt:1026`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComponent.kt#L1026))・`verticalAlignment` ([`WComponent.kt:1036`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComponent.kt#L1036)) は、親の中での部品そのものの置き場所。文字の揃えは `WTextField.textAlignment` ([`WTextField.kt:205`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextField.kt#L205))・`WLabel.textAlignment` ([`WLabel.kt:412`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WLabel.kt#L412))。ラベルの中の縦の揃えは無い。
- 何が不自然か: `SwingConstants.RIGHT` を `HorizontalAlignment.RIGHT` に置き換えるだけの書き換えでコンパイルが通り、文字ではなく部品が右に寄ります (幅を決めた部品では何も変わらない) ので、気づきにくいです。
- 直す案: `WTextField`・`WLabel` に Swing と同じ意味の `setHorizontalAlignment(int)` (Swing の定数の値) を足すか、少なくとも Javadoc と文書で強く案内する。ラベルの中の縦の揃え (`textVerticalAlignment` など) を足す。
- 文書の節: [2.14](swing_to_winui4k.md#textfield-appearance)、[2.17](swing_to_winui4k.md#textfield-notes)、[4.5](swing_to_winui4k.md#label-alignment)

<a id="a-contentpane"></a>
#### API 3. ★ `setLayout` の後の `getContentPane().add(...)` が画面に出ない

- Swing: `frame.setLayout(l)` は中身のレイアウトを替えるだけで、`frame.getContentPane()` はそのまま使える (`SpringLayout.putConstraint(..., frame.getContentPane())` もよくある)。
- WinUI4K: `WFrame.layout` の setter ([`WFrame.kt:703-715`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L703-L715)) は、中身を新しい `WLayoutPanel` に差し替える。`contentPane` ([`WFrame.kt:65`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L65)) は型を `WPanel` に保つため、画面から外れた古いパネルを返し続ける。
- 何が不自然か: `frame.setLayout(...)` の後に `frame.getContentPane().add(c)` と書くと、例外も出ずに、画面に出ないパネルに入ります。KDoc には書いてありますが、Swing のよくある書き方がそのまま壊れます。
- 直す案: `contentPane` を「今の中身」を返す `WContainer` 型にする。型を変えられないなら、差し替えた後の `contentPane` への `add` を今の中身へ回すか、例外にする。
- 文書の節: [24.1](swing_to_winui4k.md#frame-table)、[30.7](swing_to_winui4k.md#layout-spring)

<a id="a-margin"></a>
#### API 4. ★ `setMargin` が外側の余白

- Swing: `AbstractButton.setMargin(Insets)`・`JTextComponent.setMargin(Insets)` は、部品の内側の余白。
- WinUI4K: `WComponent.setMargin(left, top, right, bottom)` ([`WComponent.kt:1053`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComponent.kt#L1053)) は外側の余白。内側は `WControl.setPadding(left, top, right, bottom)` ([`WControl.kt:51`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WControl.kt#L51))。
- 何が不自然か: 同じ名前で、内と外が逆です (Swing の margin は内側、WinUI の Margin は外側)。そのうえ 4 つの数の順も Swing の `Insets` と違います ([API 29](#a-order))。
- 直す案: `WButtonBase`・`WTextField` に `setMargin(WInsets)` を足して内側の余白 (Padding) にするなど、Swing と同じ意味の名前を優先する。
- 文書の節: [1.1](swing_to_winui4k.md#button-table)、[14.5](swing_to_winui4k.md#panel-padding)

<a id="a-combo-remove"></a>
#### API 5. ★ `WComboBox.removeItem(1)` が 2 番目の項目を消す

- Swing: `removeItem(Object)` は等しい項目を消し、位置で消すのは `removeItemAt(int)`。
- WinUI4K: `removeItem(index: Int)` ([`WComboBox.kt:344`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L344)) と `removeItem(item: Any?)` ([`WComboBox.kt:354`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L354)) の両方がある。
- 何が不自然か: Java の `combo.removeItem(1)` は、Swing では Integer の 1 に等しい項目を消しますが、WinUI4K では `int` の形が選ばれて 2 番目の項目を消します。数の項目を並べたコンボで、黙って違う項目が消えます。
- 直す案: `removeItem(Int)` を外して `removeItemAt` にまとめる。
- 文書の節: [8.4](swing_to_winui4k.md#combo-model)

<a id="a-combo-selected"></a>
#### API 6. ★ `WComboBox.getSelectedItem()` が項目ではなく表示の文字列を返す

- Swing: `Object getSelectedItem()` はモデルの項目そのもの。
- WinUI4K: `val selectedItem: String?` ([`WComboBox.kt:229-230`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L229-L230)。`dataModel.selectedItem?.toString()`)。`getItemAt(i)` も表示の文字列 ([`WComboBox.kt:341`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L341))。
- 何が不自然か: 独自のオブジェクトを並べたとき、`(Country) combo.getSelectedItem()` がコンパイルできません ([8.6](swing_to_winui4k.md#combo-renderer) では `model.getSelectedItem()` に書き換えた)。文字列の項目なら黙って動くので、型が違うことに気づくのが遅れます。
- 直す案: `getSelectedItem()` は `Any?` でモデルの項目を返し、表示の文字列は `getSelectedText()` のような別の名前にする。
- 文書の節: [8.6](swing_to_winui4k.md#combo-renderer)

<a id="a-selected-text"></a>
#### API 7. ★ `WTextField.getSelectedText()` が、選んでいないときに空の文字列を返す

- Swing: `JTextComponent.getSelectedText()` は、選んでいないと `null`。
- WinUI4K: `selectedText: String` ([`WTextField.kt:214-216`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextField.kt#L214-L216))。説明にも「null ではない」とある。
- 何が不自然か: メニューを開くときに「Cut」「Copy」を有効にするかを `field.getSelectedText() != null` で決める、よくあるコードをそのまま移すと、いつも有効になります。コンパイルも通ります ([21.6](swing_to_winui4k.md#menu-listener) のサンプルは `!field.getSelectedText().isEmpty()` に書き換えた)。
- 直す案: Swing に合わせて、選んでいないときは `null` を返す (`String?`)。
- 文書の節: [21.6](swing_to_winui4k.md#menu-listener)

<a id="a-color"></a>
#### API 8. ★ `WColor.RED` などの値が AWT の `Color` と違う

- Swing: `Color.RED` = (255, 0, 0)、`Color.BLUE` = (0, 0, 255)、`Color.ORANGE` = (255, 200, 0)。
- WinUI4K: `WColor.RED` = (237, 28, 36) ([`WColor.kt:47`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WColor.kt#L47))、`WColor.BLUE` = (0, 120, 215) ([`WColor.kt:53`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WColor.kt#L53))、`WColor.ORANGE` = (255, 127, 39) ([`WColor.kt:59`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WColor.kt#L59))。
- 何が不自然か: 同じ名前で値が違うので、`Color.RED` を `WColor.RED` に置き換えると色が変わります。色を比べるコードや、画素を確かめるテストの結果も変わります。
- 直す案: AWT と同じ値にする。違う色にしたいなら、別の名前にする。
- 文書の節: [4.3](swing_to_winui4k.md#label-font)、[28.3](swing_to_winui4k.md#colorchooser-embed)

<a id="a-html"></a>
#### API 9. ★ `<html>` の文字が、そのまま表示される

- Swing: `new JLabel("<html>...<br>...</html>")`・`new JButton("<html>...")`・`setToolTipText("<html><b>Save</b><br>...")` で、何行かの文字・太字・色。
- WinUI4K: `WLabel`・`WButton`・`toolTipText` は文字をそのまま出す。何行かは `"\n"` や `textWrapping`、書式は `WRichTextBlock` や `WToolTip.content` ([`WToolTip.kt:88`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WToolTip.kt#L88)) に部品を組んで渡す。
- 何が不自然か: Swing で多く使う書き方なのに、例外にもならず、画面に `<html>` のタグがそのまま出ます。
- 直す案: `<html>` で始まる文字を受けたら、タグを取り除いて `<br>` を改行にする程度の救済をするか、WinUI の Run・Bold に変える簡単な対応をする。少なくとも警告を出す。
- 文書の節: [1.16](swing_to_winui4k.md#button-notes)、[4.7](swing_to_winui4k.md#label-wrap)、[29.3](swing_to_winui4k.md#tooltip-content)

<a id="a-wrapstyleword"></a>
#### API 10. `setWrapStyleWord(true)` で折り返さなくなる (直した: `ec8a1ea7`)

- 直した後: `wrapStyleWord` は値を覚え、RichEditBox には `WRAP` を入れるので、Swing と同じく `setLineWrap(true); setWrapStyleWord(true);` で単語の切れ目で折り返します ([表示 11](#d11))。
- 残る違い: WinUI の折り返しははじめから単語の切れ目なので、`setWrapStyleWord(false)` でも単語の切れ目で折り返します (Swing の文字の切れ目での折り返しはできない)。
- 文書の節: [12.3](swing_to_winui4k.md#textarea-wrap)

<a id="a-foreground"></a>
#### API 11. `setForeground` が入力欄の文字の色にならない (直した: `16accf8e`)

- 直した後: 入力欄 (`WTextField`・`WPasswordField`・`WTextPane`・`WAutoSuggestBox`・`WSpinner`) では、`background` と同じく TextControlForeground / ...PointerOver / ...Focused もリソースに入れるので、フォーカスを持った欄の文字もその色になります ([表示 10](#d10))。無効の見た目はテーマの既定のままです。
- 文書の節: [12.6](swing_to_winui4k.md#textarea-appearance)、[2.14](swing_to_winui4k.md#textfield-appearance)

<a id="a-table-selection"></a>
#### API 12. ★ `WTable.setSelectionMode(MULTIPLE_INTERVAL_SELECTION)` が黙って単一の選択になる

- Swing: `JTable` の既定は複数の区間の選択で、`getSelectedRows()` で何行も取れる。列やセルの選択もある。
- WinUI4K: `setSelectionMode(int)` ([`WTable.kt:393-401`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTable.kt#L393-L401)) は値を確かめた後、いつも `SINGLE_SELECTION` にする。`selectedRows` は 0 個か 1 個 ([`WTable.kt:347`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTable.kt#L347))。`columnSelectionAllowed` / `cellSelectionEnabled` は何もしない ([`WTable.kt:459-466`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTable.kt#L459-L466))。
- 何が不自然か: 例外も出ずに単一になるので、「選んだ行をまとめて消す」などのコードが、黙って 1 行だけを処理します。
- 直す案: (TableView の制約なので) MULTIPLE を渡されたら警告を出すか、Javadoc と文書で強調する。TableView が複数の選択に対応したら写す。
- 文書の節: [19.5](swing_to_winui4k.md#table-selection)

<a id="a-exception-settings"></a>
#### API 13. ★ Swing では使える設定が、例外になる

| Swing | WinUI4K | 何が起きるか | 直す案 | 文書の節 |
|---|---|---|---|---|
| `new JSpinner(new SpinnerListModel(...))`・`SpinnerDateModel` | `WSpinner.model` ([`WSpinner.kt:105`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WSpinner.kt#L105)) | `WSpinnerListModel` / `WSpinnerDateModel` のクラスはあるのに、渡すと `IllegalArgumentException` | 一覧と日時を表示できるようにする。それまでは、受け取る型を `WSpinnerNumberModel` にしてコンパイルで分かるようにする | [9.1](swing_to_winui4k.md#spinner-table) |
| `new JDialog(frame, "Find", false)` (モードレス) | `WContentDialog(owner, title, modal)` ([`WContentDialog.kt:94-95`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WContentDialog.kt#L94-L95)) | `modal = false` は `IllegalArgumentException` (ContentDialog はモーダルだけ) | 持ち主付きの `WFrame` (AppWindow の owner) を作れる `WDialog` を足す | [25.5](swing_to_winui4k.md#dialog-nonblocking) |
| `new JToolBar(JToolBar.VERTICAL)` | `WCommandBar.orientation` ([`WCommandBar.kt:266`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WCommandBar.kt#L266)) | `IllegalArgumentException`。実行して初めて分かる | 縦に並べる代わりの書き方 (`WPanel` に `WButton`) を案内する | [23.1](swing_to_winui4k.md#toolbar-table) |

<a id="a-radio"></a>
#### API 14. `WButtonGroup` に入れないラジオボタンも、1 つしか選べない

- Swing: `ButtonGroup` に入れない `JRadioButton` は、それぞれ別にオン・オフできる。
- WinUI4K: `WRadioButton` は WinUI の既定のまま、同じ親の中で自動で 1 つのグループになる ([`WRadioButton.kt:12`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WRadioButton.kt#L12))。両方に `true` を渡して作っても、片方が外れた (実行で確かめた)。
- 何が不自然か: ラジオボタンを独立したオン・オフに使うコード (まれ) や、`ButtonGroup` を忘れたコードの動きが変わります。
- 直す案: 作るときに一意の `groupName` を付けて独立させ、`WButtonGroup` に入れたときだけまとめる (Swing と同じ)。
- 文書の節: [6.2](swing_to_winui4k.md#radio-group)

<a id="a-painted"></a>
#### API 15. `setStringPainted(true)` が Swing のように描かない (`setPaintTicks(true)` は直した: `699193f8`)

- Swing: `JProgressBar.setStringPainted(true)` でバーの上に「65%」。
- WinUI4K: `isStringPainted` ([`WProgressBar.kt:99`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WProgressBar.kt#L99)) は UI オートメーションに文字を入れるだけ ([`ProgressRange.kt:76-78`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/ProgressRange.kt#L76-L78))。
- 何が不自然か: 同じ名前の設定でコンパイルが通り、画面に何も出ません ([表示 31](#d31))。
- 直す案: バーの上か横に TextBlock を重ねて描く (描かないなら名前を変える)。
- 直したもの: `WSlider` の目盛りの位置の既定を NONE にし、`paintTicks` の既定を false、`setPaintTicks(true)` でトラックの下 (BOTTOM_RIGHT) に描くようにしました (Swing と同じ。[表示 17](#d17))。
- 文書の節: [11.4](swing_to_winui4k.md#progress-string)、[10.3](swing_to_winui4k.md#slider-ticks)

<a id="a-inputmap"></a>
#### API 16. `list.getInputMap()` (WHEN_FOCUSED) に入れたキーが効かない

- Swing: `list.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), ...)` で Delete キーが効く。
- WinUI4K: WinUI のリストではフォーカスが項目にあるので、`getInputMap(WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)` ([`WComponent.kt:851`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComponent.kt#L851)) に入れないと効かない。
- 何が不自然か: Swing と同じ書き方で、黙って動きません。
- 直す案: `WList` の WHEN_FOCUSED の登録も、項目にフォーカスがあるときに効かせる。
- 文書の節: [3.11](swing_to_winui4k.md#list-delete)

<a id="a-revalidate-scroll"></a>
#### API 17. スクロールペインの中身の `revalidate()` で、続きの読み込みが余分に起きる

- Swing: `panel.add(...); panel.revalidate();` はスクロールの位置を保ち、`AdjustmentListener` も位置が変わったときだけ呼ばれる。
- WinUI4K: 中身の `revalidate()` ([`WComponent.kt:983`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComponent.kt#L983)) が祖先の `WLayoutPanel` の並べ直しを予約し、その間に ScrollViewer の表示領域が中身の高さまで広がる。位置は `0c0c992d` で、測り直した後に元へ戻すようにした。
- 何が不自然か: 位置は戻りますが、途中の一時的な位置でリスナーが呼ばれ、「末尾にいる」と判定されます。[16.5](swing_to_winui4k.md#scrollbar-loadmore) のサンプルに `items.revalidate()` を足して撮り直すと、足さないときの 40 件に対して、読み込みがもう 1 回起きて 60 件になりました。
- 直す案: 測り直しで位置を外して戻す間は、スクロールのリスナーに知らせない。
- 文書の節: [16.5](swing_to_winui4k.md#scrollbar-loadmore)

<a id="a-meaning-table"></a>
#### API 18. そのほかの、同じ名前で違う振る舞い

| Swing | WinUI4K | 何が不自然か | 直す案 | 文書の節 |
|---|---|---|---|---|
| `new Font("SansSerif", ...)`・`"Dialog"`・`"Monospaced"` | `WFont` ([`WFont.kt:18`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFont.kt#L18)) は書体の名前をそのまま WinUI に渡す | 論理フォントの名前を置き換えないので、無い書体の名前になる | `SansSerif`・`Dialog` → Segoe UI、`Monospaced` → Consolas、`Serif` → Times New Roman に置き換える | [4.3](swing_to_winui4k.md#label-font) |
| `JLabel.setEnabled(false)` で文字が灰色 | `WLabel.isEnabled` ([`WLabel.kt:213`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WLabel.kt#L213)) は `disabledIcon` を切り替えるだけ | 入力欄と一緒にラベルを無効にしても見た目が変わらない | 無効の間は文字の色をテーマの無効の色にする | [4.9](swing_to_winui4k.md#label-notes) |
| `label.setLabelFor(button)` + `setDisplayedMnemonic` は、フォーカスを移すだけ | 対象の AccessKey になる ([`WLabel.kt:446-453`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WLabel.kt#L446-L453)) | 対象がボタンだと押される。対象の `mnemonic` も上書きされる | 対象がボタンのときは、フォーカスだけを移す | [4.6](swing_to_winui4k.md#label-labelfor) |
| `JCheckBox.setIcon` / `setSelectedIcon` は、チェックの印そのものを絵に替える | 印の右にアイコンを足す ([`WButtonBase.kt:60-61`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WButtonBase.kt#L60-L61)) | 星やハートのチェックボックスが、印と絵の 2 つになる | Javadoc と文書で案内する (印を替えるにはテンプレートの差し替えが要る) | [5.1](swing_to_winui4k.md#checkbox-table) |
| `checkBox.setHorizontalTextPosition(LEADING)` で文字を印の左に | `horizontalTextPosition` ([`WButtonBase.kt:149`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WButtonBase.kt#L149)) はアイコンと文字の位置だけ | 呼べるのに効かない | FlowDirection で入れ替えるか、効かないことを Javadoc に書く | [5.1](swing_to_winui4k.md#checkbox-table) |
| `JCheckBox.setBorderPainted(true)` で枠 (既定は false) | `isBorderPainted` ([`WToggleButton.kt:267`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WToggleButton.kt#L267)) | CheckBox・RadioButton のテンプレートは枠を描かず、既定も true で逆 | 既定を false にし、true のときは枠を描く | [5.1](swing_to_winui4k.md#checkbox-table) |
| `new DefaultCellEditor(comboBox)` は渡したコンボそのものを置く | 作った時点の設定を写した XAML ([`table/TableCellEditor.kt:35-44`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/table/TableCellEditor.kt#L35-L44)) | 後から項目やリスナーを足しても効かない | 項目のリストを受け取る形にし、Javadoc で強調する | [19.7](swing_to_winui4k.md#table-edit) |
| `dialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)` は × だけを止める | `WContentDialog.defaultCloseOperation` ([`WContentDialog.kt:237`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WContentDialog.kt#L237)) は Esc と閉じるボタンを止める ([`WContentDialog.kt:549`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WContentDialog.kt#L549)) | `setCloseButtonText("Close")` で置いたボタンを押しても閉じない | 閉じるボタンは止めない (Esc だけを × とみなす) | [25.4](swing_to_winui4k.md#dialog-close) |
| `dialog.setSize(440, 300)` | `WContentDialog` が継ぐ `setSize` ([`WComponent.kt:369`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComponent.kt#L369)) | 呼べるのに大きさが変わらない (`pack` と `setLocationRelativeTo` は「何もしない」と KDoc にあるが、`setSize` は書いていない) | 上書きして KDoc に書くか、`@Deprecated` で警告を出す | [25.2](swing_to_winui4k.md#dialog-basic) |
| `setFloatable(true)`・`setRollover(false)` | `WCommandBar.isFloatable` / `isRollover` ([`WCommandBar.kt:254`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WCommandBar.kt#L254)、[`WCommandBar.kt:271`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WCommandBar.kt#L271)) | 黙って無視される | 無視する設定をクラスの説明の先頭にまとめる | [23.1](swing_to_winui4k.md#toolbar-table) |
| `toolBar.add(action)` はアイコンだけのボタン (`setHideActionText(true)`) | `WCommandBar.add(action)` ([`WCommandBar.kt:194`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WCommandBar.kt#L194)) は `NAME` をラベルにする | バーを開いたりラベルを右に出したりすると、文字が出る | `labelPosition = COLLAPSED` にするか、違いを Javadoc に書く | [23.3](swing_to_winui4k.md#toolbar-action) |
| `tree.setShowsRootHandles(false)` | `showsRootHandles` ([`WTree.kt:372`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTree.kt#L372)) は値を覚えるだけ | 例外も警告も無く効かない | 効かないことを文書でも示す | [20.7](swing_to_winui4k.md#tree-lazy) |
| `setFileSelectionMode(FILES_AND_DIRECTORIES)` | `FILES_AND_DIRECTORIES` ([`WFileChooser.kt:399`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFileChooser.kt#L399)) はファイルを選ぶダイアログになる ([`WFileChooser.kt:28`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFileChooser.kt#L28)) | 例外も出ずにフォルダーが選べない | OS のダイアログの制約なので、文書で明記する | [27.5](swing_to_winui4k.md#filechooser-folder) |
| `FileFilter` を継いで `accept` を書く | `WFileFilter` ([`WFileFilter.kt:9-12`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFileFilter.kt#L9-L12)) | ダイアログに出るのは `WFileNameExtensionFilter` と「すべてのファイル」だけで、独自のものは黙って無視される | 拡張子以外のフィルターが来たら警告を出す | [27.3](swing_to_winui4k.md#filechooser-filter) |
| `JTextPane.getCharacterAttributes()` はキャレットの前の文字の書式 | `getCharacterAttributes()` ([`WTextPane.kt:700`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextPane.kt#L700)) はキャレットの後ろの 1 文字 | 太字の語の直後で、「太字」ボタンの状態が Swing と逆になる (ソースの説明で確かめた) | キャレットの前の文字 (先頭なら後ろ) の書式を返す | [13.5](swing_to_winui4k.md#textpane-caret) |
| `doc.insertString(offs, str, null)` は書式の無い文字 | `WStyledDocument` ([`WStyledDocument.kt:3-6`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WStyledDocument.kt#L3-L6)) では前の文字の書式を引き継ぐ | 太字の後の文字が太字になる | `null` のときは既定の書式を入れる | [13.2](swing_to_winui4k.md#textpane-styled) |
| (トグル スイッチは無い) 2 つの状態の部品はどれも `AbstractButton` | `WToggleSwitch` ([`WToggleSwitch.kt:21`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WToggleSwitch.kt#L21)) は `WSelectable` でなく、`text` は `header` の別名 ([`WToggleSwitch.kt:53`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WToggleSwitch.kt#L53)) | `WButtonGroup` や `Action` に使えず、`setText` がスイッチの上の見出しを変える | `WSelectable` を実装して `Action` を受ける | [7.6](swing_to_winui4k.md#toggle-winui) |
| (Swing はマウスのリスナーと `getPathForLocation`) | `WTree.addItemInvokedListener` ([`WTree.kt:1030`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTree.kt#L1030)) | モデルを表示している木では、節の代わりに null が来る | モデルの木では `TreePath` を渡すリスナーを足す | [20.9](swing_to_winui4k.md#tree-notes) |
| `setMnemonic('S')` はラベルに下線 | `WButtonBase.setMnemonic` ([`WButtonBase.kt:257`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WButtonBase.kt#L257)) は Alt のヒント | 「(S)」を書く手間が残る。`_S` のような記法も無い | ラベルの `_S` の記法を足す | [1.6](swing_to_winui4k.md#button-mnemonic) |
| 既定ボタンは Enter で押される | `WFrame.defaultButton` ([`WFrame.kt:796`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L796)) | Enter を自分で処理する部品にフォーカスがあると押されない | Javadoc のとおり。文書で触れる | [1.7](swing_to_winui4k.md#button-default) |

<a id="api-default"></a>
### 2.2 既定値が違う

<a id="a-frame-layout"></a>
#### API 19. ★ `WFrame` の中身が `BorderLayout` ではなく、制約付きの `add` が例外になる

- Swing: `JFrame` の中身は既定で `BorderLayout` なので、`frame.add(toolBar, BorderLayout.NORTH)` がそのまま効く。
- WinUI4K: 既定の中身は子を縦に積む `WPanel` (子の間隔 12、周りの余白 24) ([`WFrame.kt:65`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L65)) で、`getLayout()` は null ([`WFrame.kt:703`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L703))。`frame.add(c, WBorderLayout.NORTH)` ([`WFrame.kt:676`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L676)) は、中身にレイアウト マネージャーが無いと `IllegalArgumentException` ([`ContentPanes.kt:13-18`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/ContentPanes.kt#L13-L18))。
- 何が不自然か: Swing のコードを型の名前だけ替えて移すと、実行したときに UI スレッドで例外になり、ウィンドウが出ません (実行で確かめた)。また、`setLayout` を呼ぶと余白 24 と子の間隔 12 が消える (余白の無い `WLayoutPanel` になる) ので、`setLayout` を呼ぶかどうかで見た目が大きく変わります。文書は 0.2 の表を直しました。
- 直す案: 既定の中身を `WLayoutPanel(WBorderLayout())` にする (制約なしの `add` は CENTER)。今の縦に積む形を残すなら、少なくとも制約付きの `add` が来たときに `WBorderLayout` に切り替える。
- 文書の節: [0.2](swing_to_winui4k.md#basics-differences)、[24.1](swing_to_winui4k.md#frame-table)、[30.3](swing_to_winui4k.md#layout-border)、[付録 A](swing_to_winui4k.md#checklist)

<a id="a-layoutpanel-null"></a>
#### API 20. ★ `new WLayoutPanel()` のレイアウトが null

- Swing: `new JPanel()` は `FlowLayout`。
- WinUI4K: `WLayoutPanel(layout: WLayoutManager? = null)` ([`WLayoutPanel.kt:22`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WLayoutPanel.kt#L22))。
- 何が不自然か: `new JPanel()` から移すと、ボタンを 2 つ足すと両方 x=0 に重なり、縦の `WPanel` の中に置くとパネルの高さが 0 になりました (実行で確かめた)。名前 (Layout Panel) からもマネージャーを持つと思えます。文書は 0.2・14 章・付録 A を直しました。
- 直す案: 引数なしの既定を `WFlowLayout()` にする (null のレイアウトは `new WLayoutPanel(null)` で明示する。Swing の `new JPanel(null)` と同じ)。
- 文書の節: [0.2](swing_to_winui4k.md#basics-differences)、[14.1](swing_to_winui4k.md#panel-table)、[付録 A](swing_to_winui4k.md#checklist)

<a id="a-nest"></a>
#### API 21. 入れ子の `WLayoutPanel` や、スクロールペインの中身の `WLayoutPanel` が、希望の大きさで並ばない (直した: `804e7a31`)

- 直した後: 入れ子の `WLayoutPanel` (`WBorder` の中も) は子を測った後の希望の大きさで外に並べられ、表示した後に中の大きさが変わっても外が並べ直します。`WScrollPane` の中身の `WLayoutPanel` は、`JViewport` と同じく希望の大きさと表示領域の大きい方で並び、はみ出した分をスクロールします ([表示 1](#d1))。`new JPanel(layout)` → `new WLayoutPanel(layout)` の素直な書き換えで並びます。
- 文書の節: [14.2](swing_to_winui4k.md#panel-nest)、[14.3](swing_to_winui4k.md#panel-border)、[15.2](swing_to_winui4k.md#scroll-basic)

<a id="a-scroll-horizontal"></a>
#### API 22. ★ `WScrollPane` の横のスクロールが、既定で無効

- Swing: `new JScrollPane(view)` は縦横とも `AS_NEEDED`。
- WinUI4K: `new WScrollPane(view)` の横は DISABLED で、中身を幅に合わせて縮める ([`WScrollPane.kt:46-51`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WScrollPane.kt#L46-L51)、[`WScrollPane.kt:78`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WScrollPane.kt#L78))。`getHorizontalScrollBarPolicy()` は `NEVER` を返す。
- 何が不自然か: 横に長い中身がスクロールできずに切れます。
- 直す案: 横も AUTO を既定にする (少なくとも Swing の形のコンストラクターでは `AS_NEEDED` にする)。
- 文書の節: [15.3](swing_to_winui4k.md#scroll-policy)

<a id="a-preferred-size"></a>
#### API 23. スライダー・進捗バー・進捗リング・スピナーに、希望の大きさが無い (直した: `9ffda622`)

- 直した後: `WSlider` は長さの向きが 200 (`JSlider` と同じ)、`WProgressBar` は幅 146 (`JProgressBar` と同じ)、`WProgressRing` は 32 × 32 (WinUI の既定のスタイル)、`WSpinner` はフォーカスで出る消去ボタンの分を含めた幅を、希望の大きさに持ちます ([表示 5](#d5))。文書のサンプルから `setWidth` / `setHeight` を外しました。
- 文書の節: [9.2](swing_to_winui4k.md#spinner-basic)、[10.2](swing_to_winui4k.md#slider-basic)、[11.2](swing_to_winui4k.md#progress-basic)、[11.7](swing_to_winui4k.md#progress-winui)

<a id="a-linewrap"></a>
#### API 24. ★ `WTextPane` の `lineWrap` の既定が逆

- Swing: `JTextArea.getLineWrap()` の既定は false。
- WinUI4K: `lineWrap` ([`WTextPane.kt:295`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextPane.kt#L295)) は、画面に載った後の既定が true (RichEditBox の既定のスタイル)。載る前は false と読める。
- 何が不自然か: 折り返さないはずの欄 (ログやコード) が折り返します。読む時機で値が変わります。
- 直す案: コンストラクターで NoWrap を明示して、Swing の既定に合わせる。
- 文書の節: [12.3](swing_to_winui4k.md#textarea-wrap)

<a id="a-selection-default"></a>
#### API 25. ★ リストと木の選択の既定が、1 つだけ

- Swing: `JList` の既定は `MULTIPLE_INTERVAL_SELECTION`、`JTree` (`DefaultTreeSelectionModel`) の既定は `DISCONTIGUOUS_TREE_SELECTION`。
- WinUI4K: `WList` は単一の選択 ([`WList.kt:190`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WList.kt#L190))。`WTree` は `SINGLE_TREE_SELECTION` ([`WTree.kt:900-906`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTree.kt#L900-L906))。木を複数にすると TreeView のチェックボックスの形になり、親をチェックすると子孫も選ばれる。
- 何が不自然か: Ctrl+クリックで何個も選ぶ Swing の操作が、そのままではできません。木では、複数にしたときの見た目と振る舞いも違います。
- 直す案: 既定は WinUI に合わせたままでもよいが、文書と Javadoc で強調する。
- 文書の節: [3.6](swing_to_winui4k.md#list-multiple)、[20.3](swing_to_winui4k.md#tree-selection)

<a id="a-table-column"></a>
#### API 26. 表の列の既定の幅と、数値の列の揃えが、説明と違う (直した: `7178e9a3`・`6814a8be`)

- 直した後: 幅を決めない列 (`preferredWidth` が NaN) は、`JTable` の既定 (`AUTO_RESIZE_SUBSEQUENT_COLUMNS`) のように表の幅を分け合い、右に空きを残しません。幅の決まらない入れ物の中では 120 です。並べ替えの印の場所は列ヘッダーに空けます ([表示 12](#d12))。`getColumnClass` が `Number` の列は右に寄せます ([表示 13](#d13))。
- 残る違い: `setAutoResizeMode` に当たる設定はありません (いつも幅を決めない列で分け合う)。
- 文書の節: [19.2](swing_to_winui4k.md#table-basic)、[19.4](swing_to_winui4k.md#table-columns)

<a id="a-spinner-buttons"></a>
#### API 27. `WSpinner` の上下のボタンが、既定で出ない

- Swing: `JSpinner` はいつも上下の矢印ボタンを出す。
- WinUI4K: `spinButtonPlacementMode` ([`WSpinner.kt:196-198`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WSpinner.kt#L196-L198)) の既定は WinUI と同じ HIDDEN。
- 何が不自然か: `JSpinner` を書き換えただけでは、ただの数の入力欄に見えます (文書のサンプルには全部 `setSpinButtonPlacementMode(SpinButtonPlacementMode.INLINE)` を足した)。
- 直す案: JSpinner 風のクラスなので、既定を INLINE にする。
- 文書の節: [9.2](swing_to_winui4k.md#spinner-basic)

<a id="a-defaults-table"></a>
#### API 28. そのほかの既定値の違い

| Swing の既定 | WinUI4K の既定 | 直す案 | 文書の節 |
|---|---|---|---|
| `new JSpinner()` は Integer の 0 | `WSpinner()` は NaN ([`WSpinner.kt:59`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WSpinner.kt#L59))。入力を消しても NaN ([`WSpinner.kt:50`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WSpinner.kt#L50)) | 既定を 0 にし、空欄を許すのは明示の設定にする | [9.1](swing_to_winui4k.md#spinner-table) |
| `new JSlider()` は 0〜100 で値 50 | `WSlider()` の値は 0 ([`WSlider.kt:78`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WSlider.kt#L78)) | 50 にする | [10.1](swing_to_winui4k.md#slider-table) |
| `new JComboBox<>()` + `addItem` は最初の項目を選び、選んでいる項目を消すと隣を選ぶ (どのコンストラクターでも同じ) | 引数なしと `List` のコンストラクターは選ばず、消すと未選択 ([`WComboBox.kt:45-46`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L45-L46)、[`WComboBox.kt:105`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L105))。配列とモデルのコンストラクターは Swing と同じ ([`WComboBox.kt:54-61`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L54-L61)) | Swing と同じにする | [8.4](swing_to_winui4k.md#combo-model) |
| 項目が無いときの `setSelectedIndex(2)` は `IllegalArgumentException` | 値を覚えておき、その位置の項目が足されたときに選ぶ ([`WComboBox.kt:197-212`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L197-L212)) | Swing と同じく例外にする | [8.4](swing_to_winui4k.md#combo-model) |
| `JFrame` は `HIDE_ON_CLOSE` | `DISPOSE_ON_CLOSE` ([`WFrame.kt:440`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L440))。最後のウィンドウが閉じるとアプリが終わる | 文書の 0.2 のとおり (閉じても動き続けるコードが黙って終わる点に注意) | [0.2](swing_to_winui4k.md#basics-differences)、[24.3](swing_to_winui4k.md#frame-closing) |
| `JTabbedPane` に「+」ボタンは無い | `isAddTabButtonVisible` ([`WTabView.kt:169`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTabView.kt#L169)) は true。押しても何も起きない「+」が出る | 既定を false にする (リスナーを付けたときだけ出す) | [18.2](swing_to_winui4k.md#tabbed-basic)、[18.6](swing_to_winui4k.md#tabbed-winui) |
| タブに閉じるボタンは無い | `addTab` で作るタブは閉じない ([`WTabView.kt:247`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTabView.kt#L247)) が、`new WTabViewItem(title)` は閉じるボタンが出る ([`WTabView.kt:92`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTabView.kt#L92))。押してもリスナーが無いと閉じない | 既定をそろえる。閉じるボタンが出ているタブは、既定で閉じる | [18.4](swing_to_winui4k.md#tabbed-close) |
| `JDialog` の OK / Cancel では、`getRootPane().setDefaultButton(ok)` で OK を既定ボタンにすることが多い | `WContentDialog.defaultButton` ([`WContentDialog.kt:272`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WContentDialog.kt#L272)) は NONE (Enter で何も押されない) | PRIMARY を既定にする (`WOptionPane` は PRIMARY) | [25.2](swing_to_winui4k.md#dialog-basic) |
| 幅が足りないと末尾を「...」にする | `WLabel.textTrimming` ([`WLabel.kt:403`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WLabel.kt#L403)) は NONE で、切るだけ | `CHARACTER_ELLIPSIS` にする | [4.9](swing_to_winui4k.md#label-notes) |
| `JColorChooser` は既定で透明度のスライダーがある | `isAlphaEnabled` ([`WColorPicker.kt:109`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WColorPicker.kt#L109)) は false | 既定は WinUI のままでよいが、文書で明記する | [28.4](swing_to_winui4k.md#colorchooser-alpha) |
| `DefaultTreeCellRenderer` は L&F のフォルダーと葉のアイコン | `leafIcon` / `openIcon` / `closedIcon` は null ([`WTreeCellRenderer.kt:35-46`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTreeCellRenderer.kt#L35-L46)) | `Symbol.FOLDER` / `Symbol.DOCUMENT` を既定にするか、簡単に付ける方法を足す | [20.6](swing_to_winui4k.md#tree-renderer) |
| ツールチップはマウスの位置の下 | `WToolTip.placement` ([`WToolTip.kt:96`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WToolTip.kt#L96)) の既定は上 | 文書で既定が上であることを書く。ずらす量 (offset) を足す | [29.2](swing_to_winui4k.md#tooltip-placement) |
| `isIndeterminate()` は false | `new WProgressRing()` は true、`new WProgressRing(0, 100)` と `new WProgressRing(model)` は false ([`WProgressRing.kt:10`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WProgressRing.kt#L10)、[`WProgressRing.kt:25-40`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WProgressRing.kt#L25-L40)) | どちらかにそろえる | [11.7](swing_to_winui4k.md#progress-winui) |
| `getInputMap()` に矢印や Ctrl+C などの既定のキーが入っている | 入っていない | 文書のとおり (既定のキーを上書き・列挙するコードはそのまま動かない) | [2.12](swing_to_winui4k.md#textfield-keybinding) |
| `setVisibleRowCount` の既定 8 が高さに効く | 設定するまで効かない。高さは「行数 × 40 + 余白」([`WList.kt:325-336`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WList.kt#L325-L336)) | 文書で行の高さの違いに触れる | [3.10](swing_to_winui4k.md#list-rows) |

<a id="api-name"></a>
### 2.3 名前や型・単位・引数の順が違う

<a id="a-order"></a>
#### API 29. ★ 余白の 4 つの数の順が、API ごとに違う

- Swing: `new Insets(top, left, bottom, right)`・`createEmptyBorder(top, left, bottom, right)`・`GridBagConstraints.insets` (いつも上・左・下・右)。
- WinUI4K: `WInsets(top, left, bottom, right)` ([`WLayoutManager.kt:10`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WLayoutManager.kt#L10)) は上・左・下・右だが、`WComponent.setMargin(left, top, right, bottom)` ([`WComponent.kt:1053`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComponent.kt#L1053))・`WControl.setPadding` ([`WControl.kt:51`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WControl.kt#L51))・`WBorder.setPadding` ([`WBorder.kt:83`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WBorder.kt#L83)) は左・上・右・下。
- 何が不自然か: Swing の `createEmptyBorder(0, 4, 0, 8)` と WinUI4K の `setMargin(4, 0, 8, 0)` を並べると、同じ 4 つの数の並びが違い、取り違えやすいです。ライブラリの中でも `WInsets` と `setMargin` で順が逆です。
- 直す案: `setMargin(WInsets)` / `setPadding(WInsets)` の多重定義を足して、Swing の順の `WInsets` で渡せるようにする。
- 文書の節: [14.5](swing_to_winui4k.md#panel-padding)、[15.6](swing_to_winui4k.md#scroll-header)、[30.6](swing_to_winui4k.md#layout-gridbag)

<a id="a-double"></a>
#### API 30. ★ 値や大きさが double

- Swing: `int JSlider.getValue()`・`int JProgressBar.getValue()`・`(Integer) spinner.getValue()`・`int TableColumn.getPreferredWidth()`・`new FlowLayout(LEFT, 10, 10)`・`popup.show(c, x, y)` (int)。
- WinUI4K: `WSlider.value` ([`WSlider.kt:143`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WSlider.kt#L143))・`WProgressBar.value` ([`WProgressBar.kt:60`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WProgressBar.kt#L60))・`WSpinner.value` ([`WSpinner.kt:128-132`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WSpinner.kt#L128-L132))・`TableColumn.preferredWidth` ([`table/TableColumn.kt:117`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/table/TableColumn.kt#L117))・`WComponent.width` / `height` ([`WComponent.kt:203`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComponent.kt#L203))・`WFlowLayout.hgap` ([`WFlowLayout.kt:18`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFlowLayout.kt#L18))・`WFlyoutBase.show(invoker, x: Double, y: Double)` ([`WFlyout.kt:89`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFlyout.kt#L89)) がどれも `Double`。一方 `WList.fixedCellHeight` ([`WList.kt:292`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WList.kt#L292)) や `WFrame.setSize` ([`WFrame.kt:526`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L526)) は `Int`。
- 何が不自然か: Java の `"Volume: " + slider.getValue()` が黙って「Volume: 70.0」になります。`(Integer) spinner.getValue()` や `int v = progress.getValue();` はコンパイルできません。Kotlin では `WSlider(0, 100, 30)`・`WFlowLayout(LEFT, 10, 10)`・`popup.show(e.component, e.x, e.y)` が通らず、`.0` や `toDouble()` が要ります。同じ式の中で Int と Double が混ざります。
- 直す案: Swing で int のものは int の多重定義や getter (`getIntValue` など) を足す。Kotlin 向けに Int を受ける形を足す。スピナーの `getValue()` はモデルの値 (`Object`) にする。
- 文書の節: [9.3](swing_to_winui4k.md#spinner-listener)、[10.2](swing_to_winui4k.md#slider-basic)、[11.8](swing_to_winui4k.md#progress-kotlin)、[19.4](swing_to_winui4k.md#table-columns)、[22.3](swing_to_winui4k.md#popup-mouse)、[30.9](swing_to_winui4k.md#layout-kotlin)、[3.16](swing_to_winui4k.md#list-kotlin)

<a id="a-generic"></a>
#### API 31. ★ `WList` と `WComboBox` に型引数が無い

- Swing: `JList<E>.getSelectedValue(): E`・`getSelectedValuesList(): List<E>`、`JComboBox<E>.addItem(E)`。
- WinUI4K: `WList` は `selectedValue: Any?` ([`WList.kt:126`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WList.kt#L126))・`selectedValuesList: List<Any?>` ([`WList.kt:130`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WList.kt#L130))。`WComboBox` ([`WComboBox.kt:50`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L50)) の `addItem` ([`WComboBox.kt:312`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L312))・`insertItemAt` ([`WComboBox.kt:317`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L317)) は `String` だけを受ける。モデル (`WDefaultListModel<E>`・`WDefaultComboBoxModel<E>`) は型引数を持つ。
- 何が不自然か: 選んだ値を読むたびにキャストが要ります。`JComboBox<String>` の `<String>` を消す書き換えが要り、文字列でない項目はモデルを作ってから渡すしかありません。
- 直す案: `WList<E>`・`WComboBox<E>` にする。
- 文書の節: [3.4](swing_to_winui4k.md#list-selected)、[3.9](swing_to_winui4k.md#list-renderer)、[8.6](swing_to_winui4k.md#combo-renderer)

<a id="a-owner"></a>
#### API 32. ★ ダイアログの持ち主に `WFrame` を渡せない API がある

- Swing: `JColorChooser.showDialog(frame, ...)`・`new JDialog(frame, ...)` のように、`JFrame` も `Component` も渡せる。
- WinUI4K: `WFileChooser`・`WOptionPane`・`WContentDialog.showAndWait` ([`WContentDialog.kt:344`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WContentDialog.kt#L344)) は `WDialogParent` ([`WDialogParent.kt:13`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WDialogParent.kt#L13)。`WFrame` と `WComponent` の両方) で受けるが、`WColorPicker.showDialog(owner: WComponent, ...)` ([`WColorPicker.kt:239`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WColorPicker.kt#L239)、[`WColorPicker.kt:249`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WColorPicker.kt#L249)) と `WContentDialog.show(owner: WComponent, ...)` ([`WContentDialog.kt:334`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WContentDialog.kt#L334)) は `WComponent` だけ。`WFrame` ([`WFrame.kt:38`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L38)) は `WComponent` ではない。
- 何が不自然か: `WColorPicker.showDialog(frame, ...)` と `dialog.show(frame, ...)` はコンパイルできません。同じ「ダイアログの親」なのに型がそろわず、サンプルではラベルを持ち主に渡すしかありませんでした。null も渡せません。コンストラクターで渡した owner も `show` では使われません。
- 直す案: ダイアログの親はすべて `WDialogParent?` にそろえる (null ならアクティブな `WFrame`)。owner のプロパティを使う `show(onClosed)` を足す。`SwingUtilities.getWindowAncestor` 風の static もあると移しやすい。
- 文書の節: [28.2](swing_to_winui4k.md#colorchooser-dialog)、[25.5](swing_to_winui4k.md#dialog-nonblocking)

<a id="a-class-names"></a>
#### API 33. クラスの名前から、Swing の部品が引けない

| Swing | WinUI4K | 何が不自然か | 直す案 | 文書の節 |
|---|---|---|---|---|
| `JTextArea` | `WTextPane` ([`WTextPane.kt:43`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextPane.kt#L43)) | `WTextArea` が無く、名前から探せない | `WTextArea` を足す | [12 章](swing_to_winui4k.md#textarea) |
| `JMenu` | `WMenuBarItem` ([`WMenuBar.kt:76`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WMenuBar.kt#L76)) | 「Item」が付くので項目 (`JMenuItem`) に見える | `WMenu` を用意する | [21.1](swing_to_winui4k.md#menu-table) |
| `JMenuItem` / `JCheckBoxMenuItem` / `JRadioButtonMenuItem` / `JPopupMenu` | `WMenuFlyoutItem` ([`WMenuFlyoutItem.kt:43`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WMenuFlyoutItem.kt#L43)) / `WToggleMenuFlyoutItem` ([`WMenuFlyoutItem.kt:365`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WMenuFlyoutItem.kt#L365)) / `WRadioMenuFlyoutItem` ([`WMenuFlyoutItem.kt:461`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WMenuFlyoutItem.kt#L461)) / `WMenuFlyout` ([`WMenuFlyout.kt:14`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WMenuFlyout.kt#L14)) | API はほぼ Swing と同じ名前なのに、クラスの名前だけ WinUI。特に「CheckBox」が「Toggle」 | `WMenuItem` などの別名を用意する | [21.1](swing_to_winui4k.md#menu-table)、[22.1](swing_to_winui4k.md#popup-table) |
| `JToolBar` + `JButton` | `WCommandBar` ([`WCommandBar.kt:80`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WCommandBar.kt#L80)) + `WAppBarButton` ([`WAppBarButton.kt:32`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WAppBarButton.kt#L32)) | ボタンも専用のクラスに替える必要がある | `WToolBar` の別名を用意する | [23.1](swing_to_winui4k.md#toolbar-table) |
| `JPanel` | `WLayoutPanel`。`WPanel` ([`WPanel.kt:25`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WPanel.kt#L25)) は StackPanel (「BoxLayout の JPanel 風」) | 名前から `WPanel` が `JPanel` に当たると思える | `WPanel` を `WStackPanel` などにする案 | [14.1](swing_to_winui4k.md#panel-table) |
| `GridLayout` / `FlowLayout` | `WUniformGridLayout` ([`WUniformGridLayout.kt:65`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WUniformGridLayout.kt#L65)) は `WItemsView` 専用、`WVariableSizedWrapGrid` ([`WVariableSizedWrapGrid.kt:12`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WVariableSizedWrapGrid.kt#L12)) は既定が縦向きでセルが同じ大きさ | 説明は「GridLayout 風」「FlowLayout の JPanel 風」だが、`setLayout` に渡せない・振る舞いが違う | 説明を `WGridLayout`・`WFlowLayout` を指す案内に直す | [30.1](swing_to_winui4k.md#layout-table) |
| `GridBagLayout` / `SpringLayout` | `WGrid` / `WRelativePanel` (どちらもパネル) | レイアウト マネージャーではないので `frame.setLayout` に渡せない | [API 36](#a-gridbag)・[API 49](#a-relativepanel) | [30.6](swing_to_winui4k.md#layout-gridbag)、[30.7](swing_to_winui4k.md#layout-spring) |

<a id="a-submenu"></a>
#### API 34. サブメニューが別のクラスで、`JMenu` を入れ子にする書き方ができない

- Swing: `menu.add(new JMenu("Open Recent"))` (同じ `JMenu` をバーにもサブメニューにも使う)。
- WinUI4K: バーのメニューは `WMenuBarItem` ([`WMenuBar.kt:76`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WMenuBar.kt#L76))、サブメニューは `WMenuFlyoutSubItem` ([`WMenuFlyoutItem.kt:586`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WMenuFlyoutItem.kt#L586))。`WMenuBarItem.add` は `WMenuFlyoutItemBase` しか受けない ([`WMenuBar.kt:224`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WMenuBar.kt#L224))。
- 何が不自然か: メニューを組み立てる共通の関数で作ったメニューを、バーにもサブメニューにも置くことができず、2 つのクラスに書き分けます。
- 直す案: `WMenu` ([API 33](#a-class-names)) をサブメニューとしても `add` できるようにし、入れた場所に合わせて作り分ける。
- 文書の節: [21.4](swing_to_winui4k.md#menu-submenu)

<a id="a-renderer"></a>
#### API 35. レンダラーの書き方が Swing と違い、部品ごとにもそろっていない

- Swing: `DefaultListCellRenderer` / `DefaultTableCellRenderer` / `DefaultTreeCellRenderer` を継ぎ、`super.get...RendererComponent(...)` を呼んでから `setText` / `setIcon` で自分 (ラベル) を変えて `this` を返す。表は `setValue(Object)` を上書きすることも多い。`JComboBox.setRenderer(ListCellRenderer)`。
- WinUI4K: リストは `getText(value)` / `getIcon(value)` / `configure(...)` を上書き ([`WListCellRenderer.kt:36-42`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WListCellRenderer.kt#L36-L42))。表は `getText(value)` ([`table/TableCellRenderer.kt:62`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/table/TableCellRenderer.kt#L62)) と `configure(label, value, isSelected, row, column)` ([`table/TableCellRenderer.kt:65`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/table/TableCellRenderer.kt#L65))、揃えは `horizontalAlignment: HorizontalAlignment` ([`table/TableCellRenderer.kt:42`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/table/TableCellRenderer.kt#L42))。木は 7 つの引数の `getText` ([`WTreeCellRenderer.kt:64`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTreeCellRenderer.kt#L64)) と 3 つの引数の `getIcon` ([`WTreeCellRenderer.kt:68`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTreeCellRenderer.kt#L68))。コンボは `renderer: ((Any?) -> String)?` ([`WComboBox.kt:328`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L328)) で、文字列を返す関数。
- 何が不自然か: `super` の後に `setText` する Swing のよくある形は、返すラベルが毎回新しいので効きません (ラベルを使い回すと表示に失敗する)。表の `setValue` の上書きはメソッドが無くコンパイルできません。木の `convertValueToText` ([`WTree.kt:423`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTree.kt#L423)) は上書きできません (`open` でない)。引数の数も表・リスト・木でばらばらです。
- 直す案: 表に `setValue(Object)` に当たるフックを足す。表・リスト・木で同じ形のフックにそろえる。`convertValueToText` を `open` にする。Javadoc に「Swing のどの上書きに当たるか」を書く。
- 文書の節: [3.9](swing_to_winui4k.md#list-renderer)、[19.4](swing_to_winui4k.md#table-columns)、[20.6](swing_to_winui4k.md#tree-renderer)、[8.6](swing_to_winui4k.md#combo-renderer)

<a id="a-gridbag"></a>
#### API 36. `GridBagLayout` に当たる `WGrid` で、行と列の順が逆

- Swing: `GridBagConstraints.gridx` (列)・`gridy` (行)・`gridwidth`・`weightx`・`fill`・`anchor`・`insets`。
- WinUI4K: `WGrid.add(component, row, column, rowSpan, columnSpan)` ([`WGrid.kt:85`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WGrid.kt#L85))、重みは `addRow` / `addColumn(GridLength)` で行と列に付ける。
- 何が不自然か: `gridx, gridy` の順で書き写すと行と列が入れ替わります。重みは子ではなく行と列に付け、行と列を先に作る必要があります。`insets` は子の `setMargin` で順も違い、`ipadx` / `ipady` は無いです。
- 直す案: `WGridBagLayout` と `WGridBagConstraints` を Swing と同じ計算で用意する (`WFlowLayout` などと同じ方針)。`WGrid` には取り違えを防ぐ名前付きの形 (Kotlin の `add(c, column = , row = )` など) を足す。
- 文書の節: [30.6](swing_to_winui4k.md#layout-gridbag)

<a id="a-names-table"></a>
#### API 37. そのほかの名前・型の違い

| Swing | WinUI4K | 何が不自然か | 直す案 | 文書の節 |
|---|---|---|---|---|
| `javax.swing.table.DefaultTableModel`・`javax.swing.tree.DefaultMutableTreeNode`・`DefaultListModel` | 表と木は W なしで別のパッケージ ([`table/DefaultTableModel.kt:11`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/table/DefaultTableModel.kt#L11)、[`tree/DefaultMutableTreeNode.kt:21`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/tree/DefaultMutableTreeNode.kt#L21))。リストは `WDefaultListModel` でルートのパッケージ。木のレンダラーは `WDefaultTreeCellRenderer` (ルート)、表のレンダラーは `table.DefaultTableCellRenderer`。表の編集のイベントはルート ([`WTableEvents.kt:62`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTableEvents.kt#L62)) | 付け方と置き場所がそろわない。「import は `com.appkitbox.winui4k.*`」だけでは表と木のコードがコンパイルできず、`javax.swing.table.*` と一緒に import すると名前がぶつかる | どちらかにそろえる。W なしを続けるなら、table / tree パッケージの import を文書と Javadoc に書く | [「この文書の読み方」](swing_to_winui4k.md#reading)、[19.1](swing_to_winui4k.md#table-table)、[20.1](swing_to_winui4k.md#tree-table) |
| `DefaultMutableTreeNode` (節は 1 種類) | `WTreeNode` (説明は「DefaultMutableTreeNode 風」[`WTree.kt:55-62`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTree.kt#L55-L62)) と `tree.DefaultMutableTreeNode` の 2 つ | `WTreeNode` はモデルではなく、モデルを表示している間に `addRootNode`・`selectedNode` などを使うと `IllegalStateException` ([`WTree.kt:394-396`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTree.kt#L394-L396))。同じことをする API が 2 系統ある | `WTreeNode` の説明を「Swing から移すなら tree.DefaultMutableTreeNode」に直す | [20.1](swing_to_winui4k.md#tree-table)、[20.9](swing_to_winui4k.md#tree-notes) |
| `ButtonGroup.getSelection()` は `ButtonModel`、`setSelected(ButtonModel, boolean)`、`button.getModel().isPressed()`、`setRolloverIcon`・`setPressedIcon` | `getSelection()` は `WSelectable?` ([`WButtonGroup.kt:38`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WButtonGroup.kt#L38))、`setSelected(WSelectable, Boolean)` ([`WButtonGroup.kt:129`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WButtonGroup.kt#L129))。`getModel()` とロールオーバー・押したときのアイコンは無い | `group.setSelected(b.getModel(), true)` などがコンパイルできない | 自分自身を返す `getModel()` を足せば、Swing のコードがそのまま通る | [6.3](swing_to_winui4k.md#radio-selected)、[6.5](swing_to_winui4k.md#radio-clear) |
| `addItemListener(e -> ...)` の `e` は `ItemEvent` | Java のラムダは `WItemListener` ([`WToggleButton.kt:318`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WToggleButton.kt#L318))、Kotlin のラムダは `(Boolean?) -> Unit` ([`WToggleButton.kt:297`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WToggleButton.kt#L297))。`WToggleSwitch` は `(Boolean) -> Unit` ([`WToggleSwitch.kt:155`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WToggleSwitch.kt#L155)) | 同じ書き方で Java と Kotlin の受け取る型が違う。Kotlin で `e.stateChange` と書けない | Kotlin のラムダも `WItemEvent` にそろえ、状態だけ欲しいときは別の名前にする | [5.3](swing_to_winui4k.md#checkbox-listener)、[5.6](swing_to_winui4k.md#checkbox-kotlin) |
| `JList.getSelectionMode(): int` | `setSelectionMode(Int)` ([`WList.kt:190`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WList.kt#L190)) は Swing の定数、`selectionMode` ([`WList.kt:409`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WList.kt#L409)) は `ListViewSelectionMode` の列挙 | setter と getter の型が違い、Kotlin の `selectionMode = ...` は別物 | getter を Swing の int にそろえる | [3.6](swing_to_winui4k.md#list-multiple) |
| `split.addPropertyChangeListener(DIVIDER_LOCATION_PROPERTY, e -> e.getNewValue())` | `addChangeListener` ([`WSplitPane.kt:302`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WSplitPane.kt#L302))。プロパティ名・古い値・新しい値は無い | `e.getNewValue()` を使うコードを書き換える | `addPropertyChangeListener(String, listener)` を足す | [17.4](swing_to_winui4k.md#split-listener) |
| `getRootPane().setDefaultButton(okButton)` | `WFrame.defaultButton` は `WButton?` ([`WFrame.kt:796`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L796))、`WContentDialog.defaultButton` は列挙 ([`WContentDialog.kt:272`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WContentDialog.kt#L272)) | 同じ `setDefaultButton` で受ける型が違う | KDoc で違いに触れる | [1.7](swing_to_winui4k.md#button-default)、[25.2](swing_to_winui4k.md#dialog-basic) |
| `setMinimumSize(Dimension)` と `setPreferredSize(Dimension)` は同じ型 | `WFrame.minimumSize` は `WDimension?` ([`WFrame.kt:586`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L586)、整数)、`WComponent.preferredSize` は `WSize` ([`WComponent.kt:447`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComponent.kt#L447)、小数) | どちらを使うか迷い、`new WSize(360, 240)` を渡すとコンパイルエラー | どちらかに寄せる。`setMinimumSize(WSize)` を足す | [24.2](swing_to_winui4k.md#frame-basic) |
| `frame.setIconImage(Image)` | `setIconImage(path: String)` ([`WFrame.kt:990`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L990)) と `setIconImage(WImageIcon)` ([`WFrame.kt:1008`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFrame.kt#L1008)。ファイルだけ) | jar の中の画像や、プログラムで描いた画像をアイコンにできない | バイト列や InputStream を受ける形を足す | [24.4](swing_to_winui4k.md#frame-icon) |
| `Box Box.createHorizontalBox()` | `WBox.createHorizontalBox(): WLayoutPanel` ([`WBox.kt:24`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WBox.kt#L24)) | `WBox buttons = WBox.createHorizontalBox();` がコンパイルできない (`WBox` は object) | 文書のとおり `WLayoutPanel` と書く。型を合わせるなら `WBox` を `WLayoutPanel` の派生にする | [30.5](swing_to_winui4k.md#layout-box) |
| `BorderLayout.getConstraints(c)` は `"North"` などの文字列 | `Constraint?` ([`WBorderLayout.kt:157`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WBorderLayout.kt#L157)) | 文字列の比較を書き換える (`==` で比べられる) | 今のままでよい | [30.3](swing_to_winui4k.md#layout-border) |
| `SwingUtilities.isEventDispatchThread()` | `WinUiUtilities.isDispatchThread` ([`WinUiUtilities.kt:156`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WinUiUtilities.kt#L156)) | 名前が少し違う | 文書の 0.2 のとおり | [0.2](swing_to_winui4k.md#basics-differences) |
| `UndoManager.canUndo()` | `WTextField.canUndo()` はメソッド ([`WTextField.kt:357`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextField.kt#L357))、`WTextPane.canUndo` はプロパティ ([`WTextPane.kt:648`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextPane.kt#L648)。Java では `getCanUndo()`) | 同じことをするのに名前が分かれる | `WTextPane` もメソッドにそろえる | [2.13](swing_to_winui4k.md#textfield-undo)、[12.1](swing_to_winui4k.md#textarea-table) |
| (DocumentListener) | `WTextField.addTextChangedListener` は文字を受ける ([`WTextField.kt:659-662`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextField.kt#L659-L662))、`WTextPane.addTextChangedListener` は引数なし ([`WTextPane.kt:832-839`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextPane.kt#L832-L839)) | `WTextField` のラムダを `WTextPane` に移すとコンパイルできない | `WTextPane` にも文字を受ける形を足す | [2.4](swing_to_winui4k.md#textfield-change)、[12.1](swing_to_winui4k.md#textarea-table) |
| `SpinnerNumberModel.setStepSize` | スピナーは `smallChange` ([`WSpinner.kt:149`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WSpinner.kt#L149))、モデルは `stepSize` | スピナーに `setStepSize` が無い | スピナーにも `stepSize` を足す | [9.4](swing_to_winui4k.md#spinner-decimal) |
| `JLabel.getFont()` は null を返さない前提で `deriveFont` をつなぐ | `WLabel.font` は `WFont?` ([`WLabel.kt:321`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WLabel.kt#L321)) だが、読むと null にならない ([`FontProperties.kt:92`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/FontProperties.kt#L92))。`WControl.font` ([`WControl.kt:85`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WControl.kt#L85)) も同じ | Kotlin で `heading.font = heading.font.deriveFont(...)` と書けず `!!` が要る | 読む側は null にならない型にする | [4.3](swing_to_winui4k.md#label-font) |

<a id="api-timing"></a>
### 2.4 表示した直後に使えないなど、使う時機の制約

<a id="a-settext-before"></a>
#### API 38. 表示する前の `setText` が、`addTextChangedListener` に届かない (直した: `e354832a`)

- 直した後: `WTextField.addTextChangedListener`・`WPasswordField.addPasswordChangedListener`・`WTextPane.addTextChangedListener` のリスナーは、Swing の `DocumentListener` と同じく、表示する前でも `setText` の中ですぐ呼ばれます。後から届く WinUI の通知は、同じ文字列なら知らせません ([表示 2](#d2))。
- 残る違い: `WAutoSuggestBox` は、表示する前の文字を表示した後に 1 回だけ (`PROGRAMMATIC_CHANGE` で) 知らせます (その場では呼ばない)。
- 文書の節: [2.4](swing_to_winui4k.md#textfield-change)、[3.13](swing_to_winui4k.md#list-filter)、[19.6](swing_to_winui4k.md#table-sort)

<a id="a-label-settext"></a>
#### API 39. 表示した後の `WLabel.setText` で、並べ直されない (直した: `355623db`)

- 直した後: `WLabel` の文字・アイコン・フォント・折り返し、ボタン (`WButtonBase`・`WSplitButton`) の文字・アイコン、`WControl` の `font`・`padding`・`border` を替えると、Swing と同じく自分で `revalidate()` を呼びます ([表示 4](#d4))。`WImageIcon` の画像を読み込み終えたときも、アイコンを表示している部品を並べ直します (`c906b813`)。
- 文書の節: [4.2](swing_to_winui4k.md#label-basic)

<a id="a-after-show-dialog"></a>
#### API 40. 表示した直後のウィンドウに、ダイアログを開くと例外になる (直した: `e652273a`)

- 直した後: 持ち主が表示したウィンドウの中身に入っていれば、`setVisible(true)` の直後・`invokeLater` の中・`windowOpened` の中でも例外にならず、`show` は並べた後で開き、`showAndWait` (`WOptionPane` を含む) は中身を読み込むまで待ってから開きます。どのウィンドウにも載っていない持ち主は、これまでどおり `IllegalArgumentException` です。
- 文書の節: [25.2](swing_to_winui4k.md#dialog-basic)、[26.2](swing_to_winui4k.md#optionpane-message)

<a id="a-after-show-menu"></a>
#### API 41. 表示した直後にメニューやポップアップを開いても、黙って開かない (メニューは直した: `e065f680`)

- 直した後: 表示した直後の `WMenuBarItem.setPopupMenuVisible(true)`・`doClick()` と、`WMenuFlyout` などのフライアウトの `show` / `showAt` は、頼みを覚えて並べた後で開きます。開くまでの間も `isPopupMenuVisible()` は true です。
- 残る不自然さ: `WToolTip.setOpen(true)`・`WCommandBar.setOpen(true)` (「…」のメニュー)・`WTeachingTip.show()` は、表示した直後に呼ぶと黙って開きません。文書のサンプルはまだ `WinUiUtilities.schedule(500, ...)` で待っています。ボタンの `setFlyout` で付けたフライアウトも、表示した直後の `doClick()` では開きません (撮り直しで確かめた。[22.5](swing_to_winui4k.md#popup-button) のサンプルも待っている)。
- 直す案: ほかの操作と同じく、最初のレイアウトの前の頼みを覚えて、並べた後で開く。
- 文書の節: [21.2](swing_to_winui4k.md#menu-basic)、[22.3](swing_to_winui4k.md#popup-mouse)、[29.4](swing_to_winui4k.md#tooltip-show)、[23.6](swing_to_winui4k.md#toolbar-winui)

<a id="a-before-show-selection"></a>
#### API 42. 表示する前の選択やスクロールが、画面に写らない (直した: `3c075d1f`・`a343108e`)

- 直した後: 表示する前・ウィンドウに入れた直後の `WList` / `WListBox` の `ensureIndexIsVisible`・`setSelectedValue(..., true)`・`scrollIntoView` は、覚えておいて並べた後でスクロールします。表示する前の `WTable.setRowSelectionInterval` は、並べた後で表の選択に写し直します。表示する前の `clearSelection` は、選択のモデルからも外します ([表示 7](#d7))。
- 文書の節: [3.7](swing_to_winui4k.md#list-select)、[19.3](swing_to_winui4k.md#table-model)、[19.5](swing_to_winui4k.md#table-selection)

<a id="a-after-show-position"></a>
#### API 43. 表示した直後のスクロールの位置・仕切りの位置が効かない (直した: `0c0c992d`・`9092016b`)

- 直した後: 表示した直後の `WViewport.viewPosition`・`WScrollPaneBar.value` は、頼まれた位置を覚えて並べた後で当て、その間も頼んだ値を返します。`WLayoutPanel` の中の `WSplitPane` は割り当てられた大きさで仕切りを決めるので、表示した直後の `setDividerLocation(200)` も、表示する前の割合 (`setResizeWeight` と組み合わせても) も効きます ([表示 7](#d7)・[表示 8](#d8))。
- 残る不自然さ: 表示した直後 (最初のレイアウトの前) は中身の大きさが決まっていないので、スクロールバーの `getMaximum()` と `WViewport.getViewSize()` は 0 を返します。「開いたら末尾までスクロールする」(`setValue(getMaximum())`) は、まだ待たないと書けません ([16.2](swing_to_winui4k.md#scrollbar-bottom))。最初の `getLastDividerLocation()` は、Swing の 0 ではなく -1 です。
- 直す案: 末尾へのスクロールを頼む API (`scrollToBottom` など) を足すか、最初のレイアウトの前の `getMaximum()` を中身の希望の大きさから求める。
- 文書の節: [15.4](swing_to_winui4k.md#scroll-position)、[16.2](swing_to_winui4k.md#scrollbar-bottom)、[17.4](swing_to_winui4k.md#split-listener)、[17.3](swing_to_winui4k.md#split-vertical)

<a id="a-scroll-listener"></a>
#### API 44. スクロールのリスナーが、後から呼ばれる

- Swing: `JViewport.setViewPosition`・`JScrollBar.setValue` の中で `ChangeListener`・`AdjustmentListener` が呼ばれる。
- WinUI4K: `WViewport.addChangeListener` ([`WViewport.kt:51`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WViewport.kt#L51)) と `WScrollPaneBar.addAdjustmentListener` は ScrollViewer の ViewChanged で呼ばれ、設定から戻った時点ではまだ呼ばれていない。中身の大きさだけが変わったときは呼ばれない。`getAdjustmentType()` はいつも `TRACK`。
- 何が不自然か: 設定の直後にリスナーの結果 (ラベルの文字など) を読むコードは動きません。
- 直す案: 設定のときは、ViewChanged を待たずにその場でリスナーを呼ぶ (後から来る ViewChanged は、値が同じなら知らせない)。
- 文書の節: [15.5](swing_to_winui4k.md#scroll-listener)、[16.3](swing_to_winui4k.md#scrollbar-listener)

<a id="api-missing"></a>
### 2.5 Swing にあって WinUI4K に無い

<a id="a-inputverifier"></a>
#### API 45. ★ `InputVerifier` が無い

- Swing: `JComponent.setInputVerifier(InputVerifier)` で、検証に失敗したらフォーカスを残す。
- WinUI4K: 無い (`addFocusListener` の focusLost で代わりに書く)。
- 直す案: `WInputVerifier` を足し、WinUI の LosingFocus (`Cancel = true`) で実現する。
- 文書の節: [2.10](swing_to_winui4k.md#textfield-validate)

<a id="a-timer"></a>
#### API 46. ★ 繰り返しの `Timer`・`invokeAndWait`・`SwingWorker` が無い

- Swing: `javax.swing.Timer(ms, listener)` (`start()` / `stop()`、繰り返し)・`SwingUtilities.invokeAndWait`・`SwingWorker<T, V>` (`publish` / `process` / 進み具合)。
- WinUI4K: `WinUiUtilities.schedule(delayMillis, Runnable): AutoCloseable` ([`WinUiUtilities.kt:180`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WinUiUtilities.kt#L180)) は 1 回だけ。`invokeLater` ([`WinUiUtilities.kt:168`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WinUiUtilities.kt#L168)) はある。`invokeAndWait` と `SwingWorker` に当たるものは無い (`CompletableFuture` + `invokeLater` で書く)。
- 何が不自然か: 時計や点滅、裏の処理の進み具合のような、よく使う書き方を手で組み直す必要があり、書き換えが機械的になりません。
- 直す案: `WTimer` (繰り返しと `start()` / `stop()`)、`invokeAndWait`、`WSwingWorker` (または `WinUiUtilities.runInBackground`) を足す。
- 文書の節: [0.2](swing_to_winui4k.md#basics-differences)、[1.13](swing_to_winui4k.md#button-background)、[11.5](swing_to_winui4k.md#progress-background)

<a id="a-label-background"></a>
#### API 47. ★ ラベルやパネルに、背景・枠・内側の余白を付けられない

- Swing: `label.setOpaque(true); label.setBackground(Color.YELLOW);`・`label.setBorder(...)`・`panel.setBorder(createLineBorder(...))`・`createEmptyBorder(...)`・`createTitledBorder(...)`。
- WinUI4K: `background`・`border` は `WControl` ([`WControl.kt:138`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WControl.kt#L138)、[`WControl.kt:176`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WControl.kt#L176)) にだけあり、`WLabel` (`WComponent` の派生) と `WLayoutPanel` (`WContainer` の派生) には無い。線の枠は `WBorder` で包み、`WLayoutPanel` の余白は `insets` ([`WLayoutPanel.kt:33`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WLayoutPanel.kt#L33))。`WPanel` には内側の余白が無い。`TitledBorder` は無い。
- 何が不自然か: 色の付いた見出しや状態の帯、パネルに枠を付ける 1 行が、別の部品で包む書き方に変わります。`WLayoutPanel` の不具合 ([API 21](#a-nest)) を避けて `WPanel` を使うと、余白を外側の `setMargin` で取るしかありません。
- 直す案: `WLabel` に `background`・`border` (`setOpaque` は既定 false) を足す。`WLayoutPanel.setBorder(WControlBorder)` (空の枠は `insets`、線の枠は中で `Border` に包む) と `WBorderFactory.createTitledBorder` を足す。`WPanel` に `padding` を足す。
- 文書の節: [14.3](swing_to_winui4k.md#panel-border)、[14.4](swing_to_winui4k.md#panel-background)、[14.5](swing_to_winui4k.md#panel-padding)、[4.9](swing_to_winui4k.md#label-notes)

<a id="a-layouts"></a>
#### API 48. ★ `CardLayout` などのレイアウト マネージャーが無い

- Swing: `CardLayout` (`show` / `next` / `previous`)・`GroupLayout`・`OverlayLayout`・`GridBagLayout`・`SpringLayout`。
- WinUI4K: `CardLayout`・`GroupLayout`・`OverlayLayout` は無い。`GridBagLayout`・`SpringLayout` に当たるのはパネルの `WGrid`・`WRelativePanel` で、`setLayout` に渡せない。
- 何が不自然か: ウィザードや設定の画面でよく使う `CardLayout` を、`remove` / `add` で書き換える必要があります。
- 直す案: `WCardLayout` (全部の子を同じ大きさに置き、1 つだけ見せる) と `WOverlayLayout` を、Swing と同じ計算で足す。`GroupLayout` は `MigLayoutManager` を案内する。
- 文書の節: [30.1](swing_to_winui4k.md#layout-table)、[付録 B](swing_to_winui4k.md#none)

<a id="a-relativepanel"></a>
#### API 49. `SpringLayout` に当たる `WRelativePanel` に、距離・ベースライン・循環の検出が無い

- Swing: `SpringLayout.putConstraint(edge, c, pad, edge, anchor)`・`BASELINE`・`Spring`。
- WinUI4K: `WRelativePanel.placeRightOf` ([`WRelativePanel.kt:26`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WRelativePanel.kt#L26))・`alignVerticalCenterWith` ([`WRelativePanel.kt:54`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WRelativePanel.kt#L54)) など。
- 何が不自然か: 距離の引数が無く、子の `setMargin` で取ります。BASELINE の揃えがありません。2 つの部品が互いを参照すると、WinUI が未処理の例外を UI スレッドに投げてウィンドウが真っ黒になり、呼んだ場所では例外にならないので原因の行が分かりません ([表示 36](#d36))。
- 直す案: Kotlin の側で関係を覚えて、循環になる呼び出しをその場で `IllegalArgumentException` にする。クラスの説明に、距離とベースラインが無いことを書く。
- 文書の節: [30.7](swing_to_winui4k.md#layout-spring)

<a id="a-missing-table"></a>
#### API 50. そのほかの、Swing にあって無いもの

| 部品 | Swing の API | WinUI4K の今 | 直す案 | 文書の節 |
|---|---|---|---|---|
| 入力欄 | `JTextArea.setTabSize`、`JTextComponent.setCaretColor` / `setDisabledTextColor` | 無い (`setSelectionColor` だけある。[`WTextField.kt:549`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextField.kt#L549)) | `tabSize` を RichEditBox の DefaultTabStop で足す。キャレットの色は CaretBrush で | [12.1](swing_to_winui4k.md#textarea-table)、[2.1](swing_to_winui4k.md#textfield-table) |
| 入力欄 | `JFormattedTextField`、`JSpinner.NumberEditor(spinner, "0.00")` | 無い (数は `WSpinner` が近い) | `WFormattedTextField` を足す。`WSpinner` は NumberFormatter を包んで書式を足す | [付録 B](swing_to_winui4k.md#none)、[9.4](swing_to_winui4k.md#spinner-decimal) |
| 書式のある文 | `StyleConstants.setBackground` / `setStrikeThrough` / `setAlignment`、`addStyle` / `setLogicalStyle` / `setParagraphAttributes`、`insertIcon` / `insertComponent`、`StyledEditorKit.BoldAction` など | 属性は 6 つだけ ([`WTextPane.kt:37-38`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextPane.kt#L37-L38)、[`WStyleConstants.kt:7-8`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WStyleConstants.kt#L7-L8))。切り替えは `toggleSelectionBold()` / `toggleSelectionItalic()` ([`WTextPane.kt:740-750`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTextPane.kt#L740-L750)) だけで、下線の切り替えは無い | 背景・打ち消し線・段落の揃えから足す。`toggleSelectionUnderline()` と書式の Action を足す | [13.1](swing_to_winui4k.md#textpane-table)、[13.4](swing_to_winui4k.md#textpane-toggle) |
| HTML | `new JEditorPane("text/html", html)`・`setPage(url)`・`addHyperlinkListener` | 無い。近いのは `WRichTextBlock.addParagraph` と `ParagraphBuilder` ([`WRichTextBlock.kt:139`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WRichTextBlock.kt#L139)、[`WRichTextBlock.kt:223-247`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WRichTextBlock.kt#L223-L247)) で、太字と斜体を重ねられず、色・大きさ・リンク・段落の余白も付けられない | 入れ子の書式や `hyperlink(text, url)` を足す | [13.7](swing_to_winui4k.md#textpane-html) |
| 表 | `editCellAt`・`getTableHeader()`・`setRowHeight`・`scrollRectToVisible(getCellRect(...))`・`setAutoResizeMode` | 無い。行の高さは `density` ([`WTable.kt:753`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTable.kt#L753)) の 3 段、見出しは `isHeaderVisible` ([`WTable.kt:745`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTable.kt#L745)) だけ | TableView の ScrollIntoView・BeginEdit を使って `scrollRowToVisible`・`editCellAt` を足す | [19.1](swing_to_winui4k.md#table-table)、[19.7](swing_to_winui4k.md#table-edit) |
| 木 | `setEditable(true)`・`TreeCellEditor`・`startEditingAtPath` | 無い ([`WTree.kt:282`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTree.kt#L282)) | レンダラーに TextBox を出す形で足すか、代わりの方法を文書に示す | [20.5](swing_to_winui4k.md#tree-model) |
| スライダー・進捗 | `setPaintLabels`・`setLabelTable`・`setPaintTrack`・大小 2 段の目盛り、`JProgressBar(VERTICAL)` | 無い。`majorTickSpacing` があると `minorTickSpacing` は使われない ([`WSlider.kt:228-238`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WSlider.kt#L228-L238))。進捗バーの縦向きは無い ([`WProgressBar.kt:18`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WProgressBar.kt#L18)) | 目盛りの数字は Slider の外に TextBlock を並べて足す。縦は回転で | [10.3](swing_to_winui4k.md#slider-ticks)、[11.1](swing_to_winui4k.md#progress-table) |
| コンボ | `getEditor()`・`setPrototypeDisplayValue`・`setKeySelectionManager` | 無い。入力中の文字は `text` ([`WComboBox.kt:262`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L262))、幅は `setWidth` | `setPrototypeDisplayValue` を幅の計算として足す | [8.1](swing_to_winui4k.md#combo-table) |
| スクロール | `setUnitIncrement(16)`・`setBlockIncrement`・`setMaximum`・`setModel`、単独の `JScrollBar` | `WAdjustable` ([`WAdjustable.kt:8`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WAdjustable.kt#L8)) は `value` だけ書ける。単独のスクロールバーは無い | 増分は受け付ける (できなければ何もしない形でコンパイルだけ通す)。WinUI の ScrollBar を包んだ `WScrollBar` を足す | [16.1](swing_to_winui4k.md#scrollbar-table)、[16.7](swing_to_winui4k.md#scrollbar-notes) |
| 分割 | `setOneTouchExpandable(true)` | 無い | 仕切りに小さなボタンを 2 つ置いて足す | [17.5](swing_to_winui4k.md#split-nested)、[17.8](swing_to_winui4k.md#split-notes) |
| タブ | `tabs.remove(1)`、`setTabPlacement`・`setTabComponentAt`・`setBackgroundAt`・`setTabLayoutPolicy` | `remove(component)` ([`WTabView.kt:325`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTabView.kt#L325)) だけで、Java の `tabs.remove(1)` はコンパイルできない (位置で外すのは `removeTabAt`。[`WTabView.kt:319`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WTabView.kt#L319))。ほかは無い | `remove(int)` を足す。`setTabComponentAt` は TabViewItem.Header に部品を置いて足す | [18.4](swing_to_winui4k.md#tabbed-close)、[18.1](swing_to_winui4k.md#tabbed-table) |
| メニュー・ツールバー | サブメニューの `setPopupMenuVisible(true)` / `doClick()`、`new JToggleButton(icon, true)`、`new JButton(icon)`、左から並べるツールバー | `WMenuFlyoutSubItem` には無い (`WMenuBarItem` にはある)。`WAppBarToggleButton(label, icon)` ([`WAppBarToggleButton.kt:14`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WAppBarToggleButton.kt#L14)) に選んだ状態を渡す形は無い。`WAppBarButton` にアイコンだけの形は無い。`WCommandBar` を左に寄せる方法が無い ([表示 28](#d28)) | `isPopupMenuVisible` / `doClick` を足す。`(label, icon, selected)` と `(icon)` のコンストラクターを足す。左寄せ・ラベルを右に出す Swing 風の設定を足す | [21.4](swing_to_winui4k.md#menu-submenu)、[23.4](swing_to_winui4k.md#toolbar-toggle)、[23.2](swing_to_winui4k.md#toolbar-basic) |
| ツールチップ | `ToolTipManager` (`setInitialDelay`・`setDismissDelay`・`setEnabled(false)`)、`getToolTipText(MouseEvent)` の上書き、`getToolTipLocation` | 無い。文字のヒントをプログラムから出す口も無い (`WToolTip.isOpen` ([`WToolTip.kt:111`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WToolTip.kt#L111)) は `WToolTip` を付けたときだけ)。位置は 5 つ ([`WToolTip.kt:13-28`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WToolTip.kt#L13-L28)) で、ずらす量は指定できない | 全体の有効・無効を足す。ToolTip.Opened を出して文字を差し替えられるようにする。`showToolTip()`・offset を足す | [29.1](swing_to_winui4k.md#tooltip-table)、[29.4](swing_to_winui4k.md#tooltip-show)、[29.7](swing_to_winui4k.md#tooltip-notes) |
| ファイル・色 | `setAccessory`・`setFileView`・`setFileHidingEnabled`・埋め込み、`showDialog(c, title, initial, true)` (透明度)・`createDialog`・`setChooserPanels`・`setPreviewPanel` | ファイルのダイアログの作り替えは無い ([`WFileChooser.kt:34`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WFileChooser.kt#L34))。`WColorPicker.showDialog` は中でピッカーを作り ([`WColorPicker.kt:254-261`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WColorPicker.kt#L254-L261))、透明度を選ばせる口が無い | ファイルは OS の制約なので文書で明記。色は `showDialog(..., colorTransparencySelectionEnabled)` を足す | [27.8](swing_to_winui4k.md#filechooser-notes)、[28.4](swing_to_winui4k.md#colorchooser-alpha)、[28.7](swing_to_winui4k.md#colorchooser-notes) |
| ウィンドウ | `setMaximumSize`・`setIconImages`・`setOpacity`、`getRootPane()`、持ち主付きのトップレベル ウィンドウ | 無い (`WAppWindow.create(presenter, owner)` ([`WAppWindow.kt:123`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WAppWindow.kt#L123)) はあるが `WFrame` とつながっていない) | OverlappedPresenter の最大の大きさで `maximumSize` を足す。`getRootPane()` を薄いオブジェクトで返す | [24.1](swing_to_winui4k.md#frame-table)、[1.7](swing_to_winui4k.md#button-default) |
| リスト | `setLayoutOrientation(HORIZONTAL_WRAP)`・`setDragEnabled` | 無い (`WItemsView` を案内) | 文書で案内する | [3.17](swing_to_winui4k.md#list-notes) |

<a id="api-lang"></a>
### 2.6 Kotlin・Java から使うときの不便

<a id="a-tooltip-ambiguous"></a>
#### API 51. ★ Java の `component.setToolTip(null)` が曖昧でコンパイルできない

- Swing: `setToolTipText(null)` でヒントを消す (`JComponent` に `setToolTip` は無い)。
- WinUI4K: 文字のヒントのプロパティ `var toolTip: String?` ([`WComponent.kt:1227`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComponent.kt#L1227)。Java では `setToolTip(String)`) と `fun setToolTip(toolTip: WToolTip)` ([`WComponent.kt:1249`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComponent.kt#L1249)) が同じ名前。別名の `toolTipText` ([`WComponent.kt:1270`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComponent.kt#L1270)) もある。
- 何が不自然か: Java の `b.setToolTip(null)` は「reference to setToolTip is ambiguous」でコンパイルエラーです。Kotlin では `button.toolTip = WToolTip()` が型エラーで、読むと `String?` が返り、付けた `WToolTip` は取り出せません。
- 直す案: 文字のプロパティは `toolTipText` だけにし、`var toolTip: WToolTip?` (読むと付けた `WToolTip`、null で外す) にする。
- 文書の節: [29.3](swing_to_winui4k.md#tooltip-content)、[29.6](swing_to_winui4k.md#tooltip-kotlin)

<a id="a-actionlistener-kotlin"></a>
#### API 52. ★ Kotlin から `WActionListener` を `addActionListener` に渡せない

- Swing: `ActionListener l = ...; a.addActionListener(l); b.addActionListener(l);` (1 つのリスナーを共有する)。
- WinUI4K: `WActionListener` を受ける形は、Kotlin では `addActionListenerForJava` という名前 ([`WButtonBase.kt:399-407`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WButtonBase.kt#L399-L407)、`@JvmName("addActionListener")`)。Kotlin の `addActionListener` は関数の型だけ ([`WButtonBase.kt:383`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WButtonBase.kt#L383))。`WToggleSwitch.addActionListener(WActionListener)` ([`WToggleSwitch.kt:210`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WToggleSwitch.kt#L210)) は Kotlin でも同じ名前。
- 何が不自然か: Kotlin で `val l = WActionListener { ... }; button.addActionListener(l)` がコンパイルできません。部品によって違います。
- 直す案: `WActionListener` を受ける形を Kotlin からも同じ名前で見えるようにする (関数の型の形を `@JvmSynthetic` の拡張関数にするなど)。
- 文書の節: [1.9](swing_to_winui4k.md#button-command)、[1.15](swing_to_winui4k.md#button-kotlin)

<a id="a-lang-table"></a>
#### API 53. そのほかの Kotlin・Java の不便

| Swing | WinUI4K | 何が不自然か | 直す案 | 文書の節 |
|---|---|---|---|---|
| `new MigLayout("wrap 2", "[right][grow, fill]")` | `MigLayoutManager(layoutConstraints, columnConstraints, rowConstraints)` ([`MigLayoutManager.kt:50`](../winui4k-extension-miglayout/src/main/kotlin/com/appkitbox/winui4k/extension/miglayout/MigLayoutManager.kt#L50)) に `@JvmOverloads` が無い | Java から使えるのは引数なしと 3 つの形だけで、2 つの形がコンパイルできない。名前も `MigLayout` ではない | `@JvmOverloads` を付ける | [30.6](swing_to_winui4k.md#layout-gridbag) |
| `Point p = viewport.getViewPosition(); p.y`、`getViewSize().height` (int のフィールド) | `WPoint` ([`WAppWindow.kt:14`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WAppWindow.kt#L14)) は Kotlin の data class で Java からは `p.getY()`。`WSize` ([`WLayoutManager.kt:7`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WLayoutManager.kt#L7)) は `getHeight()` が double | ほぼ全部の行の書き換えになる | `@JvmField` を付けて `p.x` / `size.width` で読めるようにする | [15.4](swing_to_winui4k.md#scroll-position)、[16.4](swing_to_winui4k.md#scrollbar-sync) |
| (Kotlin から Swing の節を使うと `node.userObject`・`node.childCount` と書ける) | `tree.DefaultMutableTreeNode` は Kotlin の関数で作られ ([`tree/DefaultMutableTreeNode.kt:18-19`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/tree/DefaultMutableTreeNode.kt#L18-L19))、Kotlin からも `getUserObject()` と書く | Kotlin から Swing を使うより Java 寄りの書き方になり、`WTree` のプロパティと混ざる | 関数を残しつつ、Kotlin 向けの拡張プロパティを足す | [20.8](swing_to_winui4k.md#tree-kotlin) |
| `setRenderer(ListCellRenderer)` | `WComboBox.renderer: ((Any?) -> String)?` ([`WComboBox.kt:328`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WComboBox.kt#L328)) | Java の型は `kotlin.jvm.functions.Function1<Object, String>` で見慣れない | `java.util.function.Function<Object, String>` を受ける形を足す | [8.6](swing_to_winui4k.md#combo-renderer) |
| `char[] getPassword()` | Java は `getPassword(): CharArray` ([`WPasswordField.kt:118`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WPasswordField.kt#L118))、Kotlin は `password: String` ([`WPasswordField.kt:104`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WPasswordField.kt#L104)) | Kotlin と Java で型が違う。WinUI 側に String の写しが残るので、0 で埋めても消えない ([`WPasswordField.kt:44`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WPasswordField.kt#L44)) | 文書の 0 埋めのコードに、消えないことを書き添える | [2.15](swing_to_winui4k.md#textfield-password) |
| `KeyStroke.getKeyStroke("control Z")` | `WKeyStroke.getKeyStroke(s: String?): WKeyStroke?` ([`WKeyStroke.kt:169`](../winui4k/src/main/kotlin/com/appkitbox/winui4k/WKeyStroke.kt#L169)) | 書式が違うと null (Swing と同じ) なので、Kotlin では `!!` が要る | 今のままでよい | [2.12](swing_to_winui4k.md#textfield-keybinding) |

<a id="summary"></a>
## まとめ: 優先して直したいもの

2026-11-07 に直したもの (入れ子の `WLayoutPanel`・表示した後の `WLabel.setText`・表示する前の `setText`・起動した直後のフォーカスの枠・表示した直後のダイアログとメニュー・幅を指定しない部品・`setWrapStyleWord(true)`) は、表から外しました ([1.1](#display-bug))。

| 順 | 項目 | 種類 | 理由 |
|---|---|---|---|
| 1 | [`WFrame` の中身が `BorderLayout` でない](#a-frame-layout) | 既定値 | Swing のいちばんよくある書き方 (`frame.add(c, BorderLayout.NORTH)`) が実行時に例外になり、ウィンドウが出ない |
| 2 | [`new WTextField("…")` が案内文](#a-placeholder) | 意味の違い | 同じ形のコンストラクターで意味が違い、コンパイルが通る。`(String, int)` も無い |
| 3 | [`setHorizontalAlignment` が文字の揃えでない](#a-alignment) | 意味の違い | 機械的な書き換えでコンパイルが通り、黙って違う動きになる |
| 4 | [`new WLayoutPanel()` のレイアウトが null](#a-layoutpanel-null) | 既定値 | `new JPanel()` から移すと子が重なる・高さが 0 になる |
| 5 | [`WComboBox.removeItem(1)`・`getSelectedItem()`](#a-combo-remove) | 意味の違い | 黙って違う項目が消える。選んだ項目の型が違う |
| 6 | [`WColor.RED` などの値が違う](#a-color) | 意味の違い | 置き換えるだけで色が変わり、色を比べるコードやテストも変わる |
