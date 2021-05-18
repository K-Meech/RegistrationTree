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
    // crops in pixel space of full-resolution
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

    // rounded to nearest full voxel at full-resolution
    public Interval getImageCropIntervalVoxelSpace(Transformer.ImageType imageType, String name ) {
            return getVoxelInterval( name, imageType, 0);
    }

    public RealInterval getImageCropRealIntervalVoxelSpace(Transformer.ImageType imageType, String name ) {
        if ( imageType == Transformer.ImageType.FIXED ) {
            return fixedImageCrops.get( name );
        } else {
            return movingImageCrops.get( name );
        }
    }

    // rounded to nearest full voxel at resolution level
    public Interval getImageCropIntervalVoxelSpace(Transformer.ImageType imageType, String name, int level ) {
        return getVoxelInterval( name, imageType, level );
    }

    public RealInterval getImageCropPhysicalSpace( Transformer.ImageType imageType, String name, int level ) {

        Interval voxelCrop = getImageCropIntervalVoxelSpace( imageType, name, level );
        double[] intervalMax = voxelCrop.maxAsDoubleArray();
        double[] intervalMin = voxelCrop.minAsDoubleArray();

        double[] voxelSizeAtLevel;
        if ( level != 0 ) {
            voxelSizeAtLevel = transformer.getSourceVoxelSize(imageType, level);
        } else {
            voxelSizeAtLevel = transformer.getSourceVoxelSize( imageType );
        }

        // get interval in physical space
        for (int i = 0; i<voxelSizeAtLevel.length; i++) {
            intervalMax[i] = intervalMax[i] * voxelSizeAtLevel[i];
            intervalMin[i] = intervalMin[i] * voxelSizeAtLevel[i];
        }

        return new FinalRealInterval(intervalMin, intervalMax);
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
        // double[] max = new double[ 3 ];

        long[] sourceVoxelDimensions = transformer.getSourceVoxelDimensions( imageType );
        // double[] sourceVoxelSize = transformer.getSourceVoxelSize( imageType );
        // for ( int i = 0; i < sourceVoxelSize.length; i++ ) {
        //     max[i] = sourceVoxelDimensions[i] * sourceVoxelSize[i];
        // }

        // have to remove 1 from each dimension as imglib2 indexes from 0
        return Intervals.createMinMaxReal(
                0, 0, 0,
                sourceVoxelDimensions[0] - 1, sourceVoxelDimensions[1] - 1, sourceVoxelDimensions[2] - 1);
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

    public Interval getVoxelInterval( String cropName, Transformer.ImageType imageType, int level )
    {
        RealInterval voxelCropIntervalFullRes = null;
        if (imageType == Transformer.ImageType.FIXED ) {
            voxelCropIntervalFullRes =  fixedImageCrops.get( cropName );
        } else {
            voxelCropIntervalFullRes = movingImageCrops.get( cropName );
        }

        double[] intervalMax = voxelCropIntervalFullRes.maxAsDoubleArray();
        double[] intervalMin = voxelCropIntervalFullRes.minAsDoubleArray();

        // convert crop to physical space, then to pixel space of relevant level
        if ( level != 0 ) {

            // get interval in physical space
            double[] voxelSizeFullRes = transformer.getSourceVoxelSize( imageType );
            for (int i = 0; i<voxelSizeFullRes.length; i++) {
                intervalMax[i] = intervalMax[i] * voxelSizeFullRes[i];
                intervalMin[i] = intervalMin[i] * voxelSizeFullRes[i];
            }

            // get interval in voxel space at the chosen level
            double[] voxelSizeAtLevel = transformer.getSourceVoxelSize( imageType, level );
            for (int i = 0; i<voxelSizeAtLevel.length; i++) {
                intervalMax[i] = intervalMax[i] / voxelSizeAtLevel[i];
                intervalMin[i] = intervalMin[i] / voxelSizeAtLevel[i];
            }

        }

        // round to nearest voxel, ensuring stays in range of image data
        final long[] min = new long[ 3 ];
        final long[] max = new long[ 3 ];
        long[] voxelDimensionsAtLevel = transformer.getSourceVoxelDimensions( imageType, level );

        for ( int d = 0; d < 3; d++ )
        {
            long minVal = Math.round( intervalMin[d] );
            long maxVal = Math.round( intervalMax[d] );

            if ( minVal < 0 ) {
                min[d] = 0;
            } else {
                min[d] = minVal;
            }

            // have to take away one as imglib2 indexes from 0
            if ( maxVal > voxelDimensionsAtLevel[d] - 1 ) {
                max[d] = voxelDimensionsAtLevel[d] - 1;
            } else {
                max[d] = maxVal;
            }
        }

        return new FinalInterval( min, max );
    }






}
