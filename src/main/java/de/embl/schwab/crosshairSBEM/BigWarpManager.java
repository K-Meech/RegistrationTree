package de.embl.schwab.crosshairSBEM;

import bdv.ij.util.ProgressWriterIJ;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import de.embl.schwab.crosshairSBEM.ui.BigWarpUI;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.utility.ui.RepeatingReleasedEventsFixer;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class BigWarpManager {

    private SpimData movingSource;
    private SpimData fixedSource;
    private BigWarp bw;
    private Transformer transformer;

    private String transformName;

    public BigWarpManager(Transformer transformer) {
        this.transformer = transformer;
    }

    public void openBigwarp( String transformName ) {
        this.transformName = transformName;
        openBigwarp( transformer.getSpimData(Transformer.ImageType.MOVING),
                transformer.getSpimData(Transformer.ImageType.FIXED),
                transformer.getSourcePath(Transformer.ImageType.MOVING) );
    }

    public void openBigwarpAtSelectedNode( String transformName ) {
        this.transformName = transformName;
        AffineTransform3D fullTransform = transformer.getUi().getTree().getFullTransformOfSelectedNode();
        TransformedSource transformedSource = transformer.createTransformedSource( Transformer.ImageType.MOVING, fullTransform );
        openBigWarp( transformedSource, transformer.getSource(Transformer.ImageType.FIXED), transformer.getSourcePath(Transformer.ImageType.MOVING));
    }

    private void openBigwarp ( SpimData movingSource, SpimData fixedSource, String movingSourcePath ) {
        // TODO - would be nice if clsoing bigwarp also closed the little crosshair panel
        // TODO - make sure it can open from an existing transformed source, and add on top
            (new RepeatingReleasedEventsFixer()).install();
            BigWarp.BigWarpData<?> bigWarpData = BigWarpInit.createBigWarpData(movingSource, fixedSource);
            openBigwarp( bigWarpData, movingSourcePath );
    }

    private void openBigWarp(Source movingSource, Source fixedSource, String movingSourcePath ) {
        (new RepeatingReleasedEventsFixer()).install();
        Source[] fixedSources = new Source[] {fixedSource};
        Source[] movingSources = new Source[]{movingSource};
        BigWarp.BigWarpData<?> bigWarpData = BigWarpInit.createBigWarpData(movingSources, fixedSources, new String[] {"moving", "fixed"});
        openBigwarp( bigWarpData, movingSourcePath );
    }

    private void openBigwarp( BigWarp.BigWarpData<?> bigWarpData, String movingSourcePath ) {
        try {
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
        // TODO - concatenate the chain of transforms
        AffineTransform3D bigWarpTransform = bw.affine3d();
        transformer.showSource( bigWarpTransform );
        transformer.getUi().getTree().addRegistrationNodeAtLastSelection( new CrosshairAffineTransform(bigWarpTransform, transformName));
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
