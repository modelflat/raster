package raster;

class Plotter implements Runnable {
    public enum Mode {SOLID, CHUNKS, CLEAR}

    private RasterPlot parent;

    private int workDone = 0;

    private Mode mode;

    Plotter(RasterPlot parent, Mode mode) {
        this.parent = parent;
        this.mode = mode;
    }

    public void renderSolid() {
        // bring variables even closer
        float mix = parent.getBounds().getMinX();
        float miy = parent.getBounds().getMinY();

        float scaleX = (float) parent.getScaleX();
        float scaleY = (float) parent.getScaleY();

        int w = (int) parent.getResolution().getWidth();

        ColoringRule rule = parent.getColoringRule();
        int[] plot = parent.plotPixels;

        ///
        while (true) {
            int y = parent.pool.decrementAndGet();
            if (y < 0) {
                return;
            }
            for (int x = 0; x < w; x++) {
                plot[x + y * w] = rule.colorFunction(mix + (float) x * scaleX, -miy - (float) y * scaleY);
            }
            workDone++;
        }
        ///
    }

    public void renderChunks() {

        float mix = parent.getBounds().getMinX();
        float max = parent.getBounds().getMaxX();
        float miy = parent.getBounds().getMinY();
        float may = parent.getBounds().getMaxY();

        float scaleX = (float) parent.getScaleX();
        float scaleY = (float) parent.getScaleY();

        int w = (int) parent.getResolution().getWidth();
        int h1 = (int) parent.getResolution().getHeight() - 1;

        ColoringRule rule = parent.getColoringRule();
        int[] plot = parent.plotPixels;

        while (true) {
            int nextChunk = parent.pool.decrementAndGet();
            if (nextChunk < 0) {
                return;
            }
            float[] chunk = parent.chunks.get(nextChunk);
            int N = chunk.length;
            float X, Y;
            ///
            for (int x = 0, y = 1; x < N; x += 2, y += 2) {
                X = chunk[x];
                Y = chunk[y];
                if (X > mix && X < max && Y > miy && Y < may) {
                    plot[(int) ((X - mix) / scaleX) + (h1 - (int) ((Y - miy) / scaleY)) * w] = rule.colorFunction(X, Y);
                }
            }
            workDone++;
            ///
        }
    }

    public void clear() {
        ColoringRule rule = parent.getColoringRule();
        int color = rule.getBackColor();
        int w = parent.getResolution().width;
        int[] plot = parent.plotPixels;


        while (true) {
            int y = parent.pool.decrementAndGet();
            if (y < 0) {
                return;
            }
            for (int x = 0; x < w; x++) {
                plot[x + y * w] = color;
            }
            workDone++;
        }
    }

    public void run() {
        long time = System.nanoTime();
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
        time = parent.uToMs(System.nanoTime() - time);
        parent.getLogger().info(String.format("thread completed in %d ms! Mode: %s; work done: %d", time,
                mode.toString(), workDone));
    }
}
