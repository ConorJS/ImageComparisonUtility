package imaging;

import javafx.util.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class SamplerHighMem {

    private File file;
    public File getFile() {
        return this.file;
    }

    private BufferedImage image;
    private int height;
    private int width;

    // cached fingerprint
    private List<Pair<Point, Color>> fingerprint = null;

    // parameters that were called to produce the cached fingerprint
    private int accuracyX_cached;
    private int accuracyY_cached;
    private int passesPerBlock_cached;

    public SamplerHighMem(File file) {
        this.file = file;

        try {

            this.image = ImageIO.read(file);
            this.height = this.image.getHeight();
            this.width = this.image.getWidth();

        } catch (IOException e) {
            System.out.print("Failed to create an ImageInputStream from file: " + file.getName());
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.print("Failed to create an ImageInputStream from file: " + file.getName());
            e.printStackTrace();
        }
    }

    public List<Pair<Point, Color>> getFingerprint(int accuracyX, int accuracyY, int passesPerBlock) {

        if (!((this.accuracyX_cached == accuracyX) && (this.accuracyY_cached == accuracyY)
                && (this.passesPerBlock_cached == passesPerBlock) && (this.fingerprint != null))) {

            this.calculateFingerprint(accuracyX, accuracyY, passesPerBlock);
            this.accuracyX_cached = accuracyX;
            this.accuracyY_cached = accuracyY;
            this.passesPerBlock_cached = passesPerBlock;

        }
        // else: reusing cached fingerprint

        return this.fingerprint;
    }

    public void calculateFingerprint(int accuracyX, int accuracyY, int passesPerBlock) {

        accuracyX = accuracyX <= 100 ? accuracyX : 100;
        accuracyY = accuracyY <= 100 ? accuracyY : 100;

        double stepSizeX = width / accuracyX;
        double stepSizeY = height / accuracyY;

        byte[] rasterMatrix;

        rasterMatrix = getPixelMatrix_MethodOne();

        ArrayList<Pair<Point, Color>> blockAverages = new ArrayList<>();

        // rectangle height = stepSizeX
        // start point = stepSizeX / 2, stepSizeY / 2
        // end point = width - (stepSize / 2), height - (stepSize / 2)
        for (double i = (stepSizeY / 2); i <= (height - (stepSizeY / 2)); i += stepSizeY) {
            for (double j = (stepSizeX / 2); j <= (width - (stepSizeX / 2)); j += stepSizeX) {

                ArrayList<Color> pixels = new ArrayList<>();

                for (int k = 0; k < passesPerBlock; k++) {

                    // get a pixel from the block being examined
                    pixels.add(getPixelColor(rasterMatrix,
                            (int) ((j + ((Math.random() - 0.5) * stepSizeX))),
                            (int) ((i + ((Math.random() - 0.5) * stepSizeY)))));
                }

                // get the 'average' color value for the subject block
                Color averagePixel = getAverageOfPixels(pixels);
                blockAverages.add(new Pair(new Point((int)j, (int)i), averagePixel));

                // TODO: more comprehensive 'averaging' i.e. average of all 'mostly-<color>' pixels
                // TODO: so we would look at the reddish pixels separately to the greenish pixels, for example
            }
        }

        this.fingerprint = blockAverages;
    }

    // ** Reusable **
    // Get the pixel array (byte form - RGB values start at 0, go to 127, rollover to -128 and end at -1)
    private byte[] getPixelMatrix_MethodOne() {

        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        return pixels;
    }

    private Color getPixelColor(byte[] rasterArray, int x, int y) {

        int r = rasterArray[((((y * width) + x) * 3) + RGB.RED.value)];
        int g = rasterArray[((((y * width) + x) * 3) + RGB.GREEN.value)];
        int b = rasterArray[((((y * width) + x) * 3) + RGB.BLUE.value)];

        r = (r < 0) ? r + 256 : r;
        g = (g < 0) ? g + 256 : g;
        b = (b < 0) ? b + 256 : b;

        return new Color(r, g, b);
    }

    private Color getAverageOfPixels(ArrayList<Color> pixels) {
        double r = 0;
        double g = 0;
        double b = 0;

        for (Color pixel : pixels) {
            r += pixel.getRed();
            g += pixel.getGreen();
            b += pixel.getBlue();
        }

        r /= pixels.size();
        g /= pixels.size();
        b /= pixels.size();

        return new Color((int)r, (int)g, (int)b);
    }

    private enum RGB {
        BLUE(0),
        GREEN(1),
        RED(2);

        private final int value;

        private RGB(int value) {
            this.value = value;
        }
    }
}
