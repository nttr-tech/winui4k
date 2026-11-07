package com.appkitbox.winui4k.swing_to_winui4k.jcolorchooser.shape;

import com.appkitbox.winui4k.ColorSpectrumShape;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WColorPicker;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WinUiUtilities;

/** 28.5 Ring spectrum and folded text inputs (WinUI4K). */
public class ShapeWinUI4K {
    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("28.5 Ring and More button (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WColorPicker picker = new WColorPicker(new WColor(0, 120, 215));
            picker.setSpectrumShape(ColorSpectrumShape.RING);   // a color wheel instead of a box
            picker.setMoreButtonVisible(true);                  // fold the RGB / HSV inputs behind "More"
            picker.setHexInputVisible(false);                   // hide the "#0078D7" input

            frame.add(picker);

            frame.setSize(720, 780);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
