package com.appkitbox.winui4k.swing_to_winui4k.jbutton.background;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** 1.13 Run long tasks in the background (Swing). */
public class BackgroundSwing {
    private static String fetch() throws InterruptedException {
        Thread.sleep(2000);
        return "Download complete";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("1.13 Background Work (Swing)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JLabel status = new JLabel();
            JButton download = new JButton("Download");
            download.addActionListener(e -> {
                download.setEnabled(false);
                status.setText("Downloading...");
                new SwingWorker<String, Void>() {
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
