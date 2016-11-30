package raster;

import utils.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The <code>RasterPlot</code> class encapsulates such things as plot plane, data series, and coloring
 * rule for this data series.
 */

public class RasterPlot {

    public enum LabelPosition {UPPER_LEFT, UPPER_RIGHT, CENTER, BOTTOM_LEFT, BOTTOM_RIGHT}

    private ArrayList<float[]> chunks;
    private AtomicInteger pool = new AtomicInteger();
    private int[] plotPixels;

    private BufferedImage plot;
    private Dimension resolution;
    private Font labelFont = new Font("Times New Roman", Font.ROMAN_BASELINE, 20);
    private Color labelColor = Color.WHITE;

    private int maxThreadCount;
    private int imageType;

    private Logger logger; // TODO : remove

    private ColoringRule coloringRule;
    private Bounds bounds;

    private ExecutorService threadPool = Executors.newFixedThreadPool(4);

    /**
     * Constructor for <code>RasterPlot</code> class.
     *
     * @param resolution Resolution of plot plane image, in plotPixels.
     */
    public RasterPlot(Dimension resolution) {
        this(resolution, Bounds.createDefaultBounds(), ColoringRule.createDefaultColoringRule());
    }

    /**
     * Constructor for <code>RasterPlot</code> class.
     *
     * @param resolution   Resolution of plot plane image, in plotPixels.
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
     * @param resolution Resolution of plot plane image, in plotPixels
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
     * @param resolution     Resolution of plot plane image, in plotPixels.
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
        this.chunks = new ArrayList<>();
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
    public RasterPlot renderChunks() throws ExecutionException {
        render(this.chunks.size() < maxThreadCount ? this.chunks.size() : maxThreadCount,
                RenderMode.CHUNKS);
        return this;
    }

    /**
     * Fills whole plot plane with colors determined by <code>ColoringRule</code>
     *
     * @return this
     */
    public RasterPlot renderSolid() throws ExecutionException {
        render(maxThreadCount, RenderMode.SOLID);
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
    public RasterPlot clearPlot() throws ExecutionException {
        render(maxThreadCount, RenderMode.CLEAR);
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
        return this;
    }

    public RasterPlot setBounds(Rectangle coords) {
        int x1 = coords.x;
        int x2 = Math.max(0, coords.x + coords.width);
        int y1 = coords.y;
        int y2 = Math.max(0, coords.y + coords.height);
        return setBounds(boxToBounds(new int[]{
                Math.min(x1, x2),
                Math.min(y1, y2),
                Math.max(x1, x2),
                Math.max(y1, y2)
        }));
    }

    /**
     * @return Current bounds of plot plane.
     */
    public Bounds getBounds() {
        return this.bounds;
    }

    /**
     * @return Current resolution (in plotPixels).
     */
    public Dimension getResolution() {
        return resolution;
    }

    /**
     * Sets new resolution of plot plane (in plotPixels).
     *
     * @param resolution New resolution.
     * @return this
     */
    public RasterPlot setResolution(Dimension resolution) {
        this.resolution = resolution;
        reallocImage();
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
        FileOutputStream out = new FileOutputStream(filename);
        ImageIO.write(this.plot, format, out);
        out.close();
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
        Point pixelCoord = planeToPixel(x, y);
        Graphics2D g2d = plot.createGraphics();
        g2d.setColor(labelColor);
        g2d.setFont(labelFont);
        g2d.drawString(text, pixelCoord.x, pixelCoord.y);
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

    public void drawBox(int[] rect, int color) {
        int xSize = resolution.width;
        int ySize = resolution.height;
        for (int i = 0; i < ySize; i++) {
            //plotPixels.put(rect[0] + i*xSize, color);
            plotPixels[rect[0] + i * xSize] = color;
            //plotPixels.put(rect[2] + i*xSize, color);
            plotPixels[rect[2] + i * xSize] = color;
        }
        for (int i = 0; i < xSize; i++) {
            //plotPixels.put(i + rect[1]*xSize, color);
            plotPixels[i + rect[1] * xSize] = color;
            //plotPixels.put(i + rect[3]*xSize, color);
            plotPixels[i + rect[3] * xSize] = color;
        }
    }

    public boolean applyBoundingBox(double applyThreshold) {
        return applyBoundingBox(applyThreshold, coloringRule.getBackColor());
    }

    public boolean applyBoundingBox(double applyThreshold, int backColor) {
        Bounds newBounds = computeBoundingBox(backColor);
        if (newBounds.getSpanY() / bounds.getSpanY() > applyThreshold ||
                newBounds.getSpanX() / bounds.getSpanX() > applyThreshold) {
            bounds = newBounds;
            return true;
        }
        return false;
    }

    public Bounds computeAndDrawBoundingBox(int boxColor) {
        int[] box = computePixelBoundingBox();
        drawBox(box, boxColor);
        return boxToBounds(box);
    }

    public Bounds computeBoundingBox() {
        return computeBoundingBox(coloringRule.getBackColor());
    }

    public Bounds computeBoundingBox(int backColor) {
        return boxToBounds(computePixelBoundingBox(backColor));
    }

    public int[] computePixelBoundingBox() {
        return computePixelBoundingBox(coloringRule.getBackColor());
    }

    public int[] computePixelBoundingBox(int backColor) {
        int[] boundingBox = new int[4];
        ArrayList<Future<Integer>> futures = new ArrayList<>(4);
        futures.add(threadPool.submit(new BoxSideFinder(FinderMode.MIN_X, backColor)));
        futures.add(threadPool.submit(new BoxSideFinder(FinderMode.MIN_Y, backColor)));
        futures.add(threadPool.submit(new BoxSideFinder(FinderMode.MAX_X, backColor)));
        futures.add(threadPool.submit(new BoxSideFinder(FinderMode.MAX_Y, backColor)));
        int i = 0;
        try {
            for (Future<Integer> f : futures) {
                boundingBox[i++] = f.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return boundingBox;
    }

    public Point planeToPixel(double x, double y) {
        return new Point(
                (int) ((x - bounds.getMinX()) / getScaleX()),
                (int) (resolution.getHeight() - 1 - ((y - bounds.getMinY()) / getScaleY())));
    }

    public float[] pixelToPlane(int x, int y) {
        float[] result = new float[2];
        result[0] = (float) (bounds.getMinX() + getScaleX() * x);
        result[1] = -(float) (bounds.getMinY() + getScaleY() * y);
        return result;
    }

    public void shutdown() {
        threadPool.shutdown();
    }

    private Bounds boxToBounds(int[] box) {
        // thanks to github.com/madstrix for helping with bounds computing optimization
        float h = (float) resolution.getHeight();
        float w = (float) resolution.getWidth();
        float sX = bounds.getSpanX();
        float sY = bounds.getSpanY();
        return new Bounds(
                bounds.getMinX() + ((float) box[0] / w) * sX,
                bounds.getMinY() + (1 - (float) box[3] / h) * sY,
                bounds.getMaxX() - (1 - (float) box[2] / w) * sX,
                bounds.getMaxY() - ((float) box[1] / h) * sY);
    }

    private void reallocImage() {
        if (this.plot != null)
            plot.getGraphics().dispose();
        this.plot = new BufferedImage(resolution.width, resolution.height, imageType);
        this.plotPixels = // IntBuffer.wrap(
                ((DataBufferInt) this.plot.getRaster().getDataBuffer()).getData();//);
    }

    /**
     * Main render function.
     */
    private synchronized void render(int threadCount, RenderMode mode) throws ExecutionException {
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
                workSize = 0;
        }

        // init work pool
        pool.set(workSize);

        // start threads
        ArrayList<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(threadPool.submit(new Plotter(mode)));
        }

        // wait for threads to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException ignored) {
            }
        }

        plot.flush();
    }

