package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WContentDialog from Java the way a developer used to JDialog would write it
 * ({@code new WContentDialog(owner, title, true)}, {@code setVisible(true)} not returning until closed,
 * an anonymous subclass of the WindowAdapter-style {@link WContentDialogAdapter}, button listener lambdas and canceling).
 * Keys are sent to the shared window with {@link TestKeyboard}.
 */
class WContentDialogForJavaTest {
    private static final long TIMEOUT_MILLIS = 5_000;
    private static final long OPEN_SETTLE_MILLIS = 600;

    private static void awaitEvents(List<String> events, List<String> expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!snapshot(events).equals(expected) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(expected, snapshot(events));
    }

    private static List<String> snapshot(List<String> events) {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    private static boolean isLoaded(WContentDialog dialog) {
        return OpenContentDialogs.isLoaded(dialog);
    }

    /** Waits until the dialog opens and can receive keys. */
    private static void awaitOpen(WContentDialog dialog) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!isLoaded(dialog) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertTrue(isLoaded(dialog));
        Thread.sleep(OPEN_SETTLE_MILLIS);
    }

    private static void awaitClosed(WContentDialog dialog) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (isLoaded(dialog) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertFalse(isLoaded(dialog));
    }

    @Test
    void canBeCreatedWithTheSameArgumentsAsJDialogGetTitleReadsBackFromTheNativeSideAndModelessThrowsIllegalArgumentException() {
        onUiThread(() -> {
            WButton owner = new WButton("Owner");
            WContentDialog dialog = new WContentDialog(owner, "Settings", true);
            assertSame(owner, dialog.getOwner());
            assertEquals("Settings", dialog.getTitle());
            assertTrue(dialog.isModal());
            assertFalse(dialog.isVisible());
            dialog.setTitle("Advanced Settings");
            assertEquals("Advanced Settings", dialog.getTitle());
            assertNull(new WContentDialog(null, "No owner").getOwner());
            assertEquals("Heading", new WContentDialog("Heading", new WLabel("Body")).getTitle());
            assertThrows(IllegalArgumentException.class, () -> new WContentDialog(owner, "Modeless", false));
            assertThrows(IllegalArgumentException.class, () -> dialog.setModal(false));
            dialog.setFullSizeDesired(true);
            assertTrue(dialog.isFullSizeDesired());
        });
    }

    @Test
    void setVisibleTrueDoesNotReturnUntilClosedAndWContentDialogAdapterAndButtonLambdasAreCalledInOrder() throws InterruptedException {
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        WButton owner = onUiThreadGet(() -> new WButton("Open"));
        UiTestHarness.attachAndAwaitLoaded(owner);
        try {
            WContentDialog dialog = onUiThreadGet(() -> {
                WContentDialog d = new WContentDialog(owner, "Save", true);
                d.setContent(new WLabel("Do you want to save?"));
                d.setPrimaryButtonText("Save");
                d.setCloseButtonText("Cancel");
                d.addDialogListener(new WContentDialogAdapter() {
                    @Override
                    public void dialogOpened(WContentDialogEvent e) {
                        events.add("opened");
                    }

                    @Override
                    public void dialogClosed(WContentDialogEvent e) {
                        events.add("closed:" + e.getResult() + ":" + (e.getDialog() == d));
                    }
                });
                d.addButtonClickListener(e -> events.add("button:" + e.getButton()));
                return d;
            });
            WinUiUtilities.invokeLater(() -> {
                dialog.setVisible(true);
                events.add("returned:" + dialog.isVisible());
            });
            awaitOpen(dialog);
            awaitEvents(events, Collections.singletonList("opened"));
            TestKeyboard.INSTANCE.press(0x1B, 0x01); // Esc counts as pressing the close button
            awaitEvents(events, Arrays.asList("opened", "button:CLOSE", "closed:NONE:true", "returned:false"));
            awaitClosed(dialog);
        } finally {
            UiTestHarness.detach(owner);
        }
    }

    @Test
    void setCancelInAButtonListenerKeepsItOpenAndPressingAfterFixingTheInputMakesShowAndWaitReturnPrimary() throws InterruptedException {
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        WButton owner = onUiThreadGet(() -> new WButton("Open"));
        UiTestHarness.attachAndAwaitLoaded(owner);
        try {
            WTextField name = onUiThreadGet(WTextField::new);
            WContentDialog dialog = onUiThreadGet(() -> {
                WContentDialog d = new WContentDialog("Name", name);
                d.setPrimaryButtonText("OK");
                d.setDefaultButton(ContentDialogButton.PRIMARY);
                d.addButtonClickListener(e -> {
                    if (e.getButton() == ContentDialogButton.PRIMARY && name.getText().isEmpty()) {
                        events.add("empty");
                        e.setCancel(true);
                    }
                });
                return d;
            });
            WinUiUtilities.invokeLater(() -> events.add("returned:" + dialog.showAndWait(owner)));
            awaitOpen(dialog);
            TestKeyboard.INSTANCE.pressEnter();
            awaitEvents(events, Collections.singletonList("empty"));
            assertTrue(onUiThreadGet(dialog::isVisible));
            onUiThread(() -> name.setText("Smith"));
            TestKeyboard.INSTANCE.pressEnter();
            awaitEvents(events, Arrays.asList("empty", "returned:PRIMARY"));
            awaitClosed(dialog);
        } finally {
            UiTestHarness.detach(owner);
        }
    }

    @Test
    void openingAnotherDialogInTheSameWindowWhileOneIsOpenThrowsIllegalStateException() throws InterruptedException {
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        WButton owner = onUiThreadGet(() -> new WButton("Open"));
        UiTestHarness.attachAndAwaitLoaded(owner);
        try {
            WContentDialog first = onUiThreadGet(() -> new WContentDialog("First"));
            onUiThread(() -> {
                first.setCloseButtonText("Close");
                first.show(owner, result -> events.add("closed:" + result));
                WContentDialog second = new WContentDialog("Second");
                assertThrows(IllegalStateException.class, () -> second.show(owner));
            });
            awaitOpen(first);
            onUiThread(first::dispose);
            awaitEvents(events, Collections.singletonList("closed:NONE"));
            awaitClosed(first);
        } finally {
            UiTestHarness.detach(owner);
        }
    }
}
