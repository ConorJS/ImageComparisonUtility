package ui;

import imaging.ImageComparisonUtility;
import imaging.threading.FindDuplicatesWorker;
import imaging.util.SimplePair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;


public class UI {

    private static final String DEFAULT_PATH = "C:\\Users\\Conor\\Pictures\\Wallpaper";

    private ImageComparisonUtility app;

    private final JFrame windowFrame = new JFrame("Image Comparison Utility");

    private final JPanel containerTop = new JPanel();

    private final JPanel containerMiddleText = new JPanel();
    private final JScrollPane containerMiddle_ScrollableWrapper = new JScrollPane(containerMiddleText);
    private final JLabel containerMiddleText_Label = new JLabel();

    private final JPanel containerBottom = new JPanel();
    private final JTextField pathInputField = new JTextField(48);
    private final JButton calculateButton = new JButton("Find similar images");

    private JProgressBar loadingBar = null;
    private int progress = 0;

    private String uiText = "";

    public UI(ImageComparisonUtility app) { // set up
        this.app = app;

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (ClassNotFoundException | UnsupportedLookAndFeelException |
                InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        this.windowFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.windowFrame.setMinimumSize(new Dimension(850, 375));

        this.windowFrame.getContentPane().add(this.containerTop, BorderLayout.NORTH);

        this.containerMiddleText_Label.setFont(new Font("Segoe UI", 0, 12));
        this.containerMiddleText.add(this.containerMiddleText_Label, BorderLayout.NORTH);
        this.containerMiddle_ScrollableWrapper.getVerticalScrollBar().setUnitIncrement(24);
        this.windowFrame.getContentPane().add(this.containerMiddle_ScrollableWrapper, BorderLayout.CENTER);

        this.calculateButton.setFont(new Font("Segoe UI", 0, 14));
        this.calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clickCalculateButton();
            }
        });

        this.windowFrame.add(this.containerBottom, BorderLayout.SOUTH);
        this.containerBottom.add(pathInputField, BorderLayout.WEST);
        this.pathInputField.setText(DEFAULT_PATH);
        this.containerBottom.add(calculateButton, BorderLayout.CENTER);
    }

    public void showUI() {
        this.windowFrame.pack();
        this.windowFrame.setVisible(true);
    }

    private void clickCalculateButton() {
        String tryPath = pathInputField.getText();

        File tryFile = new File(tryPath);

        this.clearTextOfUI();
        if (tryFile.exists()) {

            if (tryFile.isDirectory()) {
                if (this.loadingBar != null) {
                    this.containerTop.remove(this.loadingBar);
                    this.progress = 0;
                }
                this.loadingBar = new JProgressBar(0, tryFile.listFiles().length);
                this.containerTop.add(this.loadingBar, BorderLayout.CENTER);
                this.loadingBar.setStringPainted(true);

                FindDuplicatesWorker findDuplicatesWorker = new FindDuplicatesWorker(this, tryPath);
                findDuplicatesWorker.start();

            } else {
                this.appendUIText("Path: \"" + tryPath + "\" is not a directory.", true);
            }
        } else {
            this.appendUIText("Path: \"" + tryPath + "\" doesn't exist.", true);
        }
    }

    public void performDuplicateSearch(String path) {

        List<SimplePair<SimplePair<String, String>, Double>> duplicates = app.runImageComparisonForPath(path);

        this.appendUIText("<b>Total duplicates: " + duplicates.size() + "</b>", true);
        this.appendUIText("", true);

        for (SimplePair<SimplePair<String, String>, Double> duplicate : duplicates) {

            String file1 = duplicate.getKey().getKey();
            String file2 = duplicate.getKey().getValue();
            Double diff = duplicate.getValue();

            if (diff.equals(0.0)) {
                this.appendUIText(file1 + " duplicates " + file2 +
                        "    <b><font color=\"red\">Binary same</font color></b>", true);

            } else {
                this.appendUIText(file1 + " duplicates " + file2, true);
            }
        }
    }

    public void clearTextOfUI() {
        this.setTextOfUI("");
    }

    public void setTextOfUI(String text) {
        this.uiText = text;

        this.containerMiddleText_Label.setText("<html>" + this.uiText + "</html>");
    }

    public void appendUIText(Object text, boolean newLine) {
        String str = text.toString();
        this.setTextOfUI(this.uiText + (newLine ? "<br>" : "") + str);
    }

    protected void incrementProgress(int blocks) {
        this.progress += blocks;
        this.loadingBar.setValue(this.progress);
    }
}
