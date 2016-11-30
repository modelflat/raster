package raster;

public class Bounds {
    private float minX;
    private float maxX;
    private float minY;
    private float maxY;

    public Bounds(float[] bounds) {
        this(bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    public Bounds(float xmin, float ymin, float xmax, float ymax) {
        this.minX = xmin;
        this.minY = ymin;
        this.maxX = xmax;
        this.maxY = ymax;
    }

    public static Bounds createDefaultBounds() {
        return new Bounds(-1, -1, 1, 1);
    }

    public float getSpanX() {
        return maxX - minX;
    }

    public float getSpanY() {
        return maxY - minY;
    }

    public float getMinX() {
        return minX;
    }

    public void setMinX(float minX) {
        this.minX = minX;
    }

    public float getMaxX() {
        return maxX;
    }

    public void setMaxX(float maxX) {
        this.maxX = maxX;
    }

    public float getMinY() {
        return minY;
    }

    public void setMinY(float minY) {
        this.minY = minY;
    }

    public float getMaxY() {
        return maxY;
    }

    public void setMaxY(float maxY) {
        this.maxY = maxY;
    }

    public float[] getAll() {
        return new float[]{this.minX, this.minY, this.maxX, this.maxY};
    }

    public void setAll(float[] bounds) {
        this.minX = bounds[0];
        this.minY = bounds[1];
        this.maxX = bounds[2];
        this.maxY = bounds[3];
    }

    @Override
    public String toString() {
        return String.format("Bounds<(%f,%f), (%f,%f)>", minX, minY, maxX, maxY);
    }
}
