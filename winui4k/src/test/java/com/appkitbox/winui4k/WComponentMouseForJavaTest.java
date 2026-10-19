package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use the same shape as Component.addMouseListener / addMouseMotionListener / addMouseWheelListener in the
 * plain Java a Swing developer would write (an anonymous subclass of MouseAdapter, a MouseWheelListener lambda,
 * getX / getButton / getClickCount, etc.).
 * WinUI pointer input cannot be produced with window messages, so the operations are passed via {@link TestMouse}.
 */
class WComponentMouseForJavaTest {
    /** Lays out the attached [component] on the UI thread, runs [block], and finally removes it. */
    private static void withShown(WComponent component, Runnable block) {
        UiTestHarness.attachAndAwaitLoaded(component);
        try {
            onUiThread(() -> {
                component.updateLayout();
                block.run();
            });
        } finally {
            UiTestHarness.detach(component);
        }
    }

    private static WPanel sizedPanel() {
        WPanel panel = new WPanel();
        panel.setWidth(200);
        panel.setHeight(100);
        return panel;
    }

    @Test
    void anonymousMouseAdapterSubclassCanReadClickPositionButtonAndCount() {
        WPanel panel = onUiThreadGet(WComponentMouseForJavaTest::sizedPanel);
        withShown(panel, () -> {
            List<String> calls = new ArrayList<>();
            panel.addMouseListener(new WMouseAdapter() {
                @Override
                public void mousePressed(WMouseEvent e) {
                    calls.add("pressed:" + e.getX() + "," + e.getY());
                }

                @Override
                public void mouseClicked(WMouseEvent e) {
                    assertSame(panel, e.getSource());
                    assertSame(panel, e.getComponent());
                    assertEquals(WMouseEvent.MOUSE_CLICKED, e.getID());
                    if (e.getButton() == WMouseEvent.BUTTON1 && e.getClickCount() == 2) {
                        calls.add("double-click");
                    } else {
                        calls.add("click:" + e.getClickCount());
                    }
                    assertEquals(new WPoint(12, 34), e.getPoint());
                    assertFalse(e.isPopupTrigger());
                    assertFalse(e.isShiftDown());
                }
            });
            TestMouse mouse = new TestMouse();
            mouse.click(12.5, 34.9, WMouseEvent.BUTTON1, panel);
            mouse.advance(1);
            mouse.click(12.5, 34.9, WMouseEvent.BUTTON1, panel);
            assertEquals(Arrays.asList("pressed:12,34", "click:1", "pressed:12,34", "double-click"), calls);
        });
    }

    @Test
    void swingStyleShowingMenuOnRightClickIsPopupTriggerWorks() {
        WPanel panel = onUiThreadGet(WComponentMouseForJavaTest::sizedPanel);
        withShown(panel, () -> {
            List<String> calls = new ArrayList<>();
            panel.addMouseListener(new WMouseAdapter() {
                @Override
                public void mousePressed(WMouseEvent e) {
                    maybeShowPopup(e);
                }

                @Override
                public void mouseReleased(WMouseEvent e) {
                    maybeShowPopup(e);
                }

                private void maybeShowPopup(WMouseEvent e) {
                    if (e.isPopupTrigger()) {
                        calls.add("popup:" + e.getButton() + ":" + e.getID());
                    }
                }
            });
            new TestMouse().click(5, 5, WMouseEvent.BUTTON3, panel);
            assertEquals(Collections.singletonList("popup:3:" + WMouseEvent.MOUSE_RELEASED), calls);
        });
    }

    @Test
    void mouseAdapterAddedAlsoWithAddMouseMotionListenerReceivesDrags() {
        WPanel panel = onUiThreadGet(WComponentMouseForJavaTest::sizedPanel);
        withShown(panel, () -> {
            List<String> calls = new ArrayList<>();
            WMouseAdapter adapter = new WMouseAdapter() {
                @Override
                public void mousePressed(WMouseEvent e) {
                    calls.add("pressed");
                }

                @Override
                public void mouseDragged(WMouseEvent e) {
                    calls.add("dragged:" + e.getX());
                    assertTrue((e.getModifiersEx() & WInputEvent.BUTTON1_DOWN_MASK) != 0);
                }

                @Override
                public void mouseReleased(WMouseEvent e) {
                    calls.add("released");
                }
            };
            panel.addMouseListener(adapter);
            panel.addMouseMotionListener(adapter);
            List<WComponent> targets = Collections.singletonList(panel);
            TestMouse mouse = new TestMouse();
            mouse.press(10, 10, WMouseEvent.BUTTON1, targets);
            mouse.move(150, 10, targets);
            mouse.move(250, 10, targets);
            mouse.release(250, 10, WMouseEvent.BUTTON1, targets);
            assertEquals(Arrays.asList("pressed", "dragged:150", "dragged:250", "released"), calls);
            assertArrayEquals(new WMouseListener[] {adapter}, panel.getMouseListeners());
            assertArrayEquals(new WMouseMotionListener[] {adapter}, panel.getMouseMotionListeners());
            assertEquals(0, panel.getMouseWheelListeners().length);
        });
    }

    @Test
    void mouseWheelListenerCanBeLambdaAndGetWheelRotationIsPositiveTowardUser() {
        WPanel panel = onUiThreadGet(WComponentMouseForJavaTest::sizedPanel);
        withShown(panel, () -> {
            List<Integer> rotations = new ArrayList<>();
            WMouseWheelListener listener = e -> {
                rotations.add(e.getWheelRotation());
                e.consume();
            };
            panel.addMouseWheelListener(listener);
            List<WComponent> targets = Collections.singletonList(panel);
            TestMouse mouse = new TestMouse();
            mouse.wheel(5, 5, -120, targets);
            mouse.wheel(5, 5, 240, targets);
            panel.removeMouseWheelListener(listener);
            mouse.wheel(5, 5, 120, targets);
            assertEquals(Arrays.asList(1, -2), rotations);
            assertEquals(Arrays.asList("handled:", "handled:"), mouse.getCaptures());
        });
    }

    @Test
    void mouseEventCanBeCreatedManuallyToCallListenersAndConstantsMatchAwt() {
        assertEquals(500, WMouseEvent.MOUSE_CLICKED);
        assertEquals(501, WMouseEvent.MOUSE_PRESSED);
        assertEquals(506, WMouseEvent.MOUSE_DRAGGED);
        assertEquals(3, WMouseEvent.BUTTON3);
        assertEquals(1 << 10, WInputEvent.BUTTON1_DOWN_MASK);
        assertEquals(1 << 14, WInputEvent.getMaskForButton(4));
        onUiThread(() -> {
            WButton button = new WButton("OK");
            WMouseEvent event = new WMouseEvent(button, WMouseEvent.MOUSE_PRESSED, 0L,
                WInputEvent.SHIFT_DOWN_MASK | WInputEvent.BUTTON1_DOWN_MASK, 3, 4, 1, false, WMouseEvent.BUTTON1);
            assertEquals(3, event.getX());
            assertEquals(4.0, event.getPreciseY());
            assertTrue(event.isShiftDown());
            assertEquals(WInputEvent.SHIFT_MASK | WInputEvent.BUTTON1_MASK, event.getModifiers());
            assertFalse(event.isConsumed());
            event.consume();
            assertTrue(event.isConsumed());
            WMouseWheelEvent wheel = new WMouseWheelEvent(button, WMouseEvent.MOUSE_WHEEL, 0L, 0, 0, 0, 0, false,
                WMouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 2);
            assertEquals(6, wheel.getUnitsToScroll());
            assertEquals(2.0, wheel.getPreciseWheelRotation());
        });
    }
}
