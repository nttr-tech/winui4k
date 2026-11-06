package com.appkitbox.winui4k.swing_to_winui4k.jbutton;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WinUiUtilities;
import java.util.concurrent.CompletableFuture;

/** 1.13 Running a long task in the background (WinUI4K). */
public class BackgroundWinUI4K {
    private static String fetch() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        return "The download finished";
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.13 Background work (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel status = new WLabel();
            WButton download = new WButton("Download");
            download.addActionListener(e -> {
                download.setEnabled(false);
                status.setText("Downloading...");
                CompletableFuture
                    .supplyAsync(() -> fetch())               // Runs on another thread
                    .whenComplete((result, error) -> WinUiUtilities.invokeLater(() -> {   // Back to the UI thread
                        download.setEnabled(true);
                        status.setText(error == null ? result : "Failed: " + error.getMessage());
                    }));
            });

            frame.add(download);
            frame.add(status);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
