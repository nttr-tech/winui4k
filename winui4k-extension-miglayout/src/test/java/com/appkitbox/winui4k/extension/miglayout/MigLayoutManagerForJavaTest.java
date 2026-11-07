package com.appkitbox.winui4k.extension.miglayout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Tests that check, as with Swing's new MigLayout(...), that it can be created from Java with constructors of 0 to 3 arguments. */
class MigLayoutManagerForJavaTest {
    @Test
    void allConstraintsAreEmptyWithoutArguments() {
        MigLayoutManager layout = new MigLayoutManager();
        assertEquals("", layout.getLayoutConstraints());
        assertEquals("", layout.getColumnConstraints());
        assertEquals("", layout.getRowConstraints());
    }

    @Test
    void onlyTheLayoutConstraintsCanBePassed() {
        MigLayoutManager layout = new MigLayoutManager("wrap 2");
        assertEquals("wrap 2", layout.getLayoutConstraints());
        assertEquals("", layout.getColumnConstraints());
        assertEquals("", layout.getRowConstraints());
    }

    @Test
    void theLayoutAndColumnConstraintsCanBePassed() {
        MigLayoutManager layout = new MigLayoutManager("wrap 2", "[right][grow, fill]");
        assertEquals("wrap 2", layout.getLayoutConstraints());
        assertEquals("[right][grow, fill]", layout.getColumnConstraints());
        assertEquals("", layout.getRowConstraints());
    }

    @Test
    void allThreeConstraintsCanBePassed() {
        MigLayoutManager layout = new MigLayoutManager("wrap 2", "[right][grow, fill]", "[][grow]");
        assertEquals("wrap 2", layout.getLayoutConstraints());
        assertEquals("[right][grow, fill]", layout.getColumnConstraints());
        assertEquals("[][grow]", layout.getRowConstraints());
    }
}
