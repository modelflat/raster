import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import raster.ColoringRule;
import raster.RasterPlot;
import utils.Logger;

import java.awt.*;
import java.io.IOException;

public class BasicTests {

    private RasterPlot rasterPlot;

    @BeforeClass
    public void initialization() {
        rasterPlot = new RasterPlot(new Dimension(1024, 1024));
        rasterPlot.setLogger(new Logger(System.out, Logger.ALL));
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
                .saveToFile("test.png", "png");
    }

}
