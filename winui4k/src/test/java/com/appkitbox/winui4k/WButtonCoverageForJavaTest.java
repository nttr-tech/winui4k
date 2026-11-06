package com.appkitbox.winui4k;

import static com.appkitbox.winui4k.UiTestHarness.onUiThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests that use, from Java, the public button members that had no Java tests (groupName, AppBarButton properties, the ButtonGroup lists). */
class WButtonCoverageForJavaTest {
    @Test
    void theNameSetWithSetGroupNameCanBeReadWithGetGroupName() {
        onUiThread(() -> {
            WRadioButton radio = new WRadioButton("Small");
            assertEquals("", radio.getGroupName());
            radio.setGroupName("size");
            assertEquals("size", radio.getGroupName());
        });
    }

    @Test
    void wAppBarButtonLabelPositionShortcutTextAndCompactModeCanBeSet() {
        onUiThread(() -> {
            WAppBarButton save = new WAppBarButton("Save");
            save.setLabelPosition(CommandBarLabelPosition.COLLAPSED);
            save.setKeyboardAcceleratorText("Ctrl+S");
            save.setCompact(true);
            assertEquals(CommandBarLabelPosition.COLLAPSED, save.getLabelPosition());
            assertEquals("Ctrl+S", save.getKeyboardAcceleratorText());
            assertTrue(save.isCompact());
        });
    }

    @Test
    void buttonsInAButtonGroupCanBeReadInTheSameOrderWithGetButtonsAndGetElements() {
        onUiThread(() -> {
            WButtonGroup group = new WButtonGroup();
            WRadioButton small = new WRadioButton("Small");
            WRadioButton large = new WRadioButton("Large");
            group.add(small);
            group.add(large);
            assertEquals(2, group.getButtons().size());
            assertEquals(small, group.getButtons().get(0));
            assertEquals(small, group.getElements().nextElement());
        });
    }
}
