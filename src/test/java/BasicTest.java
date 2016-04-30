import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import raster.ColoringRule;
import raster.RasterPlot;
import utils.Logger;

import java.awt.*;
import java.io.IOException;

public class BasicTest {

    public RasterPlot rasterPlot;

    @BeforeClass
    public void initializationTest() {
        int threadCount = Runtime.getRuntime().availableProcessors();
        rasterPlot = new RasterPlot(threadCount, new Dimension(100, 100), new Logger(System.out, Logger.ALL));
    }

    @Test
    public void renderTest() {
        rasterPlot.clearPlot();
        rasterPlot.setColoringRule(new ColoringRule() {
            @Override
            public int colorFunction(float x, float y) {
                return x*y == 0.0f ? Color.BLUE.getRGB(): Color.RED.getRGB();
            }
        });
        rasterPlot.renderSolid();
        try {
            rasterPlot.saveToFile("D:\\erewer.bmp", "bmp");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