    double getScaleX() {
        return bounds.getSpanX() / resolution.getWidth();
    }

    double getScaleY() {
        return bounds.getSpanY() / resolution.getHeight();
    }

    private enum FinderMode {MIN_X, MAX_X, MIN_Y, MAX_Y}

    private enum RenderMode {SOLID, CHUNKS, CLEAR}

    private class BoxSideFinder implements Callable<Integer> {

        private int xSize;
        private int ySize;

        private FinderMode mode;
        private int backColor;

        BoxSideFinder(FinderMode mode, int backColor) {
            this.ySize = resolution.height;
            this.xSize = resolution.width;
            this.mode = mode;
            this.backColor = backColor;
        }

        private int computeXMin() {
            for (int i = 0; i < xSize; ++i) {
                for (int j = ySize - 1; j >= 0; --j) {
                    if (plotPixels[i + j * xSize] != backColor) {
                        //if (plotPixels.get(i + j*xSize) != backColor) {
                        return i;
                    }
                }
            }
            return 0;
        }

        private int computeYMin() {
            for (int i = 0; i < ySize; ++i) {
                for (int j = xSize - 1; j >= 0; --j) {
                    //if (plotPixels.get(j + i*xSize) != backColor) {
                    if (plotPixels[j + i * xSize] != backColor) {
                        return i;
                    }
                }
            }
            return 0;
        }

