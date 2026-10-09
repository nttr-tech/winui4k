# Java API リファクタリング候補

## 目的

WinUI4K は Kotlin と Java の両方から利用できることを前提とし、特に Java API は
Swing に近い命名と、Java 8 で無理なく扱える型を優先する。

Java 版 Sample Gallery の実装を通して、次の互換性改善はすでに行った。

- リスナーを `Runnable`、`Consumer`、`IntConsumer` などの Java SAM で公開
- Kotlin 側には関数型のリスナー API を維持
- デフォルト引数に対応する Java オーバーロードを追加
- companion object や object のメンバーを静的に公開
- 三状態を維持しつつ、二状態用に `isSelected` を追加
- `kotlin.Pair` を組み立てずに済む `WGradientStop`／`Map` API を追加
- Kotlin 専用の `Pair`／関数型 API を `@JvmSynthetic` で Java から隠す (項目 1)

本文書では、それらを適用した後にも残っている Java API の改善候補をまとめる。
内部実装を整理するだけの変更ではなく、Java 利用者にとっての効果、互換性、
推奨する公開シグネチャ、検証方法を基準に優先順位を付ける。

## 基本方針

今後の Java API 変更では、次の原則を守る。

1. Java 8 を最低動作バージョンとして維持する。
2. 既存の Kotlin 呼び出しを不必要に冗長にしない。
3. Java では Kotlin ランタイム固有型を意識させない。
4. Swing に対応する概念がある場合は、その命名と利用パターンを優先する。
5. 既存 API の置き換えより、互換性を維持した追加と段階的な非推奨化を優先する。
6. Java と Kotlin のソース互換性だけでなく、公開 JVM シグネチャのバイナリ互換性も確認する。

---

## 1. Kotlin 専用 API を Java から隠す (実施済み)

### 優先度

高

### 実施前の状況

`WGradientStop` と `Map<String, String>` を使う Java 向け API は追加済みだったが、
従来の `Pair` API も Java の公開シグネチャに残っていた。加えて
`WRichTextBlock.addParagraph` の Kotlin レシーバー付きラムダ版が
`kotlin.jvm.functions.Function1` として Java から見えていた。

```java
WLinearGradientPaint(List<Pair<Double, WColor>> stops);
List<Pair<Double, WColor>> getStops();

WAppNotification addButton(String content, Pair<String, String>... arguments);

void addParagraph(Function1<ParagraphBuilder, Unit> build);
```

### 実施内容

`@JvmSynthetic` はコンストラクターには付けられないため、
`WLinearGradientPaint` は内部表現ごと `List<WGradientStop>` へ統一した。
Kotlin の Pair 版は、クラスと同名のトップレベル関数 (`@JvmSynthetic`) へ移し、
`WLinearGradientPaint(listOf(0.0 to color))` の記述をそのまま維持している。
`List<Pair<...>>` と `List<WGradientStop>` は同じ `List` に型消去されるため、
コンストラクターとしては共存できないことへの対応でもある。

- `WLinearGradientPaint.stops` の型を `List<WGradientStop>` に変更
  (Java の `getStops()` も `List<WGradientStop>` を返す)
- Kotlin 向け Pair 版はトップレベル関数 `WLinearGradientPaint(List<Pair<Double, WColor>>, Double)` へ移動
- `WAppNotification.addButton(String, vararg Pair)` に `@JvmSynthetic` を付与し、
  Java 向けには `@JvmName("addButton")` の Map 版 (`@JvmOverloads` で引数なし版も生成) を公開
- `WRichTextBlock.addParagraph(ParagraphBuilder.() -> Unit)` に `@JvmSynthetic` を付与
  (Java 向けは既存の `Consumer<ParagraphBuilder>` 版)

Java から使えるのは次のみになった。

```java
new WLinearGradientPaint(
    new WGradientStop(0.0, WColor.RED),
    new WGradientStop(1.0, WColor.BLUE)
);

new WLinearGradientPaint(
    45.0,
    new WGradientStop(0.0, WColor.RED),
    new WGradientStop(1.0, WColor.BLUE)
);

notification.addButton("承認", Collections.singletonMap("action", "approve"));
notification.addButton("閉じる");

List<WGradientStop> stops = paint.getStops();
```

