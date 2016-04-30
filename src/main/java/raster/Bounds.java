package raster;

@SuppressWarnings({"WeakerAccess", "unused"})
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

	public final float getMinX() {
		return minX;
	}

	public final void setMinX(float minX) {
		this.minX = minX;
	}

	public final float getMaxX() {
		return maxX;
	}

	public final void setMaxX(float maxX) {
		this.maxX = maxX;
	}

	public final float getMinY() {
		return minY;
	}

	public final void setMinY(float minY) {
		this.minY = minY;
	}

	public final float getMaxY() {
		return maxY;
	}

	public final void setMaxY(float maxY) {
		this.maxY = maxY;
	}
	
	public final float[] getAll() {
		return new float[] {this.minX, this.minY, this.maxX, this.maxY};
	}
	
	public final void setAll(float[] bounds) {
		this.minX = bounds[0];
		this.minY = bounds[1];
		this.maxX = bounds[2];
		this.maxY = bounds[3];
	}
}
