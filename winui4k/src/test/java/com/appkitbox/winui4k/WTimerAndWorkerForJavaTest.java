package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WTimer / WinUiUtilities.invokeAndWait / WSwingWorker written the way developers who have used Swing's javax.swing.Timer /
 * SwingUtilities.invokeAndWait / SwingWorker write Java. Compiling at all (lambda listeners, setRepeats /
 * isRunning, the checked exceptions of invokeAndWait, throws Exception of doInBackground, publish's varargs, setProgress, getState) checks
 * the API's shape, and the bodies check the behavior.
 */
class WTimerAndWorkerForJavaTest {
    @Test
    void timerCanBeCreatedWithALambdaListenerAndIsCalledOnlyOnceWithSetRepeatsFalse() throws InterruptedException {
        CountDownLatch fired = new CountDownLatch(1);
        List<String> commands = Collections.synchronizedList(new ArrayList<String>());
        WTimer timer = new WTimer(20, e -> {
            commands.add(e.getActionCommand());
            fired.countDown();
        });
        timer.setRepeats(false);
        timer.setActionCommand("once");
        timer.setInitialDelay(10);
        timer.setDelay(30);
        timer.start();
        assertTrue(fired.await(5, TimeUnit.SECONDS));
        Thread.sleep(150);
        assertEquals(Collections.singletonList("once"), commands);
        assertFalse(timer.isRunning());
        assertFalse(timer.isRepeats());
        assertEquals(10, timer.getInitialDelay());
        assertEquals(30, timer.getDelay());
    }

    @Test
    void invokeAndWaitRunsOnTheUiThreadAndWaitsAndDeclaresCheckedExceptions() throws InterruptedException, InvocationTargetException {
        AtomicBoolean onUiThread = new AtomicBoolean(false);
        WinUiUtilities.invokeAndWait(() -> onUiThread.set(WinUiUtilities.isDispatchThread()));
        assertTrue(onUiThread.get());
    }

    @Test
    void canBeWrittenAsAnAnonymousClassOfTheSameShapeAsSwingWorkerAndPublishSetProgressAndGetWork() throws InterruptedException, ExecutionException {
        List<String> processed = Collections.synchronizedList(new ArrayList<String>());
        CountDownLatch done = new CountDownLatch(1);
        WSwingWorker<Integer, String> worker = new WSwingWorker<Integer, String>() {
            @Override
            protected Integer doInBackground() throws Exception {
                publish("one", "two");
                setProgress(100);
                return 42;
            }

            @Override
            protected void process(List<String> chunks) {
                processed.addAll(chunks);
            }

            @Override
            protected void done() {
                done.countDown();
            }
        };
        assertEquals(WSwingWorker.StateValue.PENDING, worker.getState());
        worker.execute();
        assertEquals(42, (int) worker.get());
        assertTrue(done.await(5, TimeUnit.SECONDS));
        UiTestHarness.onUiThread(() -> { });
        assertEquals(java.util.Arrays.asList("one", "two"), processed);
        assertEquals(100, worker.getProgress());
    }
}
