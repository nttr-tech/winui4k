package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests that use input fields from Java in the forms written with Swing's JTextField / JPasswordField / JTextPane (removing all via getActionListeners, getText(offs, len),
 * setFocusAccelerator, setSelectionColor, read / write, new JPasswordField(columns)).
 */
class WTextComponentSwingForJavaTest {
    @Test
    void removingAllListenersReturnedByGetActionListenersMeansPostActionEventCallsNothing() {
        onUiThread(() -> {
            List<String> calls = new ArrayList<>();
            WTextField field = new WTextField();
            field.addActionListener(e -> calls.add("listener"));
            field.addActionListener(() -> calls.add("runnable"));
            assertEquals(2, field.getActionListeners().length);
            for (WActionListener l : field.getActionListeners()) {
                field.removeActionListener(l);
            }
            field.postActionEvent();
            assertTrue(calls.isEmpty());

            WPasswordField password = new WPasswordField();
            password.addActionListener(e -> calls.add("password"));
            for (WActionListener l : password.getActionListeners()) {
                password.removeActionListener(l);
            }
            password.postActionEvent();
            assertTrue(calls.isEmpty());
        });
    }

    @Test
    void passingAPositionAndLengthToGetTextExtractsOnlyPartOfIt() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setText("Hello, world");
            assertEquals("world", field.getText(7, 5));
            WTextPane pane = new WTextPane();
            pane.setText("Line 1\nLine 2");
            assertEquals("Line 2", pane.getText(7, 6));
            assertThrows(IllegalArgumentException.class, () -> field.getText(10, 5));
        });
    }

    @Test
    void theCharacterSetWithSetFocusAcceleratorCanBeReadInUpperCaseFromGetFocusAccelerator() {
        onUiThread(() -> {
            WTextField field = new WTextField();
            field.setFocusAccelerator('n');
            assertEquals('N', field.getFocusAccelerator());
            WPasswordField password = new WPasswordField();
            password.setFocusAccelerator('p');
            assertEquals('P', password.getFocusAccelerator());
            WTextPane pane = new WTextPane();
            pane.setFocusAccelerator('\0');
            assertEquals('\0', pane.getFocusAccelerator());
        });
    }

    @Test
    void theColorSetWithSetSelectionColorCanBeReadWithGetSelectionColor() {
        onUiThread(() -> {
            WColor yellow = new WColor(255, 255, 0);
            WTextField field = new WTextField();
            field.setSelectionColor(yellow);
            assertEquals(yellow, field.getSelectionColor());
            WPasswordField password = new WPasswordField();
            password.setSelectionColor(yellow);
            assertEquals(yellow, password.getSelectionColor());
            WTextPane pane = new WTextPane();
            pane.setSelectionColor(yellow);
            assertEquals(yellow, pane.getSelectionColor());
        });
    }

    @Test
    void readAndWriteCanTransferStringsBetweenAReaderAndAWriter() {
        onUiThread(() -> {
            try {
                WTextPane pane = new WTextPane();
                pane.read(new StringReader("First line\r\nSecond line"), null);
                assertEquals("First line\nSecond line", pane.getText());
                StringWriter out = new StringWriter();
                pane.write(out);
                assertEquals("First line\nSecond line", out.toString());

                WTextField field = new WTextField();
                field.read(new StringReader("Name"), "Description");
                StringWriter fieldOut = new StringWriter();
                field.write(fieldOut);
                assertEquals("Name", fieldOut.toString());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Test
    void canBeCreatedWithAColumnCountLikeJPasswordField() {
        onUiThread(() -> {
            WPasswordField password = new WPasswordField(10);
            assertEquals(10, password.getColumns());
            password.setColumns(20);
            assertEquals(20, password.getColumns());
        });
    }
}
