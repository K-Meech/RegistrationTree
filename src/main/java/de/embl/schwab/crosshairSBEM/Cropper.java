package de.embl.schwab.crosshairSBEM;

import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import bdv.util.BdvFunctions;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.metaimage_io.MetaImage_Writer;
import ij.ImagePlus;
import mpicbg.spim.data.SpimData;
import net.imglib2.*;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.ops.parse.token.Real;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Cropper {

    private Transformer transformer;
    // crops in physical space
    private Map<String, RealInterval> fixedImageCrops;
    private Map<String, RealInterval> movingImageCrops;

    public Cropper ( Transformer transformer ) {

        this.transformer = transformer;
        fixedImageCrops = new HashMap<>();
        movingImageCrops = new HashMap<>();
    }

    // TODO - make so can re-use crops, and not write the same crop over and over
    // perhaps crops in top of folder, then one folder per transformation (e.g. elastix or bigwarp) to hold metadata
    // THen at the end can delete the folder or copy it for reference

    private String[] setToString( Set<String> set ) {
        String[] stringArray = new String[set.size()];
        int i = 0;
        for (String key: set) {
            stringArray[i] = key;
            i++;
        }

        return stringArray;
    }

    public String[] getImageCropNames( Transformer.ImageType imageType ) {
        if (imageType == Transformer.ImageType.FIXED ) {
            return setToString( fixedImageCrops.keySet() );
        } else {
            return setToString( movingImageCrops.keySet() );
        }
    }

    public RealInterval getImageCropInterval( Transformer.ImageType imageType, String name ) {
        if (imageType == Transformer.ImageType.FIXED ) {
            return fixedImageCrops.get( name );
        } else {
            return movingImageCrops.get( name );
        }
    }

    // TODO - make crop dialog deal with transforms, so always crops in real pixel orientation for writing out
    // TODO - y dim seems integer??
    public void crop(Transformer.ImageType imageType, String cropName) {
        // https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/process/crop/CropDialog.java
        //https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/process/crop/CropDialog.java#L58

        TransformedRealBoxSelectionDialog.Result result = createTransformedRealBoxSelectionDialog( imageType );

        if ( result != null ) {
            if ( imageType == Transformer.ImageType.FIXED ) {
                // TODO - give teh crop a name
                fixedImageCrops.put(cropName, result.getInterval() );
            } else {
                movingImageCrops.put(cropName, result.getInterval());
            }
            // int level = chooseSourceLevel( imageType );
            // cropper.writeCrop(result, imageType, level, tempdir );
        }
    }

    private TransformedRealBoxSelectionDialog.Result createTransformedRealBoxSelectionDialog(Transformer.ImageType imageType) {
        // based on calbirated real box stuff here: https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/boundingbox/BoundingBoxDialog.java#L144
        final AffineTransform3D boxTransform = transformer.getBaseTransform( imageType );

        // set sensible initial intervals
        FinalRealInterval rangeInterval = getRangeInterval( imageType );
        FinalRealInterval initialInterval = Intervals.createMinMaxReal( 0, 0, 0,
                rangeInterval.realMax(0)/2,
                rangeInterval.realMax(1)/2,
                rangeInterval.realMax(2)/2);

        TransformedRealBoxSelectionDialog.Result result =  BdvFunctions.selectRealBox(
                transformer.getBdv(),
                boxTransform,
                initialInterval,
                rangeInterval,
                BoxSelectionOptions.options()
                        .title( "Cropping: " + imageType.name() + " Units: " + transformer.getSourceUnit( imageType ) )
        );

        if ( result.isValid() ) {
            return result;
        } else {
            return null;
        }
    }

    private FinalRealInterval getRangeInterval(Transformer.ImageType imageType)
    {
        double[] max = new double[ 3 ];

        long[] sourceVoxelDimensions = transformer.getSourceVoxelDimensions( imageType );
        double[] sourceVoxelSize = transformer.getSourceVoxelSize( imageType );
        for ( int i = 0; i < sourceVoxelSize.length; i++ ) {
            max[i] = sourceVoxelDimensions[i] * sourceVoxelSize[i];
        }
        return Intervals.createMinMaxReal(
                0, 0, 0,
                max[0], max[1], max[2]);
    }

    public boolean doesCropExist( String cropName, Transformer.ImageType imageType, String dir ) {

        boolean cropInList = false;
        if ( imageType == Transformer.ImageType.FIXED ) {
            cropInList = fixedImageCrops.containsKey( cropName );
        } else {
            cropInList = movingImageCrops.containsKey( cropName );
        }

        boolean fileExists = new File( dir, imageType.name() + "_" + cropName + ".mhd" ).exists();

        return cropInList && fileExists;

    }






}
