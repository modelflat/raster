package raster;

import java.awt.*;

public abstract class ColoringRule {

    private int backColor;

    public ColoringRule() {
        backColor = Color.WHITE.getRGB();
    }

    public ColoringRule(int backColor) {
        this.backColor = backColor;
    }

    public ColoringRule(Color backColor) {
        this.backColor = backColor.getRGB();
    }

    public static ColoringRule createDefaultColoringRule() {
        return new ColoringRule(Color.WHITE) {
            @Override
            public int colorFunction(float x, float y) {
                return 0;
            }
        };
    }

    public int getBackColor() {
        return backColor;
    }

    public void setBackColor(int backColor) {
        this.backColor = backColor;
    }

    /**
     * This function is used by <code>RasterPlot</code> class to determine a color of point with
     * coordinates (x, y). It should return an integer which represents this color
     * using ARGB color model (0xAARRGGBB).
     *
     * @param x - x-coordinate
     * @param y - y-coordinate
     * @return Resulting color.
     */
    public abstract int colorFunction(float x, float y);
}
