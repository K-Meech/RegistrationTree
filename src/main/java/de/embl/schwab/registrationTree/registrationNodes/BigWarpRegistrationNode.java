package de.embl.schwab.registrationTree.registrationNodes;

import bigwarp.BigWarp;

import java.util.ArrayList;

public class BigWarpRegistrationNode extends RegistrationNode {

    // For now, we only support affine transforms
    public String transformType;
    public ArrayList<Double[]> movingLandmarks;
    public ArrayList<Double[]> targetLandmarks;

    public BigWarpRegistrationNode( BigWarp bw, String transformName ) {
        super( bw.getMovingToFixedTransformAsAffineTransform3D().inverse(), transformName );
        this.transformType = bw.getTransformType();
        this.movingLandmarks = bw.getLandmarkPanel().getTableModel().getPoints( true );
        this.targetLandmarks = bw.getLandmarkPanel().getTableModel().getPoints( false );
    }
}
