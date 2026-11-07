package com.appkitbox.winui4k.swing_to_winui4k.jspinner.range;

import com.appkitbox.winui4k.SpinButtonPlacementMode;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WSpinner;
import com.appkitbox.winui4k.WSpinnerNumberModel;
import com.appkitbox.winui4k.WinUiUtilities;

/** 9.5 Change the range from another value (WinUI4K). */
public class RangeWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("9.5 Linked range (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WSpinnerNumberModel fromModel = new WSpinnerNumberModel(1, 1, 31, 1);
            WSpinnerNumberModel toModel = new WSpinnerNumberModel(5, 1, 31, 1);
            fromModel.addChangeListener(e -> toModel.setMinimum((Integer) fromModel.getValue()));

            WSpinner from = new WSpinner(fromModel);
            from.setSpinButtonPlacementMode(SpinButtonPlacementMode.INLINE);
            from.setWidth(160);   // room for the number next to the buttons
            WSpinner to = new WSpinner(toModel);
            to.setSpinButtonPlacementMode(SpinButtonPlacementMode.INLINE);
            to.setWidth(160);   // room for the number next to the buttons
            frame.add(new WLabel("From:"));
            frame.add(from);
            frame.add(new WLabel("To:"));
            frame.add(to);

            frame.setSize(520, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            fromModel.setValue(10);
        });
    }
}
