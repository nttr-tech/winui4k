package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests that use WCardLayout written the way developers who have used Swing's CardLayout write Java. Compiling at all
 * (new WCardLayout() / (hgap, vgap), add(component, "name"), show / next from (WCardLayout) panel.getLayout()) checks
 * the API's shape, and the bodies check that it switches.
 */
class WCardLayoutForJavaTest {
    @Test
    void showsTheNamedChildAndMovesToTheNextWithNextWrittenTheSameWayAsCardLayout() {
        onUiThread(() -> {
            WLayoutPanel cards = new WLayoutPanel(new WCardLayout());
            WButton first = new WButton("One");
            WButton second = new WButton("Two");
            cards.add(first, "first");
            cards.add(second, "second");
            assertTrue(first.isVisible());
            assertFalse(second.isVisible());

            WCardLayout layout = (WCardLayout) cards.getLayout();
            layout.show(cards, "second");
            assertTrue(second.isVisible());
            assertFalse(first.isVisible());
            layout.next(cards);
            assertTrue(first.isVisible());
        });
    }

    @Test
    void canBeCreatedWithGapsAndReadAndWrittenWithGettersAndSetters() {
        WCardLayout layout = new WCardLayout(4, 6);
        assertEquals(4.0, layout.getHgap());
        layout.setVgap(8);
        assertEquals(8.0, layout.getVgap());
    }
}
