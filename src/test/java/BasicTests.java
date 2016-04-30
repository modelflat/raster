import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import raster.ColoringRule;
import raster.RasterPlot;
import utils.Logger;

import java.awt.*;
import java.io.IOException;

public class BasicTests {

    public RasterPlot rasterPlot;

    @BeforeClass
    public void initialization() {
        int threadCount = Runtime.getRuntime().availableProcessors();
        rasterPlot = new RasterPlot(threadCount, new Dimension(1024, 1024), new Logger(System.out, Logger.ALL));
    }

    @Test
    public void renderSolidTest() throws IOException {
        rasterPlot.clearPlot();
        rasterPlot.setColoringRule(new ColoringRule() {
            @Override
            public int colorFunction(float x, float y) {
                return x*y == 0.0f ? Color.BLUE.getRGB(): Color.RED.getRGB();
            }
        });
        rasterPlot.renderSolid();
        rasterPlot.saveToFile("test.bmp", "bmp");
    }

}
