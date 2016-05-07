import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import raster.ColoringRule;
import raster.RasterPlot;
import utils.Logger;

import java.awt.*;
import java.io.IOException;
import java.util.Random;

public class BasicTests {

    private RasterPlot rasterPlot;

    @BeforeClass
    public void initialization() {
        rasterPlot = new RasterPlot(new Dimension(1024, 1024));
        rasterPlot.setLogger(new Logger());
    }

    @Test
    public void renderSolidTest() throws IOException {
        rasterPlot.clearPlot()
                .setColoringRule(new ColoringRule() {
                    @Override
                    public int colorFunction(float x, float y) {
                        return x * x * x - y < 0.01f ? Color.RED.getRGB() : Color.GREEN.getRGB();
                    }
                })
                .renderSolid()
                .saveToFile("test_solid.png", "png");
    }

    @Test
    public void renderTest() throws Exception {
        Random rng = new Random();
        int pCount = 50000;
        for (int j = 0; j < 4; j++) { // 4 chunks
            float[] currentChunk = new float[pCount * 2]; // 5000 points each
            for (int i = 0; i < pCount * 2; i += 2) {
                currentChunk[i] = (float) rng.nextGaussian();
                currentChunk[i + 1] = 2 * (float) i / (float) pCount - 1;
            }
            rasterPlot.putChunk(currentChunk);
        }
        rasterPlot.clearPlot()
                .setColoringRule(new ColoringRule() {
                    @Override
                    public int colorFunction(float x, float y) {
                        return x * y > 0 ? Color.GREEN.getRGB() : Color.RED.getRGB();
                    }
                })
                .renderChunks()
                .saveToFile("test.png", "png");
    }

}
