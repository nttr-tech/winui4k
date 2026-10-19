package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use WAutoSuggestBox's ActionListener and icon in Java, in the form a developer who has used Swing's editable
 * JComboBox would write. The ActionListener on Enter is checked in WAutoSuggestBoxTest.
 */
class WAutoSuggestBoxForJavaTest {
    @Test
    void lambdaAndMethodReferenceActionListenersAreCalledByPostActionEventInRegistrationOrder() {
        onUiThread(() -> {
            WAutoSuggestBox box = new WAutoSuggestBox();
            box.setText("search term");
            List<String> calls = new ArrayList<>();
            box.addActionListener(e -> {
                assertSame(box, e.getSource());
                calls.add("event:" + e.getActionCommand());
            });
            box.addActionListener(() -> calls.add("runnable"));
            WActionListener byListener = e -> calls.add("listener");
            box.addActionListener(byListener);

            box.postActionEvent();
            assertEquals(Arrays.asList("event:search term", "runnable", "listener"), calls);

            calls.clear();
            box.removeActionListener(byListener);
            box.setActionCommand("search");
            box.postActionEvent();
            assertEquals(Arrays.asList("event:search", "runnable"), calls);
        });
    }

    @Test
    void setQueryIconTakesSymbolAndGetQueryIconReadsWIcon() {
        onUiThread(() -> {
            WAutoSuggestBox box = new WAutoSuggestBox();
            assertNull(box.getQueryIcon());
            box.setQueryIcon(Symbol.FIND);
            WIcon icon = box.getQueryIcon();
            assertEquals(Symbol.FIND, icon);
            box.setQueryIcon(new WImageIcon("C:/icons/find.png"));
            assertEquals(new WImageIcon("C:/icons/find.png"), box.getQueryIcon());
            box.setQueryIcon(null);
            assertNull(box.getQueryIcon());
        });
    }

    @Test
    void passingCustomWIconToSetQueryIconThrowsIllegalArgumentExceptionAndIconIsUnchanged() {
        onUiThread(() -> {
            WAutoSuggestBox box = new WAutoSuggestBox();
            box.setQueryIcon(Symbol.SAVE);
            assertThrows(IllegalArgumentException.class, () -> box.setQueryIcon(new WIcon() {
            }));
            assertEquals(Symbol.SAVE, box.getQueryIcon());
        });
    }

    @Test
    void textChangedAndQuerySubmittedCanBeAddedWithConsumerTypesAsBefore() {
        onUiThread(() -> {
            WAutoSuggestBox box = new WAutoSuggestBox("Find");
            List<String> calls = new ArrayList<>();
            box.addTextChangedListener((text, reason) -> calls.add(text + ":" + reason));
            box.addQuerySubmittedListener((query, chosen) -> calls.add(query + ":" + chosen));
            box.setText("Apple");
            assertEquals("Find", box.getPlaceholderText());
            assertEquals("Apple", box.getText());
        });
    }
}
