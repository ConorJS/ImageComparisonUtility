package imaging;

import threading.SynArrayList;
import ui.ProgressBarFeedbackProxy;

import java.io.File;
import java.io.InputStream;

public class ImageLoaderWorker extends Thread {

    private SynArrayList<Sampler> samplersList = null;
    private File pictureFile = null;
    private SamplerConfig samplerConfig;
    private ProgressBarFeedbackProxy progressBarFeedbackProxy;
    private int hash;

    private InputStream inputStream;

    public ImageLoaderWorker(SynArrayList<Sampler> samplersList, File pictureFile, SamplerConfig samplerConfig,
                             ProgressBarFeedbackProxy progressBarFeedbackProxy, int hash) {

        this.samplersList = samplersList;
        this.pictureFile = pictureFile;
        this.samplerConfig = samplerConfig;
        this.progressBarFeedbackProxy = progressBarFeedbackProxy;
        this.hash = hash;
    }

    public ImageLoaderWorker(SynArrayList<Sampler> samplersList, File pictureFile, SamplerConfig samplerConfig,
                             ProgressBarFeedbackProxy progressBarFeedbackProxy, int hash, InputStream inputStream) {

        this(samplersList, pictureFile, samplerConfig, progressBarFeedbackProxy, hash);
        this.inputStream = inputStream;
    }

    @Override
    public void run() {

        Sampler sampler;
        if (this.inputStream != null) {
            sampler = new Sampler(this.pictureFile, this.inputStream);

        } else {
            sampler = new Sampler(this.pictureFile);
        }
        sampler.setFileMdHash(hash);

        // force sampler to calculate fingerprint
        sampler.getFingerprint(this.samplerConfig);
        while (!sampler.fingerprintReady()) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        progressBarFeedbackProxy.incrementProgressBar();
        sampler.clearRaster();
        this.samplersList.add(sampler);
    }

}