### 検証結果

`com.appkitbox.winui4k` パッケージの全クラス (292 個) を `javap -v -p` で検査し、
`ACC_SYNTHETIC` が付いていない public／protected メンバーのシグネチャに
`kotlin/` 由来の型が 1 件も現れないことを確認した。

### 互換性

- `@JvmSynthetic` の追加は JVM 上のメソッド自体を削除しないため、
  既存の Kotlin ソースと既存バイナリからの呼び出しは維持できる。
- `WLinearGradientPaint.stops` の型変更はソース／バイナリ非互換。
  Kotlin から `paint.stops` を Pair として読んでいたコードは修正が必要。
- Java ソースが Pair 版を直接使っていた場合はコンパイルできなくなるため、
  リリースノートに移行例を載せる。

### 残作業

- 上記の検査を項目 3 の Java API コンパイルテストへ自動化として取り込む
  (現状は手動の `javap` 検査)。
- `data class` (`WPoint`／`WDimension`／`WRectangle`／`WSize`／`WInsets`／`WGradientStop`) が
  生成する `component1()`／`copy()` は Kotlin の分解宣言用で、Java の補完では不要。
  生成メンバーには `@JvmSynthetic` を付けられないため、隠すには `data` を外して
  `equals`／`hashCode`／`toString` を手書きするしかない。Kotlin 側の利便性を落とすので現状は許容する。

---

## 2. Kotlin `internal` 実装の JVM 公開範囲を整理する

### 優先度

高

### 現状

Kotlin の `internal` は Java のパッケージ可視性には変換されず、JVM バイトコードでは
原則として `public` になる。メソッド名はマングルされるものが多いが、
コンストラクターや内部型を含む一部の API は Java から呼べる。

生成クラスには、例えば次のような実装詳細が現れる。

```java
WToggleButton(ComPtr inspectable);
WComponent(ComPtr inspectable);

getInspectable$com_appkitbox_winui4k_winui4k();
getComObject$com_appkitbox_winui4k_winui4k();
```

これらは COM ポインターの所有権規則を知らずに呼ぶと、二重解放、リーク、
UI スレッド違反などにつながる。Java API の補完候補としても不要である。

### 推奨案

対象を次の三種類に分けて対応する。

#### 内部メソッドと内部プロパティ

Java から利用する必要がないものには `@JvmSynthetic` を付ける。
プロパティの場合は、必要に応じて getter／setter の use-site target を指定する。

```kotlin
@get:JvmSynthetic
internal val inspectable: ComPtr

@JvmSynthetic
internal fun noteAssignedSize(width: Double, height: Double)
```

#### 内部コンストラクター

`internal constructor` が Java で public になるクラスを一覧化する。
コンストラクターへ単純にアノテーションを付けられない場合や、
継承階層のため非公開化できない場合があるので、次をクラスごとに選択する。

- `private constructor` と内部ファクトリーへ移す
- Java からの継承を許可しないクラスは継承構造を閉じる
- 公開サブクラスに必要な基底コンストラクターは `protected` に限定する
- COM 型を直接渡さない安全なファクトリー境界を設ける

#### 内部パッケージ

`com.appkitbox.winui4k.internal` を Java 利用者向け API として扱わないことを
ドキュメントとモジュール境界で明示する。JPMS を採用する場合は非 export とする。
ただし Java 8 では JPMS による制限が使えないため、アノテーションや可視性の整理も必要である。

### 実施済みの範囲

項目 1 と同じ `@JvmSynthetic` のパターンで済むものだけ先に対応した。

- enum の `internal companion object` にある `of(native: Int)` (59 個) へ `@JvmSynthetic` を付与
  (`native` は winmd の生の値で、Java 利用者に見せる必要がない)
- `WCalendarView` の 2 つの enum だけ `companion object` が public だったため
  他の 57 個と同じ `internal companion object` に揃えた

このとき、`internal companion object` にしても JVM では
`public static final Companion` フィールドが残るため、
`HorizontalAlignment.Companion.of(0)` のように Java からは呼べたままになることを実測で確認した。
Kotlin の `internal` は Java 側の遮断には使えないので、メンバー単位の `@JvmSynthetic` が必要になる。

