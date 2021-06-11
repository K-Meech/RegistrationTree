package de.embl.schwab.crosshairSBEM.registrationNodes;

import bdv.util.BdvStackSource;
import net.imglib2.realtransform.AffineTransform3D;

public class RegistrationNode {
    // affines always saved with convention of fixed to moving direction

    private String name;

    // affine transform of this node
    private AffineTransform3D affine;
    // full transform, concatenating this affine to all previous transforms before it
    private AffineTransform3D fullTransform;

    // TODO record fixed and moving image

    // holds Bdvstacksource, if node is displayed
    private transient BdvStackSource src;

    public RegistrationNode( AffineTransform3D affine, AffineTransform3D fullTransform, String name ) {
        this.affine = affine;
        this.fullTransform = fullTransform;
        this.name = name;
    }

    public RegistrationNode( AffineTransform3D affine, String name ) {
        this.affine = affine;
        this.name = name;
    }

    public AffineTransform3D getAffine() {
        return affine;
    }
    public void setAffine(AffineTransform3D affine ) {
        this.affine = affine;
    }

    public AffineTransform3D getFullTransform() { return fullTransform; }
    public void setFullTransform(AffineTransform3D fullTransform) { this.fullTransform = fullTransform; }

    public String getName() { return name; }
    public void setName( String name ) { this.name = name; }

    public void setSrc(BdvStackSource src) { this.src = src; }
    public BdvStackSource getSrc() { return src; }

    public String toString() {
        return this.name;
    }
}
