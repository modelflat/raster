package raster;

class Plotter implements Runnable {
    public enum Mode {SOLID, CHUNKS, CLEAR}

    private RasterPlot parent;
    private ColoringRule rule;

    private float mix, miy, max, may;
    private int w, h;
    private float scaleX, scaleY;

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

        this.rule = parent.getColoringRule();

        this.mix = parent.getBounds().getMinX();
        this.max = parent.getBounds().getMaxX();
        this.miy = parent.getBounds().getMinY();
        this.may = parent.getBounds().getMaxY();

        this.w = (int) parent.getResolution().getWidth();
        this.h = (int) parent.getResolution().getHeight();

        this.scaleX = (float) w / (max - mix);
        this.scaleY = (float) h / (may - miy);
    }

    public void renderSolid() {
        float scaleX = (max - mix) / (float) w;
        float scaleY = (may - miy) / (float) h;
        for (int yCoord = begin; yCoord < end; yCoord++) {
            for (int xCoord = 0; xCoord < w; xCoord++) {
                parent.plotPixels[xCoord + yCoord * w] =
                        rule.colorFunction(mix + (float) xCoord * scaleX, -miy - (float) yCoord * scaleY);
            }
        }
    }

    public void renderChunks() {
        do {
            int nextChunk = parent.pool.get();
            if (nextChunk < 0) {
                return;
            }
            float[] chunk = parent.chunks.get(nextChunk);
            for (int x = 0, y = 1; x < chunk.length; x += 2, y += 2) {
                if (chunk[x] > mix && chunk[x] < max && chunk[y] > miy && chunk[y] < may) {
                    parent.plotPixels[(int) ((chunk[x] - mix) * scaleX) +
                            (h - 1 - (int) ((chunk[y] - miy) * scaleY)) * w] = rule.colorFunction(chunk[x], chunk[y]);
                }
            }
        } while (parent.pool.freeCount() > 0);
    }

    public void clear() {
        int color = rule.getBackColor();
        for (int i = begin; i < end; i++)
            parent.plotPixels[i] = color;
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
