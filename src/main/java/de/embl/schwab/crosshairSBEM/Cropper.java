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
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.io.File;
import java.util.List;

public class Cropper {

    private Transformer transformer;

    public Cropper ( Transformer transformer ) {
        this.transformer = transformer;
    }

    public TransformedRealBoxSelectionDialog.Result createTransformedRealBoxSelectionDialog(Transformer.ImageType imageType) {
        // based on calbirated real box stuff here: https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/boundingbox/BoundingBoxDialog.java#L144
        final AffineTransform3D boxTransform = transformer.getTransform( imageType );

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
                        .title( "Units: " + transformer.getSourceUnit( imageType ) )
        );

        if ( result.isValid() ) {
            return result;
        } else {
            return null;
        }
    }


    public void writeCrop(TransformedRealBoxSelectionDialog.Result result, Transformer.ImageType imageType, int level,
                           File tempDir ) {
        // export stuff https://github.com/tischi/imagej-utils/blob/9d29c1dbb5bfde784f964e29956877d2d4ddc915/src/main/java/de/embl/cba/bdv/utils/export/BdvRealSourceToVoxelImageExporter.java#L305
        // example of usage https://github.com/tischi/imagej-utils/blob/4ebabd30be230c5fb49674fb78c57cc98d8dab16/src/test/java/explore/ExploreExportSourcesFromBdv.java

        // TODO - warn that time series are not supported
        RandomAccessibleInterval rai = transformer.getRAI( imageType, level );

        // same as big data processor here: https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/process/crop/CropDialog.java#L89
        // i.e. get voxel size at that level, and use it to get a voxel interval
        double[] downsampledVoxelSize = transformer.getSourceVoxelSize( imageType, level );
        Interval voxelCropInterval = toVoxelInterval( result.getInterval(), downsampledVoxelSize );

        RandomAccessibleInterval crop =
                Views.interval( rai, voxelCropInterval );

        // TODO - generalise to not just 8-bit? e.g. what happens if I pass a 16bit to this? Does it convert to 8bit
        // sensibly or just clip?
        ImagePlus imp = ImageJFunctions.wrapUnsignedByte( crop, "towrite" );
        System.out.println(imp.getBitDepth());
        MetaImage_Writer writer = new MetaImage_Writer();

        String filenameWithExtension = imageType.name() + ".mhd";
        writer.save( imp, tempDir.getAbsolutePath(), filenameWithExtension );
    }


    public static Interval toVoxelInterval(
            RealInterval interval,
            double[] voxelSize )
    {
        final long[] min = new long[ 3 ];
        final long[] max = new long[ 3 ];

        for ( int d = 0; d < 3; d++ )
        {
            min[ d ] = Math.round( interval.realMin( d ) / voxelSize[ d ] );
            max[ d ] = Math.round( interval.realMax( d ) / voxelSize[ d ] );
        }

        return new FinalInterval( min, max );
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

    public String[] getLevelsArray( Transformer.ImageType imageType ) {
        int numLevels = transformer.getSourceNumberOfLevels( imageType );

        String[] resolutionLevels = new String[numLevels];
        for ( int i = 0; i < numLevels; i++ ) {
            resolutionLevels[i] = Integer.toString( i );
        }

        return resolutionLevels;
    }




}
