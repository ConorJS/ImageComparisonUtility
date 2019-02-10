package imaging;

import imaging.sampler.Sampler;
import imaging.util.PixelUtility;
import imaging.util.SimpleColor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageNoiseScorer {

    public static final ArrayList<String> noiseScores = new ArrayList<>(Collections.singletonList("wallpaper,score")); // debug only

    public static Double getImageNoiseScore(Sampler sampler) {
        byte[] pixels = sampler.getPixels();

        if (pixels.length != (sampler.getHeight() * sampler.getWidth() * 3)) {

            throw new RuntimeException("Sampler for " + sampler.getFile().getName() + " has dimensions: "
                    + sampler.getWidth() + "x" + sampler.getHeight()
                    + ", yet has a raster array size of: " + pixels.length
                    + ", should be size: " + (sampler.getHeight() * sampler.getWidth() * 3) );
        }

        long grossScore = 0;
        for (int i = 0; i < pixels.length - 3; i += 3) {
            grossScore += getTwoColorDifferenceScore(
                    // left pixel
                    PixelUtility.getByteColor(pixels[i], pixels[i+1], pixels[i+2]),
                    PixelUtility.getByteColor(pixels[i+3], pixels[i+4], pixels[i+5]));
        }

        return ((double)grossScore) / ((double)pixels.length);
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



