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
 */

public class RasterPlot {

    public enum LabelPosition {UPPER_LEFT, UPPER_RIGHT, CENTER, BOTTOM_LEFT, BOTTOM_RIGHT}

    LinkedList<float[]> chunks;
    WorkPool pool;
    int[] plotPixels;

    private BufferedImage plot;
    private Dimension resolution;
    private Font labelFont = new Font("Times New Roman", Font.ROMAN_BASELINE, 20);
    private Color labelColor = Color.WHITE;

    private int maxThreadCount;
    private int imageType;

    private Logger logger;

    private ColoringRule coloringRule;
    private Bounds bounds;

    private double scaleX, scaleY;

    /**
     * Constructor for <code>RasterPlot</code> class.
     *
     * @param resolution Resolution of plot plane image, in pixels.
     */
    public RasterPlot(Dimension resolution) {
        this(resolution, Bounds.createDefaultBounds(), ColoringRule.createDefaultColoringRule());
    }

    /**
     * Constructor for <code>RasterPlot</code> class.
     *
     * @param resolution   Resolution of plot plane image, in pixels.
     * @param bounds       Bounds of the plot plane.
     * @param coloringRule Coloring rule.
     */
    public RasterPlot(Dimension resolution, Bounds bounds, ColoringRule coloringRule) {
        this(Runtime.getRuntime().availableProcessors(), resolution, bounds, coloringRule, BufferedImage.TYPE_INT_ARGB,
                new Logger(System.out, Logger.Level.NOTHING));
    }

    /**
     * Constructor for <code>RasterPlot</code> class.
     *
     * @param resolution Resolution of plot plane image, in pixels
     * @param imageType Image type of plot. Valid values are all of <code>BufferedImage.TYPE_INT_*</code>.
     */
    public RasterPlot(Dimension resolution, int imageType) {
        this(Runtime.getRuntime().availableProcessors(), resolution,
                Bounds.createDefaultBounds(), ColoringRule.createDefaultColoringRule(), imageType,
                new Logger(System.out, Logger.Level.NOTHING));
    }

    /**
     * Constructor for <code>RasterPlot</code> class.
     *
     * @param maxThreadCount Maximum number of threads, which RasterPlot will use for rendering
     *                       images (actually, <code>RasterPlot</code> will <i>always</i> try to use this amount of threads).
     * @param resolution     Resolution of plot plane image, in pixels.
     * @param bounds         Bounds of coordinate plane.
     * @param coloringRule   Coloring rule.
     * @param imageType      Image type of plot. Valid values are all of <code>BufferedImage.TYPE_INT_*</code>.
     * @param logger         <code>utils.Logger</code> instance used for logging results.
     */
    public RasterPlot(int maxThreadCount,
                      Dimension resolution,
                      Bounds bounds,
                      ColoringRule coloringRule,
                      int imageType,
                      Logger logger) {
        this.chunks = new LinkedList<>();
        this.imageType = imageType;
        setMaxThreadCount(maxThreadCount);
        setResolution(resolution);
        setLogger(logger);
        setBounds(bounds);
        setColoringRule(coloringRule);
    }

    /**
     * This function puts a chunk of float data to a render chain.
     * This happens if and only if the data is representing a set of points,
     * each one having two coordinates: x and y; otherwise (if length of array
     * is an odd number) it does nothing
     *
     * @param xy float array {x1, y1, ... xN, yN}
     * @return this
     */
    public synchronized RasterPlot putChunk(float[] xy) {
        if (xy.length % 2 == 0) this.chunks.add(xy);
        return this;
    }

    /**
     * Renders all chunks that currently are in render chain, drawing
     * each point according to current coloring rule and bounds.
     *
     * @return this
     */
    public RasterPlot renderChunks() {
        // TODO: move this check into render()
        int threadCount = this.chunks.size() < maxThreadCount ? this.chunks.size() : maxThreadCount;
        logger.info(String.format(
                "Started rendering %d chunks using %d threads.",
                this.chunks.size(), threadCount));
        long timing = render(threadCount, Plotter.Mode.CHUNKS);
        logger.info(String.format("Rendering finished in %d ms!", timing));
        return this;
    }

