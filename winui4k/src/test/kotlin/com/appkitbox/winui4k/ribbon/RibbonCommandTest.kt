package com.appkitbox.winui4k.ribbon

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

/** Tests for commands (RibbonRelayCommand / RibbonAsyncCommand), the catalog, and keyboard shortcuts. */
class RibbonCommandTest : FunSpec() {
    init {
        test("RibbonRelayCommand does not execute while canExecute is false") {
            var count = 0
            var enabled = false
            val command = RibbonRelayCommand({ count++ }, { enabled })
            command.execute(null)
            count shouldBe 0
            enabled = true
            command.execute(null)
            count shouldBe 1
        }

        test("RibbonRelayCommand passes the parameter to both the action and the canExecute check") {
            var received: Any? = null
            val command = RibbonRelayCommand({ received = it }, { it == "hello" })
            command.canExecute("other") shouldBe false
            command.execute("other")
            received shouldBe null
            command.execute("hello")
            received shouldBe "hello"
        }

        test("RibbonRelayCommand.of runs an action that ignores the parameter and notifies subscribers via notifyCanExecuteChanged") {
            var count = 0
            var enabled = true
            val command = RibbonRelayCommand.of({ count++ }, { enabled })
            var notified = 0
            val listener = Runnable { notified++ }
            command.addCanExecuteChangedListener(listener)
            command.execute("ignored")
            enabled = false
            command.notifyCanExecuteChanged()
            command.canExecute(null) shouldBe false
            command.removeCanExecuteChangedListener(listener)
            command.notifyCanExecuteChanged()
            count shouldBe 1
            notified shouldBe 1
        }

        test("RibbonAsyncCommand cannot execute while running and becomes executable again when done") {
            val pending = CompletableFuture<Unit>()
            val command = RibbonAsyncCommand({ pending })
            val changes = mutableListOf<Boolean>()
            command.addCanExecuteChangedListener { changes += command.isRunning }
            val run = command.executeAsync(null)
            command.isRunning shouldBe true
            command.canExecute(null) shouldBe false
            run.isDone shouldBe false
            pending.complete(Unit)
            run.isDone shouldBe true
            command.canExecute(null) shouldBe true
            changes shouldBe listOf(true, false)
        }

        test("RibbonAsyncCommand failures are reported to listeners and are not propagated once marked handled") {
            val failing = RibbonAsyncCommand({ throw IllegalStateException("boom") })
            var reported: Throwable? = null
            failing.addErrorListener { event ->
                reported = event.exception
                event.isHandled = true
            }
            failing.executeAsync(null).get() shouldBe null
            reported.shouldBeInstanceOf<IllegalStateException>()
            failing.isRunning shouldBe false
        }

        test("an unhandled RibbonAsyncCommand failure completes the returned Future exceptionally") {
            val failed = CompletableFuture<Unit>()
            failed.completeExceptionally(IllegalArgumentException("bad"))
            val command = RibbonAsyncCommand({ failed })
            val error = shouldThrow<ExecutionException> { command.executeAsync(null).get() }
            error.cause.shouldBeInstanceOf<IllegalArgumentException>()
            command.canExecute(null) shouldBe true
        }

        test("RibbonAsyncCommand runs completion notifications on the completionExecutor") {
            val queued = mutableListOf<Runnable>()
            val pending = CompletableFuture<Unit>()
            val command = RibbonAsyncCommand({ pending })
            command.completionExecutor = Executor { queued += it }
            val run = command.executeAsync(null)
            pending.complete(Unit)
            command.isRunning shouldBe true
            queued.size shouldBe 1
            queued[0].run()
            command.isRunning shouldBe false
            run.isDone shouldBe true
        }

        test("the catalog executes ids case-insensitively and notifies state changes") {
            val catalog = RibbonCommandCatalog()
            var executed = 0
            catalog.register("bold", "Bold", { executed++ }, shortcut = "Ctrl+B")
            val changes = mutableListOf<String>()
            catalog.addCommandStateListener { changes += it.propertyName }
            val executedIds = mutableListOf<String>()
            catalog.addCommandExecutedListener { executedIds += it.id }
            catalog.execute("BOLD") shouldBe true
            catalog.setEnabled("bold", false)
            catalog.execute("bold") shouldBe false
            catalog.setChecked("bold", true)
            executed shouldBe 1
            executedIds shouldBe listOf("bold")
            changes shouldBe listOf("isEnabled", "isChecked")
            catalog.find("Bold")?.shortcut shouldBe "Ctrl+B"
            catalog.execute("unknown") shouldBe false
            catalog.unregister("bold") shouldBe true
            catalog.unregister("bold") shouldBe false
            catalog.find("bold") shouldBe null
        }

        test("the catalog keeps registration order, and a re-registered command is replaced in its original position") {
            val catalog = RibbonCommandCatalog()
            for (id in listOf("z", "a", "m", "b")) catalog.register(id, id, { })
            catalog.register("a", "A again", { })
            catalog.commands.map { it.id } shouldBe listOf("z", "a", "m", "b")
            catalog.find("a")?.label shouldBe "A again"
        }

        test("changes to a replaced old registration are not notified") {
            val catalog = RibbonCommandCatalog()
            val old = catalog.register("a", "A", { })
            catalog.register("a", "A2", { })
            val changes = mutableListOf<String>()
            catalog.addCommandStateListener { changes += it.id }
            old.isEnabled = false
            changes shouldBe emptyList()
        }

        test("the catalog notifies command registration and removal") {
            val catalog = RibbonCommandCatalog()
            val ids = mutableListOf<String>()
            catalog.addCommandsChangedListener { ids += it.id }
            catalog.register("save", "Save", { })
            catalog.unregister("save")
            ids shouldBe listOf("save", "save")
        }

        test("a command without an implementation, or one that cannot execute itself, is not executed") {
            val catalog = RibbonCommandCatalog()
            catalog.register(RibbonCommandDescriptor("empty", "Empty"))
            catalog.canExecute("empty") shouldBe false
            catalog.register("guarded", "Guarded", RibbonRelayCommand({ }, { it == "ok" }))
            catalog.canExecute("guarded") shouldBe false
            catalog.execute("guarded", "ok") shouldBe true
            catalog.resolve("guarded").shouldBeInstanceOf<RibbonRelayCommand>()
            catalog.resolve(null) shouldBe null
        }

        listOf(
            Triple("Ctrl+Shift+L", RibbonModifierKeys.CONTROL or RibbonModifierKeys.SHIFT, "L"),
            Triple("F5", RibbonModifierKeys.NONE, "F5"),
            Triple("Alt+Down", RibbonModifierKeys.ALT, "Down"),
            Triple("Ctrl+]", RibbonModifierKeys.CONTROL, "OemCloseBracket"),
            Triple("Ctrl+Plus", RibbonModifierKeys.CONTROL, "Add"),
            Triple("Ctrl+1", RibbonModifierKeys.CONTROL, "Number1"),
            Triple("cmd+s", RibbonModifierKeys.META, "S"),
        ).forEach { (text, modifiers, key) ->
            test("shortcut \"$text\" parses to modifiers $modifiers and key $key") {
                RibbonKeyGesture.parse(text) shouldBe RibbonKeyGesture(modifiers, key)
            }
        }

        listOf(
            "Ctrl + +" to "Add",
            "Ctrl+ +" to "Add",
            "ctrl+pageup" to "PageUp",
            "Alt+f4" to "F4",
            "Ctrl+Backspace" to "Back",
            "F24" to "F24",
        ).forEach { (text, key) ->
            test("the key of edge-case shortcut \"$text\" is $key") {
                RibbonKeyGesture.tryParse(text)?.key shouldBe key
            }
        }

        listOf("Ctrl+", "Ctrl+A+B", "   ", "Ctrl+Shift", "A+B").forEach { text ->
            test("invalid shortcut \"$text\" yields null without throwing") {
                RibbonKeyGesture.tryParse(text) shouldBe null
            }
        }

        test("a lone '+' and \"Ctrl++Shift\" parse without throwing") {
            RibbonKeyGesture.tryParse("+")?.key shouldBe "Add"
            RibbonKeyGesture.tryParse("Ctrl++Shift")?.modifiers shouldBe RibbonModifierKeys.CONTROL
        }

        test("parse throws on an invalid shortcut") {
            shouldThrow<IllegalArgumentException> { RibbonKeyGesture.parse("Ctrl+") }
        }

        test("shortcut matching treats Meta as Ctrl and also matches numpad digits") {
            val gesture = RibbonKeyGesture.parse("Ctrl+B")
            gesture.matches("B", RibbonModifierKeys.CONTROL) shouldBe true
            gesture.matches("b", RibbonModifierKeys.CONTROL) shouldBe true
            gesture.matches("B", RibbonModifierKeys.META) shouldBe true
            gesture.matches("B", RibbonModifierKeys.CONTROL or RibbonModifierKeys.SHIFT) shouldBe false
            gesture.matches("", RibbonModifierKeys.CONTROL) shouldBe false
            RibbonKeyGesture.parse("Ctrl+1").matches("NumberPad1", RibbonModifierKeys.CONTROL) shouldBe true
        }

        test("with treatMetaAsControl set to false, Meta does not match Ctrl") {
            RibbonKeyGesture.treatMetaAsControl = false
            try {
                RibbonKeyGesture.parse("Ctrl+B").matches("B", RibbonModifierKeys.META) shouldBe false
            } finally {
                RibbonKeyGesture.treatMetaAsControl = true
            }
        }

        test("shortcut text can be produced in Windows format and in macOS symbol format") {
            RibbonKeyGesture.parse("ctrl+shift+]").toString() shouldBe "Ctrl+Shift+]"
            RibbonKeyGesture.parse("Ctrl+Shift+L").toDisplayString(true) shouldBe "⌘⇧L"
            RibbonKeyGesture.parse("Ctrl+1").toString() shouldBe "Ctrl+1"
            RibbonKeyGesture.parse("Esc").toString() shouldBe "Esc"
        }
    }
}
