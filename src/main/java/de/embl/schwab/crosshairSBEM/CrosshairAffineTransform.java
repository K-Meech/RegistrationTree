package de.embl.schwab.crosshairSBEM;

import net.imglib2.realtransform.AffineTransform3D;

public class CrosshairAffineTransform {

    private String name;
    private AffineTransform3D affine;
    // TODO - crop used for each image
    // TODO - downsampling level used for each image
    // TODO record fixed and moving image


    // from moving to fixed
    public CrosshairAffineTransform( AffineTransform3D affine, String name ) {
        this.affine = affine;
        this.name = name;
    }

    public AffineTransform3D getAffine() {
        return affine;
    }

    public String getName() { return name; }

    public void setAffine(AffineTransform3D affine ) {
        this.affine = affine;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }


}
