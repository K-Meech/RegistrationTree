package de.embl.schwab.registrationTree;

import bdv.ij.util.ProgressWriterIJ;
import bdv.viewer.Source;
import bigwarp.BigWarp;
import bigwarp.BigWarpData;
import bigwarp.BigWarpInit;
import de.embl.schwab.registrationTree.registrationNodes.BigWarpRegistrationNode;
import de.embl.schwab.registrationTree.registrationNodes.RegistrationNode;
import de.embl.schwab.registrationTree.ui.BigWarpUI;
import de.embl.schwab.registrationTree.ui.RegistrationTree;
import ij.IJ;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import org.janelia.utility.ui.RepeatingReleasedEventsFixer;

import java.awt.*;
import java.io.File;

public class BigWarpManager {

    private BigWarp bw;
    private Transformer transformer;

    private String transformName;

    public BigWarpManager(Transformer transformer) {
        this.transformer = transformer;
    }

    // TODO - would be nice if closing bigwarp also closed the little export panel

    public void openBigwarpAtSelectedNode( String transformName ) {
        this.transformName = transformName;
        RegistrationNode regNode = transformer.getUi().getTree().getSelectedNode();
        Source fixedSource = transformer.createTransformedSource( Transformer.ImageType.FIXED, regNode );
        Source movingSource = transformer.getSource( Transformer.ImageType.MOVING );
        openBigWarp( movingSource, fixedSource );
    }

    private void openBigWarp( Source movingSource, Source fixedSource ) {
        (new RepeatingReleasedEventsFixer()).install();
        Source[] fixedSources = new Source[] {fixedSource};
        Source[] movingSources = new Source[]{movingSource};
        BigWarpData<?> bigWarpData = BigWarpInit.createBigWarpData(movingSources, fixedSources, new String[] {"moving", "fixed"});
        openBigwarp( bigWarpData );
    }

    private void openBigwarp( BigWarpData<?> bigWarpData ) {
        try {
            bw = new BigWarp(bigWarpData, "Big Warp", new ProgressWriterIJ());
            bw.getViewerFrameP().getViewerPanel().requestRepaint();
            bw.getViewerFrameQ().getViewerPanel().requestRepaint();
            bw.getLandmarkFrame().repaint();

            SpimData movingSpimData = transformer.getSpimData(Transformer.ImageType.MOVING);
            if ( movingSpimData != null ) {
                bw.setMovingSpimData( movingSpimData, new File(transformer.getSourcePath(Transformer.ImageType.MOVING)) );
            }
            bw.setTransformType("Rotation");
            new BigWarpUI(this);
        } catch (SpimDataException var4) {
            var4.printStackTrace();
        }
    }

    public void exportBigWarpToRegTree() {
        if ( !bw.getTransformType().equals("Thin Plate Spline") ) {
            BigWarpRegistrationNode bigWarpRegistrationNode = new BigWarpRegistrationNode(bw, transformName);
            RegistrationTree tree = transformer.getUi().getTree();

            tree.addRegistrationNodeAtLastSelection(bigWarpRegistrationNode);
            transformer.showSource(tree.getLastAddedNode());
        } else {
            IJ.log("Stopping... Unsupported transform type: " + bw.getTransformType() );
        }
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
