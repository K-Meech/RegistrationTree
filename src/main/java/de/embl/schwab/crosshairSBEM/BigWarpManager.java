package de.embl.schwab.crosshairSBEM;

import bdv.ij.util.ProgressWriterIJ;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import de.embl.schwab.crosshairSBEM.ui.BigWarpUI;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.utility.ui.RepeatingReleasedEventsFixer;

import java.awt.*;
import java.io.File;

public class BigWarpManager {

    private SpimData movingSource;
    private SpimData fixedSource;
    private BigWarp bw;
    private Transformer transformer;

    public BigWarpManager(Transformer transformer) {
        this.transformer = transformer;
    }

    public void openBigwarp ( SpimData movingSource, SpimData fixedSource, String movingSourcePath ) {
        // TODO - would be nice if clsoing bigwarp also closed the little crosshair panel
        try {
            (new RepeatingReleasedEventsFixer()).install();
            BigWarp.BigWarpData<?> bigWarpData = BigWarpInit.createBigWarpData(movingSource, fixedSource);
            bw = new BigWarp(bigWarpData, "Big Warp", new ProgressWriterIJ());
            bw.getViewerFrameP().getViewerPanel().requestRepaint();
            bw.getViewerFrameQ().getViewerPanel().requestRepaint();
            bw.getLandmarkFrame().repaint();
            bw.setMovingSpimData(movingSource, new File(movingSourcePath));
            bw.setTransformType("Rotation");
            new BigWarpUI(this);
        } catch (SpimDataException var4) {
            var4.printStackTrace();
        }
    }

    public void exportBigWarpToCrosshair() {
        // TODO - deal with if fixed/moving same way around, or needs to be swapped
        // TODO - check if type of transform is supported i.e. no thin plate splines!
        AffineTransform3D bigWarpTransform = bw.affine3d();
        transformer.setTransform(Transformer.ImageType.FIXED, bigWarpTransform );

        AffineTransform3D identity = new AffineTransform3D();
        identity.identity();
        transformer.setTransform(Transformer.ImageType.MOVING, identity );
        transformer.refreshBdvWindow();
        // TODO - add transform panel too
    }

    public Point getViewerFrameQLocation() {
        return bw.getViewerFrameQ().getLocation();
    }

    public int getViewerFrameQHeight() {
        return bw.getViewerFrameQ().getHeight();
    }

    public void closeAllBigWarpWindows() {
        bw.closeAll();
    }

}
