package com.appkitbox.winui4k.ribbon

import java.util.EventObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executor
import java.util.function.BooleanSupplier
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

/**
 * A lightweight [RibbonCommand] whose action is passed as a lambda (RibbonSpace's RibbonRelayCommand).
 *
 * In Kotlin, write `RibbonRelayCommand { p -> ... }` / `RibbonRelayCommand({ save() }, { canSave })` (if the action
 * does not use the parameter, just ignore the implicit `it`). In Java, create an action without a parameter with [of].
 * [execute] does nothing when [canExecute] is false. Call [notifyCanExecuteChanged] when executability changes.
 */
open class RibbonRelayCommand @JvmOverloads constructor(
    private val action: Consumer<Any?>,
    private val condition: Predicate<Any?>? = null,
) : RibbonCommand {
    private val canExecuteChangedListeners = mutableListOf<Runnable>()

    override fun canExecute(parameter: Any?): Boolean = condition?.test(parameter) ?: true

    override fun execute(parameter: Any?) {
        if (canExecute(parameter)) action.accept(parameter)
    }

    override fun addCanExecuteChangedListener(listener: Runnable) {
        canExecuteChangedListeners += listener
    }

    override fun removeCanExecuteChangedListener(listener: Runnable) {
        canExecuteChangedListeners -= listener
    }

    /** Notifies that executability changed (views update the enabled state of items). */
    fun notifyCanExecuteChanged() {
        for (listener in canExecuteChangedListeners.toList()) listener.run()
    }

    companion object {
        /** A command whose action does not use the parameter (for Java). */
        @JvmStatic
        @JvmOverloads
        fun of(action: Runnable, condition: BooleanSupplier? = null): RibbonRelayCommand =
            RibbonRelayCommand({ action.run() }, condition?.let { supplier -> Predicate { supplier.asBoolean } })
    }
}

/**
 * Indicates that the action of a [RibbonAsyncCommand] failed. Setting [isHandled] to true stops the exception from
 * propagating.
 */
class RibbonCommandErrorEvent(
    source: RibbonAsyncCommand,
    /** The exception thrown by the action. */
    val exception: Throwable,
) : EventObject(source) {
    /** Setting this to true swallows the exception (it does not propagate to the caller). */
    var isHandled: Boolean = false

    /** The command that failed. */
    override fun getSource(): RibbonAsyncCommand = super.getSource() as RibbonAsyncCommand
}

/** Receives failures of a [RibbonAsyncCommand]. */
fun interface RibbonCommandErrorListener {
    /** Called when the action fails. */
    fun commandFailed(event: RibbonCommandErrorEvent)
}

/**
 * An asynchronous command (RibbonSpace's RibbonAsyncCommand). The action returns a [CompletableFuture], and the
 * command makes itself non-executable until it completes ([isRunning]). Failures are notified to
 * [RibbonCommandErrorListener]s, and unless a listener sets [RibbonCommandErrorEvent.isHandled], the value returned by
 * [executeAsync] completes exceptionally.
 *
 * Completion notifications (changes of executability and failures) run on [completionExecutor]. If it is null, they run
 * directly on the thread where the action completed, so if a listener updates the UI, set an Executor that moves them to
 * the UI thread (such as `Executor { WinUiUtilities.invokeLater(it) }`).
 */
class RibbonAsyncCommand @JvmOverloads constructor(
    private val action: Function<Any?, out CompletableFuture<*>>,
    private val condition: Predicate<Any?>? = null,
) : RibbonCommand {
    private val canExecuteChangedListeners = mutableListOf<Runnable>()
    private val errorListeners = mutableListOf<RibbonCommandErrorListener>()

    /** Whether the action is running. */
    @Volatile
    var isRunning: Boolean = false
        private set

    /** The Executor that runs completion notifications (null means the thread where the action completed). */
    var completionExecutor: Executor? = null

    override fun canExecute(parameter: Any?): Boolean = !isRunning && (condition?.test(parameter) ?: true)

    override fun execute(parameter: Any?) {
        executeAsync(parameter)
    }

    /**
     * Runs the action and returns a Future representing completion (or an exception if the failure is not handled).
     * Completes immediately if it cannot run.
     */
    fun executeAsync(parameter: Any?): CompletableFuture<Void?> {
        val result = CompletableFuture<Void?>()
        if (!canExecute(parameter)) {
            result.complete(null)
            return result
        }
        isRunning = true
        notifyCanExecuteChanged()
        val future = try {
            action.apply(parameter)
        } catch (error: Throwable) {
            finish(error, result)
            return result
        }
        future.whenComplete { _, error ->
            val executor = completionExecutor
            if (executor == null) finish(error, result) else executor.execute { finish(error, result) }
        }
        return result
    }

    private fun finish(error: Throwable?, result: CompletableFuture<Void?>) {
        isRunning = false
        notifyCanExecuteChanged()
        if (error == null) {
            result.complete(null)
            return
        }
        val cause = if (error is CompletionException && error.cause != null) error.cause!! else error
        val event = RibbonCommandErrorEvent(this, cause)
        for (listener in errorListeners.toList()) listener.commandFailed(event)
        if (event.isHandled) result.complete(null) else result.completeExceptionally(cause)
    }

    override fun addCanExecuteChangedListener(listener: Runnable) {
        canExecuteChangedListeners += listener
    }

    override fun removeCanExecuteChangedListener(listener: Runnable) {
        canExecuteChangedListeners -= listener
    }

    /** Subscribes to failures of the action. */
    fun addErrorListener(listener: RibbonCommandErrorListener) {
        errorListeners += listener
    }

    /** Removes a listener registered with [addErrorListener]. */
    fun removeErrorListener(listener: RibbonCommandErrorListener) {
        errorListeners -= listener
    }

    /** Notifies that executability changed. */
    fun notifyCanExecuteChanged() {
        for (listener in canExecuteChangedListeners.toList()) listener.run()
    }
}