    /**
     * Fills whole plot plane with colors determined by <code>ColoringRule</code>
     *
     * @return this
     */
    public RasterPlot renderSolid() {
        logger.info(String.format("Rendering solid picture using %d threads", this.maxThreadCount));
        long timing = render(maxThreadCount, Plotter.Mode.SOLID);
        logger.info(String.format("Rendering finished in %d ms!", timing));
        return this;
    }

    /**
     * Clears render chain of RasterPlot.
     *
     * @return this
     */
    public RasterPlot clearData() {
        this.chunks.clear();
        return this;
    }

    /**
     * Clears plot with color, specified in field <code>ColoringRule.backColor</code> of current
     * coloring rule.
     *
     * @return this
     */
    public RasterPlot clearPlot() {
        logger.info(String.format("Clearing plot using %d threads", maxThreadCount));
        long timing = render(maxThreadCount, Plotter.Mode.CLEAR);
        logger.info(String.format("Clearing took %d ms", timing));
        return this;
    }

    /**
     * Sets new coloring rule.
     *
     * @param rule New coloring rule
     * @return this
     */
    public RasterPlot setColoringRule(ColoringRule rule) {
        this.coloringRule = rule;
        return this;
    }

    /**
     * @return Current coloring rule.
     */
    public ColoringRule getColoringRule() {
        return this.coloringRule;
    }

