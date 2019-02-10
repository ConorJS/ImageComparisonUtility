package imaging.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.util.ArrayList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PixelUtility {

    public static Color getPixelColor(byte[] rasterArray, int x, int y, int width, int height, String fileName)
            throws ArrayIndexOutOfBoundsException {

        int _8bitLayerCount = 3;

        // checks for raster arrays representing three 8-bit layers (typical RGB image)
        if (rasterArray.length != (height * width * 3)) {

            // checks for raster arrays representing four 8-bit layers (typical RGBA image, e.g. PNG)
            if (rasterArray.length == (height * width * 4)) {

                _8bitLayerCount = 4;

            } else {
                throw new RuntimeException("Sampler for " + fileName + " has dimensions: "
                        + width + "x" + height
                        + ", yet has a raster array size of: " + rasterArray.length
                        + ", should be size: " + (height * width * 3));
            }
        }

        try {
            int r = rasterArray[((((y * width) + x) * _8bitLayerCount) + (_8bitLayerCount - RGBA.RED.value))];
            int g = rasterArray[((((y * width) + x) * _8bitLayerCount) + (_8bitLayerCount - RGBA.GREEN.value))];
            int b = rasterArray[((((y * width) + x) * _8bitLayerCount) + (_8bitLayerCount - RGBA.BLUE.value))];

            r = (r < 0) ? r + 256 : r;
            g = (g < 0) ? g + 256 : g;
            b = (b < 0) ? b + 256 : b;

            return new Color(r, g, b);

        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Array out of bounds accessing pixel " + x + ", " + y +
                    "Raster is of size: " + rasterArray.length + ". " +
                    "Raster might not be 3 bytes per pixel (transparent PNG?), " +
                    "otherwise raster likely cleared too early.");
        }
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
