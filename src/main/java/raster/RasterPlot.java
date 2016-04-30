package raster;

import utils.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

/**
 * The <code>RasterPlot</code> class encapsulates such things as plot plane, data series, and coloring 
 * rule for this data series.
 *
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class RasterPlot {
	LinkedList<float[]> chunks;
	ChunkPool pool;
	
	int[] plotPixels;
	private BufferedImage plot;
	
	private Dimension resolution;

	private int maxThreadCount;

	private Logger logger;
	
	private ColoringRule coloringRule;
	private Bounds bounds;
	private float mix, miy, max, may; // trying to save few cycles in directDraw()
	
	/**
	 * Constructor for <code>RasterPlot</code> class.
	 * 
	 * @param maxThreadCount Maximum number of threads, which RasterPlot will use for rendering
	 * images (actually, <code>RasterPlot</code> will <i>always</i> try to use this amount of threads).
	 * @param resolution Initial resolution of plot plane, in pixels.
	 * @param logger <code>util.Logger</code> instance.
	 */
	public RasterPlot(int maxThreadCount, Dimension resolution, Logger logger) {
		this.chunks = new LinkedList<>();
		
		setMaxThreadCount(maxThreadCount);
		setResolution(resolution);
		setLogger(logger);
		setBounds(Bounds.createDefaultBounds());
		setColoringRule(ColoringRule.createDefaultColoringRule());
	}
	
	/**
	 * This function puts a chunk of float data to a render chain.
	 * This happens if and only if the data is representing a set of points,
	 * each one having two coordinates: x and y; otherwise (if length of array
	 * is an odd number) it does nothing
	 * 
	 * @param xy float array {x1, y1, ... xN, yN}
	 */
	public synchronized void putChunk(float[] xy) {
		if (xy.length % 2 == 0) this.chunks.add(xy);
	}
	
	/**
	 * Renders all chunks that currently are in render chain, drawing
	 * each point according to current coloring rule and bounds.
	 */
	public void render() {
		long time = System.nanoTime();
		pool = new ChunkPool(this.chunks.size());
		int threadCount = this.chunks.size() < maxThreadCount ? this.chunks.size() : maxThreadCount;
		info(String.format(
				"Started rendering %d chunks using %d threads.",
				this.chunks.size(), threadCount));
		
		Thread[] threads = new Thread[threadCount];
		
		for (int i = 0; i < threadCount; i++) {
			threads[i] = new Thread(new Plotter(this), "PlotterThread#" + i);
			threads[i].start();
		}
		for (int i = 0; i < threadCount; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				error(e.getMessage());
			}
		}
		plot.flush();
		time = System.nanoTime() - time;
		info(String.format("Rendering finished in %d ms!", time / 1000000));
	}
	
	/**
	 * Fills whole plot plane with colors determined by <code>ColoringRule</code>
	 */
	public void renderSolid() {
		long time = System.nanoTime();
		info(String.format("Rendering solid picture using %d threads", this.maxThreadCount));
		int portion = resolution.height / this.maxThreadCount;
		Thread[] threads = new Thread[4];
		for (int i = 0; i < this.maxThreadCount; i++) {
			final int begin = i*portion;
			final int end = (i+1)*portion;
			threads[i] = new Thread(new Runnable() {
				public void run() {
                    float scaleX = (max - mix) / (float)getResolution().getWidth();
                    float scaleY = (may - miy) / (float)getResolution().getHeight();
					for (int yCoord = begin; yCoord < end; yCoord++) {
						for (int xCoord = 0; xCoord < resolution.width; xCoord++) {
							plotPixels[xCoord + yCoord*resolution.width] =
                                    coloringRule.colorFunction(mix + (float)xCoord*scaleX, -miy - (float)yCoord*scaleY);
						}
			 		}
				}
			});
			threads[i].start();
		}
		for (int i = 0; i < this.maxThreadCount; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				error(e.getMessage());
			}
		}
        plot.flush();
		time = System.nanoTime() - time;
		info(String.format("Rendering finished in %d ms!", time / 1000000));
	}
	
	/**
	 * Draw a point at x,y on plot, using color function defined in <code>ColoringRule</code>. 
	 * Fast enough to be used inside loops.
	 * 
	 * @param x x-coordinate
	 * @param y y-coordinate
	 */
	public void directDraw(float x, float y) {
		if (x > mix && x < max && y > miy && y < may) {
			plotPixels[(int)((x - mix) / (max - mix) * resolution.width) + 
			           (resolution.height - 1 - (int)((y - miy) / (may - miy) * resolution.height)) * resolution.width] = 
			           coloringRule.colorFunction(x, y);
		}
	}
	
	/**
	 * Clears render chain of RasterPlot.
	 */
	public void clearData() {
		this.chunks.clear();
	}
	
	/**
	 * Clears plot with color, specified in field <code>ColoringRule.backColor</code> of current
	 * coloring rule.
	 * 
	 */
	public void clearPlot() {
		long time = System.nanoTime();
		info(String.format("Clearing plot using %d threads", maxThreadCount));
		Thread[] threads = new Thread[maxThreadCount];
		int seg = plot.getHeight() * plot.getWidth() / maxThreadCount;
		for (int i = 0; i < maxThreadCount; i++) {
			final int begin = seg*i;
			final int end = i == (maxThreadCount-1) ? plot.getHeight() * plot.getWidth() : seg*(i+1);
			final int color = coloringRule.getBackColor();
			threads[i] = new Thread(new Runnable() {
				public void run() {
					for (int i = begin; i < end; i++)
						plotPixels[i] = color;
				}
			});
			threads[i].start();
		}
		for (int i = 0; i < maxThreadCount; i++)
			try {
				threads[i].join();
			} catch (InterruptedException e) {
                error(e.getMessage());
			}
		time = System.nanoTime() - time;
		info(String.format("Clearing took %d ms", time / 1000000));
	}
	
	/**
	 * Sets new coloring rule.
	 * 
	 * @param rule New coloring rule
	 */
	public void setColoringRule(ColoringRule rule) {
		this.coloringRule = rule;
	}
	
	/**
	 * @return Current coloring rule.
	 */
	public ColoringRule getColoringRule() {
		return this.coloringRule;
	}
	
	/**
	 * Sets new bounds of the plot plane.
	 * @param bounds new bounds
	 */
	public void setBounds(Bounds bounds) {
		this.bounds = bounds;
		this.max = bounds.getMaxX();
		this.mix = bounds.getMinX();
		this.miy = bounds.getMinY();
		this.may = bounds.getMaxY();
	}
	
	public void reallocImage() {
		if (this.plot != null) 
			plot.getGraphics().dispose();
		this.plot = new BufferedImage(resolution.width, resolution.height, BufferedImage.TYPE_INT_RGB);
		this.plotPixels = ((DataBufferInt)this.plot.getRaster().getDataBuffer()).getData();
	}
	
	/**
	 * @return Current bounds of plot plane.
	 */
	public Bounds getBounds() {
		return this.bounds;
	}
	
	/**
	 * @return Current resolution (in pixels).
	 */
	public final Dimension getResolution() {
		return resolution;
	}
	
	/**
	 * Sets new resolution of plot plane (in pixels).
	 * @param resolution New resolution.
	 */
	public final void setResolution(Dimension resolution) {
		this.resolution = resolution;
		reallocImage();
	}
	
	/**
	 * Returns current <code>utils.Logger</code> instance.
	 * @return <code>utils.Logger</code> instance.
	 */
	public final Logger getLogger() {
		return logger;
	}

	/**
	 * Sets new <code>utils.Logger</code> instance, which can be used to obtain information about
	 * current state, errors, and warnings. 
	 * @param logger New <code>utils.Logger</code> instance.
	 */
	public final void setLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Returns number of threads this instance of <code>RasterPlot</code> can use.
	 * 
	 * @return Number of threads
	 */
	public final int getMaxThreadCount() {
		return maxThreadCount;
	}

	/**
	 * Set maximum number of threads to specified value.
	 * 
	 * @param maxThreadCount new thread limit.
	 */
	public final void setMaxThreadCount(int maxThreadCount) {
		this.maxThreadCount = maxThreadCount;
	}

	/**
	 * @return the plot
	 */
	public BufferedImage getPlot() {
		return plot;
	}

	/**
	 * Saves current plot image to specified file.
	 * 
	 * @param filename file to be written
	 * @param format image format
	 */
	public void saveToFile(String filename, String format) throws IOException {
		long time = System.nanoTime();
		info("Writing current plot image to file (" + filename + ")");
        FileOutputStream out = new FileOutputStream(filename);
        ImageIO.write(this.plot, format, out);
        out.close();
		time = System.nanoTime() - time;
		info("File written (" + filename + ") in " + time / 1000000 + " ms");
	}
	
	void info(Object o) {
		if (this.logger == null) return;
		logger.info(o);
	}
	
	void warn(Object o) {
		if (this.logger == null) return;
		logger.warn(o);
	}
	
	void error(Object o) {
		if (this.logger == null) return;
		logger.error(o);
	}
}
