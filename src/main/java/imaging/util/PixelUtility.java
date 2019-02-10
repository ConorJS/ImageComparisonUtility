package imaging.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.util.ArrayList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PixelUtility {

    public static Color getPixelColor(byte[] rasterArray, int x, int y, int width) throws ArrayIndexOutOfBoundsException {
        try {
            int r = rasterArray[((((y * width) + x) * 3) + RGB.RED.value)];
            int g = rasterArray[((((y * width) + x) * 3) + RGB.GREEN.value)];
            int b = rasterArray[((((y * width) + x) * 3) + RGB.BLUE.value)];

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

    private enum RGB {
        BLUE(0),
        GREEN(1),
        RED(2);

        private final int value;

        private RGB(int value) { this.value = value; }
    }

}
