package imaging.util;

public enum RGBA {
    // values represent offset position of a colour byte within a raster matrix
    RED(1),
    GREEN(2),
    BLUE(3),
    ALPHA(4);

    public final int value;

    private RGBA(int value) { this.value = value; }
}
