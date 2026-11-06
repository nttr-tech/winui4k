package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static com.appkitbox.winui4k.UiTestHarness.onUiThreadGet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WComponent's setMaxWidth / setMargin and adding and removing ActualThemeChanged, Loaded and SizeChanged listeners
 * added as Runnables from Java (in Java, pass the same Runnable instance to remove to remove it).
 */
class WComponentAppearanceForJavaTest {
    @Test
    void setMaxWidthCanBeReadBackWithGetMaxWidthAndDefaultsToUnlimited() {
        onUiThread(() -> {
            WButton button = new WButton("Press");
            assertEquals(Double.POSITIVE_INFINITY, button.getMaxWidth());
            button.setMaxWidth(80);
            assertEquals(80.0, button.getMaxWidth());
            button.setMargin(1, 2, 3, 4);
        });
    }

    @Test
    void aRunnableListenerPassedToRemoveIsNotCalledAndOnlyTheKeptListenerIsCalled() throws InterruptedException {
        LinkedBlockingQueue<String> loaded = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<ElementTheme> themes = new LinkedBlockingQueue<>();
        List<String> removed = Collections.synchronizedList(new ArrayList<>());
        Runnable removedLoaded = () -> removed.add("loaded");
        Runnable removedSize = () -> removed.add("size");
        Runnable removedTheme = () -> removed.add("theme");
        WButton button = onUiThreadGet(() -> {
            WButton b = new WButton("Press");
            b.addLoadedListener(removedLoaded);
            b.addSizeChangedListener(removedSize);
            b.addActualThemeChangedListener(removedTheme);
            b.addLoadedListener(() -> loaded.add("loaded"));
            b.addActualThemeChangedListener(() -> themes.add(b.getActualTheme()));
            b.removeLoadedListener(removedLoaded);
            b.removeSizeChangedListener(removedSize);
            b.removeActualThemeChangedListener(removedTheme);
            return b;
        });
        UiTestHarness.attach(button);
        try {
            assertEquals("loaded", loaded.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS));
            themes.clear();
            ElementTheme target = onUiThreadGet(() -> {
                ElementTheme opposite = button.getActualTheme() == ElementTheme.DARK ? ElementTheme.LIGHT : ElementTheme.DARK;
                button.setRequestedTheme(opposite);
                return opposite;
            });
            ElementTheme theme = themes.poll(UiTestHarness.TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull(theme);
            assertEquals(target, theme);
            onUiThread(button::updateLayout);
            assertTrue(removed.isEmpty(), removed::toString);
        } finally {
            UiTestHarness.detach(button);
        }
    }
}
