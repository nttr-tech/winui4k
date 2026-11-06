# Swing から WinUI4K への書き換え例

この文書では、Swing のコンポーネントでよく書くコードと、それを WinUI4K で書いたコードを並べて示します。
コンポーネントごとに、実際のアプリで使う場面の大半 (目安として 8 割) をカバーするユースケースを選び、
Swing のコードと WinUI4K のコードを対にしました。

今の版で扱うコンポーネントは `JButton`・`JTextField`・`JList` の 3 つです。ほかのコンポーネントは順に追加します。

## 目次

- [この文書の読み方](#reading)
- [0. 共通の基本](#basics)
  - [0.1 最小のアプリ](#basics-minimal)
  - [0.2 Swing との主な違い](#basics-differences)
  - [0.3 Kotlin で書く](#basics-kotlin)
- [1. JButton → WButton](#button)
  - [1.1 API の対応表](#button-table)
  - [1.2 ボタンを作ってクリックに応える](#button-click)
  - [1.3 有効・無効を切り替える](#button-enabled)
  - [1.4 アイコンを付ける](#button-icon)
  - [1.5 ツールチップを出す](#button-tooltip)
  - [1.6 ニーモニックでキーボードから押す](#button-mnemonic)
  - [1.7 Enter で押される既定ボタン](#button-default)
  - [1.8 Action でボタンとメニューの処理を共有する](#button-action)
  - [1.9 1 つのリスナーで複数のボタンを処理する](#button-command)
  - [1.10 プログラムからクリックする](#button-doclick)
  - [1.11 フォント・色・平らなボタン](#button-appearance)
  - [1.12 大きさを決める](#button-size)
  - [1.13 時間のかかる処理を裏で動かす](#button-background)
  - [1.14 アクセントとフライアウト](#button-winui)
  - [1.15 Kotlin で書く](#button-kotlin)
  - [1.16 注意点](#button-notes)
- [2. JTextField → WTextField](#textfield)
  - [2.1 API の対応表](#textfield-table)
  - [2.2 入力欄を作って値を読み書きする](#textfield-basic)
  - [2.3 Enter で処理する](#textfield-enter)
  - [2.4 入力のたびに反応する](#textfield-change)
  - [2.5 入力できる文字を制限する](#textfield-filter)
  - [2.6 文字数を制限する](#textfield-maxlength)
  - [2.7 読み取り専用にする](#textfield-readonly)
  - [2.8 案内文 (プレースホルダー) と見出しを付ける](#textfield-placeholder)
  - [2.9 フォーカスを受けたら全選択する](#textfield-selectall)
  - [2.10 フォーカスを外したときに検証する](#textfield-validate)
  - [2.11 選択範囲とキャレットを操作する](#textfield-caret)
  - [2.12 Esc で入力を消すキーバインド](#textfield-keybinding)
  - [2.13 元に戻す・やり直す](#textfield-undo)
  - [2.14 文字の揃えと見た目](#textfield-appearance)
  - [2.15 JPasswordField → WPasswordField](#textfield-password)
  - [2.16 Kotlin で書く](#textfield-kotlin)
  - [2.17 注意点](#textfield-notes)
- [3. JList → WList](#list)
  - [3.1 API の対応表](#list-table)
  - [3.2 固定の項目を並べる](#list-basic)
  - [3.3 モデルで項目を追加・削除する](#list-model)
  - [3.4 選んだ項目を読む](#list-selected)
  - [3.5 選択の変化に反応する](#list-listener)
  - [3.6 複数選択を許す](#list-multiple)
  - [3.7 プログラムから選んでスクロールする](#list-select)
  - [3.8 ダブルクリックで開く](#list-doubleclick)
  - [3.9 独自のオブジェクトを並べて表示を変える](#list-renderer)
  - [3.10 見せる行の数と項目の高さ](#list-rows)
  - [3.11 Delete キーで選んだ項目を消す](#list-delete)
  - [3.12 項目を上下に動かす](#list-move)
  - [3.13 入力に合わせて絞り込む](#list-filter)
  - [3.14 右クリックメニューを付ける](#list-popup)
  - [3.15 項目のクリックとチェックボックスの選択](#list-winui)
  - [3.16 Kotlin で書く](#list-kotlin)
  - [3.17 注意点](#list-notes)
- [付録 A. 移植のチェックリスト](#checklist)

<a id="reading"></a>
## この文書の読み方

- 各ユースケースは「Swing (Java)」と「WinUI4K (Java)」のコードを並べます。
  WinUI4K は Swing と同じ名前の API を Java からそのまま呼べるので、2 つのコードの**違う行**が移植で直すところです。
- 各章の終わりに、同じ内容を Kotlin で書いた例を載せます。
- コード片は UI スレッドの中 (`SwingUtilities.invokeLater` / `WinUiUtilities.invokeLater` に渡す処理の中) で動かし、
  変数 `frame` (`JFrame` / `WFrame`) があり、[0.1](#basics-minimal) と同じく `FlowLayout` / `WFlowLayout` で並べるものとします。
- import は次のとおりです。WinUI4K のコードは `java.awt` と `javax.swing` を使いません。

  | | import |
  |---|---|
  | Swing | `javax.swing.*`、`javax.swing.event.*`、`javax.swing.text.*`、`java.awt.*`、`java.awt.event.*` |
  | WinUI4K | `com.appkitbox.winui4k.*` |

- 各ユースケースの「実行した画面」は、[`winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k) の
  `jbutton` / `jtextfield` / `jlist` のサンプル (`*Swing.java` と `*WinUI4K.java`) を実際に動かして撮った画像です。
  サンプルは、文書のコード片に、ウィンドウを作って部品を載せる処理と、見どころが画面に出るように起動時に 1 回だけ行う操作を足したものです。
  マウスもキーも使わず、起動した直後の画面を撮っているので、ツールチップやメニューのように操作で出るものは写っていません。
  Swing は既定の Look & Feel (Metal) のままです。撮り直すときは、リポジトリのルートで `.\gradlew :winui4k-all:shadowJar` の後に
  `pwsh winui4k\src\test\java\com\appkitbox\winui4k\swing_to_winui4k\capture.ps1` を実行します。
- WinUI4K の大きさと位置の単位は DIP (96 dpi の画面での 1 ピクセル) です。Swing のピクセルと同じ数値を指定すれば、
  ほぼ同じ見た目の大きさになります。

<a id="basics"></a>
## 0. 共通の基本

<a id="basics-minimal"></a>
### 0.1 最小のアプリ

ボタンを 1 つ置いたウィンドウを出すアプリです。

**Swing (Java)**

```java
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class HelloSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("サンプル");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            frame.add(new JButton("OK"));
            frame.setSize(400, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
```

**WinUI4K (Java)**

```java
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WinUiUtilities;

public class HelloWinUI {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("サンプル");
            frame.setLayout(new WFlowLayout());
            frame.add(new WButton("OK"));
            frame.setSize(400, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
```

<a id="basics-differences"></a>
### 0.2 Swing との主な違い

| 項目 | Swing | WinUI4K |
|---|---|---|
| UI スレッドへの投入 | `SwingUtilities.invokeLater` | `WinUiUtilities.invokeLater` (WinUI がまだ動いていなければ起動してから実行する) |
| UI スレッドかどうか | `SwingUtilities.isEventDispatchThread()` | `WinUiUtilities.isDispatchThread()` |
| × で閉じたとき | 既定は `HIDE_ON_CLOSE`。アプリを終えるには `EXIT_ON_CLOSE` を指定する | 既定は `DISPOSE_ON_CLOSE`。最後のウィンドウが閉じるとアプリが終わる (`WinUiUtilities.setExitOnLastWindowClosed(false)` で止められる) |
| ウィンドウの中身の既定 | `BorderLayout`。制約なしの `frame.add(c)` は中央 | `WBorderLayout` (Swing と同じ)。制約なしの `frame.add(c)` は中央 |
| `BorderLayout` で並べる | `frame.add(c, BorderLayout.NORTH)` | `frame.add(c, WBorderLayout.NORTH)` |
| パネル | `new JPanel()` (`FlowLayout`) | `new WLayoutPanel()` (`WFlowLayout`。Swing と同じ)。`new WLayoutPanel(layout)` は `new JPanel(layout)` に当たる |
| 縦か横に一列に並べる | `Box` / `BoxLayout` | `WBox` / `WBoxLayout`。WinUI の StackPanel を包んだ `WPanel` (`new WPanel(8.0, Orientation.HORIZONTAL)` など。8 は子の間隔) も使える |
| 初めのフォーカス | ウィンドウがアクティブになると最初の部品 | 同じ。別の部品にするときは、表示した直後に `requestFocusInWindow()` を呼ぶ |
| 時間待ちの実行 | `javax.swing.Timer` | `WinUiUtilities.schedule(ミリ秒, 処理)` (1 回だけ実行。戻り値の `close()` で取り消せる) |

`WPanel` の縦の並びでは、幅を指定した子は**中央寄せ**になります。左に寄せるときは
`component.setHorizontalAlignment(HorizontalAlignment.LEFT)` を指定します。

<a id="basics-kotlin"></a>
### 0.3 Kotlin で書く

WinUI4K は Kotlin で書かれており、Kotlin からはプロパティとラムダで書けます。

```kotlin
import com.appkitbox.winui4k.WButton
import com.appkitbox.winui4k.WFlowLayout
import com.appkitbox.winui4k.WFrame
import com.appkitbox.winui4k.WinUiUtilities

fun main() {
    WinUiUtilities.invokeLater {
        val frame = WFrame("サンプル")
        frame.layout = WFlowLayout()
        frame.add(WButton("OK"))
        frame.setSize(400, 300)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}
```

Java の `getText()` / `setText(...)` は Kotlin では `text`、`isEnabled()` / `setEnabled(...)` は `isEnabled` のように、
プロパティとして読み書きします。

<a id="button"></a>
## 1. JButton → WButton

`WButton` は WinUI の `Button` を包んだクラスで、`JButton` の主な API を同じ名前で持っています。

<a id="button-table"></a>
### 1.1 API の対応表

| やりたいこと | Swing | WinUI4K | 違い |
|---|---|---|---|
| 文字のボタン | `new JButton("OK")` | `new WButton("OK")` | 同じ |
| 文字とアイコン | `new JButton("保存", icon)` | `new WButton("保存", Symbol.SAVE)` | アイコンは `Symbol` (組み込み)・`WImageIcon` (画像)・`WFontIcon` (グリフ) |
| クリックに応える | `addActionListener(e -> ...)` | `addActionListener(e -> ...)` | 引数なしの `() -> ...` (`Runnable`) も渡せる |
| 有効・無効 | `setEnabled(false)` | `setEnabled(false)` | 同じ |
| ツールチップ | `setToolTipText("...")` | `setToolTipText("...")` | 同じ |
| ニーモニック | `setMnemonic(KeyEvent.VK_S)` | `setMnemonic('S')` / `setMnemonic(WKeyEvent.VK_S)` | 文字に下線は引かれず、Alt を押すとキーのヒントが出る |
| 既定ボタン | `frame.getRootPane().setDefaultButton(b)` | `frame.setDefaultButton(b)` | 既定ボタンはアクセントの色になる |
| Action | `new JButton(action)` | `new WButton(action)` | `AbstractAction` → `WAbstractAction` |
| コマンド名 | `setActionCommand("save")` | `setActionCommand("save")` | 同じ |
| プログラムからクリック | `doClick()` | `doClick()` | 同じ |
| フォント | `setFont(new Font(...))` | `setFont(new WFont(...))` | `Font` → `WFont` |
| 色 | `setBackground(Color.RED)` | `setBackground(WColor.RED)` | `Color` → `WColor` |
| 平らなボタン | `setContentAreaFilled(false)` | `setContentAreaFilled(false)` | 同じ |
| 枠を消す | `setBorderPainted(false)` | `setBorderPainted(false)` | 同じ |
| フォーカスの枠を消す | `setFocusPainted(false)` | `setFocusPainted(false)` | 同じ |
| アイコンと文字の位置 | `setHorizontalTextPosition(SwingConstants.CENTER)` | `setHorizontalTextPosition(HorizontalTextPosition.CENTER)` | 定数が列挙型になる |
| 大きさ | `setPreferredSize(new Dimension(120, 32))` | `setPreferredSize(new WSize(120, 32))` / `setWidth(120)` | `Dimension` → `WSize` |
| 強調したボタン | (なし) | `setAccent(true)` | WinUI のアクセントのスタイル |
| 押すと開くメニュー | `JPopupMenu` を自分で出す | `setFlyout(flyout)` | WinUI のフライアウト |

<a id="button-click"></a>
### 1.2 ボタンを作ってクリックに応える

いちばんよく書くコードです。押すとラベルの文字を変えます。

**Swing (Java)**

```java
JLabel message = new JLabel("まだ押していません");
JButton button = new JButton("押してください");
button.addActionListener(e -> message.setText("押されました"));

frame.add(button);
frame.add(message);
```

**WinUI4K (Java)**

```java
WLabel message = new WLabel("まだ押していません");
WButton button = new WButton("押してください");
button.addActionListener(e -> message.setText("押されました"));

frame.add(button);
frame.add(message);
```

**実行した画面** ([`ClickSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/ClickSwing.java) / [`ClickWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/ClickWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Click (Swing)](images/swing_to_winui4k/JButton/ClickSwing.png) | ![Click (WinUI4K)](images/swing_to_winui4k/JButton/ClickWinUI4K.png) |

イベントを使わないなら、引数のないラムダも渡せます。

```java
button.addActionListener(() -> message.setText("押されました"));
```

リスナーを外すときは、渡したものと同じオブジェクトを `removeActionListener` に渡します (Swing と同じ)。

<a id="button-enabled"></a>
### 1.3 有効・無効を切り替える

チェックボックスの状態に合わせて「次へ」ボタンを有効にします。

**Swing (Java)**

```java
JCheckBox agree = new JCheckBox("利用規約に同意する");
JButton next = new JButton("次へ");
next.setEnabled(false);
agree.addItemListener(e -> next.setEnabled(agree.isSelected()));
```

**WinUI4K (Java)**

```java
WCheckBox agree = new WCheckBox("利用規約に同意する");
WButton next = new WButton("次へ");
next.setEnabled(false);
agree.addItemListener(e -> next.setEnabled(agree.isSelected()));
```

**実行した画面** ([`EnabledSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/EnabledSwing.java) / [`EnabledWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/EnabledWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Enabled (Swing)](images/swing_to_winui4k/JButton/EnabledSwing.png) | ![Enabled (WinUI4K)](images/swing_to_winui4k/JButton/EnabledWinUI4K.png) |

無効なボタンは、クリックしても `doClick()` を呼んでもリスナーが呼ばれません (Swing と同じ)。

<a id="button-icon"></a>
### 1.4 アイコンを付ける

**Swing (Java)**

```java
JButton save = new JButton("保存", new ImageIcon("icons/save.png"));
JButton delete = new JButton(new ImageIcon("icons/delete.png"));  // アイコンだけ
JButton top = new JButton("ダウンロード", new ImageIcon("icons/download.png"));
top.setHorizontalTextPosition(SwingConstants.CENTER);
top.setVerticalTextPosition(SwingConstants.BOTTOM);              // 文字をアイコンの下に
```

**WinUI4K (Java)**

```java
WButton save = new WButton("保存", Symbol.SAVE);                  // Windows の組み込みのアイコン
WButton delete = new WButton(Symbol.DELETE);                      // アイコンだけ
WButton top = new WButton("ダウンロード", new WImageIcon("icons/download.png"));  // 画像ファイル
top.setHorizontalTextPosition(HorizontalTextPosition.CENTER);
top.setVerticalTextPosition(VerticalTextPosition.BOTTOM);
```

**実行した画面** ([`IconSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/IconSwing.java) / [`IconWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/IconWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Icon (Swing)](images/swing_to_winui4k/JButton/IconSwing.png) | ![Icon (WinUI4K)](images/swing_to_winui4k/JButton/IconWinUI4K.png) |

WinUI4K の画面では、3 つ目のボタンの文字が出ず、画像も小さく切れています。文字をアイコンの下に置いた (`HorizontalTextPosition.CENTER` と `VerticalTextPosition.BOTTOM`) 画像のアイコンのボタンを、`WFlowLayout` などで並べるパネルや縦に積む `WPanel` に置いたときに起きる、今の版の WinUI4K の不具合です。`new WPanel(8.0, Orientation.HORIZONTAL)` のような横に並べる `WPanel` に入れると、正しく表示されます。

- `Symbol` は Windows のアイコン フォント (Segoe Fluent Icons) のアイコンで、テーマの文字色に合わせて描かれます。
  画像を用意しなくてよいので、まず `Symbol` に合うものがないか探すのがおすすめです。
- 画像のアイコンは `WImageIcon` です。`ImageIcon(String)` と同じくファイルのパスを渡し、元の大きさのまま表示します。
- フォントのグリフを使うときは `new WFontIcon("")` のように書きます。
- アイコンと文字の間隔は `setIconTextGap(4)` で変えられます (既定は Swing と同じ 4)。
- 押している間・無効なときのアイコンは `setSelectedIcon` / `setDisabledIcon` で替えられます。

<a id="button-tooltip"></a>
### 1.5 ツールチップを出す

**Swing (Java)**

```java
JButton refresh = new JButton("更新");
refresh.setToolTipText("一覧を最新の状態にします (F5)");
```

**WinUI4K (Java)**

```java
WButton refresh = new WButton("更新");
refresh.setToolTipText("一覧を最新の状態にします (F5)");
```

**実行した画面** ([`ToolTipSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/ToolTipSwing.java) / [`ToolTipWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/ToolTipWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![ToolTip (Swing)](images/swing_to_winui4k/JButton/ToolTipSwing.png) | ![ToolTip (WinUI4K)](images/swing_to_winui4k/JButton/ToolTipWinUI4K.png) |

WinUI のツールチップ (`ToolTip`) で表示します。表示の位置などを細かく決めるときは、`WToolTip` を作って `setToolTip(toolTip)` に渡します。

<a id="button-mnemonic"></a>
### 1.6 ニーモニックでキーボードから押す

Alt+S で「保存」ボタンを押せるようにします。

**Swing (Java)**

```java
JButton save = new JButton("保存(S)");
save.setMnemonic(KeyEvent.VK_S);
```

**WinUI4K (Java)**

```java
WButton save = new WButton("保存(S)");
save.setMnemonic('S');            // WKeyEvent.VK_S を渡してもよい
```

**実行した画面** ([`MnemonicSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/MnemonicSwing.java) / [`MnemonicWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/MnemonicWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Mnemonic (Swing)](images/swing_to_winui4k/JButton/MnemonicSwing.png) | ![Mnemonic (WinUI4K)](images/swing_to_winui4k/JButton/MnemonicWinUI4K.png) |

WinUI のアクセス キー (`AccessKey`) で実現しています。Swing と違い、ラベルの文字に下線は引かれません。
Alt を押すとボタンの上にキーのヒント (「S」) が出て、続けて S を押すとクリックされます。

<a id="button-default"></a>
### 1.7 Enter で押される既定ボタン

ダイアログの「OK」のように、Enter キーで押されるボタンです。

**Swing (Java)**

```java
JButton ok = new JButton("OK");
JButton cancel = new JButton("キャンセル");
frame.getRootPane().setDefaultButton(ok);
```

**WinUI4K (Java)**

```java
WButton ok = new WButton("OK");
WButton cancel = new WButton("キャンセル");
frame.setDefaultButton(ok);
```

**実行した画面** ([`DefaultButtonSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/DefaultButtonSwing.java) / [`DefaultButtonWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/DefaultButtonWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![DefaultButton (Swing)](images/swing_to_winui4k/JButton/DefaultButtonSwing.png) | ![DefaultButton (WinUI4K)](images/swing_to_winui4k/JButton/DefaultButtonWinUI4K.png) |

- 既定ボタンは WinUI のアクセントの色 (Windows の設定の色) で表示されます。
- 1 行の `WTextField` で Enter を押したとき、その入力欄に `ActionListener` が無ければ既定ボタンが押されます (`JTextField` と同じ)。
- ほかのボタンにフォーカスがあるときは、Enter でそのボタンが押されます。

<a id="button-action"></a>
### 1.8 Action でボタンとメニューの処理を共有する

ボタンとメニューの両方から同じ処理を呼び、無効にするときは両方まとめて無効にします。

**Swing (Java)**

```java
Action saveAction = new AbstractAction("保存", new ImageIcon("icons/save.png")) {
    @Override
    public void actionPerformed(ActionEvent e) {
        save();
    }
};
saveAction.putValue(Action.SHORT_DESCRIPTION, "ファイルを保存します");

JButton saveButton = new JButton(saveAction);
JMenuItem saveItem = new JMenuItem(saveAction);

saveAction.setEnabled(false);   // ボタンとメニューの両方が無効になる
```

**WinUI4K (Java)**

```java
WAction saveAction = new WAbstractAction("保存", Symbol.SAVE) {
    @Override
    public void actionPerformed(WActionEvent e) {
        save();
    }
};
saveAction.putValue(WAction.SHORT_DESCRIPTION, "ファイルを保存します");

WButton saveButton = new WButton(saveAction);
WMenuFlyoutItem saveItem = new WMenuFlyoutItem(saveAction);

saveAction.setEnabled(false);   // ボタンとメニューの両方が無効になる
```

**実行した画面** ([`ActionSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/ActionSwing.java) / [`ActionWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/ActionWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Action (Swing)](images/swing_to_winui4k/JButton/ActionSwing.png) | ![Action (WinUI4K)](images/swing_to_winui4k/JButton/ActionWinUI4K.png) |

- `NAME` は文字、`SMALL_ICON` はアイコン、`SHORT_DESCRIPTION` はツールチップ、`MNEMONIC_KEY` はニーモニックになります (Swing と同じ)。
- アイコンだけのボタンにするときは `saveButton.setHideActionText(true)` を指定します。
- メニュー バーのメニュー項目は `WMenuFlyoutItem` です (`WMenuBar` に入れて `frame.setJMenuBar(menuBar)` で付けます)。

<a id="button-command"></a>
### 1.9 1 つのリスナーで複数のボタンを処理する

電卓のように、たくさんのボタンを 1 つのリスナーで受けます。

**Swing (Java)**

```java
JTextField display = new JTextField(10);
ActionListener append = e -> display.setText(display.getText() + e.getActionCommand());

JPanel keys = new JPanel(new GridLayout(0, 3, 4, 4));
for (int i = 1; i <= 9; i++) {
    JButton key = new JButton(String.valueOf(i));
    key.addActionListener(append);      // actionCommand は既定でボタンの文字
    keys.add(key);
}
```

**WinUI4K (Java)**

```java
WTextField display = new WTextField(10);
WActionListener append = e -> display.setText(display.getText() + e.getActionCommand());

WLayoutPanel keys = new WLayoutPanel(new WGridLayout(0, 3, 4, 4));
for (int i = 1; i <= 9; i++) {
    WButton key = new WButton(String.valueOf(i));
    key.addActionListener(append);      // actionCommand は既定でボタンの文字
    keys.add(key);
}
```

**実行した画面** ([`ActionCommandSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/ActionCommandSwing.java) / [`ActionCommandWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/ActionCommandWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![ActionCommand (Swing)](images/swing_to_winui4k/JButton/ActionCommandSwing.png) | ![ActionCommand (WinUI4K)](images/swing_to_winui4k/JButton/ActionCommandWinUI4K.png) |

WinUI4K の画面では、数字のボタンが出ていません。`WLayoutPanel` の中に入れた `WLayoutPanel` (`JPanel` の中の `JPanel` に当たる) が、表示した後に中の子の大きさに合わせて広がらない、今の版の WinUI4K の不具合です。

- `actionCommand` を設定しないときは、ボタンの文字が `e.getActionCommand()` になります (Swing と同じ)。
  文字と違うコマンド名にするときは `key.setActionCommand("digit-1")` を指定します。
- どのボタンが押されたかは `e.getSource()` でも分かります。
- レイアウト マネージャーを使うパネルは `WLayoutPanel` です (`new JPanel(layout)` に当たります)。引数なしの `new WLayoutPanel()` は、`new JPanel()` と同じく `WFlowLayout` で並べます。

<a id="button-doclick"></a>
### 1.10 プログラムからクリックする

**Swing (Java)**

```java
search.doClick();
```

**WinUI4K (Java)**

```java
search.doClick();
```

**実行した画面** ([`DoClickSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/DoClickSwing.java) / [`DoClickWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/DoClickWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![DoClick (Swing)](images/swing_to_winui4k/JButton/DoClickSwing.png) | ![DoClick (WinUI4K)](images/swing_to_winui4k/JButton/DoClickWinUI4K.png) |

マウスで押したときと同じ順にリスナーが呼ばれます。Swing の `doClick(pressTime)` は押した見た目のまま UI スレッドを止めますが、
WinUI4K は押した見た目を作らず、UI スレッドも止めません。

<a id="button-appearance"></a>
### 1.11 フォント・色・平らなボタン

**Swing (Java)**

```java
JButton danger = new JButton("削除");
danger.setFont(new Font("Yu Gothic UI", Font.BOLD, 14));
danger.setForeground(Color.WHITE);
danger.setBackground(new Color(196, 43, 28));

JButton link = new JButton("詳しく見る");
link.setContentAreaFilled(false);     // 背景を塗らない
link.setBorderPainted(false);         // 枠を描かない
link.setFocusPainted(false);          // フォーカスの枠を描かない
```

**WinUI4K (Java)**

```java
WButton danger = new WButton("削除");
danger.setFont(new WFont("Yu Gothic UI", WFont.BOLD, 14));
danger.setForeground(WColor.WHITE);
danger.setBackground(new WColor(196, 43, 28));

WButton link = new WButton("詳しく見る");
link.setContentAreaFilled(false);
link.setBorderPainted(false);
link.setFocusPainted(false);
```

**実行した画面** ([`AppearanceSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/AppearanceSwing.java) / [`AppearanceWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/AppearanceWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Appearance (Swing)](images/swing_to_winui4k/JButton/AppearanceSwing.png) | ![Appearance (WinUI4K)](images/swing_to_winui4k/JButton/AppearanceWinUI4K.png) |

- WinUI4K はマウスを重ねたときと押したときの背景色も、指定した色から作ります。
- `setBackground(null)` / `setForeground(null)` でテーマの色に戻ります。
- `setContentAreaFilled(false)` のボタンは WinUI の「Subtle」のスタイル (マウスを重ねたときだけ背景が出る) になります。
- 色を固定すると、ダーク テーマに切り替えたときに読みにくくなることがあります。強調だけなら [1.14](#button-winui) のアクセントを使う方が、
  テーマに合わせて色が変わります。

<a id="button-size"></a>
### 1.12 大きさを決める

**Swing (Java)**

```java
JButton ok = new JButton("OK");
ok.setPreferredSize(new Dimension(120, 32));
```

**WinUI4K (Java)**

```java
WButton ok = new WButton("OK");
ok.setPreferredSize(new WSize(120, 32));
// 幅だけなら
ok.setWidth(120);
```

**実行した画面** ([`SizeSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/SizeSwing.java) / [`SizeWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/SizeWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Size (Swing)](images/swing_to_winui4k/JButton/SizeSwing.png) | ![Size (WinUI4K)](images/swing_to_winui4k/JButton/SizeWinUI4K.png) |

`setPreferredSize` は幅と高さを固定します。幅か高さの片方だけを固定するときは `setWidth` / `setHeight` を使います。
最小・最大の大きさは `setMinimumSize` / `setMaximumSize` です。

<a id="button-background"></a>
### 1.13 時間のかかる処理を裏で動かす

ボタンを押したらネットワークからデータを取り、取り終えたら結果を表示します。
処理の間はボタンを無効にします。

**Swing (Java)**

```java
JLabel status = new JLabel();
JButton download = new JButton("ダウンロード");
download.addActionListener(e -> {
    download.setEnabled(false);
    status.setText("ダウンロード中...");
    new SwingWorker<String, Void>() {
        @Override
        protected String doInBackground() throws Exception {
            return fetch();                       // 別のスレッドで動く
        }

        @Override
        protected void done() {                   // UI スレッドで動く
            download.setEnabled(true);
            try {
                status.setText(get());
            } catch (Exception ex) {
                status.setText("失敗しました: " + ex.getMessage());
            }
        }
    }.execute();
});
```

**WinUI4K (Java)**

```java
WLabel status = new WLabel();
WButton download = new WButton("ダウンロード");
download.addActionListener(e -> {
    download.setEnabled(false);
    status.setText("ダウンロード中...");
    CompletableFuture
        .supplyAsync(() -> fetch())               // 別のスレッドで動く
        .whenComplete((result, error) -> WinUiUtilities.invokeLater(() -> {   // UI スレッドへ戻す
            download.setEnabled(true);
            status.setText(error == null ? result : "失敗しました: " + error.getMessage());
        }));
});
```

**実行した画面** ([`BackgroundSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/BackgroundSwing.java) / [`BackgroundWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/BackgroundWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Background (Swing)](images/swing_to_winui4k/JButton/BackgroundSwing.png) | ![Background (WinUI4K)](images/swing_to_winui4k/JButton/BackgroundWinUI4K.png) |

WinUI4K には `SwingWorker` に当たるクラスが無いので、`CompletableFuture` や `ExecutorService` で裏の処理を動かし、
画面を触る処理は `WinUiUtilities.invokeLater` で UI スレッドへ戻します。UI スレッドの外から `W*` クラスを触ってはいけないのは
Swing と同じです。

<a id="button-winui"></a>
### 1.14 アクセントとフライアウト

Swing には無いが、WinUI ではよく使う機能です。

```java
// 画面でいちばん大事な操作を、Windows の設定のアクセントの色で強調する
WButton submit = new WButton("送信");
submit.setAccent(true);

// 押すとメニューを開くボタン (Swing では JPopupMenu を自分で show する)
WMenuFlyout menu = new WMenuFlyout();
menu.add("PDF で書き出す").addActionListener(() -> exportPdf());
menu.add("CSV で書き出す").addActionListener(() -> exportCsv());
WButton export = new WButton("書き出す");
export.setFlyout(menu);
```

**実行した画面** ([`AccentFlyoutWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jbutton/AccentFlyoutWinUI4K.java))

![AccentFlyout (WinUI4K)](images/swing_to_winui4k/JButton/AccentFlyoutWinUI4K.png)

右に下向きの矢印が付いたボタンにするときは `WDropDownButton`、本体と矢印の部分を分けたボタンは `WSplitButton` を使います。

<a id="button-kotlin"></a>
### 1.15 Kotlin で書く

この章のユースケースをまとめて Kotlin で書くと、次のようになります。

```kotlin
import com.appkitbox.winui4k.*
import java.util.concurrent.CompletableFuture

fun main() {
    WinUiUtilities.invokeLater {
        val frame = WFrame("ボタンのサンプル")
        frame.layout = WFlowLayout()
        val status = WLabel("まだ押していません")

        // クリックに応える (1.2)
        val hello = WButton("押してください")
        hello.addActionListener { status.text = "押されました" }

        // アイコン・ツールチップ・ニーモニック (1.4〜1.6)
        val save = WButton("保存(S)", Symbol.SAVE)
        save.toolTipText = "ファイルを保存します"
        save.setMnemonic('S')

        // Action の共有 (1.8)
        val deleteAction = object : WAbstractAction("削除", Symbol.DELETE) {
            override fun actionPerformed(e: WActionEvent) {
                status.text = "削除しました"
            }
        }
        val delete = WButton(deleteAction)

        // 平らなボタン (1.11)
        val link = WButton("詳しく見る").apply {
            isContentAreaFilled = false
            isBorderPainted = false
        }

        // 裏の処理 (1.13)
        val download = WButton("ダウンロード")
        download.addActionListener {
            download.isEnabled = false
            CompletableFuture.supplyAsync { fetch() }.whenComplete { result, error ->
                WinUiUtilities.invokeLater {
                    download.isEnabled = true
                    status.text = if (error == null) result else "失敗しました: ${error.message}"
                }
            }
        }

        // 既定ボタンとアクセント (1.7、1.14)
        val ok = WButton("OK")
        ok.addActionListener { e -> status.text = "${e.actionCommand} が押されました" }
        frame.defaultButton = ok

        val row = WPanel(8.0, Orientation.HORIZONTAL)
        listOf(hello, save, delete, link, download, ok).forEach { row.add(it) }
        frame.add(row)
        frame.add(status)
        frame.setSize(720, 240)
        frame.isVisible = true
    }
}

private fun fetch(): String {
    Thread.sleep(1000)
    return "ダウンロードしました"
}
```

Kotlin では `isEnabled`・`isContentAreaFilled`・`toolTipText`・`defaultButton` のようにプロパティで書けます。
`addActionListener` には、イベントを受け取る `{ e -> ... }` と受け取らない `{ ... }` のどちらも渡せます。

<a id="button-notes"></a>
### 1.16 注意点

- ニーモニックの文字に下線は出ず、Alt を押したときにキーのヒントが出ます。ラベルに「(S)」と書いておくと、
  Swing と同じく利用者がキーに気づきやすくなります。
- 既定ボタンにすると見た目が変わり、アクセントの色になります。外すと元の見た目に戻ります。
- `JButton` の HTML の文字は使えません。Swing の `<html>` で始まる文字は解釈されず、そのまま表示されます。
  複数行や書式のある中身は `setContent(component)` に `WLabel` などを入れます。
- ボタンのグループ: ラジオボタンをまとめる `ButtonGroup` は `WButtonGroup` です。

<a id="textfield"></a>
## 2. JTextField → WTextField

`WTextField` は WinUI の `TextBox` を包んだクラスです。`JTextField` の主な API に加え、`Document` に当たる `WDocument`
(リスナーとフィルター) も持っています。

<a id="textfield-table"></a>
### 2.1 API の対応表

| やりたいこと | Swing | WinUI4K | 違い |
|---|---|---|---|
| 空の入力欄 | `new JTextField()` | `new WTextField()` | 同じ |
| 列数で幅を決める | `new JTextField(20)` | `new WTextField(20)` | 同じ |
| 初めの文字列 | `new JTextField("東京")` | `WTextField f = new WTextField(); f.setText("東京");` | **`WTextField(String)` は案内文** (下の注意を参照) |
| 読み書き | `getText()` / `setText(...)` | `getText()` / `setText(...)` | 同じ |
| Enter で処理 | `addActionListener(...)` | `addActionListener(...)` | 同じ |
| 変化の通知 | `getDocument().addDocumentListener(...)` | `addDocumentListener(...)` / `addTextChangedListener(text -> ...)` | 入力欄に直接付けられる |
| 入力の制限 | `((AbstractDocument) getDocument()).setDocumentFilter(...)` | `getDocument().setDocumentFilter(...)` | キャストが要らない |
| 文字数の上限 | `DocumentFilter` を書く | `setMaxLength(10)` | WinUI の機能 |
| 読み取り専用 | `setEditable(false)` | `setEditable(false)` | 同じ |
| 案内文 | (なし) | `setPlaceholderText("...")` | WinUI の機能 |
| 見出し | `JLabel` + `setLabelFor` | `setHeader("...")` | 入力欄の上に表示する |
| 全選択 | `selectAll()` | `selectAll()` | 同じ |
| 選択範囲 | `select(s, e)` / `getSelectedText()` | `select(s, e)` / `getSelectedText()` | 同じ |
| キャレット | `setCaretPosition(n)` | `setCaretPosition(n)` | 同じ |
| フォーカス | `requestFocusInWindow()` | `requestFocusInWindow()` | 同じ |
| フォーカスの通知 | `addFocusListener(...)` | `addFocusListener(...)` | 同じ (`FocusAdapter` → `WFocusAdapter`) |
| 入力の検証 | `setInputVerifier(...)` | `addFocusListener(...)` で自分で書く | `InputVerifier` は無い |
| 元に戻す | `UndoManager` を自分で付ける | 組み込み (Ctrl+Z)。`undo()` / `canUndo()` | WinUI の機能 |
| 文字の揃え | `setHorizontalAlignment(JTextField.RIGHT)` | `setTextAlignment(TextAlignment.RIGHT)` | 名前が違う |
| 選択の色 | `setSelectionColor(...)` | `setSelectionColor(...)` | 同じ |
| キーバインド | `getInputMap()` / `getActionMap()` | `getInputMap()` / `getActionMap()` | 同じ |
| パスワード | `JPasswordField` | `WPasswordField` | [2.15](#textfield-password) |
| 複数行 | `JTextArea` | `WTextField` に `setAcceptsReturn(true)` | [2.17](#textfield-notes) |

<a id="textfield-basic"></a>
### 2.2 入力欄を作って値を読み書きする

名前を入れてボタンを押すと、あいさつを表示します。

**Swing (Java)**

```java
JTextField name = new JTextField(20);
name.setText("山田");
JLabel greeting = new JLabel();
JButton greet = new JButton("あいさつ");
greet.addActionListener(e -> greeting.setText("こんにちは、" + name.getText().trim() + " さん"));
```

**WinUI4K (Java)**

```java
WTextField name = new WTextField(20);
name.setText("山田");
WLabel greeting = new WLabel();
WButton greet = new WButton("あいさつ");
greet.addActionListener(e -> greeting.setText("こんにちは、" + name.getText().trim() + " さん"));
```

**実行した画面** ([`BasicSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/BasicSwing.java) / [`BasicWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/BasicWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Basic (Swing)](images/swing_to_winui4k/JTextField/BasicSwing.png) | ![Basic (WinUI4K)](images/swing_to_winui4k/JTextField/BasicWinUI4K.png) |

`getText()` は今の文字列をそのまま返します (前後の空白も残ります)。

<a id="textfield-enter"></a>
### 2.3 Enter で処理する

検索欄で Enter を押したら検索します。

**Swing (Java)**

```java
JTextField query = new JTextField(20);
query.addActionListener(e -> search(query.getText()));
```

**WinUI4K (Java)**

```java
WTextField query = new WTextField(20);
query.addActionListener(e -> search(query.getText()));
```

**実行した画面** ([`EnterSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/EnterSwing.java) / [`EnterWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/EnterWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Enter (Swing)](images/swing_to_winui4k/JTextField/EnterSwing.png) | ![Enter (WinUI4K)](images/swing_to_winui4k/JTextField/EnterWinUI4K.png) |

- 修飾キーなしの Enter で呼ばれます。`e.getActionCommand()` は、`setActionCommand` を設定していなければ入力欄の文字列です (Swing と同じ)。
- `ActionListener` が 1 つも無い入力欄で Enter を押すと、ウィンドウの既定ボタン ([1.7](#button-default)) が押されます。

<a id="textfield-change"></a>
### 2.4 入力のたびに反応する

入力した文字数を表示し、空のときは「登録」ボタンを無効にします。

**Swing (Java)**

```java
JTextField title = new JTextField(30);
JLabel count = new JLabel("0 文字");
JButton register = new JButton("登録");
register.setEnabled(false);

title.getDocument().addDocumentListener(new DocumentListener() {
    @Override public void insertUpdate(DocumentEvent e) { update(); }
    @Override public void removeUpdate(DocumentEvent e) { update(); }
    @Override public void changedUpdate(DocumentEvent e) { update(); }

    private void update() {
        count.setText(title.getText().length() + " 文字");
        register.setEnabled(!title.getText().isEmpty());
    }
});
```

**WinUI4K (Java)**

```java
WTextField title = new WTextField(30);
WLabel count = new WLabel("0 文字");
WButton register = new WButton("登録");
register.setEnabled(false);

title.addTextChangedListener(text -> {
    count.setText(text.length() + " 文字");
    register.setEnabled(!text.isEmpty());
});
```

**実行した画面** ([`ChangeSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/ChangeSwing.java) / [`ChangeWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/ChangeWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Change (Swing)](images/swing_to_winui4k/JTextField/ChangeSwing.png) | ![Change (WinUI4K)](images/swing_to_winui4k/JTextField/ChangeWinUI4K.png) |

どちらのサンプルも、表示する前に `title.setText("今日のメモ")` を呼んでいます。Swing のリスナーはその変更を受け取りますが、WinUI4K の `addTextChangedListener` のリスナーは、入力欄が画面に表示される前の変更を受け取りません (WinUI の `TextChanged` が届かないため)。そのため WinUI4K の画面は「0 文字」のままです。表示した後の変更 (入力や `setText`) は受け取ります。

- `addTextChangedListener` は、文字が変わるたびに今の文字列を受け取ります。3 つのメソッドを書き分ける必要がありません。
- Swing と同じ `WDocumentListener` (`insertUpdate` / `removeUpdate` / `changedUpdate`) も使えます。
  変わった位置と長さ (`e.getOffset()` / `e.getLength()`) が要るときはこちらを使います。

  ```java
  title.addDocumentListener(new WDocumentListener() {
      @Override public void insertUpdate(WDocumentEvent e) { update(); }
      @Override public void removeUpdate(WDocumentEvent e) { update(); }
      @Override public void changedUpdate(WDocumentEvent e) { update(); }
  });
  ```

  `title.getDocument().addDocumentListener(...)` と書いても同じです。

<a id="textfield-filter"></a>
### 2.5 入力できる文字を制限する

郵便番号の欄に数字だけを入力できるようにします。

**Swing (Java)**

```java
JTextField zip = new JTextField(8);
((AbstractDocument) zip.getDocument()).setDocumentFilter(new DocumentFilter() {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        if (isDigits(string)) super.insertString(fb, offset, string, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        if (text == null || isDigits(text)) super.replace(fb, offset, length, text, attrs);
    }
});

static boolean isDigits(String s) {
    return s.chars().allMatch(Character::isDigit);
}
```

**WinUI4K (Java)**

```java
WTextField zip = new WTextField(8);
zip.getDocument().setDocumentFilter(new WDocumentFilter() {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, WAttributeSet attr) {
        if (isDigits(string)) super.insertString(fb, offset, string, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, WAttributeSet attrs) {
        if (text == null || isDigits(text)) super.replace(fb, offset, length, text, attrs);
    }
});
```

**実行した画面** ([`FilterSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/FilterSwing.java) / [`FilterWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/FilterWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Filter (Swing)](images/swing_to_winui4k/JTextField/FilterSwing.png) | ![Filter (WinUI4K)](images/swing_to_winui4k/JTextField/FilterWinUI4K.png) |

- `AbstractDocument` へのキャストと `BadLocationException` の `throws` が要らなくなるほかは、Swing と同じ形です。
- 入力を断ると、入力欄は入力の前の状態のままになります。大文字に直すなど書き換えて通すこともできます
  (`super.replace(fb, offset, length, text.toUpperCase(), attrs)`)。
- 日本語入力 (IME) の変換中の文字も、入力のたびにフィルターを通ります。

<a id="textfield-maxlength"></a>
### 2.6 文字数を制限する

**Swing (Java)**

```java
// Swing には文字数の上限の設定が無いので、DocumentFilter で書く
((AbstractDocument) code.getDocument()).setDocumentFilter(new DocumentFilter() {
    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        int newLength = fb.getDocument().getLength() - length + (text == null ? 0 : text.length());
        if (newLength <= 10) super.replace(fb, offset, length, text, attrs);
    }
    // insertString も同じように書く
});
```

**WinUI4K (Java)**

```java
code.setMaxLength(10);   // 0 は上限なし
```

**実行した画面** ([`MaxLengthSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/MaxLengthSwing.java) / [`MaxLengthWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/MaxLengthWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![MaxLength (Swing)](images/swing_to_winui4k/JTextField/MaxLengthSwing.png) | ![MaxLength (WinUI4K)](images/swing_to_winui4k/JTextField/MaxLengthWinUI4K.png) |

WinUI の `TextBox.MaxLength` で、上限を超える分の入力は入りません。

<a id="textfield-readonly"></a>
### 2.7 読み取り専用にする

**Swing (Java)**

```java
JTextField id = new JTextField(10);
id.setText("A-0001");
id.setEditable(false);
```

**WinUI4K (Java)**

```java
WTextField id = new WTextField(10);
id.setText("A-0001");
id.setEditable(false);
```

**実行した画面** ([`ReadOnlySwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/ReadOnlySwing.java) / [`ReadOnlyWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/ReadOnlyWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![ReadOnly (Swing)](images/swing_to_winui4k/JTextField/ReadOnlySwing.png) | ![ReadOnly (WinUI4K)](images/swing_to_winui4k/JTextField/ReadOnlyWinUI4K.png) |

読み取り専用でも、選択とコピーはできます (Swing と同じ)。入力も選択もさせないときは `setEnabled(false)` にします。

<a id="textfield-placeholder"></a>
### 2.8 案内文 (プレースホルダー) と見出しを付ける

未入力のときに薄い文字で「例: 山田太郎」と出し、入力欄の上に「氏名」と表示します。

**Swing (Java)**

```java
// Swing には案内文の機能が無い。見出しは JLabel を別に置いて結び付ける
JLabel label = new JLabel("氏名");
JTextField name = new JTextField(20);
label.setLabelFor(name);
label.setDisplayedMnemonic(KeyEvent.VK_N);
frame.add(label);
frame.add(name);
// 案内文は paintComponent を上書きして描くか、外部ライブラリ (FlatLaf の JTextField.placeholderText など) に頼る
```

**WinUI4K (Java)**

```java
WTextField name = new WTextField("例: 山田太郎");   // コンストラクターの文字列は案内文
name.setHeader("氏名");                             // 入力欄の上に表示する見出し
frame.add(name);
```

**実行した画面** ([`PlaceholderSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/PlaceholderSwing.java) / [`PlaceholderWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/PlaceholderWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Placeholder (Swing)](images/swing_to_winui4k/JTextField/PlaceholderSwing.png) | ![Placeholder (WinUI4K)](images/swing_to_winui4k/JTextField/PlaceholderWinUI4K.png) |

- 案内文は `setPlaceholderText(...)` でも設定できます。
- 見出しは入力欄の一部として、入力欄の上に描かれます。
  見出しを別のラベルで置きたいときは、Swing と同じく `WLabel` の `setLabelFor(name)` を使います。

<a id="textfield-selectall"></a>
### 2.9 フォーカスを受けたら全選択する

上書きで入力しやすいように、入力欄に入ったら中身を全部選びます。

**Swing (Java)**

```java
JTextField amount = new JTextField(10);
amount.addFocusListener(new FocusAdapter() {
    @Override
    public void focusGained(FocusEvent e) {
        amount.selectAll();
    }
});
```

**WinUI4K (Java)**

```java
WTextField amount = new WTextField(10);
amount.addFocusListener(new WFocusAdapter() {
    @Override
    public void focusGained(WFocusEvent e) {
        amount.selectAll();
    }
});
```

**実行した画面** ([`SelectAllSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/SelectAllSwing.java) / [`SelectAllWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/SelectAllWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![SelectAll (Swing)](images/swing_to_winui4k/JTextField/SelectAllSwing.png) | ![SelectAll (WinUI4K)](images/swing_to_winui4k/JTextField/SelectAllWinUI4K.png) |

画面を開くと、Swing と同じく最初の入力欄にフォーカスが当たります。別の部品に当てるときは、Swing と同じく表示した直後に `requestFocusInWindow()` を呼びます。

<a id="textfield-validate"></a>
### 2.10 フォーカスを外したときに検証する

年齢の欄を離れたときに、0〜150 の数字か確かめます。

**Swing (Java)**

```java
JTextField age = new JTextField(4);
age.setInputVerifier(new InputVerifier() {
    @Override
    public boolean verify(JComponent input) {
        return isValidAge(((JTextField) input).getText());   // false ならフォーカスを離さない
    }
});
```

**WinUI4K (Java)**

```java
WTextField age = new WTextField(4);
WLabel error = new WLabel();
error.setForeground(WColor.RED);
age.addFocusListener(new WFocusAdapter() {
    @Override
    public void focusLost(WFocusEvent e) {
        error.setText(isValidAge(age.getText()) ? "" : "0〜150 の数字で入力してください");
    }
});
```

**実行した画面** ([`ValidateSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/ValidateSwing.java) / [`ValidateWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/ValidateWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Validate (Swing)](images/swing_to_winui4k/JTextField/ValidateSwing.png) | ![Validate (WinUI4K)](images/swing_to_winui4k/JTextField/ValidateWinUI4K.png) |

WinUI4K には `InputVerifier` が無く、フォーカスが移るのを止めることもできません。フォーカスを外したときにメッセージを出すか、
「登録」ボタンを押したときにまとめて検証します。Windows のアプリではフォーカスを止めず、近くにメッセージを出すのが普通です。

<a id="textfield-caret"></a>
### 2.11 選択範囲とキャレットを操作する

**Swing (Java)**

```java
JTextField text = new JTextField(30);
text.setText("Hello, World");
text.select(7, 12);                          // "World" を選ぶ
String selected = text.getSelectedText();    // "World"
text.replaceSelection("Swing");              // "Hello, Swing"
text.setCaretPosition(text.getText().length());   // 末尾へ
text.addCaretListener(e -> position.setText("位置: " + e.getDot()));
```

**WinUI4K (Java)**

```java
WTextField text = new WTextField(30);
text.setText("Hello, World");
text.select(7, 12);
String selected = text.getSelectedText();
text.replaceSelection("WinUI");              // "Hello, WinUI"
text.setCaretPosition(text.getText().length());
text.addCaretListener(e -> position.setText("位置: " + e.getDot()));
```

**実行した画面** ([`CaretSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/CaretSwing.java) / [`CaretWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/CaretWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Caret (Swing)](images/swing_to_winui4k/JTextField/CaretSwing.png) | ![Caret (WinUI4K)](images/swing_to_winui4k/JTextField/CaretWinUI4K.png) |

`getSelectionStart()` / `getSelectionEnd()`、`moveCaretPosition(n)` (キャレットを動かして選択を広げる) も Swing と同じです。
クリップボードは `copy()` / `cut()` / `paste()` で操作します。

<a id="textfield-keybinding"></a>
### 2.12 Esc で入力を消すキーバインド

**Swing (Java)**

```java
JTextField query = new JTextField(20);
query.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");
query.getActionMap().put("clear", new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
        query.setText("");
    }
});
```

**WinUI4K (Java)**

```java
WTextField query = new WTextField(20);
query.getInputMap().put(WKeyStroke.getKeyStroke(WKeyEvent.VK_ESCAPE, 0), "clear");
query.getActionMap().put("clear", new WAbstractAction() {
    @Override
    public void actionPerformed(WActionEvent e) {
        query.setText("");
    }
});
```

**実行した画面** ([`KeyBindingSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/KeyBindingSwing.java) / [`KeyBindingWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/KeyBindingWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![KeyBinding (Swing)](images/swing_to_winui4k/JTextField/KeyBindingSwing.png) | ![KeyBinding (WinUI4K)](images/swing_to_winui4k/JTextField/KeyBindingWinUI4K.png) |

`KeyStroke` → `WKeyStroke`、`KeyEvent` → `WKeyEvent` に替えるだけです。`WKeyStroke.getKeyStroke("ctrl ENTER")` のような
文字列の形も使えます。Swing と違い、入力欄の既定のキー操作 (矢印や Ctrl+C など) はマップに入っていません。

<a id="textfield-undo"></a>
### 2.13 元に戻す・やり直す

**Swing (Java)**

```java
JTextField memo = new JTextField(30);
UndoManager undo = new UndoManager();
memo.getDocument().addUndoableEditListener(undo);
memo.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "undo");
memo.getActionMap().put("undo", new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
        if (undo.canUndo()) undo.undo();
    }
});
// やり直し (Ctrl+Y) も同じように書く
```

**WinUI4K (Java)**

```java
WTextField memo = new WTextField(30);
// Ctrl+Z / Ctrl+Y は何も書かなくても動く。メニューなどから呼ぶときは
undoButton.addActionListener(e -> { if (memo.canUndo()) memo.undo(); });
redoButton.addActionListener(e -> { if (memo.canRedo()) memo.redo(); });
```

**実行した画面** ([`UndoSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/UndoSwing.java) / [`UndoWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/UndoWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Undo (Swing)](images/swing_to_winui4k/JTextField/UndoSwing.png) | ![Undo (WinUI4K)](images/swing_to_winui4k/JTextField/UndoWinUI4K.png) |

WinUI の入力欄は元に戻す・やり直しの機能と右クリックのメニュー (切り取り・コピー・貼り付けなど) を最初から持っています。

<a id="textfield-appearance"></a>
### 2.14 文字の揃えと見た目

金額の欄を右揃えにし、等幅のフォントにします。

**Swing (Java)**

```java
JTextField price = new JTextField(10);
price.setHorizontalAlignment(JTextField.RIGHT);
price.setFont(new Font("Consolas", Font.PLAIN, 14));
price.setForeground(new Color(0, 90, 158));
price.setToolTipText("税込みの金額");
```

**WinUI4K (Java)**

```java
WTextField price = new WTextField(10);
price.setTextAlignment(TextAlignment.RIGHT);
price.setFont(new WFont("Consolas", WFont.PLAIN, 14));
price.setForeground(new WColor(0, 90, 158));
price.setToolTipText("税込みの金額");
```

**実行した画面** ([`AppearanceSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/AppearanceSwing.java) / [`AppearanceWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/AppearanceWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Appearance (Swing)](images/swing_to_winui4k/JTextField/AppearanceSwing.png) | ![Appearance (WinUI4K)](images/swing_to_winui4k/JTextField/AppearanceWinUI4K.png) |

**注意**: WinUI4K の `setHorizontalAlignment` は入力欄の中の文字ではなく、入力欄そのものを親の中でどこに置くかの設定です
(すべてのコンポーネントが持つレイアウトの設定)。文字の揃えは `setTextAlignment` を使います。

<a id="textfield-password"></a>
### 2.15 JPasswordField → WPasswordField

**Swing (Java)**

```java
JPasswordField password = new JPasswordField(20);
password.addActionListener(e -> {
    char[] value = password.getPassword();
    try {
        login(value);
    } finally {
        Arrays.fill(value, '\0');
    }
});
```

**WinUI4K (Java)**

```java
WPasswordField password = new WPasswordField(20);
password.setHeader("パスワード");
password.addActionListener(e -> {
    char[] value = password.getPassword();
    try {
        login(value);
    } finally {
        Arrays.fill(value, '\0');
    }
});
```

**実行した画面** ([`PasswordSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/PasswordSwing.java) / [`PasswordWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jtextfield/PasswordWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Password (Swing)](images/swing_to_winui4k/JTextField/PasswordSwing.png) | ![Password (WinUI4K)](images/swing_to_winui4k/JTextField/PasswordWinUI4K.png) |

- 伏せ字は `setEchoChar('*')` で変えられます。
- WinUI のパスワード欄では入力中に目のボタンで中身を確かめられます。出し方は `setPasswordRevealMode(PasswordRevealMode.HIDDEN)` などで変えます。
- `WPasswordField` では、利用者の入力は `WDocumentFilter` を通りません (WinUI が入力の前に断る仕組みを持たないため)。

<a id="textfield-kotlin"></a>
### 2.16 Kotlin で書く

この章のユースケースをまとめて Kotlin で書くと、次のようになります。

```kotlin
import com.appkitbox.winui4k.*

fun main() {
    WinUiUtilities.invokeLater {
        val frame = WFrame("入力欄のサンプル")
        frame.layout = WFlowLayout()

        // 案内文・見出し・Enter・入力のたびの通知 (2.3、2.4、2.8)
        val name = WTextField("例: 山田太郎").apply { header = "氏名" }
        val count = WLabel("0 文字")
        name.addTextChangedListener { text -> count.text = "${text.length} 文字" }
        name.addActionListener { register(name.text) }

        // 数字だけ・文字数の上限 (2.5、2.6)
        val zip = WTextField(8).apply {
            header = "郵便番号"
            maxLength = 7
            document.documentFilter = object : WDocumentFilter() {
                override fun insertString(fb: FilterBypass, offset: Int, string: String?, attr: WAttributeSet?) {
                    if (string.orEmpty().all(Char::isDigit)) super.insertString(fb, offset, string, attr)
                }

                override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String?, attrs: WAttributeSet?) {
                    if (text.orEmpty().all(Char::isDigit)) super.replace(fb, offset, length, text, attrs)
                }
            }
        }

        // フォーカスで全選択・離れたら検証 (2.9、2.10)
        val error = WLabel().apply { foreground = WColor.RED }
        val age = WTextField(4).apply { header = "年齢" }
        age.addFocusListener(object : WFocusAdapter() {
            override fun focusGained(e: WFocusEvent) = age.selectAll()

            override fun focusLost(e: WFocusEvent) {
                val valid = age.text.toIntOrNull() in 0..150
                error.text = if (valid) "" else "0〜150 の数字で入力してください"
            }
        })

        // Esc で消す (2.12)
        name.inputMap.put(WKeyStroke.getKeyStroke(WKeyEvent.VK_ESCAPE, 0), "clear")
        name.actionMap.put(
            "clear",
            object : WAbstractAction() {
                override fun actionPerformed(e: WActionEvent) {
                    name.text = ""
                }
            },
        )

        // 右揃えの金額・読み取り専用 (2.7、2.14)
        val price = WTextField(10).apply {
            header = "金額"
            textAlignment = TextAlignment.RIGHT
            text = "1,280"
            isEditable = false
        }

        listOf(name, count, zip, age, error, price).forEach { frame.add(it) }
        frame.setSize(480, 560)
        frame.isVisible = true
        name.requestFocusInWindow()
    }
}

private fun register(name: String) = println("登録: $name")
```

<a id="textfield-notes"></a>
### 2.17 注意点

- `new WTextField("文字列")` の文字列は初めの文字列ではなく案内文になります。`JTextField(String text)` と同じつもりで書くと、
  入力欄は空のまま薄い文字が出ます。初めの文字列は `setText(...)` で入れます。`JTextField(String, int)` に当たるコンストラクターはありません。
  列数だけを渡す `new WTextField(20)` は Swing と同じです。
- `setHorizontalAlignment` は文字の揃えの設定ではありません。文字の揃えは `setTextAlignment` です ([2.14](#textfield-appearance))。
- `InputVerifier` が無いので、フォーカスの移動を止める検証はできません ([2.10](#textfield-validate))。
- 複数行の入力 (`JTextArea`): `WTextField` に `setAcceptsReturn(true)` と `setTextWrapping(TextWrapping.WRAP)` を設定すると、
  複数行の入力欄になります。高さは `setHeight(...)` で決めます。WinUI の入力欄は中身が増えると入力欄の中でスクロールするので
  (スクロールバーは既定のスタイルでは出ません)、`JScrollPane` に当たるものに入れる必要はありません。
- 書式のある文字 (`JTextPane`): `WTextPane` を使います。

<a id="list"></a>
## 3. JList → WList

`WList` は WinUI の `ListView` を包んだクラスです。Swing と同じく、データはモデル (`WListModel`。既定は `WDefaultListModel`) が持ち、
選択は `WListSelectionModel` でも扱えます。

<a id="list-table"></a>
### 3.1 API の対応表

| やりたいこと | Swing | WinUI4K | 違い |
|---|---|---|---|
| 配列から作る | `new JList<>(array)` | `new WList(array)` | **型引数が無い** (`WList` はジェネリクスでない) |
| モデルから作る | `new JList<>(model)` | `new WList(model)` | 同じ |
| スクロール | `new JScrollPane(list)` に入れる | 入れなくてよい | WinUI のリストは自分でスクロールする |
| 動的なデータ | `DefaultListModel<E>` | `WDefaultListModel<E>` | 同じメソッド |
| 選んだ要素 | `getSelectedValue()` (型は `E`) | `getSelectedValue()` (型は `Object`) | キャストが要る。文字列なら `getSelectedItem()` |
| 選んだ位置 | `getSelectedIndex()` | `getSelectedIndex()` | 同じ |
| 複数の選択 | `getSelectedValuesList()` / `getSelectedIndices()` | 同じ | 戻り値は `List<Object>` |
| 選択モード | `setSelectionMode(ListSelectionModel.SINGLE_SELECTION)` | `setSelectionMode(WListSelectionModel.SINGLE_SELECTION)` | **既定が違う** (Swing は複数、WinUI4K は 1 つ) |
| 選択の通知 | `addListSelectionListener(e -> ...)` | `addListSelectionListener(e -> ...)` | 同じ |
| 選ぶ | `setSelectedIndex(i)` / `setSelectedValue(v, true)` | 同じ | 同じ |
| 見えるまでスクロール | `ensureIndexIsVisible(i)` | `ensureIndexIsVisible(i)` | 同じ |
| 位置から要素 | `locationToIndex(point)` | `locationToIndex(point)` | 同じ |
| 表示を変える | `setCellRenderer(...)` | `setCellRenderer(...)` | **呼ぶたびに新しいコンポーネントを返す** |
| 行の数 | `setVisibleRowCount(5)` | `setVisibleRowCount(5)` | 同じ |
| 行の高さ | `setFixedCellHeight(28)` | `setFixedCellHeight(28)` | 同じ |
| 右クリックメニュー | `setComponentPopupMenu(popup)` | `setComponentPopupMenu(flyout)` | `JPopupMenu` → `WMenuFlyout` |
| 項目のクリック | (なし) | `setItemClickEnabled(true)` + `addItemClickListener(...)` | WinUI の機能 |

<a id="list-basic"></a>
### 3.2 固定の項目を並べる

**Swing (Java)**

```java
String[] fruits = {"りんご", "みかん", "ぶどう", "もも", "さくらんぼ"};
JList<String> list = new JList<>(fruits);
list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
JScrollPane scroll = new JScrollPane(list);
scroll.setPreferredSize(new Dimension(200, 150));
frame.add(scroll);
```

**WinUI4K (Java)**

```java
String[] fruits = {"りんご", "みかん", "ぶどう", "もも", "さくらんぼ"};
WList list = new WList(fruits);           // 既定で 1 つだけ選べる
list.setWidth(200);
list.setHeight(150);
frame.add(list);
```

**実行した画面** ([`BasicSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/BasicSwing.java) / [`BasicWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/BasicWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Basic (Swing)](images/swing_to_winui4k/JList/BasicSwing.png) | ![Basic (WinUI4K)](images/swing_to_winui4k/JList/BasicWinUI4K.png) |

- `WList` に型引数はありません (`JList<String>` → `WList`)。
- WinUI のリストは自分でスクロールバーを出すので、`JScrollPane` に当たる `WScrollPane` に入れる必要はありません。
  入れると、リストが表示に要る項目だけを作る仕組み (仮想化) が効かなくなるので、入れないでください。
- 各要素は `toString()` の文字列で表示します (Swing と同じ)。
- Kotlin や Java の `List` からも作れます (`new WList(Arrays.asList("a", "b"))`)。

<a id="list-model"></a>
### 3.3 モデルで項目を追加・削除する

入力欄の文字をリストに足し、選んだ項目を消します。

**Swing (Java)**

```java
DefaultListModel<String> model = new DefaultListModel<>();
model.addElement("牛乳");
model.addElement("卵");
JList<String> list = new JList<>(model);

JTextField input = new JTextField(15);
JButton add = new JButton("追加");
add.addActionListener(e -> {
    if (!input.getText().isEmpty()) {
        model.addElement(input.getText());
        input.setText("");
    }
});

JButton remove = new JButton("削除");
remove.addActionListener(e -> {
    int index = list.getSelectedIndex();
    if (index >= 0) model.remove(index);
});

JButton clear = new JButton("すべて削除");
clear.addActionListener(e -> model.clear());
```

**WinUI4K (Java)**

```java
WDefaultListModel<String> model = new WDefaultListModel<>();
model.addElement("牛乳");
model.addElement("卵");
WList list = new WList(model);

WTextField input = new WTextField(15);
WButton add = new WButton("追加");
add.addActionListener(e -> {
    if (!input.getText().isEmpty()) {
        model.addElement(input.getText());
        input.setText("");
    }
});

WButton remove = new WButton("削除");
remove.addActionListener(e -> {
    int index = list.getSelectedIndex();
    if (index >= 0) model.remove(index);
});

WButton clear = new WButton("すべて削除");
clear.addActionListener(e -> model.clear());
```

**実行した画面** ([`ModelSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/ModelSwing.java) / [`ModelWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/ModelWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Model (Swing)](images/swing_to_winui4k/JList/ModelSwing.png) | ![Model (WinUI4K)](images/swing_to_winui4k/JList/ModelWinUI4K.png) |

- `WDefaultListModel` は `DefaultListModel` と同じメソッド (`addElement`・`add`・`insertElementAt`・`set`・`remove`・`removeElement`・
  `clear`・`getSize`・`getElementAt`・`indexOf` など) を持ちます。モデルを変えると、その場で表示に写ります。
- 選択はモデルの変更に合わせてずれます (前に挿入すると後ろへずれ、選んだ要素を消すと選択が外れる。Swing と同じ)。
- モデルを作らずに、リストの `addItem("...")` / `removeItem(i)` / `removeAllItems()` で既定のモデルを直接編集することもできます。
- 自分でモデルを作るときは、Swing と同じく `WAbstractListModel` を継承して `getSize()` と `getElementAt(i)` を書きます。

<a id="list-selected"></a>
### 3.4 選んだ項目を読む

**Swing (Java)**

```java
String value = list.getSelectedValue();          // 未選択なら null
int index = list.getSelectedIndex();             // 未選択なら -1
boolean none = list.isSelectionEmpty();
```

**WinUI4K (Java)**

```java
String value = (String) list.getSelectedValue();  // 未選択なら null
String text = list.getSelectedItem();             // toString() の文字列 (未選択なら null)
int index = list.getSelectedIndex();              // 未選択なら -1
boolean none = list.isSelectionEmpty();
```

**実行した画面** ([`SelectedSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/SelectedSwing.java) / [`SelectedWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/SelectedWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Selected (Swing)](images/swing_to_winui4k/JList/SelectedSwing.png) | ![Selected (WinUI4K)](images/swing_to_winui4k/JList/SelectedWinUI4K.png) |

`getSelectedValue()` はモデルの要素そのもの (`Object`) を返すので、キャストします。
表示している文字列が欲しいだけなら `getSelectedItem()` が便利です。

<a id="list-listener"></a>
### 3.5 選択の変化に反応する

選んだ項目の説明を下に表示します。

**Swing (Java)**

```java
JLabel detail = new JLabel("選んでください");
list.addListSelectionListener(e -> {
    if (e.getValueIsAdjusting()) return;          // ドラッグ中の途中経過を飛ばす
    String value = list.getSelectedValue();
    detail.setText(value == null ? "選んでください" : value + " を選びました");
});
```

**WinUI4K (Java)**

```java
WLabel detail = new WLabel("選んでください");
list.addListSelectionListener(e -> {
    if (e.getValueIsAdjusting()) return;
    Object value = list.getSelectedValue();
    detail.setText(value == null ? "選んでください" : value + " を選びました");
});
```

**実行した画面** ([`ListenerSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/ListenerSwing.java) / [`ListenerWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/ListenerWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Listener (Swing)](images/swing_to_winui4k/JList/ListenerSwing.png) | ![Listener (WinUI4K)](images/swing_to_winui4k/JList/ListenerWinUI4K.png) |

- WinUI は選択の途中経過を知らせないので、画面の操作では `getValueIsAdjusting()` はいつも `false` です。
  Swing のコードの `if` を残しておいても困りません。
- `e.getFirstIndex()` / `e.getLastIndex()` は、選択が変わったかもしれない範囲です (Swing と同じ)。
- イベントの中身を使わないなら、引数のないラムダ (`() -> ...`) も渡せます。

<a id="list-multiple"></a>
### 3.6 複数選択を許す

Ctrl / Shift で複数を選び、選んだものをまとめて処理します。

**Swing (Java)**

```java
JList<String> files = new JList<>(names);
// 既定で MULTIPLE_INTERVAL_SELECTION (Ctrl / Shift で複数)
JButton open = new JButton("開く");
open.addActionListener(e -> {
    for (String name : files.getSelectedValuesList()) openFile(name);
});
```

**WinUI4K (Java)**

```java
WList files = new WList(names);
files.setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION);   // 既定は 1 つだけなので指定する
WButton open = new WButton("開く");
open.addActionListener(e -> {
    for (Object name : files.getSelectedValuesList()) openFile((String) name);
});
```

**実行した画面** ([`MultipleSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/MultipleSwing.java) / [`MultipleWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/MultipleWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Multiple (Swing)](images/swing_to_winui4k/JList/MultipleSwing.png) | ![Multiple (WinUI4K)](images/swing_to_winui4k/JList/MultipleWinUI4K.png) |

- **既定の選択モードが違います**。Swing は複数の区間 (`MULTIPLE_INTERVAL_SELECTION`)、WinUI4K は WinUI の既定に合わせて 1 つだけ (`SINGLE_SELECTION`) です。
- `SINGLE_INTERVAL_SELECTION` (続いた範囲だけ) も指定できます。
- `getSelectedIndices()`・`setSelectionInterval(a, b)`・`addSelectionInterval(a, b)`・`clearSelection()` も Swing と同じです。
- すべて選ぶのは `selectAll()` です。

<a id="list-select"></a>
### 3.7 プログラムから選んでスクロールする

**Swing (Java)**

```java
list.setSelectedIndex(42);
list.ensureIndexIsVisible(42);
// 値で選ぶ (2 つ目の引数が true ならスクロールもする)
list.setSelectedValue("ぶどう", true);
list.clearSelection();
```

**WinUI4K (Java)**

```java
list.setSelectedIndex(42);
list.ensureIndexIsVisible(42);
list.setSelectedValue("ぶどう", true);
list.clearSelection();
```

**実行した画面** ([`SelectSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/SelectSwing.java) / [`SelectWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/SelectWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Select (Swing)](images/swing_to_winui4k/JList/SelectSwing.png) | ![Select (WinUI4K)](images/swing_to_winui4k/JList/SelectWinUI4K.png) |

WinUI4K の画面は、項目 43 を選んでいますが、その位置までスクロールしていません。リストが画面に表示される前に呼んだ `ensureIndexIsVisible` は効かないためです。表示した後に呼べば、Swing と同じく項目 43 が見える位置までスクロールします。

コードは Swing と同じです。`setSelectedIndex(-1)` でも選択を外せます。

<a id="list-doubleclick"></a>
### 3.8 ダブルクリックで開く

**Swing (Java)**

```java
list.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            int index = list.locationToIndex(e.getPoint());
            if (index >= 0) openItem(list.getModel().getElementAt(index));
        }
    }
});
```

**WinUI4K (Java)**

```java
list.addMouseListener(new WMouseAdapter() {
    @Override
    public void mouseClicked(WMouseEvent e) {
        if (e.getClickCount() == 2) {
            int index = list.locationToIndex(e.getPoint());
            if (index >= 0) openItem(list.getModel().getElementAt(index));
        }
    }
});
```

**実行した画面** ([`DoubleClickSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/DoubleClickSwing.java) / [`DoubleClickWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/DoubleClickWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![DoubleClick (Swing)](images/swing_to_winui4k/JList/DoubleClickSwing.png) | ![DoubleClick (WinUI4K)](images/swing_to_winui4k/JList/DoubleClickWinUI4K.png) |

- `MouseAdapter` → `WMouseAdapter`、`MouseEvent` → `WMouseEvent` に替えるだけです。ダブルクリックの判定には Windows の設定の時間と範囲を使います。
- タッチやペンでの操作も、マウスの左ボタンとして届きます。
- キーボードでも開けるように、Enter キーのキーバインド ([3.11](#list-delete) と同じ書き方) を足しておくと親切です。
- WinUI のアプリでは 1 回のクリックで開く形 ([3.15](#list-winui) の項目のクリック) もよく使います。

<a id="list-renderer"></a>
### 3.9 独自のオブジェクトを並べて表示を変える

社員のオブジェクトを並べ、「名前 (部署)」とアイコンで表示します。

```java
// Swing と WinUI4K で共通のデータのクラス
public final class Employee {
    private final String name;
    private final String department;

    public Employee(String name, String department) {
        this.name = name;
        this.department = department;
    }

    public String getName() { return name; }
    public String getDepartment() { return department; }

    @Override
    public String toString() { return name; }   // レンダラーを使わないときの表示
}
```

**Swing (Java)**

```java
JList<Employee> list = new JList<>(employees);
list.setCellRenderer(new DefaultListCellRenderer() {
    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Employee employee = (Employee) value;
        label.setText(employee.getName() + " (" + employee.getDepartment() + ")");
        label.setIcon(personIcon);
        return label;
    }
});
```

**WinUI4K (Java)**

```java
WList list = new WList(employees);
list.setCellRenderer(new WDefaultListCellRenderer() {
    @Override
    protected String getText(Object value) {
        Employee employee = (Employee) value;
        return employee.getName() + " (" + employee.getDepartment() + ")";
    }

    @Override
    protected WIcon getIcon(Object value) {
        return Symbol.CONTACT;
    }
});
```

**実行した画面** ([`RendererSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/RendererSwing.java) / [`RendererWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/RendererWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Renderer (Swing)](images/swing_to_winui4k/JList/RendererSwing.png) | ![Renderer (WinUI4K)](images/swing_to_winui4k/JList/RendererWinUI4K.png) |

`WDefaultListCellRenderer` は、`getText` (文字)・`getIcon` (アイコン)・`configure` (ラベルの飾り) を上書きして使います。
選んだ項目を太字にするなら `configure` を上書きします。

```java
@Override
protected void configure(WLabel label, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    if (isSelected) label.setFont(new WFont("Segoe UI", WFont.BOLD, 14));
}
```

2 行の表示など、ラベル 1 つで足りないときは、レンダラーをラムダで書いてパネルを返します。

```java
list.setCellRenderer((owner, value, index, isSelected, cellHasFocus) -> {
    Employee employee = (Employee) value;
    WPanel cell = new WPanel();
    cell.add(new WLabel(employee.getName()));
    WLabel department = new WLabel(employee.getDepartment());
    department.setForeground(WColor.GRAY);
    cell.add(department);
    return cell;              // 呼ぶたびに新しいコンポーネントを作って返す
});
```

- **レンダラーは呼ぶたびに新しいコンポーネントを返してください**。Swing はラベル 1 つを使い回して何度も描きますが、
  WinUI のコンポーネントは同時に 1 か所にしか置けないため、同じものを返すと表示に失敗します。
- 選んだ項目の背景と枠は WinUI が描くので、`isSelected` で背景を塗る必要はありません (Swing のように `setOpaque` を気にしなくてよい)。

<a id="list-rows"></a>
### 3.10 見せる行の数と項目の高さ

**Swing (Java)**

```java
list.setVisibleRowCount(5);
list.setFixedCellHeight(28);
```

**WinUI4K (Java)**

```java
list.setFixedCellHeight(28);
list.setVisibleRowCount(5);   // 高さを「5 行 × 28 + 余白」にする
```

**実行した画面** ([`RowsSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/RowsSwing.java) / [`RowsWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/RowsWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Rows (Swing)](images/swing_to_winui4k/JList/RowsSwing.png) | ![Rows (WinUI4K)](images/swing_to_winui4k/JList/RowsWinUI4K.png) |

- `setVisibleRowCount` を設定すると、リストの高さがその行数分になります。Swing と違い、設定しないうちは既定の 8 行は大きさに効きません。
- 行の高さを決めていないときは、WinUI の項目の既定の高さ (40) で行数分の高さを見積もります。
- `getFirstVisibleIndex()` / `getLastVisibleIndex()` で、見えている範囲を読めます。

<a id="list-delete"></a>
### 3.11 Delete キーで選んだ項目を消す

**Swing (Java)**

```java
list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
list.getActionMap().put("delete", new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
        int index = list.getSelectedIndex();
        if (index >= 0) model.remove(index);
    }
});
```

**WinUI4K (Java)**

```java
list.getInputMap(WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    .put(WKeyStroke.getKeyStroke(WKeyEvent.VK_DELETE, 0), "delete");
list.getActionMap().put("delete", new WAbstractAction() {
    @Override
    public void actionPerformed(WActionEvent e) {
        int index = list.getSelectedIndex();
        if (index >= 0) model.remove(index);
    }
});
```

**実行した画面** ([`DeleteSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/DeleteSwing.java) / [`DeleteWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/DeleteWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Delete (Swing)](images/swing_to_winui4k/JList/DeleteSwing.png) | ![Delete (WinUI4K)](images/swing_to_winui4k/JList/DeleteWinUI4K.png) |

WinUI のリストでは、キーボードのフォーカスはリストの中の項目にあります。項目にフォーカスがあるときも効くように、
`WHEN_ANCESTOR_OF_FOCUSED_COMPONENT` (リストかその中のどれかにフォーカスがあるとき) のマップに入れてください。

<a id="list-move"></a>
### 3.12 項目を上下に動かす

選んだ項目を 1 つ上へ動かし、選択も付いていかせます。

**Swing (Java)**

```java
JButton up = new JButton("上へ");
up.addActionListener(e -> {
    int index = list.getSelectedIndex();
    if (index <= 0) return;
    String item = model.remove(index);
    model.add(index - 1, item);
    list.setSelectedIndex(index - 1);
    list.ensureIndexIsVisible(index - 1);
});
```

**WinUI4K (Java)**

```java
WButton up = new WButton("上へ", Symbol.UP);
up.addActionListener(e -> {
    int index = list.getSelectedIndex();
    if (index <= 0) return;
    String item = model.remove(index);
    model.add(index - 1, item);
    list.setSelectedIndex(index - 1);
    list.ensureIndexIsVisible(index - 1);
});
```

**実行した画面** ([`MoveSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/MoveSwing.java) / [`MoveWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/MoveWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Move (Swing)](images/swing_to_winui4k/JList/MoveSwing.png) | ![Move (WinUI4K)](images/swing_to_winui4k/JList/MoveWinUI4K.png) |

モデルの操作は Swing とまったく同じです。

<a id="list-filter"></a>
### 3.13 入力に合わせて絞り込む

入力欄に入れた文字を含む項目だけを表示します。

**Swing (Java)**

```java
List<String> all = Arrays.asList("東京", "大阪", "名古屋", "札幌", "福岡", "京都");
DefaultListModel<String> model = new DefaultListModel<>();
all.forEach(model::addElement);
JList<String> list = new JList<>(model);

JTextField filter = new JTextField(15);
filter.getDocument().addDocumentListener(new DocumentListener() {
    @Override public void insertUpdate(DocumentEvent e) { apply(); }
    @Override public void removeUpdate(DocumentEvent e) { apply(); }
    @Override public void changedUpdate(DocumentEvent e) { apply(); }

    private void apply() {
        model.clear();
        all.stream().filter(s -> s.contains(filter.getText())).forEach(model::addElement);
    }
});
```

**WinUI4K (Java)**

```java
List<String> all = Arrays.asList("東京", "大阪", "名古屋", "札幌", "福岡", "京都");
WDefaultListModel<String> model = new WDefaultListModel<>();
all.forEach(model::addElement);
WList list = new WList(model);

WTextField filter = new WTextField("絞り込み");
filter.addTextChangedListener(text -> {
    model.clear();
    all.stream().filter(s -> s.contains(text)).forEach(model::addElement);
});
```

**実行した画面** ([`FilterSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/FilterSwing.java) / [`FilterWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/FilterWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Filter (Swing)](images/swing_to_winui4k/JList/FilterSwing.png) | ![Filter (WinUI4K)](images/swing_to_winui4k/JList/FilterWinUI4K.png) |

どちらのサンプルも、表示する前に `filter.setText("京")` を呼んでいます。WinUI4K の画面が絞り込まれていないのは、[2.4](#textfield-change) と同じく、表示する前の変更が `addTextChangedListener` のリスナーに届かないためです。

候補を出す入力欄そのものが欲しいときは、WinUI の `AutoSuggestBox` を包んだ `WAutoSuggestBox` も使えます。

<a id="list-popup"></a>
### 3.14 右クリックメニューを付ける

**Swing (Java)**

```java
JPopupMenu popup = new JPopupMenu();
JMenuItem rename = new JMenuItem("名前を変える");
rename.addActionListener(e -> renameSelected());
JMenuItem delete = new JMenuItem("削除");
delete.addActionListener(e -> deleteSelected());
popup.add(rename);
popup.addSeparator();
popup.add(delete);
list.setComponentPopupMenu(popup);
```

**WinUI4K (Java)**

```java
WMenuFlyout popup = new WMenuFlyout();
popup.add("名前を変える").addActionListener(() -> renameSelected());
popup.addSeparator();
popup.add(new WMenuFlyoutItem("削除", Symbol.DELETE)).addActionListener(() -> deleteSelected());
list.setComponentPopupMenu(popup);
```

**実行した画面** ([`PopupSwing.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/PopupSwing.java) / [`PopupWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/PopupWinUI4K.java))

| Swing | WinUI4K |
|---|---|
| ![Popup (Swing)](images/swing_to_winui4k/JList/PopupSwing.png) | ![Popup (WinUI4K)](images/swing_to_winui4k/JList/PopupWinUI4K.png) |

- `JPopupMenu` → `WMenuFlyout`、`JMenuItem` → `WMenuFlyoutItem` です。`add(String)` は作った項目を返すので、そのままリスナーを付けられます。
- 右クリックのほか、Shift+F10 やアプリケーション キー、タッチの長押しでも開きます (WinUI の `ContextFlyout`)。

<a id="list-winui"></a>
### 3.15 項目のクリックとチェックボックスの選択

Swing には無いが、WinUI のリストでよく使う機能です。

```java
// 項目のクリック: 選択とは別に「押された」ことを受け取る (ナビゲーションのメニューなど)
WList menu = new WList(new String[] {"ドキュメント", "ピクチャ", "ミュージック"});
menu.setItemClickEnabled(true);
menu.addItemClickListener(item -> navigateTo(item));   // item は押された項目の文字列

// チェックボックスで選ぶ複数選択 (タッチでも複数を選びやすい)
WList tags = new WList(new String[] {"仕事", "家族", "趣味", "買い物"});
tags.setSelectionMode(ListViewSelectionMode.MULTIPLE);
```

**実行した画面** ([`ItemClickWinUI4K.java`](../winui4k/src/test/java/com/appkitbox/winui4k/swing_to_winui4k/jlist/ItemClickWinUI4K.java))

![ItemClick (WinUI4K)](images/swing_to_winui4k/JList/ItemClickWinUI4K.png)

- `addItemClickListener` のリスナーは選択の状態に関係なく、押すたびに呼ばれます。選択させずにクリックだけを受け取るなら
  `setSelectionMode(ListViewSelectionMode.NONE)` と組み合わせます。
- `ListViewSelectionMode` は WinUI の選択モード (`NONE`・`SINGLE`・`MULTIPLE`・`EXTENDED`) です。`MULTIPLE` はクリックで選択を切り替える
  チェックボックスの形、`EXTENDED` は Ctrl / Shift の形 (Swing の `MULTIPLE_INTERVAL_SELECTION` に当たる) です。

<a id="list-kotlin"></a>
### 3.16 Kotlin で書く

この章のユースケースをまとめて Kotlin で書くと、次のようになります。

```kotlin
import com.appkitbox.winui4k.*

data class Employee(val name: String, val department: String) {
    override fun toString() = name
}

fun main() {
    WinUiUtilities.invokeLater {
        val frame = WFrame("リストのサンプル")
        frame.layout = WFlowLayout()

        // モデルとリスト (3.3)
        val model = WDefaultListModel<Employee>().apply {
            addElement(Employee("佐藤", "営業部"))
            addElement(Employee("鈴木", "開発部"))
            addElement(Employee("高橋", "総務部"))
        }
        val list = WList(model).apply {
            width = 280.0
            fixedCellHeight = 36
            visibleRowCount = 6
        }

        // レンダラー (3.9)
        list.cellRenderer = object : WDefaultListCellRenderer() {
            override fun getText(value: Any?): String {
                val employee = value as Employee
                return "${employee.name} (${employee.department})"
            }

            override fun getIcon(value: Any?): WIcon = Symbol.CONTACT
        }

        // 選択の通知 (3.5)
        val detail = WLabel("選んでください")
        list.addListSelectionListener {
            val employee = list.selectedValue as Employee?
            detail.text = employee?.let { "${it.name} さんを選びました" } ?: "選んでください"
        }

        // ダブルクリック (3.8)
        list.addMouseListener(object : WMouseAdapter() {
            override fun mouseClicked(e: WMouseEvent) {
                if (e.clickCount != 2) return
                val index = list.locationToIndex(e.point)
                if (index >= 0) detail.text = "${model.getElementAt(index).name} さんを開きます"
            }
        })

        // Delete キーで削除 (3.11)
        val deleteSelected = object : WAbstractAction("削除", Symbol.DELETE) {
            override fun actionPerformed(e: WActionEvent) {
                val index = list.selectedIndex
                if (index >= 0) model.remove(index)
            }
        }
        list.getInputMap(WComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(WKeyStroke.getKeyStroke(WKeyEvent.VK_DELETE, 0), "delete")
        list.actionMap.put("delete", deleteSelected)

        // 右クリックメニュー (3.14)
        list.componentPopupMenu = WMenuFlyout().apply { add(deleteSelected) }

        // 追加 (3.3)
        val name = WTextField("名前")
        val add = WButton("追加", Symbol.ADD)
        add.addActionListener {
            if (name.text.isNotBlank()) {
                model.addElement(Employee(name.text.trim(), "未配属"))
                list.selectedIndex = model.size - 1
                list.ensureIndexIsVisible(model.size - 1)
                name.text = ""
            }
        }

        val row = WPanel(8.0, Orientation.HORIZONTAL)
        row.add(name)
        row.add(add)
        row.add(WButton(deleteSelected))
        frame.add(row)
        frame.add(list)
        frame.add(detail)
        frame.setSize(480, 480)
        frame.isVisible = true
    }
}
```

<a id="list-notes"></a>
### 3.17 注意点

- `WList` には型引数がありません。`getSelectedValue()` と `getSelectedValuesList()` は `Object` を返すので、キャストが要ります。
  モデル (`WDefaultListModel<E>`) には型引数があるので、要素はモデルから型付きで読むこともできます (`model.getElementAt(list.getSelectedIndex())`)。
- 既定の選択モードが違い、Swing は複数、WinUI4K は 1 つです ([3.6](#list-multiple))。
- `WList` は `JScrollPane` に当たるものに入れないでください。WinUI のリストは自分でスクロールします。大きさは `setWidth` / `setHeight` か `setVisibleRowCount` で決めます。
- レンダラーは毎回新しいコンポーネントを返す必要があります。Swing のように 1 つのラベルを使い回すと表示に失敗します ([3.9](#list-renderer))。
- フォーカスはリストの中の項目にあるので、キーバインドは `WHEN_ANCESTOR_OF_FOCUSED_COMPONENT` のマップに入れます ([3.11](#list-delete))。
- `setModel` でモデルを差し替えると、表示を作り直して選択を外します (Swing と同じ)。
- ドラッグでの並べ替え・`JList` の `setDragEnabled`: この版では扱いません。
- `setLayoutOrientation` (横に流す並べ方): `WList` にはありません。タイルのように並べるときは WinUI の `ItemsView` を包んだ `WItemsView` を使います。

<a id="checklist"></a>
## 付録 A. 移植のチェックリスト

Swing のコードを WinUI4K に移すときに、順に確かめる点です。

1. import と型名を替える: `javax.swing.J*` / `java.awt.*` の型を `com.appkitbox.winui4k.W*` に替えます
   (`JButton` → `WButton`、`Color` → `WColor`、`Font` → `WFont`、`KeyStroke` → `WKeyStroke`、`KeyEvent` → `WKeyEvent`、
   `AbstractAction` → `WAbstractAction`、`DefaultListModel` → `WDefaultListModel`、`Dimension` → `WSize` など)。
2. UI スレッドへの投入を替える: `SwingUtilities.invokeLater` → `WinUiUtilities.invokeLater`。`SwingWorker` は
   `CompletableFuture` などと `invokeLater` の組み合わせにします ([1.13](#button-background))。
3. パネルの型を替える: `new JPanel()` は `new WLayoutPanel()` (既定は `WFlowLayout`)、`new JPanel(layout)` は
   `new WLayoutPanel(layout)` にします。`WFrame` の中身は `JFrame` と同じ `BorderLayout` なので、`frame.add(c, BorderLayout.NORTH)` は
   `frame.add(c, WBorderLayout.NORTH)` にするだけです ([0.2](#basics-differences))。
4. `new WTextField("...")` を確かめる: 初めの文字列のつもりなら `setText` に直します ([2.17](#textfield-notes))。
5. 文字の揃えを確かめる: `setHorizontalAlignment(JTextField.RIGHT)` は `setTextAlignment(TextAlignment.RIGHT)` に直します。
6. `JScrollPane` を外す: `JList` を包んでいた `JScrollPane` は外します ([3.2](#list-basic))。
7. リストの選択モードを確かめる: Swing の既定 (複数) に頼っていたなら、`setSelectionMode(WListSelectionModel.MULTIPLE_INTERVAL_SELECTION)` を足します。
8. リストの型を直す: `JList<E>` を `WList` にし、`getSelectedValue()` にキャストを足します。
9. レンダラーを直す: 1 つのラベルを使い回すレンダラーは、毎回新しいコンポーネントを返す形に直します。
10. Swing 独自の仕組みを置き換える: `InputVerifier` はフォーカスのリスナーへ ([2.10](#textfield-validate))、`UndoManager` は組み込みの元に戻すへ
    ([2.13](#textfield-undo))、HTML の文字はコンポーネントの中身へ ([1.16](#button-notes)) 置き換えます。
11. WinUI の機能で簡単にできるところを探す: 案内文 (`setPlaceholderText`)・見出し (`setHeader`)・文字数の上限 (`setMaxLength`)・
    アクセントのボタン (`setAccent`)・ボタンのフライアウト (`setFlyout`) は、Swing で自前で書いていたコードを置き換えられます。