残っているのは、mangled 名を持つ内部メソッド (`getInspectable$com_appkitbox_winui4k_winui4k` など)、
`ComPtr` を受け取る内部コンストラクター、`internal` クラス自体の露出。

### 注意点

内部コンストラクターの変更は、公開クラスの継承可否や Kotlin のモジュール内実装に影響する。
一括置換せず、生成された Java シグネチャを確認しながら段階的に行う。

### 完了条件

- Java の通常のソースコードから `ComPtr` を受け取るコンストラクターを呼べない。
- Java 向け API 一覧に `$com_appkitbox...` の内部メソッドを掲載しない。
- 公開クラスを Java から継承できるかどうかが、クラスごとに明確になっている。

---

## 3. Java API 専用のコンパイルテストを追加する

### 優先度

高

### 目的

Java Sample Gallery は実用的な互換性テストになっているが、サンプルで使わない
オーバーロードや戻り値までは検査できない。Java API の契約を直接テストする
小さなコンパイルテストを追加する。

### 推奨する検査

#### Java ソースのコンパイル

Java 8 のソースレベルで、次のコードがコンパイルできることを確認する。

- 引数なし／省略形のコンストラクター
- 単純なラムダによるリスナー登録
- 同じ SAM インスタンスによる登録解除
- `WGradientStop` と `Map` を使う Pair 不要 API
- `GridLength.AUTO` などの静的フィールド
- `WColor.RED` などの定数
- `isSelected()` などの JavaBeans 形式のBoolean getter

コンパイルオプションは可能な限り次を使用する。

```text
-Xlint:all
-Werror
```

JDK の廃止オプション警告など、プロジェクト全体で避けられない警告は
種類を限定して抑制する。

#### Java から見えてはいけない API

コンパイル失敗を期待するテスト、またはバイトコード検査で次を確認する。

- `kotlin.Pair` を取る Kotlin 専用オーバーロード
- `kotlin.jvm.functions.FunctionN`
- `ComPtr` を取る内部コンストラクター
- `.Companion` や object の `.INSTANCE` を必要とする入口

#### 公開シグネチャの差分

`javap -public -s`、Japicmp、または同等の API 差分ツールで、
リリース間の公開 JVM シグネチャを比較する。Kotlin 向け API 検査だけでは
Java からの見え方を保証できないため、Java 視点の検査を別に持つ。

### テスト配置案

- `winui4k/src/testJavaCompatibility/java`
- または専用の `winui4k-java-api-test` モジュール

Sample Gallery は使用例と統合テストとして維持し、API テストは最小コードで
各シグネチャを網羅する。

---

## 4. 非同期 API に `CompletionStage` を追加する

### 優先度

中

### 現状

WinRT の非同期操作は主にコールバックで公開している。

```java
dialog.show(owner, result -> handle(result));
webView.executeScript(script, json -> handle(json));
```

一回の処理なら十分だが、複数操作の連結、例外処理、タイムアウト、
テストでの完了待機が書きにくい。

### 対象候補

- `WContentDialog.show`
- `WWebView.executeScript`
- `WWebView.ensureCoreWebView2`
- 今後追加する WinRT `IAsyncAction`／`IAsyncOperation<T>` のラッパー

### 推奨案

Java 8 で利用できる `CompletionStage`／`CompletableFuture` を追加する。

```java
CompletionStage<ContentDialogResult> showAsync(WComponent owner);

CompletionStage<String> executeScriptAsync(String script);

CompletionStage<Void> ensureCoreWebView2Async();
```

利用例:

```java
dialog.showAsync(owner)
    .thenAccept(result -> {
        if (result == ContentDialogResult.PRIMARY) {
            save();
        }
    })
    .exceptionally(error -> {
        showError(error);
        return null;
    });
```

完了処理が UI スレッド上で実行されるのか、WinRT の完了スレッド上なのかを
API ドキュメントで保証する。既存コールバック版が UI スレッドで通知する場合は、
Future 版も同じ規則に揃える。

