package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WOptionPane from Java the way a developer used to JOptionPane would write it
 * (static methods and comparing with constants {@code == WOptionPane.YES_OPTION}, switch, arrays of options, callback lambdas).
 * Opened dialogs are pressed from outside with {@link OpenContentDialogs}, and keys are sent with {@link TestKeyboard}.
 */
class WOptionPaneForJavaTest {
    private static final long TIMEOUT_MILLIS = 5_000;
    private static final long OPEN_SETTLE_MILLIS = 600;

    private final List<String> events = Collections.synchronizedList(new ArrayList<>());
    private Locale savedLocale;
    private WButton owner;

    @BeforeEach
    void setUp() {
        savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.JAPAN);
        owner = onUiThreadGet(() -> new WButton("Owner"));
        UiTestHarness.attachAndAwaitLoaded(owner);
    }

    @AfterEach
    void tearDown() {
        UiTestHarness.detach(owner);
        Locale.setDefault(savedLocale);
    }

    private void awaitEvents(List<String> expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!snapshot().equals(expected) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(expected, snapshot());
    }

    private List<String> snapshot() {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    private void awaitOpen() throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!OpenContentDialogs.isOpen(owner) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertTrue(OpenContentDialogs.isOpen(owner));
        Thread.sleep(OPEN_SETTLE_MILLIS);
    }

    private void awaitClosed() throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (OpenContentDialogs.describe(owner) != null && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(null, OpenContentDialogs.describe(owner));
    }

    @Test
    void constantsAreStaticFieldsWithTheSameNamesAndValuesAsJOptionPane() {
        assertEquals(0, WOptionPane.YES_OPTION);
        assertEquals(1, WOptionPane.NO_OPTION);
        assertEquals(2, WOptionPane.CANCEL_OPTION);
        assertEquals(0, WOptionPane.OK_OPTION);
        assertEquals(-1, WOptionPane.CLOSED_OPTION);
        assertEquals(-1, WOptionPane.DEFAULT_OPTION);
        assertEquals(0, WOptionPane.YES_NO_OPTION);
        assertEquals(1, WOptionPane.YES_NO_CANCEL_OPTION);
        assertEquals(2, WOptionPane.OK_CANCEL_OPTION);
        assertEquals(0, WOptionPane.ERROR_MESSAGE);
        assertEquals(1, WOptionPane.INFORMATION_MESSAGE);
        assertEquals(2, WOptionPane.WARNING_MESSAGE);
        assertEquals(3, WOptionPane.QUESTION_MESSAGE);
        assertEquals(-1, WOptionPane.PLAIN_MESSAGE);
        assertEquals("uninitializedValue", WOptionPane.UNINITIALIZED_VALUE);
    }

    @Test
    void showConfirmDialogResultCanBeDispatchedWithSwitchAndNoIsNoOption() throws InterruptedException {
        WinUiUtilities.invokeLater(() -> {
            int answer = WOptionPane.showConfirmDialog(owner, "Save changes?", "Save", WOptionPane.YES_NO_CANCEL_OPTION,
                    WOptionPane.WARNING_MESSAGE);
            switch (answer) {
                case WOptionPane.YES_OPTION:
                    events.add("save");
                    break;
                case WOptionPane.NO_OPTION:
                    events.add("discard");
                    break;
                default:
                    events.add("stay:" + answer);
                    break;
            }
        });
        awaitOpen();
        OpenContentDialogs.press(owner, "SecondaryButton");
        awaitEvents(Collections.singletonList("discard"));
        awaitClosed();
    }

    @Test
    void closingShowConfirmDialogWithEscGivesClosedOptionDistinguishedFromCancel() throws InterruptedException {
        WinUiUtilities.invokeLater(() -> {
            if (WOptionPane.showConfirmDialog(owner, "Quit?") == WOptionPane.CLOSED_OPTION) {
                events.add("closed");
            }
        });
        awaitOpen();
        TestKeyboard.INSTANCE.press(0x1B, 0x01); // Esc
        awaitEvents(Collections.singletonList("closed"));
        awaitClosed();
    }

    @Test
    void showInputDialogReturnsTheEnteredText() throws InterruptedException {
        WinUiUtilities.invokeLater(() -> {
            String name = WOptionPane.showInputDialog(owner, "Your name?");
            events.add("name:" + name);
        });
        awaitOpen();
        OpenContentDialogs.setText(owner, "Smith");
        OpenContentDialogs.press(owner, "PrimaryButton");
        awaitEvents(Collections.singletonList("name:Smith"));
        awaitClosed();
    }

    @Test
    void showInputDialogWithAnArrayOfOptionsReturnsTheChosenElementAndShowOptionDialogReturnsThePosition() throws InterruptedException {
        Object[] sizes = {"Small", "Medium", "Large"};
        WinUiUtilities.invokeLater(() -> {
            Object size = WOptionPane.showInputDialog(owner, "Size", "Choose", WOptionPane.QUESTION_MESSAGE, null, sizes, sizes[0]);
            events.add("size:" + size);
        });
        awaitOpen();
        OpenContentDialogs.select(owner, 1);
        OpenContentDialogs.press(owner, "PrimaryButton");
        awaitEvents(Collections.singletonList("size:Medium"));
        awaitClosed();

        Object[] options = {"Overwrite", "Rename", "Cancel"};
        WinUiUtilities.invokeLater(() -> {
            int choice = WOptionPane.showOptionDialog(owner, "A file with the same name exists", "Save", WOptionPane.DEFAULT_OPTION,
                    WOptionPane.WARNING_MESSAGE, null, options, options[0]);
            events.add("choice:" + choice);
        });
        awaitOpen();
        OpenContentDialogs.press(owner, "CloseButton");
        awaitEvents(Arrays.asList("size:Medium", "choice:2"));
        awaitClosed();
    }

    @Test
    void callbackFormReceivesTheResultInALambdaAndButtonTextsCanBeChangedWithSetters() throws InterruptedException {
        onUiThread(() -> {
            WOptionPane.setYesButtonText("Do it");
            WOptionPane.setNoButtonText("Don't");
            WOptionPane.showConfirmDialog(owner, "Sync now?", "Sync", WOptionPane.YES_NO_OPTION, WOptionPane.QUESTION_MESSAGE, null,
                    answer -> events.add("answer:" + answer));
        });
        try {
            awaitOpen();
            OpenContentDialogs.Description shown = OpenContentDialogs.describe(owner);
            assertEquals("Do it", shown.getPrimary());
            assertEquals("Don't", shown.getSecondary());
            OpenContentDialogs.press(owner, "PrimaryButton");
            awaitEvents(Collections.singletonList("answer:" + WOptionPane.YES_OPTION));
            awaitClosed();
        } finally {
            onUiThread(WOptionPane::resetButtonTexts);
        }
    }

    @Test
    void nullParentThrowsIllegalArgumentException() {
        onUiThread(() -> assertThrows(IllegalArgumentException.class, () -> WOptionPane.showMessageDialog(null, "No parent")));
    }
}
