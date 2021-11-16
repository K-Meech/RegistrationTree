package de.embl.schwab.registrationTree;

import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import bdv.util.BdvFunctions;
import net.imglib2.*;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

import java.util.HashMap;
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

    public void setFixedImageCrops(Map<String, RealInterval> fixedImageCrops) {
        this.fixedImageCrops = fixedImageCrops;
    }

    public void addFixedImageCrops(Map<String, RealInterval> fixedImageCrops) {
        this.fixedImageCrops.putAll( fixedImageCrops );
    }

    public void setMovingImageCrops(Map<String, RealInterval> movingImageCrops) {
        this.movingImageCrops = movingImageCrops;
    }

    public void addMovingImageCrops(Map<String, RealInterval> movingImageCrops) {
        this.movingImageCrops.putAll( movingImageCrops );
    }

    public void removeAllCrops() {
        this.fixedImageCrops = new HashMap<>();
        this.movingImageCrops = new HashMap<>();
    }

    // rounded to nearest full voxel at full-resolution
    public Interval getImageCropIntervalVoxelSpace(Transformer.ImageType imageType, String name ) {
            return getVoxelInterval( name, imageType, 0);
    }

    // rounded to nearest full voxel at resolution level
    public Interval getImageCropIntervalVoxelSpace(Transformer.ImageType imageType, String name, int level ) {
        return getVoxelInterval( name, imageType, level );
    }

    public RealInterval getImageCropRealIntervalVoxelSpace(Transformer.ImageType imageType, String name ) {
        if ( imageType == Transformer.ImageType.FIXED ) {
            return fixedImageCrops.get( name );
        } else {
            return movingImageCrops.get( name );
        }
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

    public AffineTransform3D getCropTranslationPhysicalSpace(Transformer.ImageType imageType, boolean negative,
                                                              String name, int level ) {
        RealInterval cropInterval = getImageCropPhysicalSpace( imageType, name, level );
        return getCropTranslation( cropInterval, negative );
    }

    public AffineTransform3D getCropTranslationVoxelSpace(Transformer.ImageType imageType, boolean negative,
                                                           String name, int level ) {
        RealInterval cropInterval = getImageCropIntervalVoxelSpace( imageType, name, level );
        return getCropTranslation( cropInterval, negative );
    }

    private AffineTransform3D getCropTranslation( RealInterval cropInterval, boolean negative ) {
        double[] cropMin = cropInterval.minAsDoubleArray();
        if ( negative ) {
            for (int i = 0; i< cropMin.length; i++) {
                cropMin[i] = -1*cropMin[i];
            }
        }
        AffineTransform3D translationCrop = new AffineTransform3D();
        translationCrop.translate( cropMin );
        return translationCrop;
    }

    public boolean crop(Transformer.ImageType imageType, String cropName) {
        // https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/process/crop/CropDialog.java
        // https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/process/crop/CropDialog.java#L58

        transformer.onlyShowOriginalImage( imageType );
        TransformedRealBoxSelectionDialog.Result result = createTransformedRealBoxSelectionDialog( imageType );

        if ( result != null ) {
            if ( imageType == Transformer.ImageType.FIXED ) {
                fixedImageCrops.put(cropName, result.getInterval() );
            } else {
                movingImageCrops.put(cropName, result.getInterval());
            }

            return true;
        } else {
            return false;
        }
    }

    private TransformedRealBoxSelectionDialog.Result createTransformedRealBoxSelectionDialog(Transformer.ImageType imageType) {
        // based on calibrated real box stuff here: https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/boundingbox/BoundingBoxDialog.java#L144
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

        long[] sourceVoxelDimensions = transformer.getSourceVoxelDimensions( imageType );

        // have to remove 1 from each dimension as imglib2 indexes from 0
        return Intervals.createMinMaxReal(
                0, 0, 0,
                sourceVoxelDimensions[0] - 1, sourceVoxelDimensions[1] - 1, sourceVoxelDimensions[2] - 1);
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

    public boolean cropExists( Transformer.ImageType imageType, String cropName ) {
        if ( imageType == Transformer.ImageType.FIXED ) {
            return fixedImageCrops.containsKey( cropName );
        } else {
            return movingImageCrops.containsKey( cropName );
        }
    }

}
