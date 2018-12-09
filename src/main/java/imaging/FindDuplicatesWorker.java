package imaging;

import ui.UI;

public class FindDuplicatesWorker extends Thread {

    private UI ui;
    private String path;

    public FindDuplicatesWorker(UI ui, String path) {
        this.ui = ui;
        this.path = path;
    }

    @Override
    public void run() {
        ui.performDuplicateSearch(this.path);
    }

}
