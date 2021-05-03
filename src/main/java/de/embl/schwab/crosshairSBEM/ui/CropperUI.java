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
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

import java.io.File;
import java.util.List;

public class CropperUI {

    private Cropper cropper;

    public CropperUI( Cropper cropper ) {
        this.cropper = cropper;
    }

    public String cropDialog( Transformer.ImageType imageType ) {
        String[] currentCrops;
        if (imageType == Transformer.ImageType.FIXED) {
            currentCrops = cropper.getImageCropNames( Transformer.ImageType.FIXED );
        } else {
            currentCrops = cropper.getImageCropNames( Transformer.ImageType.MOVING );
        }

        if (currentCrops.length > 0) {

            final GenericDialog gd = new GenericDialog("Choose crop for " + imageType.name() );

            String[] choices = new String[currentCrops.length + 1];
            choices[0] = "Make new crop";
            for (int i = 0; i < currentCrops.length; i++) {
                choices[i] = currentCrops[i];
            }
            gd.addChoice("Crop for " + imageType.name(), choices, choices[0]);
            gd.showDialog();

            if (!gd.wasCanceled()) {
                int choice = gd.getNextChoiceIndex();
                if (choice == 0) {
                    String cropName = cropNameDialog( imageType );
                    cropper.crop(imageType, cropName );
                    return cropName;
                } else {
                    return choices[choice];
                }
            } else {
                throw new RuntimeException();
            }
        } else {
            // TODO - properly handle if window are cnacelled
            String cropName = cropNameDialog( imageType );
            cropper.crop(imageType, cropName);
            return cropName;
        }
    }

    public String cropNameDialog( Transformer.ImageType imageType ) {

        final GenericDialog gd = new GenericDialog("Crop name for " + imageType.name() );
        gd.addStringField( "Crop name for " + imageType.name(), "");
        gd.showDialog();

        if (!gd.wasCanceled()) {
            return gd.getNextString();
        } else {
            return null;
        }
    }




}
