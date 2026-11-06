package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Tests that use menu items from Java in the same way as Swing's JMenuItem / JMenu (constructors, setMnemonic, setAccelerator,
 * addActionListener, setAction, doClick). The native state is checked in WMenuFlyoutItemTest.
 */
class WMenuFlyoutItemForJavaTest {
    private final List<String> events = new ArrayList<>();

    private void onSave(WActionEvent e) {
        events.add("save:" + e.getActionCommand());
    }

    @Test
    void canBeCreatedWithTheSameConstructorsAsJMenuItem() {
        onUiThread(() -> {
            WMenuFlyoutItem open = new WMenuFlyoutItem("Open");
            assertEquals("Open", open.getText());
            assertNull(open.getIcon());

            WMenuFlyoutItem save = new WMenuFlyoutItem("Save", Symbol.SAVE);
            assertSame(Symbol.SAVE, save.getIcon());

            WMenuFlyoutItem print = new WMenuFlyoutItem(new WFontIcon(""));
            assertEquals("", print.getText());
            assertEquals(new WFontIcon(""), print.getIcon());

            WMenuFlyoutItem exit = new WMenuFlyoutItem("Exit", WKeyEvent.VK_X);
            assertEquals(WKeyEvent.VK_X, exit.getMnemonic());

            // A null icon resolves to the WIcon form (the legacy Symbol form is not visible from Java source)
            WMenuFlyoutItem plain = new WMenuFlyoutItem("Close", null);
            assertNull(plain.getIcon());

            WMenuFlyoutItem empty = new WMenuFlyoutItem();
            assertEquals("", empty.getText());
        });
    }

    @Test
    void addActionListenerAcceptsLambdasAnonymousClassesMethodReferencesAndRunnables() {
        onUiThread(() -> {
            WMenuFlyoutItem item = new WMenuFlyoutItem("Save");
            item.addActionListener(e -> events.add("lambda:" + (e.getSource() == item) + ":" + e.getActionCommand()));
            item.addActionListener(new WActionListener() {
                @Override
                public void actionPerformed(WActionEvent e) {
                    events.add("anonymous:" + e.getActionCommand());
                }
            });
            item.addActionListener(this::onSave);
            item.addActionListener(() -> events.add("runnable"));
            item.setActionCommand("save");
            item.doClick();
            assertEquals(Arrays.asList("lambda:true:save", "anonymous:save", "save:save", "runnable"), events);
        });
    }

    @Test
    void aListenerRemovedWithRemoveActionListenerIsNotCalled() {
        onUiThread(() -> {
            WMenuFlyoutItem item = new WMenuFlyoutItem("Save");
            WActionListener listener = e -> events.add("listener");
            Runnable runnable = () -> events.add("runnable");
            item.addActionListener(listener);
            item.addActionListener(runnable);
            item.removeActionListener(listener);
            item.removeActionListener(runnable);
            item.doClick();
            assertTrue(events.isEmpty());
        });
    }

    @Test
    void aDisabledItemIsNotCalledByDoClick() {
        onUiThread(() -> {
            WMenuFlyoutItem item = new WMenuFlyoutItem("Save");
            item.addActionListener(e -> events.add("clicked"));
            item.setEnabled(false);
            item.doClick();
            assertTrue(events.isEmpty());
        });
    }

    @Test
    void setMnemonicAndSetAcceleratorAreWrittenTheSameWayAsJMenuItem() {
        onUiThread(() -> {
            WMenuFlyoutItem item = new WMenuFlyoutItem("Save");
            item.setMnemonic('s');
            assertEquals(WKeyEvent.VK_S, item.getMnemonic());
            item.setMnemonic(WKeyEvent.VK_A);
            assertEquals(WKeyEvent.VK_A, item.getMnemonic());

            item.setAccelerator(WKeyStroke.getKeyStroke(WKeyEvent.VK_S, WInputEvent.CTRL_DOWN_MASK));
            assertEquals(WKeyStroke.getKeyStroke("ctrl S"), item.getAccelerator());
            item.setAccelerator(null);
            assertNull(item.getAccelerator());

            assertThrows(IllegalArgumentException.class, () -> item.setAccelerator(WKeyStroke.getKeyStroke('a')));
            assertThrows(IllegalArgumentException.class, () -> item.setMnemonic('!'));
        });
    }

    @Test
    void getActionCommandDefaultsToTheText() {
        onUiThread(() -> {
            WMenuFlyoutItem item = new WMenuFlyoutItem("Open");
            assertEquals("Open", item.getActionCommand());
            item.setActionCommand("open");
            assertEquals("open", item.getActionCommand());
            item.setActionCommand(null);
            assertEquals("Open", item.getActionCommand());
        });
    }

