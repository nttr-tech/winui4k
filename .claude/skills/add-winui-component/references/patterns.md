# 実装パターン集

ラッパークラスの実装は既存コードのパターンを踏襲する。各パターンの規範実装 (お手本) を挙げる。

## レイヤー構成 (技術スタック 1 層 = 1 パッケージ)

| パッケージ | 役割 |
|---|---|
| `internal.ffi.api` | FFI バックエンド SPI (Ptr, CallDescriptor, StructType, FfiBackend, Ffi)。バックエンド非依存 |
| `internal.ffi.panama` | Panama (java.lang.foreign) バックエンド。**java.lang.foreign を参照してよいのはここだけ** |
| `internal.win32` | Win32 フラット API (Win32: DPI 宣言、moduleFilePath) |
| `internal.com` | COM の基盤 (ComPtr, Guid, checkHr / WindowsRuntimeException) |
| `internal.winrt` | WinRT ランタイム (Hstring, KComObject, WinRtRuntime, Activation, PropertyValues, Pinterface, Async, addEventHandler) |
| `internal.winui` | WinUI 3 / WinAppSDK 固有 (ABI 定数の *Interop オブジェクト, Dispatcher, WinAppSdkBootstrap, XamlStructs) |
| `com.appkitbox.winui4k` (ルート) | Swing 風の公開 API (WinUiUtilities と W* クラス)。ユーザーが触るのはここだけ (他は internal) |

依存方向: `com.appkitbox.winui4k → internal.winui → internal.winrt → internal.com → internal.ffi.api` (`internal.win32 → internal.ffi.api`)。

`inspectable` は各 W* クラスが持つ既定インターフェースのポインタ。別インターフェースの
メンバーを使うときは `queryInterface` したものを `by lazy` で保持する (WButton の
`contentControl` / `buttonBase` 参照)。

## プロパティの型別パターン

| WinRT の型 | get | put | 規範実装 |
|---|---|---|---|
| HSTRING | `ptr.getString(slot)` | `Hstring.use(value) { h -> ptr.call(slot, h) }` | WTextField.text |
| boolean | `ptr.getBool(slot)` | `ptr.putBool(slot, value)` | WControl.isEnabled |
| INT32 | `ptr.getInt(slot)` | `ptr.call(slot, value)` | WRepeatButton.delay |
| enum | `MyEnum.of(ptr.getInt(slot))` | `ptr.call(slot, value.native)` | WButtonBase.clickMode |
| DOUBLE | `ptr.getDouble(slot)` | `ptr.call(slot, value)` | WLabel.fontSize |
| オブジェクト参照 | `ptr.getPtr(slot)` / null になりうるなら `getPtrOrNull` | `ptr.call(slot, obj.ptr)`。null 許容はそのまま null を渡す | WButton.flyout |
| Object (box された値) | `PropertyValues.unboxString(boxed)` → 使用後 `boxed.release()` | `PropertyValues.boxString(value)` → put 後 `release()` | WButtonBase.text |
| IReference\<Boolean\> (null あり) | `getPtrOrNull` → `PropertyValues.unboxBool(boxed)` → `release()` | `PropertyValues.boxBool(value)` を `queryInterface(FoundationInterop.IID_IReference_Boolean)` してから put。null は `null` | WToggleButton.isChecked |
| 構造体の値渡し | `XamlStructs.getColor` (out 引数にポインタ渡し) | `XamlStructs.putThickness / putCornerRadius / putGridLength / putColor` (新しい構造体はここに追加) | WColorPicker.color, WComponent.margin |
| Brush (色) | — | `WColor.createBrush()` で SolidColorBrush を作り put 後 `release()` | WBorder.borderColor |
| Windows.Foundation.Uri | `getPtrOrNull(slot)` → `getString(IUriRuntimeClass_get_AbsoluteUri)` | `Activation.factory(CLS_Uri, IID_IUriRuntimeClassFactory).getPtr(CreateUri, hstring)` で生成して put | WHyperlinkButton.navigateUri |

- enum は同じファイル内に `enum class Xxx(internal val native: Int)` +
  `internal companion object { fun of(native: Int) = entries.first { it.native == native } }`
  で定義する。各値に日本語 KDoc、クラス KDoc に「値は winmd から抽出」と明記する
  (規範実装: WButtonBase.kt の ClickMode、WSlider.kt の TickPlacement)。
- コンストラクタ引数で初期値を受けるときは、既定値のままなら put しない
  (`if (text.isNotEmpty()) this.text = text` の形。無駄な COM 呼び出しを避ける)。

## 添付プロパティ (Canvas.Left, Grid.Row, RelativePanel.RightOf など)

レイアウトパネルの添付プロパティは `IXxxStatics` の Set メソッドを呼ぶ。規範実装:
**WCanvas / WGrid / WRelativePanel** の `private companion object { val statics }`。要点:

- `Activation.factory(XamlInterop.CLS_Xxx, XamlInterop.IID_IXxxStatics)` を companion の `by lazy` で保持する
- 第 1 引数は winmd の宣言どおり UIElement か FrameworkElement (Grid は FrameworkElement)
- boolean 引数 (RelativePanel.AlignXxxWithPanel) は 1 バイトなので
  `callWith` で `ArgKind.U8` を含む `CallDescriptor` を明示する (WRelativePanel.putBool 参照)
- 子の追加前に Set しても効く (添付プロパティは要素側に保存される)

## イベント購読 (delegate の実装)

**internal/winrt/Events.kt の `ComPtr.addEventHandler` / `removeEventHandler` と
ルートパッケージの Events.kt の `ListenerTokens` を使う**
(delegate の KComObject 実装と token 管理を共通化済み)。規範実装:
**WButtonBase.addActionListener / removeActionListener** (Click イベント)。要点:

- `addEventHandler(名前, ハンドラ IID, add スロット) { sender, args -> ... }` が token を返すので
  `ListenerTokens` に「リスナー → token」を保持し、remove で `removeEventHandler` に渡す
- args からイベント引数を読む場合は `ComPtr(args)` に包んでスロットを呼ぶ
  (規範実装: WSlider.addChangeListener の NewValue、WList.addItemClickListener の ClickedItem)
- 1 リスナーで複数イベントを購読する場合は token の配列を保持する
  (規範実装: WToggleButton.addItemListener — Checked / Unchecked / Indeterminate の 3 本)
- イベントの型が `TypedEventHandler<TSender, TArgs>` の場合、IID は winmd に無いので
  `Pinterface.iid` で実行時計算する (規範実装: XamlInterop.IID_SplitButtonClickHandler)。署名は
  `pinterface({TypedEventHandler ベース IID};rc(TSender 完全名;{既定 IID});rc(TArgs 完全名;{既定 IID}))`。
  **TArgs が Object のときは `rc(...)` の代わりに `cinterface(IInspectable)`**
  (規範実装: XamlInterop.IID_RatingControlValueChangedHandler)

## Kotlin 側で WinRT インターフェースを実装する

ICommand のように XAML へ渡すオブジェクトを Kotlin で実装する場合の規範実装: **WCommand**。

- delegate と違い WinRT インターフェースは IInspectable 行が必要
  (`inspectable = true` が既定)。メソッドは vtbl[6] から winmd の宣言順
- out 引数への書き込みは `Ffi.backend.memory` の `putLong` / `putByte` などで行う
- 受け取ったポインタを保持するなら `addRef()`、手放すとき `release()`

## ジェネリックインターフェース (IVector<T> など)

実体 IID は winmd に無いので `Pinterface.iid(署名)` で実行時に SHA-1 計算する。
規範実装: FoundationInterop.IID_IVector_UIElement、XamlInterop.IID_ResourceManagerRequestedHandler
(TypedEventHandler<T1, T2>)。既に型付きポインタを持っている場合は QI せず
スロットを直接呼んでよい (WPanel.add のコメント参照)。

## 落とし穴

- **OS ランタイムクラスの生成** (Windows.Foundation.Uri など):
  `Activation.factory(CLS, ファクトリ IID).getPtr(CreateXxx スロット, 引数)` で作る
  (規範実装: WHyperlinkButton.navigateUri)。
- **activatable (既定ファクトリ) のクラス** (ToggleSwitch / ScrollViewer / Primitives.RepeatButton):
  `Activation.activate(CLS).queryInterface(既定 IID)` で生成する。composable とはダンプの
  `activatable factory: <default IActivationFactory>` で見分ける。

- **スロット番号の規約**: IUnknown = 0..2、IInspectable = 3..5、インターフェース本体は 6 から。
  `dump_winmd.py` は補正済みの `vtbl[n]` を表示するのでそのまま使う。
- **WinRT の boolean は 1 バイト**。`getBool` / `putBool` を使う (JAVA_INT で読まない)。
- **ComPtr.call の引数型推論**は Ptr / ComPtr / null (→NULL ポインタ) / Int / Long /
  Double / StructValue (構造体値渡し) のみ。byte 引数などは `callWith` で
  `CallDescriptor` (internal.ffi.api) を明示する。
- **HSTRING の一時利用は `Hstring.use`** でスコープ解放する。
- **Content など Object 型プロパティ**は文字列と UIElement の両方が入りうる。
  get で `unboxString` が null を返すケースを考慮する (WButton.text / content の排他を参照)。
- **AutoSuggestBox の候補リストは ItemsSource でしか更新されない**。Items (ItemCollection) に
  Append してもポップアップに反映されない (テンプレート内のリストが ItemsSource を参照するため)。
  `IIterable<Object>` を Kotlin 実装して put_ItemsSource する
  (規範実装: WAutoSuggestBox.setSuggestions の StringIterable)。
- **UI スレッド制約**: すべての W* API は `WinUiUtilities.invokeLater` のコールバック
  (WinUI の UI スレッド) 上でのみ使用できる。初回の invokeLater で WinUI が自動起動する。