Kotlin では core API を無理に `suspend` にせず、
`winui4k-extension-coroutines` に suspend ラッパーを追加する。

### 例外とキャンセル

- HRESULT エラーは Future を exceptionally complete する。
- 呼び出し前提を満たさない場合に「何もせず戻る」のではなく、
  Future 版では失敗理由を通知する。
- WinRT 操作を取り消せる場合は、`CompletableFuture.cancel` との連携を検討する。

### 互換性

既存の `show`／`executeScript` は残し、追加 API として提供する。

---

## 5. リスナー購読ハンドルを追加する

### 優先度

中

### 現状

Swing 形式の `addXxxListener`／`removeXxxListener` では、解除のために
登録時と同じリスナーインスタンスを保持する必要がある。

```java
DoubleConsumer listener = value -> update(value);
slider.addChangeListener(listener);
slider.removeChangeListener(listener);
```

これは Swing として自然な一方、画面単位で複数購読をまとめて破棄する場合には
管理コードが増える。

### 推奨案

既存 API を維持したまま、購読ハンドルを返す別名 API を追加する。

```java
AutoCloseable onChange(DoubleConsumer listener);
AutoCloseable onAction(Runnable listener);
```

利用例:

```java
AutoCloseable subscription =
    slider.onChange(value -> update(value));

subscription.close();
```

独自型を使う場合は、例外を宣言しない `close()` を持つ型が扱いやすい。

```java
public interface WSubscription extends AutoCloseable {
    @Override
    void close();
}
```

`close()` は冪等にし、複数回呼んでも安全にする。購読元のコンポーネントが先に
破棄された場合も例外にしない。

### Kotlin API

Kotlin では戻り値を `use` する用途より、画面のライフサイクルに保持して
まとめて解除する用途が中心になる。Java と共通の `WSubscription` をそのまま
返してよい。

---

## 6. 複雑なイベントにイベント型と専用 SAM を導入する

### 優先度

中

### 現状

単純なクリックには `Runnable`、単一値には `Consumer<T>` が適している。
一方、複数の値や nullable 値を渡すイベントでは、汎用 SAM だけでは意味が分かりにくい。

例:

```java
BiConsumer<String, TextChangeReason>
BiConsumer<Boolean, WebErrorStatus>
Consumer<Boolean>
```

Java の生成シグネチャだけを見ると、各引数が何を意味するか分かりにくく、
イベントへ情報を追加するとリスナーシグネチャの変更が必要になる。

### 専用型を検討する対象

- `WAutoSuggestBox` の TextChanged／QuerySubmitted
- `WWebView` の NavigationStarting／NavigationCompleted
- `WTeachingTip` の Close
- `WToggleButton` の三状態変更
- 今後、sender、旧値、新値、キャンセル可否などを追加するイベント

### 推奨案

Java 8 で利用できる通常クラスと `@FunctionalInterface` を定義する。

```java
public final class NavigationCompletedEvent {
    private final boolean successful;
    private final WebErrorStatus errorStatus;

    public boolean isSuccessful() { ... }
    public WebErrorStatus getErrorStatus() { ... }
}

@FunctionalInterface
public interface NavigationCompletedListener {
    void navigationCompleted(NavigationCompletedEvent event);
}
```

Kotlin 実装では `fun interface` を利用できるが、Java から見た抽象メソッドが
`void` を返すことを必ずコンパイルテストで確認する。

### 適用しない対象

次のような単純なイベントは、JDK 標準 SAM の方が短く自然なので変更しない。

- 引数なしのアクション: `Runnable`
- インデックス: `IntConsumer`
- 数値: `DoubleConsumer`
- 単純な文字列通知: `Consumer<String>`

専用型をすべてのイベントへ機械的に導入しない。

---

## 7. 二状態と三状態の変更通知を分離する

### 優先度

中

### 現状

`WToggleButton.isChecked` は WinUI の三状態を表すため `Boolean?` であり、
Java では `Boolean` になる。`isSelected` は二状態用として追加済みだが、
`WCheckBox` や `WRadioButton` の変更リスナーは三状態の
`Consumer<Boolean>` を共有している。

