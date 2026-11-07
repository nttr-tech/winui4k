package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests that use the columns of WTextField the way developers used to Swing's JTextField(int columns) / setColumns
 * write it in Java.
 * The width determined by columns is checked in WTextFieldColumnsTest.
 */
class WTextFieldColumnsForJavaTest {
    @Test
    void createdWithTheColumnsConstructorAndReadableByGetColumns() {
        onUiThread(() -> {
            WTextField field = new WTextField(20);
            assertEquals(20, field.getColumns());
            assertEquals("", field.getText());
        });
    }

    @Test
    void setColumnsChangesItAndANegativeValueThrowsWithoutChangingTheValue() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            assertEquals(0, field.getColumns());
            field.setColumns(8);
            assertEquals(8, field.getColumns());
            assertThrows(IllegalArgumentException.class, () -> field.setColumns(-1));
            assertEquals(8, field.getColumns());
        });
    }

    @Test
    void stringAndColumnsConstructorSetsTheTextAndColumnsLikeJTextField() {
        onUiThread(() -> {
            WTextField field = new WTextField("12-345", 10);
            assertEquals("12-345", field.getText());
            assertEquals(10, field.getColumns());
            assertEquals("", field.getPlaceholderText());
        });
    }

    @Test
    void singleStringConstructorSetsTheFieldTextLikeJTextFieldAndColumnsIsZero() {
        onUiThread(() -> {
            WTextField field = new WTextField("Tokyo");
            assertEquals("Tokyo", field.getText());
            assertEquals(0, field.getColumns());
            assertEquals("", field.getPlaceholderText());
            assertEquals("", new WTextField((String) null).getText());
        });
    }

    @Test
    void stringAndColumnsConstructorThrowsForNegativeColumns() {
        onUiThread(() -> assertThrows(IllegalArgumentException.class, () -> new WTextField("x", -1)));
    }
}
