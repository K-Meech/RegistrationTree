package de.embl.schwab.crosshairSBEM.ui;

import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import bdv.util.BdvFunctions;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.schwab.crosshairSBEM.Cropper;
import de.embl.schwab.crosshairSBEM.Transformer;
import ij.gui.GenericDialog;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

import java.io.File;
import java.util.List;

public class CropperUI {

    private Cropper cropper;

    public CropperUI( Cropper cropper ) {
        this.cropper = cropper;
    }

    // TODO - make crop dialog deal with transforms, so always crops in real pixel orientation for writing out
    // TODO - y dim seems integer??
    public void cropDialog( Transformer.ImageType imageType, File tempdir ) {

        // https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/process/crop/CropDialog.java
        //https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/process/crop/CropDialog.java#L58

        TransformedRealBoxSelectionDialog.Result result = cropper.createTransformedRealBoxSelectionDialog( imageType );

        if ( result != null ) {
            int level = chooseSourceLevel( imageType );
            cropper.writeCrop(result, imageType, level, tempdir );
        }
    }

    // saving to mhd - https://github.com/embl-cba/elastixWrapper/blob/edb37861b497747217a8e9dd9e579fd8d8a325bb/src/main/java/de/embl/cba/elastixwrapper/elastix/ElastixWrapper.java#L479
    // save to mhd AND enable choosing of name

    private int chooseSourceLevel( Transformer.ImageType imageType ) throws RuntimeException {
        final GenericDialog gd = new GenericDialog( "Choose resolution level..." );

        String[] resolutionLevels = cropper.getLevelsArray( imageType );
        gd.addChoice("Level:", resolutionLevels, resolutionLevels[0]);
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            return gd.getNextChoiceIndex();
        } else {
            throw new RuntimeException();
        }
    }


}