    @Test
    void setActionBindsTheActionsValuesAndBehavior() {
        onUiThread(() -> {
            WAction save = new WAbstractAction("Save", Symbol.SAVE) {
                @Override
                public void actionPerformed(WActionEvent e) {
                    events.add("performed:" + e.getActionCommand());
                }
            };
            save.putValue(WAction.MNEMONIC_KEY, WKeyEvent.VK_S);
            save.putValue(WAction.ACCELERATOR_KEY, WKeyStroke.getKeyStroke("ctrl S"));
            save.putValue(WAction.SHORT_DESCRIPTION, "Save over the file");

            WMenuFlyoutItem item = new WMenuFlyoutItem(save);
            assertSame(save, item.getAction());
            assertEquals("Save", item.getText());
            assertSame(Symbol.SAVE, item.getIcon());
            assertEquals(WKeyEvent.VK_S, item.getMnemonic());
            assertEquals(WKeyStroke.getKeyStroke("ctrl S"), item.getAccelerator());
            assertEquals("Save over the file", item.getToolTipText());
            item.doClick();
            assertEquals(Arrays.asList("performed:Save"), events);

            save.setEnabled(false);
            assertFalse(item.isEnabled());
            item.setAction(null);
            assertEquals("", item.getText());
            assertNull(item.getAccelerator());
        });
    }

    @Test
    void subMenusCanHaveAnIconAndAMnemonicLikeJMenu() {
        onUiThread(() -> {
            WMenuFlyoutSubItem recent = new WMenuFlyoutSubItem("Recent files", Symbol.CLOCK);
            assertSame(Symbol.CLOCK, recent.getIcon());
            recent.setIcon(new WFontIcon(""));
            assertEquals(new WFontIcon(""), recent.getIcon());
            recent.setMnemonic('r');
            assertEquals(WKeyEvent.VK_R, recent.getMnemonic());

            WToggleMenuFlyoutItem bold = new WToggleMenuFlyoutItem("Bold", Symbol.BOLD);
            assertSame(Symbol.BOLD, bold.getIcon());
        });
    }

    @Test
    void aCustomWIconImplementationThrowsIllegalArgumentException() {
        onUiThread(() -> {
            WIcon custom = new WIcon() {
            };
            WMenuFlyoutItem item = new WMenuFlyoutItem("Save");
            assertThrows(IllegalArgumentException.class, () -> item.setIcon(custom));
            assertNull(item.getIcon());
        });
    }

    // ---- Binary compatibility with the legacy forms (Symbol icons, Kotlin lambdas that do not receive the event) ----

    @Test
    void theLegacySymbolConstructorAndSetIconGetIconRemainAsSyntheticMembersAndAreCallable() throws Exception {
        for (Class<?> type : Arrays.asList(WMenuFlyoutItem.class, WToggleMenuFlyoutItem.class, WMenuFlyoutSubItem.class)) {
            Constructor<?> constructor = type.getConstructor(String.class, Symbol.class);
            Class<?> marker = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker");
            assertTrue(type.getConstructor(String.class, Symbol.class, int.class, marker).isSynthetic());
            Method setter = type.getMethod("setIcon", Symbol.class);
            List<Method> symbolGetters = Arrays.stream(type.getMethods())
                .filter(m -> m.getName().equals("getIcon") && m.getReturnType() == Symbol.class)
                .collect(Collectors.toList());
            assertTrue(constructor.isSynthetic(), type.getName());
            assertTrue(setter.isSynthetic(), type.getName());
            assertEquals(1, symbolGetters.size(), type.getName());
            assertTrue(symbolGetters.get(0).isSynthetic());
            onUiThread(() -> {
                try {
                    Object item = constructor.newInstance("Save", Symbol.SAVE);
                    assertSame(Symbol.SAVE, symbolGetters.get(0).invoke(item));
                    setter.invoke(item, Symbol.OPEN_FILE);
                    assertSame(Symbol.OPEN_FILE, symbolGetters.get(0).invoke(item));
                    type.getMethod("setIcon", WIcon.class).invoke(item, new WFontIcon(""));
                    assertNull(symbolGetters.get(0).invoke(item)); // A non-Symbol icon is null in the legacy form
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    @Test
    void theLegacyAddActionListenerForKotlinLambdasWithoutTheEventRemainsAsASyntheticMember() throws Exception {
        Class<?> function0 = Class.forName("kotlin.jvm.functions.Function0");
        assertTrue(WMenuFlyoutItem.class.getMethod("addActionListener", function0).isSynthetic());
        assertTrue(WMenuFlyoutItem.class.getMethod("removeActionListener", function0).isSynthetic());
    }
}
