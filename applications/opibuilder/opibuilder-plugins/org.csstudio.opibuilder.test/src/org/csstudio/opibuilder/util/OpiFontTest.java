package org.csstudio.opibuilder.util;

import static org.junit.Assert.assertEquals;

import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OpiFontTest {

    private final int fontSize = 10;

    @Test
    public void testSizeInPoints() {
        OPIFont opiFont = new OPIFont(new FontData("Arial", fontSize, 0), false) {
            @Override  // Can't call this without the RCP running
            protected boolean getDefaultIsInPixels() {
                return false;
            }
        };
        // Returned size should be 10 points
        assertEquals(fontSize, opiFont.getFontData().getHeight());
    }

    @Test
    public void testSizeInPixels() throws Exception {
        OPIFont opiFont = new OPIFont(new FontData("Arial", fontSize, 0), true) {
            @Override  // Can't call this without the RCP running
            protected boolean getDefaultIsInPixels() {
                return false;
            }
        };
        // Returned size should be 10 pixels converted to points based on the display DPI
        assertEquals(fontSize * OPIFont.POINTS_PER_INCH / Display.getDefault().getDPI().y,
                opiFont.getFontData().getHeight());
    }

}
