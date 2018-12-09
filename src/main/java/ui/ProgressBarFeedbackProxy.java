package ui;

public class ProgressBarFeedbackProxy implements UITaskFeedbackProxy {

    private UI ui;

    public ProgressBarFeedbackProxy (UI ui) {
        this.ui = ui;
    }

    public void incrementProgressBar(int blocks) {
        this.ui.incrementProgress(blocks);
    }

    public void incrementProgressBar() {
        this.ui.incrementProgress(1);
    }

    //debug
    public void setText(String text) {
        this.ui.setTextOfUI(text);
    }
}