    /**
     * Sets new bounds of the plot plane.
     *
     * @param bounds new bounds
     * @return this
     */
    public RasterPlot setBounds(Bounds bounds) {
        this.bounds = bounds;
        if (resolution != null) calculateScales();
        return this;
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
    public Dimension getResolution() {
        return resolution;
    }

    /**
     * Sets new resolution of plot plane (in pixels).
     *
     * @param resolution New resolution.
     * @return this
     */
    public RasterPlot setResolution(Dimension resolution) {
        this.resolution = resolution;
        reallocImage();
        if (bounds != null) calculateScales();
        return this;
    }

    /**
     * Returns current <code>utils.Logger</code> instance.
     *
     * @return <code>utils.Logger</code> instance.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Sets new <code>utils.Logger</code> instance, which can be used to obtain information about
     * current state, errors, and warnings.
     *
     * @param logger New <code>utils.Logger</code> instance.
     * @return this
     */
    public RasterPlot setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Returns number of threads this instance of <code>RasterPlot</code> can use.
     *
     * @return Number of threads
     */
    public int getMaxThreadCount() {
        return maxThreadCount;
    }

    /**
     * Set maximum number of threads to specified value.
     *
     * @param maxThreadCount new thread limit.
     * @return this
     */
    public RasterPlot setMaxThreadCount(int maxThreadCount) {
        this.maxThreadCount = maxThreadCount;
        return this;
    }

    /**
     * @return plot plane image.
     */
    public BufferedImage getPlot() {
        return plot;
    }

    /**
     * Saves current plot image to specified file.
     *
     * @param filename file to be written
     * @param format   image format
     * @return this
     */
    public RasterPlot saveToFile(String filename, String format) throws IOException {
        long time = System.nanoTime();
        logger.info("Writing current plot image to file (" + filename + ")");
        FileOutputStream out = new FileOutputStream(filename);
        ImageIO.write(this.plot, format, out);
        out.close();
        time = System.nanoTime() - time;
        logger.info("File written (" + filename + ") in " + uToMs(time) + " ms");
        return this;
    }

    /**
     * Draws a text string at the specified location on plot, using current <code>Font</code> and <code>Color</code>, specified for
     * this instance of <code>RasterPlot</code>. Default font is Times New Roman - size 20, default color is white.
     * Might be very slow for huge images.
     *
     * @param text     Text string to be drawn
     * @param position Label position
     * @return this
     */
    public RasterPlot drawLabel(String text, LabelPosition position) {
        logger.info(String.format("Drawing text at (%s) with font %s, color %d",
                position.toString(), labelFont.getName(), labelColor.getRGB()));
        long time = System.nanoTime();
        Graphics2D g2d = plot.createGraphics();
        g2d.setFont(labelFont);
        g2d.setColor(labelColor);
        int strWidth = g2d.getFontMetrics().stringWidth(text);
        int strHeight = g2d.getFontMetrics().getHeight();
        int x, y;
        switch (position) {
            case BOTTOM_LEFT:
                x = 0;
                y = resolution.height - strHeight;
                break;
            case BOTTOM_RIGHT:
                x = resolution.width - strWidth;
                y = resolution.height - strHeight;
                break;
            case CENTER:
                x = (resolution.width - strWidth) / 2;
                y = (resolution.height - strHeight) / 2;
                break;
            case UPPER_RIGHT:
                x = resolution.width - strWidth;
                y = strHeight;
                break;
            case UPPER_LEFT:
            default:
                x = 0;
                y = strHeight;
        }
        g2d.drawString(text, x, y);
        logger.info(String.format("Text drawn in %d ms", uToMs(System.nanoTime() - time)));
        return this;
    }

    /**
     * Draws a text string at the specified location on plot, using current <code>Font</code> and <code>Color</code>, specified for
     * this instance of <code>RasterPlot</code>. Default font is Times New Roman - size 20, default color is white.
     * Might be very slow for huge images.
     *
     * @param text Text string to be drawn
     * @param x    x-coordinate in plot plane coordinates
     * @param y    y-coordinate in plot plane coordinates
     * @return this
     */
    public RasterPlot drawLabel(String text, float x, float y) {
        long time = System.nanoTime();
        logger.info(String.format("Drawing text at (%f, %f) with font %s, color %d",
                x, y, labelFont.getName(), labelColor.getRGB()));
        Point pixelCoord = planeToPixel(x, y);
        Graphics2D g2d = plot.createGraphics();
        g2d.setColor(labelColor);
        g2d.setFont(labelFont);
        g2d.drawString(text, pixelCoord.x, pixelCoord.y);
        logger.info(String.format("Text drawn in %d ms", uToMs(System.nanoTime() - time)));
        return this;
    }

    /**
     * Get current label font.
     *
     * @return Font
     */
    public Font getLabelFont() {
        return labelFont;
    }

    /**
     * Set new font for labels.
     *
     * @param labelFont - new Font
     * @return this
     */
    public RasterPlot setLabelFont(Font labelFont) {
        this.labelFont = labelFont;
        return this;
    }

    /**
     * Get current label color.
     *
     * @return Color
     */
    public Color getLabelColor() {
        return labelColor;
    }

    /**
     * Set new label Color.
     *
     * @param labelColor new color
     * @return this;
     */
    public RasterPlot setLabelColor(Color labelColor) {
        this.labelColor = labelColor;
        return this;
    }

    private void reallocImage() {
        if (this.plot != null)
            plot.getGraphics().dispose();
        this.plot = new BufferedImage(resolution.width, resolution.height, imageType);
        this.plotPixels = ((DataBufferInt) this.plot.getRaster().getDataBuffer()).getData();
    }

    private void calculateScales() {
        scaleX = (bounds.getMaxX() - bounds.getMinX()) / resolution.getWidth();
        scaleY = (bounds.getMaxY() - bounds.getMinY()) / resolution.getHeight();
    }

    private Point planeToPixel(double x, double y) {
        return new Point(
                (int) ((x - bounds.getMinX()) / scaleX),
                (int) (resolution.getHeight() - 1 - ((y - bounds.getMinY()) / scaleY)));
    }

    private Point pixelToPlane(int x, int y) {
        Point result = new Point();
        result.setLocation(bounds.getMinX() + (double) x * scaleX, -bounds.getMinY() - (double) y * scaleY);
        return result;
    }

    /**
     * Main render function.
     *
     * @return time spent on rendering in ms.
     */
    private long render(int threadCount, Plotter.Mode mode) {
        long time = System.nanoTime();
        // determine work size
        int workSize;
        switch (mode) {
            case CLEAR:
            case SOLID:
                workSize = resolution.height;
                break;
            case CHUNKS:
                workSize = this.chunks.size();
                break;
            default:
                logger.error("Cannot determine workSize: unknown mode");
                return 0;
        }
        // create work pool
        pool = new WorkPool(workSize);
        // start threads
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(new Plotter(this, mode), "PlotterThread#" + i);
            threads[i].start();
        }
        // wait for threads to complete
        for (int i = 0; i < threadCount; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
        plot.flush();
        return uToMs(System.nanoTime() - time);
    }

    long uToMs(long time) {
        return time / 1000000;
    }

    double getScaleY() {
        return scaleY;
    }

    double getScaleX() {
        return scaleX;
    }
}
