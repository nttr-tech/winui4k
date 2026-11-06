package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WColorPicker from Java in the same way developers familiar with Swing's JColorChooser would write it. */
class WColorPickerForJavaTest {
    private static final long TIMEOUT_MILLIS = 5_000;
    private static final long OPEN_SETTLE_MILLIS = 600;

    @Test
    void constructorsCanBeWrittenInThreeWaysNoArgsInitialColorAndModel() {
        onUiThread(() -> {
            new WColorPicker();
            assertEquals(new WColor(10, 20, 130), new WColorPicker(new WColor(10, 20, 130)).getColor());
            WDefaultColorSelectionModel model = new WDefaultColorSelectionModel(WColor.BLUE);
            WColorPicker picker = new WColorPicker(model);
            assertEquals(WColor.BLUE, picker.getColor());
            assertSame(model, picker.getSelectionModel());
        });
    }

    @Test
    void anAnonymousChangeListenerOnTheModelHasTheModelAsSourceAndReadsTheColorFromGetSelectedColor() {
        onUiThread(() -> {
            WColorPicker picker = new WColorPicker(WColor.ORANGE);
            WColorSelectionModel model = picker.getSelectionModel();
            List<Object> sources = new ArrayList<>();
            List<WColor> colors = new ArrayList<>();
            model.addChangeListener(new WChangeListener() {
                @Override
                public void stateChanged(WChangeEvent e) {
                    sources.add(e.getSource());
                    colors.add(((WColorSelectionModel) e.getSource()).getSelectedColor());
                }
            });
            picker.setColor(WColor.BLUE);
            picker.setColor(WColor.BLUE);
            assertEquals(Arrays.asList(WColor.BLUE), colors);
            assertSame(model, sources.get(0));
        });
    }

    @Test
    void aLambdaChangeListenerIsAlsoCalledWhenTheModelColorIsSetAndIsRemovedWithRemove() {
        onUiThread(() -> {
            WDefaultColorSelectionModel model = new WDefaultColorSelectionModel();
            WColorPicker picker = new WColorPicker(model);
            List<WColor> colors = new ArrayList<>();
            WChangeListener listener = e -> colors.add(model.getSelectedColor());
            model.addChangeListener(listener);
            model.setSelectedColor(WColor.GREEN);
            assertEquals(WColor.GREEN, picker.getColor());
            model.removeChangeListener(listener);
            model.setSelectedColor(WColor.RED);
            assertEquals(Arrays.asList(WColor.GREEN), colors);
        });
    }

    @Test
    void theExistingAddChangeListenerCanBeWrittenWithAnImplicitlyTypedLambda() {
        onUiThread(() -> {
            WColorPicker picker = new WColorPicker(WColor.ORANGE);
            List<WColor> colors = new ArrayList<>();
            picker.addChangeListener(color -> colors.add(color));
            picker.setColor(WColor.BLUE);
            assertEquals(Arrays.asList(WColor.BLUE), colors);
        });
    }

    @Test
    void showDialogCanBeCalledTheSameWayAsSwingAndReturnsTheColorOnOkAndNullOnCancel() throws InterruptedException {
        WButton owner = onUiThreadGet(() -> new WButton("Owner"));
        UiTestHarness.attachAndAwaitLoaded(owner);
        try {
            WinUiUtilities.invokeLater(() -> events.add("r:" + WColorPicker.showDialog(owner, "Choose a color", WColor.ORANGE)));
            awaitOpen(owner);
            OpenContentDialogs.press(owner, "PrimaryButton");
            awaitEvents("r:" + WColor.ORANGE);
            awaitClosed(owner);

            WinUiUtilities.invokeLater(() -> events.add("r:" + WColorPicker.showDialog(owner, "Choose a color", WColor.ORANGE)));
            awaitOpen(owner);
            OpenContentDialogs.press(owner, "CloseButton");
            awaitEvents("r:" + WColor.ORANGE, "r:null");
            awaitClosed(owner);
        } finally {
            UiTestHarness.detach(owner);
        }
    }

    @Test
    void theCallbackFormOfShowDialogCanReceiveTheResultWithALambda() throws InterruptedException {
        WButton owner = onUiThreadGet(() -> new WButton("Owner"));
        UiTestHarness.attachAndAwaitLoaded(owner);
        try {
            onUiThread(() -> WColorPicker.showDialog(owner, "Color", WColor.BLUE, color -> events.add("cb:" + color)));
            awaitOpen(owner);
            OpenContentDialogs.press(owner, "PrimaryButton");
            awaitEvents("cb:" + WColor.BLUE);
            awaitClosed(owner);
        } finally {
            UiTestHarness.detach(owner);
        }
    }

    private final List<String> events = Collections.synchronizedList(new ArrayList<>());

    private void awaitEvents(String... expected) throws InterruptedException {
        List<String> want = Arrays.asList(expected);
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!snapshot().equals(want) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(want, snapshot());
    }

    private List<String> snapshot() {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    private static void awaitOpen(WComponent owner) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!OpenContentDialogs.isOpen(owner) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertTrue(OpenContentDialogs.isOpen(owner));
        Thread.sleep(OPEN_SETTLE_MILLIS);
    }

    private static void awaitClosed(WComponent owner) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (OpenContentDialogs.describe(owner) != null && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertNull(OpenContentDialogs.describe(owner));
    }
}
