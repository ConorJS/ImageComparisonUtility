package imaging.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import main.ApplicationConfig;

import java.awt.*;
import java.util.ArrayList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PixelUtility {

    // TODO: Refactor to use SimpleColor? Should also address performance concern in Sampler#calculateFingerprint
    public static Color getPixelColor(byte[] rasterMatrix, int x, int y, int width, int height, String fileName)
            throws ArrayIndexOutOfBoundsException {

        int detectedLayerCount = 0;
        boolean validRasterArray = false;
        Color color = null;
        for (Integer validLayerCount : ApplicationConfig.VALID_LAYER_COUNTS) {

            if (rasterMatrix.length == (height * width * validLayerCount)) {

                validRasterArray = true;
                detectedLayerCount = validLayerCount;

                try {
                    int r = rasterMatrix[((((y * width) + x) * detectedLayerCount) + (detectedLayerCount - RGBA.RED.value))];
                    int g = rasterMatrix[((((y * width) + x) * detectedLayerCount) + (detectedLayerCount - RGBA.GREEN.value))];
                    int b = rasterMatrix[((((y * width) + x) * detectedLayerCount) + (detectedLayerCount - RGBA.BLUE.value))];

                    r = (r < 0) ? r + 256 : r;
                    g = (g < 0) ? g + 256 : g;
                    b = (b < 0) ? b + 256 : b;

                    color = new Color(r, g, b);

                } catch (ArrayIndexOutOfBoundsException e) {

                    throw new ArrayIndexOutOfBoundsException("Array out of bounds accessing pixel " + x + ", " + y +
                            "Raster likely cleared too early.");
                }
            }
        }

        if (!validRasterArray) {
            throw new RuntimeException("Sampler for " + fileName + " has dimensions: "
                    + width + "x" + height
                    + ", yet has a raster array size of: " + rasterMatrix.length
                    + ", should be size: " + (height * width * 3));
        }

        return color;
    }

    public static SimpleColor getByteColor(byte rb, byte gb, byte bb) {
        return new SimpleColor(rb, gb, bb);
    }

    public static Color getAverageOfPixels(ArrayList<Color> pixels) {
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

}