そのため、二状態としてしか使っていないコードでも次の記述が必要になる。

```java
checkBox.addItemListener(checked -> {
    control.setEnabled(Boolean.TRUE.equals(checked));
});
```

### 推奨案 A: 三状態を enum で表す

```java
public enum CheckState {
    CHECKED,
    UNCHECKED,
    INDETERMINATE
}
```

三状態 API:

```java
CheckState getCheckState();
void setCheckState(CheckState state);
void addCheckStateListener(Consumer<CheckState> listener);
```

`null` に意味を持たせずに済み、switch で安全に処理できる。

### 推奨案 B: 二状態専用リスナーを追加する

Java 標準には `BooleanConsumer` がないため、専用 SAM を定義する。

```java
@FunctionalInterface
public interface BooleanConsumer {
    void accept(boolean value);
}
```

二状態 API:

```java
void addSelectionListener(BooleanConsumer listener);
void removeSelectionListener(BooleanConsumer listener);
```

三状態を必要とするコードだけが既存の `addItemListener` を使う。

### Swing との対応

より Swing に寄せるなら、`ItemEvent` 相当のイベントオブジェクトと
`ItemListener` を定義する案もある。ただし、単純な二状態取得にイベントオブジェクトを
要求すると冗長になるため、実際の利用例を比較して決定する。

### 互換性

既存の `isChecked`／`addItemListener` は三状態 API として残す。
`isSelected` と二状態リスナーを追加 API とする。

---

## 8. コレクション API に配列／vararg オーバーロードを追加する

### 優先度

中

### 現状

Java Sample Gallery には次の形が多い。

```java
new WComboBox(Arrays.asList("A", "B", "C"));
new WList(Arrays.asList("A", "B", "C"));
new WTable(Arrays.asList(column1, column2));
```

Java 9 以降なら `List.of` を使えるが、WinUI4K は Java 8 をサポートするため、
ドキュメントやサンプルでは `Arrays.asList` が必要になる。

### 推奨案

少数の固定項目を扱うコントロールに vararg オーバーロードを追加する。

```java
new WComboBox("A", "B", "C");
new WList("A", "B", "C");
new WListBox("A", "B", "C");
new WTable(column1, column2);

comboBox.setItems("A", "B", "C");
itemsView.setItems(item1, item2);
```

Kotlin 側では `vararg` を自然に利用できる。既存の `List` 版は、
動的に生成した項目や大量データ用として残す。

### 設計上の注意

- 引数なしコンストラクターとのオーバーロード解決をJavaコンパイルテストで確認する。
- 配列を内部で保持せず、防御的にコピーする。
- `List` の入力を後から呼び出し側が変更した場合の挙動も統一する。
- 将来モデル型を汎用化する場合は、`Collection<? extends T>` 相当の受け入れも検討する。

---

## 9. Java 向け nullability を明示する

### 優先度

中

### 現状

Kotlin 対応 IDE は Kotlin メタデータから nullability を推測できるが、
すべての Java コンパイラー、静的解析器、ドキュメント生成器が同じように扱うとは限らない。

特に次の API では null に明確な意味がある。

- 未選択時の `selectedItem`／`selectedNode`
- `WToggleButton.isChecked` の不確定状態
- `WCalendarDatePicker.date`
- `WTimePicker.selectedTime`
- `WOverlappedPresenter` のサイズ制約
- Navigation や Tree のイベント引数
- コンテンツ、ToolTip、Flyout などの解除

### 推奨案

JSpecify の `@NullMarked` と `@Nullable` を公開 API に導入する。

- パッケージまたはモジュールを原則 `@NullMarked` にする。
- null を許可する引数と戻り値だけへ `@Nullable` を付ける。
- Kotlin の型とアノテーションが食い違わないことを検査する。

### 注意点

- annotation dependency が利用者のランタイムに不要な構成にする。
- Java 8 のコンパイルと Javadoc 生成で問題がないバージョンを選ぶ。
- nullability 強化による IDE 警告の変化を、API 互換性変更として記録する。

---

## 10. ネイティブリソースのライフサイクルを Java で表現する

### 優先度

中から低

### 現状

