package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests that use WExpander's addChangeListener and header from Java as a Swing developer would write it. */
class WExpanderForJavaTest {
    @Test
    void passingALambdaToAddChangeListenerReadsTheStateFromGetSourceOnEveryExpandAndCollapse() {
        onUiThread(() -> {
            WExpander expander = new WExpander("Details");
            List<Boolean> states = new ArrayList<>();
            List<Object> sources = new ArrayList<>();
            WChangeListener listener = e -> {
                sources.add(e.getSource());
                states.add(((WExpander) e.getSource()).isExpanded());
            };
            expander.addChangeListener(listener);
            assertArrayEquals(new WChangeListener[] {listener}, expander.getChangeListeners());

            expander.setExpanded(true);
            expander.setExpanded(false);
            assertEquals(Arrays.asList(true, false), states);
            assertSame(expander, sources.get(0));

            expander.removeChangeListener(listener);
            expander.setExpanded(true);
            assertEquals(2, states.size());
        });
    }

    @Test
    void getHeaderAndSetHeaderReadAndWriteTheString() {
        onUiThread(() -> {
            WExpander expander = new WExpander();
            assertEquals("", expander.getHeader());
            expander.setHeader("Heading");
            assertEquals("Heading", expander.getHeader());
        });
    }
}
