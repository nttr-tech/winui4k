# CsWinRT の COM 参照管理を Java で実現する

COM オブジェクトは参照カウントで寿命を管理し、Java や C# のオブジェクトはトレース型 GC で寿命を管理する。
この2つのモデルは前提が食い違っており、素朴につなぐとリーク、二重解放、別スレッドからの不正な解放を招く。

本書は、この橋渡しを CsWinRT(C#/.NET 向けの WinRT 相互運用ランタイム)がどう実装しているかを解説し、同じ設計を JVM 上で実現するための方針をまとめる。
対象読者は、COM と JVM のどちらかに実務経験のあるシニアエンジニアを想定する。
スコープは COM オブジェクトの参照管理(寿命管理)に限る。
投影コードの生成、データ型のマーシャリング、イベントといった周辺機構は扱わない。

構成は次の通り。
第1部で2つの寿命管理モデルとそのミスマッチを整理し、第2部で CsWinRT の実装(リポジトリ `src/WinRT.Runtime` 配下)を実コードの参照付きで読み解く。
第3部で各機構を Java 22 以降の API で実現する設計を示し、第4部で Java 8 まで遡るバージョン対応戦略を述べる。

---

## 第1部 概念編

### COM の寿命管理契約

COM のすべてのオブジェクトは `IUnknown` インターフェースを実装し、**参照カウント**(オブジェクト自身が保持する「自分を参照している者の数」)で寿命を管理する。
利用者は参照を保持するときに `AddRef` を呼んでカウントを増やし、手放すときに `Release` を呼んで減らす。
カウントが 0 になった瞬間、オブジェクトは自分自身を解放する。

この仕組みが成立するのは、すべての利用者が所有権の規約を守るからである。
規約の要点は次の3つに集約できる。

- **戻り値の所有権**：インターフェースポインタを返す関数は、カウントを 1 増やした状態で返す。受け取った側がそのポインタの所有者となり、`Release` の義務を負う。
- **引数の借用**：引数として渡されたポインタは、呼び出しの間だけ借用できる。呼び出しを超えて保持するなら、自分で `AddRef` して所有権を確保する。
- **決定的解放**：最後の `Release` の呼び出しと同時に、デストラクタが同期的に実行される。解放のタイミングをコードで制御できる。

### トレース型 GC の寿命管理

一方、.NET と JVM が採用する**トレース型 GC** は、参照の数を数えない。
GC はルート(スタック、静的フィールドなど)から参照をたどり、到達できたオブジェクトを生存、できなかったオブジェクトを回収対象とする。

この方式の性質のうち、COM との接続で問題になるのは次の3点である。

- 回収のタイミングは非決定的で、到達不能になった瞬間ではなく、次に GC が走ったときにまとめて処理される。
- ファイナライズ処理(.NET のファイナライザ、Java の Cleaner)は、アプリケーションのスレッドではなく専用スレッドで実行される。
- GC が認識するのはマネージヒープだけで、オブジェクトの裏にあるネイティブリソースの量は見えない。

### 2つのモデルのミスマッチ

両者を橋渡ししようとすると、次の4つのミスマッチに直面する。
以後、本書はこの番号で各問題を参照する。

- **ミスマッチ①(解放通知の不在)**：COM は「不要になったら `Release` を呼べ」と要求するが、トレース型 GC には「このオブジェクトへの最後の参照が消えた」という瞬間を検出する仕組みがない。参照カウントを持たないからである。
- **ミスマッチ②(ネイティブメモリの不可視)**：数十バイトのマネージラッパーの裏に、数メガバイトのネイティブオブジェクト(ビットマップなど)がぶら下がっていても、GC はヒープ使用量の変化を観測できず、回収を急ぐ動機を持たない。
- **ミスマッチ③(境界をまたぐ循環)**：ネイティブオブジェクトがマネージオブジェクトを参照し、そのマネージオブジェクトが元のネイティブオブジェクトを参照する循環は、どちらのモデルも単独では回収できない。参照カウントは循環を数え続け、GC はネイティブ側の参照グラフをたどれない。
- **ミスマッチ④(解放スレッドの制約)**：COM にはアパートメント[^apartment]という規約があり、STA に属するオブジェクトのメソッド(`Release` を含む)は、そのアパートメントのスレッドから呼ばなければならない。しかし GC のファイナライズは専用スレッドで走るため、素朴に実装するとこの規約に違反する。

[^apartment]: COM のスレッドモデル。STA(single-threaded apartment)のオブジェクトは特定のスレッドに束縛され、MTA(multi-threaded apartment)のオブジェクトは任意のスレッドから呼べる。スレッドを問わない性質は agile と呼ばれる。

### 橋渡しの一般戦略

ミスマッチ①に対する古典的な解は、「参照カウント1つ分の所有権を、マネージラッパー1個の寿命に対応付ける」ことである。
ラッパーの生成時に参照を1つ確保し、GC がラッパーの到達不能を検出したら(ファイナライズ処理の中で)`Release` を呼ぶ。
つまり、GC の到達可能性判定を、参照カウントを減らすトリガーとして流用する。

この戦略の上に、双方向のプロキシ構造が組み立てられる。

- **RCW**(runtime callable wrapper)：ネイティブの COM オブジェクトをマネージコードから使うためのラッパー。上記の戦略で寿命を管理する。
- **CCW**(COM callable wrapper)：マネージオブジェクトをネイティブコードに COM オブジェクトとして見せるためのラッパー。自前の参照カウントを実装し、カウントが 0 でない間は対象のマネージオブジェクトを GC から保護する。

残るミスマッチ②③④は、この基本構造だけでは解決しない。
CsWinRT がそれぞれをどう解いているかが、第2部の主題である。

---

## 第2部 C# 実装編(CsWinRT の読解)

### 参照1つを1オブジェクトで表す IObjectReference

CsWinRT の参照管理の最小単位は `IObjectReference` クラス(`ObjectReference.cs:24`)である。
1インスタンスが「COM インターフェースポインタ1本と、その参照カウント1つ分の所有権」を表す。
同じ COM オブジェクトに対して複数の `IObjectReference` が存在してよく、それぞれが独立に1カウントを所有する。

取得のファクトリは所有権の扱いで2系統に分かれており、第1部で述べた COM の所有権規約に対応している。

- **`Attach`**(`ObjectReference.cs:566`)：カウント済みのポインタの所有権を移譲して包む。`AddRef` は呼ばない。引数のポインタ変数を `IntPtr.Zero` にクリアし、呼び出し元がうっかり二重解放できないようにする。
- **`FromAbi`**(`ObjectReference.cs:620`)：ABI 越しに借用したポインタを包む。呼び出しを超えて保持することになるので、`Marshal.AddRef` してから包む(`ObjectReference.cs:627`)。

### 解放の2経路

解放はすべて `Dispose()`(`ObjectReference.cs:341`)に集約され、そこへ至る経路が2つある。

1つは明示的な `Dispose()`(C# の `using` 文を含む)で、COM 本来の決定的解放に相当する。

もう1つはファイナライザ(`ObjectReference.cs:148`)である。
GC がラッパーの到達不能を検出すると、ファイナライザスレッドが `Dispose()` を呼び、その中の `Release()`(`ObjectReference.cs:410`)がネイティブの参照カウントを減らす。
これがミスマッチ①(解放通知の不在)への回答であり、第1部の一般戦略をそのまま実装したものである。

2つの経路は同時に走りうるため、`Dispose()` は3状態のフラグを CAS(`Interlocked.CompareExchange`、`ObjectReference.cs:373`)で遷移させて、`Release` が一度しか実行されないことを保証する。

- **`NOT_DISPOSED`**：初期状態。
- **`DISPOSE_PENDING`**：あるスレッドが解放処理を実行中。解放処理自身がポインタへアクセスする必要があるため、この状態ではまだ利用可能として扱う。
- **`DISPOSE_COMPLETED`**：解放完了。以後のアクセスは `ObjectDisposedException` を投げる。

### GC へのネイティブメモリの申告

ミスマッチ②(ネイティブメモリの不可視)には、`GC.AddMemoryPressure` で対処している。
`IObjectReference` のコンストラクタで一律 1000 バイト分の圧力を申告し(`ObjectReference.cs:145`、定数は `ComWrappersSupport.cs:41`)、`Dispose` 時に同量を取り下げる。
実際のネイティブオブジェクトの大きさを追跡するのではなく、1ラッパーあたり固定値という簡略化したモデルで、GC の発火頻度を底上げする設計である。

### RCW のアイデンティティキャッシュ

RCW の生成は `ComWrappersSupport.CreateRcwForComObject`(`ComWrappersSupport.net5.cs:88`)が入口で、.NET 5 以降のランタイム API である `ComWrappers.GetOrCreateObjectForComInstance`(`ComWrappersSupport.net5.cs:107`)に委譲する。

この API は、`IUnknown` ポインタの同一性をキーとするキャッシュをランタイム内部に持つ。
同じ COM オブジェクトに対しては常に同じ C# オブジェクトが返るため、参照の等価比較が期待通りに動く。
キャッシュの保持は弱参照であり、C# 側の参照がなくなれば RCW は通常どおり GC 対象になる。
回収されればファイナライザ経由で `Release` が走り、ネイティブ側のカウントが減る。

### CCW とマネージオブジェクトの保護

逆方向は `ComWrappersSupport.CreateCCWForObject`(`ComWrappersSupport.net5.cs:167`)が `ComWrappers.GetOrCreateComInterfaceForObject` を呼び、マネージオブジェクトに COM の vtable を持つ CCW を生成する。

CCW は自前の参照カウントを実装しており、カウントが 0 でない間、ランタイムが内部の GCHandle で対象のマネージオブジェクトを強参照する。
ネイティブコードが `AddRef` を保持している限りマネージオブジェクトは回収されず、ネイティブ側が `Release` してカウントが 0 になると保護が外れて通常の GC 対象に戻る。

ここまでで、一方向の参照はどちらの向きも安全になった。
問題は両方向が同時に成立する場合、すなわちミスマッチ③である。

### 言語境界をまたぐ循環と IReferenceTracker

WinUI(XAML)アプリでは、次の循環が日常的に発生する。

```
C# のページオブジェクト ──(RCW 経由)──> ネイティブの Button
        ↑                                     │
        └──(CCW 経由のイベントハンドラ)─────────┘
```

ネイティブの Button はハンドラの CCW にカウントを保持しているのでページは GC されず、ページは Button の RCW を保持しているのでネイティブ側のカウントも減らない。
どちらのモデルも単独ではこの循環を回収できない。

CsWinRT はこれを、XAML ランタイムが提供する `IReferenceTracker` プロトコルと .NET GC の協調で解決する。
関与する仕掛けは3つある。

第一に、CsWinRT は自分の `ComWrappers` 実装を `RegisterForTrackerSupport`(`ComWrappersSupport.net5.cs:57`)でランタイムに登録し、RCW 生成時に `TrackerObject` フラグ(`ComWrappersSupport.net5.cs:106`)、CCW 生成時に `TrackerSupport` フラグを常に指定する。
これで .NET GC 自体がトラッカープロトコルの参加者になる。

第二に、RCW の初期化時にオブジェクトへ `IReferenceTracker` を問い合わせ、実装していればそのポインタを `IObjectReference.ReferenceTrackerPtr`(`ObjectReference.cs:83`)に保存する(`ComWrappersHelper.Init`、`ComWrappersSupport.net5.cs:409`)。

第三に、以後その参照を増減するたびに、通常の `AddRef`/`Release` に加えて `AddRefFromTrackerSource`/`ReleaseFromTrackerSource`(`ObjectReference.cs:430-444`)を呼び、「この参照はマネージ側が保持している」とネイティブのトラッカーランタイムへ申告する。

この申告があると、GC のマークフェーズ中に .NET ランタイムと XAML の参照トラッカーが相互にオブジェクトグラフを照会できる。
「マネージ側からしか参照されていないネイティブオブジェクト」と「ネイティブ側からしか参照されていないマネージオブジェクト」だけで構成された循環は全体が到達不能と判定され、まとめて回収される。

留意すべきは、この解決が .NET ランタイム自体の拡張点(GC とトラッカーの統合)に依存している点である。
ライブラリのコードだけでは実現できておらず、この事実が第3部の設計を大きく制約する。

### アパートメントと解放スレッド

ミスマッチ④(解放スレッドの制約)には、ラッパーの型を分けることで対処している。

ラッパー生成時に、対象が agile(スレッドを問わない)かどうかを `IAgileObject` への問い合わせで判定する(`ObjectReference.cs:528-543` の `Attach` の分岐)。
agile であれば通常の `ObjectReference<T>` を作り、ファイナライザスレッドから直接 `Release` してよい。

agile でなければ `ObjectReferenceWithContext<T>`(`ObjectReference.cs:739`)を作り、生成時点の COM コンテキスト(コンテキストコールバックとトークン)を記録する。
このクラスは `Release` をオーバーライドし(`ObjectReference.cs:940`)、`Context.CallInContext` で元のアパートメントへマーシャルしてから `Release` を実行する。
アパートメントが既に破棄されていた場合は、マーシャルせずに解放するフォールバックへ落ちる。

さらに、別コンテキストからポインタが使われた場合のために、`AgileReference`(`AgileReference.cs:56`)がコンテキスト間のプロキシ解決を担う。
実装は `RoGetAgileReference`(`AgileReference.cs:77`)で、この API が使えない環境では GIT(global interface table)へフォールバックする(`AgileReference.cs:86`)。
解決したプロキシはコンテキストトークンをキーに辞書へキャッシュされる(`ObjectReference.cs:744` の `CachedContext`)。

### 集約における自己循環の回避

C# で WinRT の composable クラス(継承可能クラス)を継承すると、C# 側のオブジェクト(CCW)とネイティブの inner オブジェクトが1つの COM アイデンティティを構成する。
このとき inner へ普通に `AddRef` すると自分自身への参照となり、循環して永遠に解放されない。

`ComWrappersHelper.Init`(`ComWrappersSupport.net5.cs:483-501`)は、集約シナリオで `IObjectReference` に2つのフラグ(`ObjectReference.cs:77-79`)を立ててこれを避ける。

- **`IsAggregated`**：`QueryInterface` の結果ポインタは即座に `Release` するが、ポインタ自体は使い続ける。
- **`PreventReleaseOnDispose`**：inner の解放は RCW の破棄に任せ、`Dispose` では `Release` しない。

### 一時所有権のための ObjectReferenceValue

ABI 呼び出しの引数受け渡しのように、参照が呼び出しの間だけ生きればよい場面もある。
そのたびにクラスの `IObjectReference` を生成するのは無駄なので、アロケーション不要の構造体 `ObjectReferenceValue`(`ObjectReference.cs:1042`)が用意されている。
`Dispose` で `Release` する点、トラッカーソースへの申告を守る点は同じで、所有権をネイティブへ渡す `Detach` も備える。

### ミスマッチと解決機構の対応

第2部の内容を、第1部のミスマッチに対応付けて整理する。

| ミスマッチ | CsWinRT の解決機構 | 実装 |
|---|---|---|
| ①解放通知の不在 | ファイナライザと明示 `Dispose` の2経路で `Release` | `IObjectReference`(ObjectReference.cs:148, 341) |
| ②ネイティブメモリの不可視 | ラッパーあたり固定値のメモリ圧力申告 | `GC.AddMemoryPressure`(ObjectReference.cs:145) |
| ③境界をまたぐ循環 | GC と参照トラッカーのグラフ相互照会 | `IReferenceTracker` 統合(ComWrappersSupport.net5.cs:57, 409) |
| ④解放スレッドの制約 | 元のコンテキストへマーシャルして `Release` | `ObjectReferenceWithContext`(ObjectReference.cs:739, 940) |

このうち①②④はライブラリのコードで完結しているが、③だけは .NET ランタイムの GC 統合に依存している。
この非対称性が、次の第3部の出発点になる。

---

## 第3部 Java 実現編(Java 22 以降を本線とする設計)

### 実現可能性の3分類

CsWinRT の各機構は、JVM への移植可能性で3つに分類できる。

- **そのまま移植できる**：`IObjectReference` の構造、RCW のアイデンティティキャッシュ、アパートメント対応、集約フラグ。COM 側の仕組みか、ライブラリ内で完結するロジックだからである。
- **代替設計が必要**：解放トリガー(ファイナライザに相当する機構)、CCW によるオブジェクト保護、メモリ圧力の申告。JVM の API が .NET と異なるか、存在しないからである。
- **原理的に不可能**：ミスマッチ③の自動解決。JVM の GC には `RegisterForTrackerSupport` に相当する拡張点がなく、マークフェーズに外部の参照グラフを注入できないからである。

以下、この分類に沿って設計する。
ネイティブ呼び出しには Java 22 で正式化された FFM API(java.lang.foreign)[^ffm]を使う。

[^ffm]: Foreign Function & Memory API。JEP 454 として Java 22 で正式機能になった。ネイティブ関数呼び出し(downcall)、ネイティブからの呼び返し(upcall)、ネイティブメモリ操作を JNI なしで行える。

### ObjectReference クラスの設計

`IObjectReference` の対応物は、次の骨格を持つ。

```java
public final class ObjectReference implements AutoCloseable {
    private final long thisPtr;              // IUnknown*
    private final State state;               // Cleaner と共有する解放情報
    private final Cleaner.Cleanable cleanable;

    // State は static クラスで、生ポインタ、contextToken、
    // referenceTracker ポインタ、AtomicInteger の disposedFlags だけを持つ
}
```

解放トリガーには、ファイナライザではなく `java.lang.ref.Cleaner` を使う。
Cleaner は PhantomReference ベースで、ファイナライザと違ってオブジェクトの復活がなく、登録した処理は専用スレッドで確実に一度だけ走る。

Cleaner には設計上の鉄則が1つある。
cleanup アクションに `ObjectReference` 自身を捕捉させてはならない。
捕捉すると Cleaner がオブジェクトを強参照し続け、永遠に到達不能にならない。
このため解放に必要な情報(生ポインタ、コンテキスト情報、トラッカーのポインタ)は、`ObjectReference` から独立した `State` オブジェクトに分離し、それだけを cleanup アクションへ渡す。

明示的な `close()`(try-with-resources)と Cleaner の競合には、CsWinRT の3状態フラグをそのまま移植して対処する。
`State` 内の `AtomicInteger` を CAS で `NOT_DISPOSED` から `DISPOSE_PENDING` へ遷移させたスレッドだけが `Release` を実行する。

ファクトリも CsWinRT の2系統を踏襲し、所有権移譲の `attach(long ptr)` と、借用ポインタに `AddRef` してから包む `fromAbi(long ptr)` を提供する。

### premature finalization への対策

JVM 固有の落とし穴として、**premature finalization**(オブジェクトのメソッド実行中の早期回収)がある。
JIT コンパイラは「フィールドの最後の読み出し以降、this は不要」と判断でき、メソッドがまだ実行中でもオブジェクトを到達不能とみなせる。
`thisPtr` を読み出してネイティブ呼び出しを実行している最中に Cleaner が走ると、使用中のポインタに `Release` がかかる。

C# にも同じ問題があり、CsWinRT は `GC.KeepAlive` 相当の配慮を利用者に求めている(`ObjectReference.cs:315-324` の注意書き)。
Java での対応物は `Reference.reachabilityFence(this)` で、ネイティブ呼び出しを含むすべてのメソッドの `finally` 節に置く。

```java
public int invokeSlot3(long arg) {
    try {
        long ptr = state.thisPtr();
        return NativeBridge.current().invoke(ptr, 3, arg);
    } finally {
        Reference.reachabilityFence(this);
    }
}
```

### AddRef、Release、QueryInterface の呼び出し

`IUnknown` の3メソッドは vtable の先頭3スロットにあるので、FFM の `Linker.downcallHandle` でスロットの関数ポインタを直接呼ぶ。
`FunctionDescriptor` はシグネチャごとに static final でキャッシュする。

ポインタは公開 API では `MemorySegment` ではなく `long` で表現する。
FFM を使わない実装系統(第4部)と型を共有するためであり、`MemorySegment` は FFM 実装の内部でのみ使う。

### RCW のアイデンティティキャッシュ

.NET では `ComWrappers` がランタイム内部で提供していたキャッシュを、ライブラリとして自前実装する。

```java
ConcurrentHashMap<Long /* IUnknown アドレス */, WeakReference<Object>>
```

取得手順は、①受け取ったポインタを `IUnknown` へ `QueryInterface` して正準のアイデンティティを得る、②キャッシュを照会する、③ミスなら RCW を生成して登録する、の3段階である。
値が弱参照なので、Java 側の参照が消えれば RCW は GC され、Cleaner 経由で `Release` が走る。
死んだエントリは、RCW の cleanup アクションの中でキャッシュからも除去する。

### CCW の参照カウントと GC 保護

CCW の vtable は FFM の upcall stub で構築する。
vtable 自体は型ごとに1回だけ生成すればよいので、`ClassValue` にキャッシュする(.NET の `ConditionalWeakTable<Type, ...>` に相当する Java の慣用である)。

.NET ランタイムが GCHandle でやっていた「カウントが 0 でない間はマネージオブジェクトを保護する」不変条件は、次の構造で再現する。

- CCW レコードは `{ vtable 群, AtomicLong refCount, 対象オブジェクトへの参照 }` を持つ。
- `AddRef` upcall でカウントが 0 から 1 になったら、レコードをグローバルな強参照テーブルへ登録する。
- `Release` upcall でカウントが 1 から 0 になったら、強参照テーブルから外し、弱参照だけを残す。

0 と 1 の間を往復する遷移は複数スレッドから同時に起きうるため、CAS に世代カウンタを組み合わせて、古い遷移が新しい遷移の結果を上書きしないようにする。

### アパートメント対応

`ObjectReferenceWithContext` と `AgileReference` の設計は、COM 側の仕組みなのでほぼ直訳できる。
`IContextCallback` の取得、コンテキストへのマーシャル、`RoGetAgileReference` と GIT フォールバックは、いずれも downcall で呼べる Win32/COM API である。

JVM 固有の設計点は Cleaner スレッドの初期化にある。
`Cleaner.create(threadFactory)` にカスタムファクトリを渡し、スレッド起動時に `CoInitializeEx(COINIT_MULTITHREADED)` を呼んで MTA に参加させる。
これで agile なオブジェクトは Cleaner スレッドから直接 `Release` でき、agile でないオブジェクトだけが `IContextCallback` 経由のマーシャルを使う。
コンテキストが消滅していた場合に素の `Release` へフォールバックする点も CsWinRT と同じである。

### GC.AddMemoryPressure の代替

ミスマッチ②への CsWinRT の回答である `GC.AddMemoryPressure` に、JVM の対応物はない[^pressure]。
単一の代替 API が存在しないため、3つの手段を重ねて緩和する。

[^pressure]: `-XX:MaxDirectMemorySize` は DirectByteBuffer 専用の上限であり、任意のネイティブ割り当てを GC へ申告する汎用 API は JDK に存在しない。

第一に、決定的解放を API 設計で前面に出す。
`ComScope` というスコープオブジェクトを提供し、try-with-resources のスコープ内で生成された `ObjectReference` をスコープ終了時に一括 `close` する。
スコープ外へ持ち出す参照は `scope.escape(obj)` で明示させ、そこで `AddRef` して所有権を昇格する。
参照カウントを持たない Java では暗黙のエスケープを検出できないので、「持ち出しは明示する」という規約を API の形にしたものである。

第二に、オプトインの `NativeMemoryGovernor` を用意する。
生成した `ObjectReference` の数を計数し、閾値を超えたら `System.gc()` を要請する(`-XX:+ExplicitGCInvokesConcurrent` の併用を前提とする)。

第三に、ビットマップやストリームのような大きなリソースは必ず try-with-resources で扱う、という利用規約をドキュメントで課す。

どの手段も `AddMemoryPressure` の完全な代替ではなく、決定的解放への依存度が CsWinRT より高くなることは、この設計の受け入れるべき制約である。

### 循環参照の扱い

ミスマッチ③は、第3部冒頭の分類で「原理的に不可能」とした項目である。
CLR の `RegisterForTrackerSupport` に相当する拡張点が JVM にない以上、GC のマークフェーズと `IReferenceTracker` の相互照会は実装できない。

そこで、次の方針を採る。

- `IReferenceTrackerHost` は実装し、XAML ランタイムからの要請(GC の実行要請など)には応答して、プロトコルの参加者としては振る舞う。
- ただし到達性の照会には「マネージ側の参照はすべて生存」と保守的に答える。結果として、言語境界をまたぐ循環は自動回収されない。この制限は仕様として明記する。
- 補完策として、①ウィンドウやページの破棄時に配下のラッパーを一括 `close` するライフサイクル API、②UI のイベントハンドラに弱参照を使う規約、③RCW と CCW の生存対をダンプする開発時リーク診断、の3つを提供する。

JVMTI エージェントの `FollowReferences` でヒープを走査すれば、理論上は独自のサイクルコレクタを作れる。
しかし stop-the-world 前提の全ヒープ走査を GC のたびに行うコストは実用に耐えないため、実験的な付録の位置付けにとどめる。

### 集約と終了処理

集約の自己循環回避は、`isAggregated` と `preventReleaseOnDispose` の2フラグをそのまま移植する。
ロジックは第2部で述べた CsWinRT のものと同一である。

プロセス終了時は、シャットダウンフックで Cleaner のスレッドを停止し、未解放のラッパーの `Release` を意図的に行わない。
`CoUninitialize` 後の `Release` はクラッシュを招くためで、CsWinRT も同じ割り切りをしている。

---

## 第4部 Java バージョン対応戦略

### NativeBridge 抽象と実装の切り替え

第3部の設計は Java 22 以降の FFM API を前提とした。
これを Java 8 まで動かすため、ネイティブ境界を1つのインターフェースに抽象化する。

```java
public interface NativeBridge {
    long queryInterface(long thisPtr, byte[] iid);
    int  addRef(long thisPtr);
    int  release(long thisPtr);
    long invoke(long thisPtr, int slot, InvokeDescriptor desc, Object[] args);
    long createCcw(Object target, CcwVtableSpec spec);
    // HSTRING 操作、コンテキスト API など
}
```

実装は2系統を用意する。

- **FfmBridge**：Java 22 以降。第3部の設計そのもの。
- **JniBridge**：Java 8 以降。JNI のシム DLL(`jwinrt_shim.dll`)経由でネイティブを呼ぶ。

ポインタを `long` に統一したのはこの抽象化のためである。
`MemorySegment` を公開 API に出すと、FFM のない環境でクラス解決に失敗する。

配布は Multi-Release JAR で行う。
base のクラスを Java 8 でコンパイルし、`META-INF/versions/22` に FfmBridge を置く。
Java 8 の JVM は versions ディレクトリを無視するので、単一の JAR で全バージョンに対応できる。
実装の選択は起動時に1回、`java.version` システムプロパティの解析で行い(`Runtime.version()` は Java 9 以降のため使わない)、`-Djwinrt.bridge=jni` による明示指定も受け付ける。

### Java 8 フォールバックの対応表

第3部で使った Java 9 以降の API には、それぞれ Java 8 で使える代替がある。
完全性よりも「動くこと、制約が明確なこと」を優先した割り切りの表である。

| Java 22+ の機構 | Java 8 での代替 |
|---|---|
| FFM downcall | JNI シム DLL(AddRef/Release/QI の特化エントリと汎用 invoke) |
| FFM upcall(CCW の vtable) | シム DLL 内に事前生成した静的サンク表。各スロットが JNI の GlobalRef ハンドルを引いて Java 側のディスパッチャを呼ぶ |
| `Cleaner`(Java 9+) | `PhantomReference` と `ReferenceQueue` と自前デーモンスレッドで Cleaner と同じ構造を自作(スレッドは MTA で初期化) |
| `Reference.reachabilityFence`(Java 9+) | `synchronized (obj) {}` の既知イディオム、または空の native メソッド `keepAlive(Object)` |
| `VarHandle` の CAS | `AtomicIntegerFieldUpdater` |
| `MemorySegment` と `Arena` | `long` アドレスとシム DLL 側の alloc/free |
| `ClassValue` | Java 7 から存在するためそのまま使える |

fence の代替について補足する。
`synchronized (obj) {}` は「モニタ操作の対象は生存していなければならない」という JMM の性質を利用したイディオムで、追加のネイティブ呼び出しなしに使える。
確実性を最優先するなら、JIT が中身を観測できない空の native メソッドに `this` を渡す方法がより堅い。
どちらを既定にするかは、性能測定の上で決める。

### Java 8 での機能制限

JniBridge には、設計段階で明確にしておくべき制限が3つある。

- CCW のサンクが静的表方式でスロット数に上限を設けるため、任意のインターフェース実装をネイティブへ見せる authoring と集約は非対応とする。Java 8 では COM オブジェクトの消費(consume)に限定する。
- `ComScope` は try-with-resources(Java 7 以降)で同じ API を提供できるが、confined 検査(生成スレッド以外からのアクセス検出)は省略する。
- 呼び出しごとに JNI 境界のコストが上乗せされる。ホットパスの最適化は行わず、性能が必要な利用者には Java 22 以降を案内する。

### テスト方針

FfmBridge と JniBridge は同じ `NativeBridge` 契約を実装するので、契約テストを1セット書いて両実装で共有する。
検証項目は、参照カウントの収支(テスト側から `AddRef`/`Release` してカウントをアサートする)、`System.gc()` と Cleaner の強制実行による解放経路、STA スレッドで生成したオブジェクトの解放がコンテキストマーシャルを経由すること、CCW を C++ 側テストハーネスから操作したときの GC 越しの生存と解放、である。
実行環境は Temurin 8 と 22 以降の2系列を最低ラインとする。

---

## まとめ

COM の参照カウントとトレース型 GC のミスマッチは、①解放通知の不在、②ネイティブメモリの不可視、③境界をまたぐ循環、④解放スレッドの制約、の4つに整理できる。
CsWinRT は①をラッパーのファイナライザで、②をメモリ圧力の申告で、④をコンテキストマーシャルで、いずれもライブラリの設計として解いている。
③だけは .NET ランタイムの GC 統合という言語処理系レベルの仕掛けに依存している。

Java での実現も、この構図をなぞる。
①は Cleaner、④はコンテキストマーシャルで同等に解け、②は決定的解放への規約強化で近似できる。
③は JVM に拡張点がないため自動解決を諦め、ライフサイクル API と規約で補う。
バージョン対応は、ネイティブ境界の抽象化と Multi-Release JAR により、Java 22 以降の FFM 実装と Java 8 の JNI 実装を単一の配布物に共存させる。