`WWebView.close()` は WebView2 のブラウザープロセスと関連リソースを終了し、
呼び出し後は同じコントロールを再利用できない。

一方、`WSwipeControl.close()` は開いている Swipe UI を閉じる操作であり、
オブジェクトの破棄ではない。同じ `close` でも意味が異なる。

### 推奨案

明示的なリソース破棄を意味するクラスだけ `AutoCloseable` を実装する。

```java
try (WWebView webView = new WWebView()) {
    // 使用
}
```

ただし UI コンポーネントを try ブロックの短いスコープで管理することが
実際の画面ライフサイクルと合わない場合もある。`WFrame` や親コンテナからの削除時に
自動終了する設計も含めて検討する。

`WSwipeControl.close()` のような UI 操作は `AutoCloseable` にしない。
必要なら `closeSwipe()` など、破棄と混同しにくい別名を検討する。

### 完了条件

- `close()` が破棄を意味するクラスと、表示状態変更を意味するクラスが区別される。
- ネイティブリソースを持つクラスの再利用可否が Javadoc に明記される。
- `close()` の複数回呼び出しが安全かどうかを定義し、テストする。

---

## 11. vararg の後ろにコールバックを置かない

### 優先度

中から低

### 現状

Kotlin では末尾ラムダを使いやすくするため、次の並びが自然である。

```kotlin
fun addKeyboardAccelerator(
    key: VirtualKey,
    vararg modifiers: VirtualKeyModifier,
    action: Runnable,
)
```

Java では vararg が末尾ではないため、次のように配列を明示する必要がある。

```java
component.addKeyboardAccelerator(
    VirtualKey.S,
    new VirtualKeyModifier[] {
        VirtualKeyModifier.CONTROL,
        VirtualKeyModifier.SHIFT
    },
    () -> save()
);
```

### 推奨案

Java 用の JVM ブリッジではコールバックを先に置き、vararg を末尾にする。

```java
void addKeyboardAccelerator(
    VirtualKey key,
    Runnable action,
    VirtualKeyModifier... modifiers
);
```

Kotlin 側の既存関数は `@JvmSynthetic` で Java から隠し、
Java ブリッジは `@JvmName` を使って自然なメソッド名で公開する。
リスナー API と同じ二層構成にできる。

### 完了条件

次が Java でコンパイルできる。

```java
component.addKeyboardAccelerator(
    VirtualKey.S,
    () -> save(),
    VirtualKeyModifier.CONTROL
);
```

---

## 実施順序

互換性リスクと効果を考慮し、次の順序を推奨する。

1. ~~残存する Pair API を Java から隠す。~~ (項目 1 として実施済み)
2. Java API コンパイルテストの基盤を追加する (項目 1 の `javap` 検査もここへ取り込む)。
3. Kotlin internal API の露出を調査し、安全に隠せるものから整理する。
4. 二状態／三状態イベントを分離する。
5. `CompletionStage` ベースの非同期 API を追加する。
6. コレクションの vararg オーバーロードを追加する。
7. nullability アノテーションを導入する。
8. 購読ハンドルとリソースライフサイクル API を追加する。
9. `addKeyboardAccelerator` の Java ブリッジを追加する。

Java API テストを最初に用意することで、後続変更による Kotlin 型の再露出、
オーバーロードの曖昧化、Java 8 非互換を各コミットで検出できる。

## コミット方針

変更理由と互換性を追跡できるよう、原則として次の単位でコミットを分ける。

- Java API テスト基盤
- Kotlin 専用 API の非表示化
- internal API の非表示化
- 二状態／三状態イベント
- 非同期 API
- コレクションオーバーロード
- nullability
- 購読／ライフサイクル
- キーボードアクセラレーター

各コミットでは最低限、次を実行する。

```powershell
.\gradlew.bat :winui4k:compileKotlin
.\gradlew.bat :winui4k:compileTestKotlin
.\gradlew.bat :winui4k-sample-gallery-in-java:compileJava
.\gradlew.bat build
```

公開シグネチャを変更するコミットでは、これに Java API のコンパイルテストと
前リリースに対する JVM API 差分確認を追加する。
