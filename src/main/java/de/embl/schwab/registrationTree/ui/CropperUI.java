package de.embl.schwab.registrationTree.ui;

import de.embl.schwab.registrationTree.Cropper;
import de.embl.schwab.registrationTree.Transformer;
import ij.IJ;
import ij.gui.GenericDialog;

import static de.embl.schwab.registrationTree.StringUtils.tidyString;

public class CropperUI {

    private Cropper cropper;

    public CropperUI( Cropper cropper ) {
        this.cropper = cropper;
    }

    private String makeNewCrop( Transformer.ImageType imageType ) {
        String cropName = cropNameDialog( imageType );
        if ( cropName != null ) {
            if ( !cropper.cropExists( imageType, cropName ) ) {
                boolean successfulCrop = cropper.crop(imageType, cropName );
                if ( !successfulCrop ) {
                    cropName = null;
                }
            } else {
                IJ.log( "Stopping... That crop name already exists" );
                cropName = null;
            }
        }

        return cropName;
    }

    public String cropDialog( Transformer.ImageType imageType ) {
        String[] currentCrops;
        if (imageType == Transformer.ImageType.FIXED) {
            currentCrops = cropper.getImageCropNames( Transformer.ImageType.FIXED );
        } else {
            currentCrops = cropper.getImageCropNames( Transformer.ImageType.MOVING );
        }

        String cropName = null;
        if (currentCrops.length > 0) {
            final GenericDialog gd = new GenericDialog("Choose crop for " + imageType.name() );

            String[] choices = new String[currentCrops.length + 1];
            choices[0] = "Make new crop";
            for (int i = 0; i < currentCrops.length; i++) {
                choices[i + 1] = currentCrops[i];
            }
            gd.addChoice("Crop for " + imageType.name(), choices, choices[0]);
            gd.showDialog();


            if (!gd.wasCanceled()) {
                int choice = gd.getNextChoiceIndex();
                if (choice == 0) {
                    cropName = makeNewCrop( imageType );
                } else {
                    cropName = choices[choice];
                }
            }
        } else {
            cropName = makeNewCrop( imageType );
        }

        return cropName;
    }

    public String cropNameDialog( Transformer.ImageType imageType ) {

        final GenericDialog gd = new GenericDialog("Crop name for " + imageType.name() );
        gd.addStringField( "Crop name for " + imageType.name(), "");
        gd.showDialog();

        if (!gd.wasCanceled()) {
            return tidyString( gd.getNextString() );
        } else {
            return null;
        }
    }




}
