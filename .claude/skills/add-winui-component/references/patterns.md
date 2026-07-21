# 実装パターン集

ラッパークラスの実装は既存コードのパターンを踏襲する。各パターンの規範実装 (お手本) を挙げる。

## レイヤー構成

| パッケージ | 役割 |
|---|---|
| `ffi` | COM/Panama の基盤 (ComPtr, KComObject, Hstring, Guid, Native) |
| `winrt` | WinRT アクティベーションと box/unbox (WinRt) |
| `winui` | ABI 定数 (Abi) とアプリ起動 (WinUiToolkit) |
| `swing` | Swing 風の公開 API (W* クラス)。ユーザーが触るのはここだけ |

`inspectable` は各 W* クラスが持つ既定インターフェースのポインタ。別インターフェースの
メンバーを使うときは `queryInterface` したものを `by lazy` で保持する (WButton の
`contentControl` / `buttonBase` 参照)。

## プロパティの型別パターン

| WinRT の型 | get | put | 規範実装 |
|---|---|---|---|
| HSTRING | `ptr.getString(slot)` | `Hstring.use(value) { h -> ptr.call(slot, h) }` | WTextField.text |
| boolean | `ptr.getBool(slot)` | `ptr.putBool(slot, value)` | WControl.isEnabled |
| enum | `MyEnum.of(ptr.getInt(slot))` | `ptr.call(slot, value.native)` | WButton.clickMode |
| DOUBLE | `ptr.getDouble(slot)` | `ptr.call(slot, value)` | WLabel.fontSize |
| オブジェクト参照 | `ptr.getPtr(slot)` / null になりうるなら `getPtrOrNull` | `ptr.call(slot, obj.ptr)`。null 許容は `?: MemorySegment.NULL` | WButton.flyout |
| Object (box された値) | `WinRt.unboxString(boxed)` → 使用後 `boxed.release()` | `WinRt.boxString(value)` → put 後 `release()` | WButton.text |
| 構造体の値渡し | — | `callWith(slot, 明示 FunctionDescriptor, struct)` | WComponent.margin (Thickness) |

- enum は同じファイル内に `enum class Xxx(internal val native: Int)` +
  `internal companion object { fun of(native: Int) = entries.first { it.native == native } }`
  で定義する。各値に日本語 KDoc、クラス KDoc に「値は winmd から抽出」と明記する
  (規範実装: WButton.kt の ClickMode、WFlyout.kt の FlyoutPlacement)。
- コンストラクタ引数で初期値を受けるときは、既定値のままなら put しない
  (`if (text.isNotEmpty()) this.text = text` の形。無駄な COM 呼び出しを避ける)。

## イベント購読 (delegate の実装)

WinRT の delegate を `KComObject` で実装して `add_XXX` に渡す。規範実装:
**WButton.addActionListener / removeActionListener** (Click イベント)。要点:

- delegate は IInspectable 行を持たないので `KComObject(name, inspectable = false)`、
  `Invoke` は vtbl[3] (= addInterface のメソッドリストの先頭 1 本だけ)
- `add_XXX` の out 引数 `EventRegistrationToken` (int64) を受け取り、
  remove 用に「リスナー → token」を `ArrayDeque` などで保持する
- Invoke の FunctionDescriptor は winmd のシグネチャに合わせる
  (RoutedEventHandler は `(this, sender, args)` → `JAVA_INT, ADDRESS, ADDRESS, ADDRESS`)

## Kotlin 側で WinRT インターフェースを実装する

ICommand のように XAML へ渡すオブジェクトを Kotlin で実装する場合の規範実装: **WCommand**。

- delegate と違い WinRT インターフェースは IInspectable 行が必要
  (`inspectable = true` が既定)。メソッドは vtbl[6] から winmd の宣言順
- out 引数への書き込みは `reinterpret(サイズ)` してから `set(...)`
- 受け取ったポインタを保持するなら `addRef()`、手放すとき `release()`

## ジェネリックインターフェース (IVector<T> など)

実体 IID は winmd に無いので `WinRt.pinterfaceIid(署名)` で実行時に SHA-1 計算する。
規範実装: Abi.IID_IVector_UIElement、Abi.IID_ResourceManagerRequestedHandler
(TypedEventHandler<T1, T2>)。既に型付きポインタを持っている場合は QI せず
スロットを直接呼んでよい (WPanel.add のコメント参照)。

## 落とし穴

- **スロット番号の規約**: IUnknown = 0..2、IInspectable = 3..5、インターフェース本体は 6 から。
  `dump_winmd.py` は補正済みの `vtbl[n]` を表示するのでそのまま使う。
- **WinRT の boolean は 1 バイト**。`getBool` / `putBool` を使う (JAVA_INT で読まない)。
- **ComPtr.call の引数型推論**は MemorySegment / Int / Long / Double のみ。
  構造体値渡しや byte 引数は `callWith` で FunctionDescriptor を明示する。
- **HSTRING の一時利用は `Hstring.use`** でスコープ解放する。
- **Content など Object 型プロパティ**は文字列と UIElement の両方が入りうる。
  get で `unboxString` が null を返すケースを考慮する (WButton.text / content の排他を参照)。
- **UI スレッド制約**: すべての W* API は `WinUiToolkit.launch` のコールバック
  (WinUI の UI スレッド) 上でのみ使用できる。