package raster;

class Plotter implements Runnable {
    public enum Mode {SOLID, CHUNKS, CLEAR}

    private RasterPlot parent;

    private int begin, end;
    private Mode mode;

    Plotter(RasterPlot parent, Mode mode, int begin, int end) {
        this(parent, mode);
        this.begin = begin;
        this.end = end;
    }

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
        int end = this.end;

        ColoringRule rule = parent.getColoringRule();
        int[] plot = parent.plotPixels;

        ///
        for (int y = begin; y < end; y++) {
            for (int x = 0; x < w; x++) {
                plot[x + y * w] = rule.colorFunction(mix + (float) x * scaleX, -miy - (float) y * scaleY);
            }
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

        do {
            int nextChunk = parent.pool.get();
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
            ///
        } while (parent.pool.freeCount() > 0);
    }

    public void clear() {
        ColoringRule rule = parent.getColoringRule();
        int color = rule.getBackColor();

        int end = this.end;
        int[] plot = parent.plotPixels;

        for (int i = begin; i < end; i++)
            plot[i] = color;
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
