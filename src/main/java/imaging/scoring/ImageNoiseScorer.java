package imaging.scoring;

import imaging.sampler.Sampler;
import imaging.util.PixelUtility;
import imaging.util.RGBA;
import imaging.util.SimpleColor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import main.ApplicationConfig;

import java.util.ArrayList;
import java.util.Collections;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageNoiseScorer {

    public static final ArrayList<String> NOISE_SCORES = new ArrayList<>(Collections.singletonList("wallpaper,score")); // debug only

    // TODO: Refactor this to consider PNG files better, as well as whole application.
    // TODO: The assumption is made that there are three 8-bit layers per image, but with transparent PNGs,
    // TODO: there is also an alpha layer, making four layers.
    public static Double getImageNoiseScore(Sampler sampler) {

        byte[] rasterMatrix = sampler.getPixels();
        long grossScore = 0;

        boolean validRasterArray = false;
        int detectedLayerCount = 0;

        for (Integer validLayerCount : ApplicationConfig.VALID_LAYER_COUNTS) {

            if (rasterMatrix.length == (sampler.getHeight() * sampler.getWidth() * validLayerCount)) {

                validRasterArray = true;
                detectedLayerCount = validLayerCount;

                for (int i = 0; i < rasterMatrix.length - 3; i += 3) {

                    grossScore += getTwoColorDifferenceScore(
                            // left pixel
                            PixelUtility.getByteColor(
                                    rasterMatrix[i + validLayerCount - RGBA.RED.value],
                                    rasterMatrix[i + validLayerCount - RGBA.GREEN.value],
                                    rasterMatrix[i + validLayerCount - RGBA.BLUE.value]),

                            // right pixel
                            PixelUtility.getByteColor(
                                    rasterMatrix[i + (validLayerCount * 2) - RGBA.RED.value],
                                    rasterMatrix[i + (validLayerCount * 2) - RGBA.GREEN.value],
                                    rasterMatrix[i + (validLayerCount * 2) - RGBA.BLUE.value]));
                }
            }
        }

        if (!validRasterArray) {

            throw new RuntimeException("Sampler for " + sampler.getFile().getName() + " has dimensions: "
                    + sampler.getWidth() + "x" + sampler.getHeight()
                    + ", yet has a raster array size of: " + rasterMatrix.length
                    + ", should be size: " + (sampler.getHeight() * sampler.getWidth() * 3));
        }

        return ((double)grossScore) / ((double)(rasterMatrix.length) / ((double)detectedLayerCount));
    }

    /**
     * Calculates a differential score between two colors (pixels),
     * the more similar they are across all color layers, the more different they are
     *
     * @param color1 Color of first pixel
     * @param color2 Color of second pixel
     * @return Calculated differential score
     */
    private static int getTwoColorDifferenceScore(SimpleColor color1, SimpleColor color2) {

        return Math.abs(color1.getRed() - color2.getRed())
                + Math.abs(color1.getGreen() - color2.getGreen())
                + Math.abs(color1.getBlue() - color2.getBlue());
    }

}



