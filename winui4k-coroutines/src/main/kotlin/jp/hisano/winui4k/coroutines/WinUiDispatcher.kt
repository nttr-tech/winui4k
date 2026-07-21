package jp.hisano.winui4k.coroutines

import jp.hisano.winui4k.winui.WinUiToolkit
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.internal.MainDispatcherFactory
import kotlin.coroutines.CoroutineContext

/**
 * A dispatcher that dispatches to WinUI's UI thread and provides native
 * [delay][kotlinx.coroutines.delay] (via DispatcherQueueTimer) — the WinUI analogue of
 * kotlinx-coroutines-swing's Dispatchers.Swing.
 */
@Suppress("unused")
public val Dispatchers.WinUi: WinUiDispatcher
    get() = jp.hisano.winui4k.coroutines.WinUi

/**
 * The dispatcher for WinUI's UI thread (DispatcherQueue).
 *
 * A class for type safety and future extension (the same shape as SwingDispatcher).
 */
@OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
public sealed class WinUiDispatcher : MainCoroutineDispatcher(), Delay {
    /** @suppress */
    override fun dispatch(context: CoroutineContext, block: Runnable): Unit = WinUiToolkit.invokeLater(block::run)

    /** @suppress */
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val timer = WinUiToolkit.schedule(timeMillis) {
            with(continuation) { resumeUndispatched(Unit) }
        }
        continuation.invokeOnCancellation { timer.close() }
    }

    /** @suppress */
    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val timer = WinUiToolkit.schedule(timeMillis) {
            block.run()
        }
        return DisposableHandle { timer.close() }
    }
}

/** A factory that resolves [Dispatchers.Main] to [WinUi] via ServiceLoader. */
@OptIn(InternalCoroutinesApi::class)
internal class WinUiDispatcherFactory : MainDispatcherFactory {
    override val loadPriority: Int
        get() = 0

    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher = WinUi
}

private object ImmediateWinUiDispatcher : WinUiDispatcher() {
    override val immediate: MainCoroutineDispatcher
        get() = this

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = !WinUiToolkit.isDispatchThread

    override fun toString() = "Dispatchers.WinUi.immediate"
}

/**
 * The dispatcher instance that dispatches to WinUI's UI thread and provides native delay.
 */
internal object WinUi : WinUiDispatcher() {
    override val immediate: MainCoroutineDispatcher
        get() = ImmediateWinUiDispatcher

    override fun toString() = "Dispatchers.WinUi"
}
