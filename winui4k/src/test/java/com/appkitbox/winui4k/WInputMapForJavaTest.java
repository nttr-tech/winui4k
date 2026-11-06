package com.appkitbox.winui4k;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Tests that use the same forms as InputMap / ActionMap in the Java a Swing developer writes
 * (new InputMap(), sharing default mappings with setParent, putting an anonymous subclass of AbstractAction into an ActionMap). WinUI is not used.
 */
class WInputMapForJavaTest {
    @Test
    void setParentInheritsSharedMappings_ownPutTakesPrecedence_andRemoveReturnsToTheParentMapping() {
        WKeyStroke ctrlS = WKeyStroke.getKeyStroke("ctrl S");
        WKeyStroke escape = WKeyStroke.getKeyStroke("ESCAPE");
        WInputMap shared = new WInputMap();
        shared.put(ctrlS, "save");
        shared.put(escape, "cancel");
        WInputMap inputMap = new WInputMap();
        inputMap.setParent(shared);
        inputMap.put(ctrlS, "saveAs");

        assertSame(shared, inputMap.getParent());
        assertEquals("saveAs", inputMap.get(ctrlS));
        assertEquals("cancel", inputMap.get(escape));
        assertEquals(1, inputMap.size());
        assertArrayEquals(new WKeyStroke[] {ctrlS}, inputMap.keys());
        assertArrayEquals(new WKeyStroke[] {ctrlS, escape}, inputMap.allKeys());
        inputMap.remove(ctrlS);
        assertEquals("save", inputMap.get(ctrlS));
        assertNull(new WInputMap().keys());
    }

    @Test
    void anAnonymousSubclassOfAbstractActionPutInAnActionMapCanBeLookedUpFromAChildActionMap() {
        WAbstractAction cancel = new WAbstractAction("Cancel") {
            @Override
            public void actionPerformed(WActionEvent e) {
            }
        };
        WActionMap shared = new WActionMap();
        shared.put("cancel", cancel);
        WActionMap actionMap = new WActionMap();
        actionMap.setParent(shared);

        assertSame(cancel, actionMap.get("cancel"));
        assertNull(actionMap.keys());
        assertArrayEquals(new Object[] {"cancel"}, actionMap.allKeys());
        actionMap.put("cancel", null);
        assertSame(cancel, actionMap.get("cancel"));
        shared.clear();
        assertNull(actionMap.get("cancel"));
    }
}
