package com.appkitbox.winui4k.swing_to_winui4k.jbutton.background;

import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WFlowLayout;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WSwingWorker;
import com.appkitbox.winui4k.WinUiUtilities;

/** 1.13 Run long tasks in the background (WinUI4K). */
public class BackgroundWinUI4K {
    private static String fetch() throws InterruptedException {
        Thread.sleep(2000);
        return "Download complete";
    }

    public static void main(String[] args) {
        WinUiUtilities.invokeLater(() -> {
            WFrame frame = new WFrame("1.13 Background Work (WinUI4K)");
            frame.setLayout(new WFlowLayout());

            WLabel status = new WLabel();
            WButton download = new WButton("Download");
            download.addActionListener(e -> {
                download.setEnabled(false);
                status.setText("Downloading...");
                new WSwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        return fetch();                       // runs on another thread
                    }

                    @Override
                    protected void done() {                   // runs on the UI thread
                        download.setEnabled(true);
                        try {
                            status.setText(get());
                        } catch (Exception ex) {
                            status.setText("Failed: " + ex.getMessage());
                        }
                    }
                }.execute();
            });

            frame.add(download);
            frame.add(status);

            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