        private int computeXMax() {
            for (int i = xSize - 1; i >= 0; --i) {
                for (int j = ySize - 1; j >= 0; --j) {
                    // if (plotPixels.get(i + j*xSize) != backColor) {
                    if (plotPixels[i + j * xSize] != backColor) {
                        return i;
                    }
                }
            }
            return xSize - 1;
        }

        private int computeYMax() {
            for (int i = ySize - 1; i >= 0; --i) {
                for (int j = xSize - 1; j >= 0; --j) {
                    // if (plotPixels.get(j + i*xSize) != backColor) {
                    if (plotPixels[j + i * xSize] != backColor) {
                        return i;
                    }
                }
            }
            return ySize - 1;
        }

        @Override
        public Integer call() throws Exception {

            switch (mode) {
                case MIN_X:
                    return computeXMin();
                case MAX_X:
                    return computeXMax();
                case MIN_Y:
                    return computeYMin();
                case MAX_Y:
                    return computeYMax();
            }

            return 0;
        }

    }

    private class Plotter implements Runnable {
        private RenderMode mode;

        Plotter(RenderMode mode) {
            this.mode = mode;
        }

        void renderSolid() {
            // bring variables even closer
            float mix = RasterPlot.this.bounds.getMinX();
            float miy = -RasterPlot.this.bounds.getMinY();

            float scaleX = (float) RasterPlot.this.getScaleX();
            float scaleY = -(float) RasterPlot.this.getScaleY();

            int w = (int) RasterPlot.this.getResolution().getWidth();

            ColoringRule rule = RasterPlot.this.coloringRule;
            int[] plot = RasterPlot.this.plotPixels;

            ///
            while (true) {
                int y = RasterPlot.this.pool.decrementAndGet();
                if (y < 0) {
                    return;
                }
                for (int x = 0; x < w; x++) {
                    //plot.put(x + y * w, rule.colorFunction(mix + (float) x * scaleX, miy + (float) y * scaleY));
                    plot[x + y * w] = rule.colorFunction(mix + (float) x * scaleX, miy + (float) y * scaleY);
                }
            }
            ///
        }

        void renderChunks() {

            float mix = RasterPlot.this.bounds.getMinX();
            float max = RasterPlot.this.bounds.getMaxX();
            float miy = RasterPlot.this.bounds.getMinY();
            float may = RasterPlot.this.bounds.getMaxY();

            float scaleX = (float) RasterPlot.this.getScaleX();
            float scaleY = (float) RasterPlot.this.getScaleY();

            int w = RasterPlot.this.resolution.width;
            int h1 = RasterPlot.this.resolution.height - 1;

            ColoringRule rule = RasterPlot.this.getColoringRule();
            int[] plot = RasterPlot.this.plotPixels;

            while (true) {
                int nextChunk = RasterPlot.this.pool.decrementAndGet();
                if (nextChunk < 0) {
                    return;
                }
                float[] chunk = RasterPlot.this.chunks.get(nextChunk);
                int N = chunk.length;
                float X, Y;
                ///
                for (int x = 0, y = 1; x < N; x += 2, y += 2) {
                    X = chunk[x];
                    Y = chunk[y];
                    if (X > mix && X < max && Y > miy && Y < may) {
//                        plot.put(
//                                (int) ((X - mix) / scaleX) + (h1 - (int) ((Y - miy) / scaleY)) * w,
//                                rule.colorFunction(X, Y)
//                        );
                        plot[(int) ((X - mix) / scaleX) + (h1 - (int) ((Y - miy) / scaleY)) * w] = rule.colorFunction(X, Y);
                    }
                }
                ///
            }
        }

        void clear() {
            int color = RasterPlot.this.coloringRule.getBackColor();
            int w = RasterPlot.this.resolution.width;
            int[] plot = RasterPlot.this.plotPixels;


            while (true) {
                int y = RasterPlot.this.pool.decrementAndGet();
                if (y < 0) {
                    return;
                }
                for (int x = 0; x < w; x++) {
                    //plot.put(x + y * w, color);
                    plot[x + y * w] = color;
                }
            }
        }

        public void run() {
            switch (mode) {
                case CLEAR: {
                    clear();
                    break;
                }
                case CHUNKS: {
                    renderChunks();
                    break;
                }
                case SOLID: {
                    renderSolid();
                    break;
                }
            }
        }
    }
}
