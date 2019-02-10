package imaging.sampler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imaging.ImageNoiseScorer;
import imaging.util.PixelUtility;
import imaging.util.SimpleColor;
import imaging.util.SimplePair;
import lombok.Getter;
import lombok.Setter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

// TODO: Simplify constructor/accessor use
// TODO: (do we always get/set things like fingerprint, noiseScore and hash at the same time?)
public class Sampler {

    @JsonIgnore
    @Getter
    @Setter
    private File file;

    @JsonIgnore
    @Getter
    private byte[] pixels;

    @Getter
    @Setter
    private int fileMdHash;

    @Getter
    private int height;
    @Getter
    private int width;

    // cached fingerprint - this should be persisted to JSON
    private List<SimplePair<Point, SimpleColor>> fingerprint = null;

    @Getter
    @Setter
    private Double noiseScore = null;

    // Debug
    // TODO: Remove safely
    @JsonIgnore
    private int getFingerprintRequestCount = 0;

    // parameters that were called to produce the cached fingerprint - this should be persisted to JSON
    @Getter
    private SamplerConfig samplerConfig_cached;

    public Sampler(File file, InputStream inputStream) {
        this.file = file;
        try {
            BufferedImage image = ImageIO.read(inputStream);

            this.height = image.getHeight();
            this.width = image.getWidth();
            this.pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        } catch (IOException | NullPointerException e) {
            System.out.print("Failed to create an ImageInputStream from file: " + file.getName());
            e.printStackTrace();
        }
    }

    public Sampler(File file) {
        // TODO: Reduce this, does same thing as File, Inputstream constructor
        this.file = file;
        try {
            BufferedImage image = ImageIO.read(file);

            this.height = image.getHeight();
            this.width = image.getWidth();
            this.pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        } catch (IOException | NullPointerException e) {
            System.out.print("Failed to create an ImageInputStream from file: " + file.getName());
            e.printStackTrace();
        }
    }

    public Sampler(List<SimplePair<Point, SimpleColor>> fingerprint, SamplerConfig config, Double noiseScore) {
        this.fingerprint = fingerprint;
        this.noiseScore = noiseScore;
        samplerConfig_cached = config;
    }

    public List<SimplePair<Point, SimpleColor>> getFingerprint(SamplerConfig samplerConfig) {
        if (!samplerConfig.equals(this.samplerConfig_cached)) {
            this.calculateFingerprint(samplerConfig);
            this.samplerConfig_cached = samplerConfig;
        }
        // else: reusing cached fingerprint

        return this.fingerprint;
    }

    private void calculateFingerprint(SamplerConfig samplerConfig) {

        int accuracyX = samplerConfig.getAccuracyX();
        int accuracyY = samplerConfig.getAccuracyY();
        int passesPerBlock = samplerConfig.getPassesPerBlock();

        accuracyX = accuracyX <= 100 ? accuracyX : 100;
        accuracyY = accuracyY <= 100 ? accuracyY : 100;

        double stepSizeX = width / accuracyX;
        double stepSizeY = height / accuracyY;

        byte[] rasterMatrix;

        rasterMatrix = getPixelMatrix_MethodOne();

        ArrayList<SimplePair<Point, SimpleColor>> blockAverages = new ArrayList<>();

        // rectangle height = stepSizeX
        // start point = stepSizeX / 2, stepSizeY / 2
        // end point = width - (stepSize / 2), height - (stepSize / 2)
        for (double i = (stepSizeY / 2); i <= (height - (stepSizeY / 2)); i += stepSizeY) {
            for (double j = (stepSizeX / 2); j <= (width - (stepSizeX / 2)); j += stepSizeX) {

                // TODO: Is this causing a lot of slowdown (instantiating Colors)?
                ArrayList<Color> pixels = new ArrayList<>();

                for (int k = 0; k < passesPerBlock; k++) {

                    // get a pixel from the block being examined
                    pixels.add(PixelUtility.getPixelColor(rasterMatrix,
                            (int) ((j + ((Math.random() - 0.5) * stepSizeX))),
                            (int) ((i + ((Math.random() - 0.5) * stepSizeY))), this.width));
                }

                // get the 'average' color value for the subject block
                Color averagePixel = PixelUtility.getAverageOfPixels(pixels);
                blockAverages.add(new SimplePair(new Point((int)j, (int)i), new SimpleColor(averagePixel)));

                // TODO: more comprehensive 'averaging' i.e. average of all 'mostly-<color>' pixels
                // TODO: so we would look at the reddish pixels separately to the greenish pixels, for example
            }
        }

        Double score = ImageNoiseScorer.getImageNoiseScore(this);
        ImageNoiseScorer.NOISE_SCORES.add(this.file.getName() + "," + score);

        this.noiseScore = score;
        this.fingerprint = blockAverages;
    }

    public boolean fingerprintReady() {
        return (this.fingerprint != null);
    }

    private byte[] getPixelMatrix_MethodOne() {
        return pixels;
    }

    public void clearRaster() {
        this.pixels = new byte[0];
    }

    public Sampler copy() {
        Sampler sampler = new Sampler(this.fingerprint, this.samplerConfig_cached, this.noiseScore);
        sampler.setFile(this.file);
        sampler.setFileMdHash(this.fileMdHash);
        return sampler;
    }
}
