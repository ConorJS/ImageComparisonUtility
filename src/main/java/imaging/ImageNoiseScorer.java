package imaging;

import imaging.sampler.Sampler;
import imaging.util.PixelUtility;
import imaging.util.RGBA;
import imaging.util.SimpleColor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageNoiseScorer {

    public static final ArrayList<String> NOISE_SCORES = new ArrayList<>(Collections.singletonList("wallpaper,score")); // debug only

    // 3 layers is the most common, valid case (RGB), 4 occurs sometimes as well (RGBA)
    // TODO: Refactor so that this is in a common location for all application usages,
    // TODO: this needs to be available PixelUtility#getPixelColor
    private static final ArrayList<Integer> VALID_LAYER_COUNTS = new ArrayList<>(Arrays.asList(3, 4));

    // TODO: Refactor this to consider PNG files better, as well as whole application.
    // TODO: The assumption is made that there are three 8-bit layers per image, but with transparent PNGs,
    // TODO: there is also an alpha layer, making four layers.
    public static Double getImageNoiseScore(Sampler sampler) {

        byte[] rasterMatrix = sampler.getPixels();
        long grossScore = 0;

        boolean validRasterArray = false;
        int detectedLayerCount = 0;

        for (Integer validLayerCount : VALID_LAYER_COUNTS) {

            if (rasterMatrix.length == (sampler.getHeight() * sampler.getWidth() * validLayerCount)) {

                validRasterArray = true;
                detectedLayerCount = validLayerCount;

                for (int i = 0; i < rasterMatrix.length - 3; i += 3) {

                    grossScore += getTwoColorDifferenceScore(
                            // left pixel
                            PixelUtility.getByteColor(
                                    rasterMatrix[validLayerCount - RGBA.RED.value],
                                    rasterMatrix[validLayerCount - RGBA.GREEN.value],
                                    rasterMatrix[validLayerCount - RGBA.BLUE.value]),

                            // right pixel
                            PixelUtility.getByteColor(
                                    rasterMatrix[(validLayerCount * 2) - RGBA.RED.value],
                                    rasterMatrix[(validLayerCount * 2) - RGBA.GREEN.value],
                                    rasterMatrix[(validLayerCount * 2) - RGBA.BLUE.value]));
                }
            }
        }

        if (!validRasterArray) {

            throw new RuntimeException("Sampler for " + sampler.getFile().getName() + " has dimensions: "
                    + sampler.getWidth() + "x" + sampler.getHeight()
                    + ", yet has a raster array size of: " + rasterMatrix.length
                    + ", should be size: " + (sampler.getHeight() * sampler.getWidth() * 3));
        }

        return ((double)grossScore) / ((double)rasterMatrix.length / detectedLayerCount);
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



