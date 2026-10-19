package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Tests that use toolbar buttons from Java in the same way as setIcon / setAction of JButton / JToggleButton.
 * That the icon goes into the native AppBarButton.Icon is checked in WAppBarButtonTest.
 */
class WAppBarButtonForJavaTest {
    @Test
    void canUseSymbolOrFontGlyphAsIconAndReadItWithGetIconLikeJButton() {
        onUiThread(() -> {
            WAppBarButton like = new WAppBarButton("Like", Symbol.LIKE);
            assertSame(Symbol.LIKE, like.getIcon());

            WIcon glyph = new WFontIcon("\uE8E5");
            WAppBarButton open = new WAppBarButton("Open", glyph);
            assertEquals(glyph, open.getIcon());

            open.setIcon(new WImageIcon("C:/icons/open.png"));
            assertEquals(new WImageIcon("C:/icons/open.png"), open.getIcon());
            open.setIcon(null);
            assertNull(open.getIcon());
        });
    }

    @Test
    void toggleToolbarButtonAlsoAcceptsWIcon() {
        onUiThread(() -> {
            WAppBarToggleButton bold = new WAppBarToggleButton("Bold", Symbol.BOLD);
            assertSame(Symbol.BOLD, bold.getIcon());
            bold.setIcon(new WFontIcon("\uE8DD"));
            assertEquals(new WFontIcon("\uE8DD"), bold.getIcon());
        });
    }

    @Test
    void setTextAndGetTextReadAndWriteTheLabelLikeJButton() {
        onUiThread(() -> {
            WAppBarButton button = new WAppBarButton("Save", Symbol.SAVE);
            assertEquals("Save", button.getText());
            button.setText("Overwrite");
            assertEquals("Overwrite", button.getLabel());

            WAppBarToggleButton toggle = new WAppBarToggleButton();
            toggle.setText("Bold");
            assertEquals("Bold", toggle.getLabel());
            assertEquals("Bold", toggle.getActionCommand());
        });
    }

    @Test
    void selfImplementedWIconThrowsIllegalArgumentException() {
        onUiThread(() -> {
            WIcon custom = new WIcon() {
            };
            WAppBarButton button = new WAppBarButton("Save");
            assertThrows(IllegalArgumentException.class, () -> button.setIcon(custom));
            assertNull(button.getIcon());
        });
    }

    @Test
    void setActionBindsTheActionIconAndBehavior() {
        onUiThread(() -> {
            int[] performed = {0};
            WAction save = new WAbstractAction("Save", Symbol.SAVE) {
                @Override
                public void actionPerformed(WActionEvent e) {
                    performed[0]++;
                }
            };
            WAppBarButton button = new WAppBarButton();
            button.setAction(save);
            assertSame(Symbol.SAVE, button.getIcon());
            button.doClick();
            assertEquals(1, performed[0]);
        });
    }

    // ---- Binary compatibility with the legacy form (Symbol) ----

    @Test
    void legacySymbolConstructorAndSetIconGetIconRemainAsSyntheticMembersAndCanBeCalled() throws Exception {
        for (Class<?> type : Arrays.asList(WAppBarButton.class, WAppBarToggleButton.class)) {
            Constructor<?> constructor = type.getConstructor(String.class, Symbol.class);
            // The synthetic constructor for default arguments, used by Kotlin callers compiled with arguments omitted, also remains
            Class<?> marker = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker");
            assertTrue(type.getConstructor(String.class, Symbol.class, int.class, marker).isSynthetic());
            Method setter = type.getMethod("setIcon", Symbol.class);
            List<Method> symbolGetters = Arrays.stream(type.getMethods())
                .filter(m -> m.getName().equals("getIcon") && m.getReturnType() == Symbol.class)
                .collect(Collectors.toList());
            // Not visible from Java source (javac ignores synthetic members), but already compiled calls can still link
            assertTrue(constructor.isSynthetic());
            assertTrue(setter.isSynthetic());
            assertEquals(1, symbolGetters.size());
            assertTrue(symbolGetters.get(0).isSynthetic());
            onUiThread(() -> {
                try {
                    Object button = constructor.newInstance("Save", Symbol.SAVE);
                    assertSame(Symbol.SAVE, symbolGetters.get(0).invoke(button));
                    setter.invoke(button, Symbol.OPEN_FILE);
                    assertSame(Symbol.OPEN_FILE, symbolGetters.get(0).invoke(button));
                    ((WButtonBase) button).setIcon(new WFontIcon("\uE8E5"));
                    assertNull(symbolGetters.get(0).invoke(button)); // an icon that is not a Symbol is null in the legacy form
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }
}
