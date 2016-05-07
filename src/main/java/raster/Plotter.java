package raster;

class Plotter implements Runnable {
    private RasterPlot parent;
    private ColoringRule rule;

    private float mix, miy, max, may;
    private int w, h;
    private float scaleX, scaleY;

    Plotter(RasterPlot parent) {
        this.parent = parent;
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

    public void run() {
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
}
