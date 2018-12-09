package main;

import filehandling.FileHandlerUtil;
import imaging.Sampler;
import javafx.util.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PixelArtRestorer {

    private static final int BLOCK_SIZE_CHECK_LIMIT = 35;

    private int height;
    private int width;
    private byte[] pixels;

    public PixelArtRestorer(String sourcePath, String destinationPath) {

        File file = new File(sourcePath);

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

    int determineBlockSize() {
        return this.determineBlockSize(this.pixels);
    }

    private int determineBlockSize(byte[] imagePixels) {

        java.util.List<Pair<Integer, Double>> blockSizeColorDivergenceTotals = new ArrayList<>();

        // Try blocks of size 2 x 2 through to size MAX x MAX
        for (int i = 2; i < BLOCK_SIZE_CHECK_LIMIT; i++) {

            double divergenceTotal = 0;
            int blocksChecked = 0;

            // Image height
            for (int j = 0; j < Math.floorDiv(this.height, i); j++) {

                // Image width (each row processed here)
                for (int k = 0; k < Math.floorDiv(this.width, i); k++) {

                    // Get the average of all the pixels in this box
                    divergenceTotal += getMeanDivergenceFromRasterBlock(imagePixels,
                            k*i, j*i, (k*i)+i, (j*i)+i);

                    blocksChecked++;
                }
            }

            blockSizeColorDivergenceTotals.add(new Pair<>(i, divergenceTotal / blocksChecked));
        }

        // best candidate is the block size that produces the least variance from the mean colour for each block
        int bestCandidate = -1;
        double bestCandidatesDivergenceScore = -1;
        for (Pair<Integer, Double> blockSizeColorDivergenceTotal : blockSizeColorDivergenceTotals) {

            // The  <=  comparison is important because it means block size 10 will take
            // precedent over one of its factors (e.g. 5) on the merit of being a larger block size.
            // --
            // A homogenous block of size 10 x 10 will also consist of four homogenous blocks each of size 5 x 5,
            // and 25 homogenous blocks of size 2 x 2 - we can infer from this the block size of the art is 10 x 10
            if ((bestCandidatesDivergenceScore == -1) ||
                    (blockSizeColorDivergenceTotal.getValue() <= bestCandidatesDivergenceScore)) {

                bestCandidate = blockSizeColorDivergenceTotal.getKey();
                bestCandidatesDivergenceScore = blockSizeColorDivergenceTotal.getValue();

            }
        }

        return bestCandidate;
    }

    private double getMeanDivergenceFromRasterBlock(byte[] wholeRaster, int lowerX, int lowerY, int upperX, int upperY) {

        ArrayList<Color> pixelColors = new ArrayList<>();

        for (int i = lowerY; i < upperY; i++) {
            for (int j = lowerX; j < upperX; j++) {
                pixelColors.add(getPixelColor(wholeRaster, j, i));
            }
        }

        Color averageColor = getAverageOfPixels(pixelColors);

        int divergenceTotal = 0;
        for (int i = lowerY; i < upperY; i++) {
            for (int j = lowerX; j < upperX; j++) {
                divergenceTotal += diffRGB(getPixelColor(wholeRaster, j, i), averageColor);
            }
        }

        // mean divergence
        return divergenceTotal / ((upperX - lowerX) * (upperY - lowerY));
    }

    private Color getPixelColor(byte[] rasterArray, int x, int y) {
        // uses class scoped width variable

        try {
            int r = rasterArray[((((y * width) + x) * 3) + RGB.RED.value)];
            int g = rasterArray[((((y * width) + x) * 3) + RGB.GREEN.value)];
            int b = rasterArray[((((y * width) + x) * 3) + RGB.BLUE.value)];

            r = (r < 0) ? r + 256 : r;
            g = (g < 0) ? g + 256 : g;
            b = (b < 0) ? b + 256 : b;

            return new Color(r, g, b);

        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Raster is " + (pixels != null ? "of size: " + pixels.length + ". " : "empty. "));
        }
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

        private RGB(int value) { this.value = value; }
    }

    private int diffRGB(Color color1, Color color2) {
        int diff = 0;
        diff += Math.abs(color1.getRed() - color2.getRed());
        diff += Math.abs(color1.getGreen() - color2.getGreen());
        diff += Math.abs(color1.getBlue() - color2.getBlue());

        return diff;
    }

}
