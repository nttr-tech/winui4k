---
name: kotlin-lint-rules
description: winui4k プロジェクトの Kotlin コード記述ルール (ktlint / detekt の役割分担と、detekt 指摘への対応方針)。Use when writing or editing Kotlin code in this repo, when detekt reports findings (via the PostToolUse hook or `gradlew detekt`), when deciding whether to fix, @Suppress, or change detekt.yml, or when the user asks about 「lint」「detekt」「ktlint」「静的解析」.
---

# Kotlin コードの lint ルール (ktlint / detekt)

## 役割分担

- **ktlint (Spotless 経由)**: フォーマットと命名。設定は
  `buildSrc/src/main/kotlin/winui4k.kotlin-common.gradle.kts` の `ktlintOverrides`。
  Claude Code の PostToolUse hook が編集ごとに `spotlessApply` で自動整形する。
- **detekt**: それ以外の静的解析 (複雑度・例外処理・バグの芽)。設定は
  `config/detekt/detekt.yml` (デフォルト設定との差分のみ)。ktlint と重複する
  フォーマット・命名系ルールは detekt 側で無効化してある。
- 実行: 各モジュールの `detekt` タスク (`check` に含まれ CI でも走る)。
  単一ファイルは `./gradlew.bat detektFile -PdetektFile=<path>` (hook が編集ごとに実行)。
- detekt 1.23 の埋め込みコンパイラは JDK 25 で動かないため、JDK 21 toolchain の
  別プロセスで detekt-cli を実行している。Gradle プラグイン方式へ戻さないこと。

## detekt の指摘が出たときの対応順序

1. **まず修正を検討する**。指摘が妥当ならコードを直す
   (例: `throw IllegalStateException(...)` → `error(...)`、未使用宣言の削除)。
2. **コードが意図的にそうなっている場合だけ `@Suppress` する**。ルール自体は有効なまま、
   その箇所だけ理由付きで抑制するのがこのプロジェクトの標準手法 (下記参照)。
3. **ルールがこのコードベースの性質と根本的に合わない場合のみ `detekt.yml` を変更する**。
   FFI / COM 相互運用ライブラリという性質に由来する誤検知が大量に出る場合が該当
   (例: MagicNumber は vtable スロット・IID 定数のため無効化済み)。
   detekt.yml の変更はユーザーに確認してから行うこと。
4. baseline ファイル (detekt-baseline.xml) は使わない方針。抱えている例外は
   `@Suppress` としてコード上で見える状態を保つ。

## @Suppress で分かりやすいコードにする手法

抑制は「握りつぶし」ではなく「この関数は意図的にルールから外れている」という宣言として書く。

- **最小スコープに付ける**: ファイル全体ではなく、対象の関数・クラス宣言に付ける。
- **必ず理由を書く**: 同じ行の行末コメント、または直前のコメントで
  「なぜこのコードはルールに従わなくてよいのか」という制約・仕様を書く。
  「detekt を黙らせるため」のようなツール都合の説明は書かない。
- **理由がコードの設計判断そのものなら通常コメント + @Suppress を分けて書く**。

例 (行末コメント型):

```kotlin
@Suppress("LongParameterList") // 引数は Thickness 構造体のフィールドと 1:1 対応
fun putThickness(target: ComPtr, slot: Int, left: Double, top: Double, right: Double, bottom: Double) {
```

例 (直前コメント型 — 理由が長い場合):

```kotlin
// ネイティブ側の巨大なリソースを持つ COM 参照は JVM ヒープ上では小さく見え GC が
// 追いつかないため、生存数がしきい値を超えたら明示的に GC を要請するのが役割そのもの
@Suppress("ExplicitGarbageCollectionCall")
fun onAdopted() {
```

- **EmptyFunctionBlock は @Suppress ではなく空ブロック内のコメントで解消する**
  (コメントがあれば指摘されない):

```kotlin
override fun paintDebugOutline(showVisualPadding: Boolean) {
    // デバッグ描画は未対応 (WinUI 側に直接描画する手段を持たない)
}
```

## このプロジェクトで確立済みの @Suppress パターン

新しいコードが同じ性質を持つ場合は同じ理由で抑制してよい。

| ルール | 抑制してよい典型例 |
|---|---|
| LargeClass | winmd 由来の ABI 定数 (IID / vtable スロット) を集約した *Interop.kt |
| LongMethod | サンプル (gallery / notes) の宣言的な UI 構築関数、ABI 手順を 1 箇所で追わせたい関数 |
| CyclomaticComplexMethod / ComplexCondition | レイアウトアルゴリズムや UUID 生成など、分岐の多さが仕様に由来する関数 |
| NestedBlockDepth | COM 参照の try/finally 解放や ABI の場合分けで階層が深く見えるだけの関数 |
| LongParameterList | ネイティブ構造体のフィールドと 1:1 対応する引数を持つ関数 |
| PrintStackTrace | ロギング基盤を持たないため stderr 出力が最終手段の COM コールバック境界 |
| ExplicitGarbageCollectionCall | ネイティブメモリ圧迫時の明示的 GC 要請 (NativeMemoryGovernor) |

## detekt.yml で無効化・調整済みのルール (再有効化しない)

- `naming` ルールセット全体、`MaxLineLength`、`NewLineAtEndOfFile`、`WildcardImport`
  — ktlint と重複 (役割分担)
- `MagicNumber` — vtable スロット・IID などの数値定数が本体のコードベースのため
- `TooManyFunctions` — W* ラッパーは WinUI の API 数に比例して関数が増えるため
- `TooGenericExceptionCaught` — COM コールバック境界では Throwable 捕捉が必須のため
- `SpreadOperator` — FFI の可変長引数渡しで回避不能のため
- `ReturnCount` — ガード節による早期 return は Kotlin では慣用的なため
- `DestructuringDeclarationWithTooManyEntries` は 4 要素まで許容 — ARGB 分解のため
