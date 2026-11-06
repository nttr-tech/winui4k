import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

/**
 * Prints the actual values of Swing's KeyStroke (toString, getModifiers, hashCode and so on).
 * The expected values of WKeyStrokeTest / WKeyStrokeForJavaTest were taken from this output (run on Temurin JDK 25 on 2026-10-20).
 *
 * Run: java tools/swing_probes/KeyStrokeProbe.java
 */
@SuppressWarnings("deprecation") // Also measures how the old-style modifiers (CTRL_MASK and so on) are handled
public class KeyStrokeProbe {
    public static void main(String[] args) {
        print("VK_S, CTRL_MASK", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        print("VK_S, CTRL_DOWN_MASK", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        print("VK_S, CTRL_DOWN_MASK | SHIFT_DOWN_MASK",
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        print("VK_S, 0x3fff", KeyStroke.getKeyStroke(KeyEvent.VK_S, 0x3fff));
        print("VK_S, BUTTON1_MASK", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.BUTTON1_MASK));
        print("VK_F4, ALT_DOWN_MASK, true", KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK, true));
        print("'a'", KeyStroke.getKeyStroke('a'));
        print("'a', 0", KeyStroke.getKeyStroke('a', 0));
        print("0x1234, 0", KeyStroke.getKeyStroke(0x1234, 0));
        String[] texts = {
            "ctrl shift S", "alt F4", "control DELETE", "meta W", "shift typed a", "typed a", "released ENTER",
            "pressed ENTER", "ctrl pressed S", "S", "altGraph Q", "button1 A", "ctrl s", "CTRL S", "ctrl", "",
            "  ctrl   S  ", "ctrl typed ab", "typed a b", "pressed ctrl S", "ctrl FOO", "alt shift ctrl meta X",
            "shift  S", "ctrl 9", "SEMICOLON", "typed  ", "typed",
        };
        for (String text : texts) {
            print("\"" + text + "\"", KeyStroke.getKeyStroke(text));
        }
        print("null", KeyStroke.getKeyStroke((String) null));
        System.out.println("getKeyStroke(VK_S, CTRL_MASK) == getKeyStroke(\"ctrl S\"): "
                + (KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK) == KeyStroke.getKeyStroke("ctrl S")));
    }

    private static void print(String label, KeyStroke stroke) {
        if (stroke == null) {
            System.out.println(label + " => null");
            return;
        }
        System.out.println(label + " => " + stroke + " | keyCode=" + stroke.getKeyCode() + " modifiers=" + stroke.getModifiers()
                + " keyChar=" + (int) stroke.getKeyChar() + " onKeyRelease=" + stroke.isOnKeyRelease()
                + " keyEventType=" + stroke.getKeyEventType() + " hashCode=" + stroke.hashCode());
    }
}
