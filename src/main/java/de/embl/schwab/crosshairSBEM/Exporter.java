package de.embl.schwab.crosshairSBEM;

import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import de.embl.cba.metaimage_io.MetaImage_Writer;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.view.Views;

import java.io.File;

public class Exporter {

    private Transformer transformer;

    public Exporter( Transformer transformer ) {
        this.transformer = transformer;
    }

    public void writeImage(Transformer.ImageType imageType, RealInterval cropInterval, int level, File tempDir ) {
        // export stuff https://github.com/tischi/imagej-utils/blob/9d29c1dbb5bfde784f964e29956877d2d4ddc915/src/main/java/de/embl/cba/bdv/utils/export/BdvRealSourceToVoxelImageExporter.java#L305
        // example of usage https://github.com/tischi/imagej-utils/blob/4ebabd30be230c5fb49674fb78c57cc98d8dab16/src/test/java/explore/ExploreExportSourcesFromBdv.java

        // TODO - warn that time series are not supported
        RandomAccessibleInterval rai = transformer.getRAI( imageType, level );

        // same as big data processor here: https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/process/crop/CropDialog.java#L89
        // i.e. get voxel size at that level, and use it to get a voxel interval
        double[] downsampledVoxelSize = transformer.getSourceVoxelSize( imageType, level );
        Interval voxelCropInterval = toVoxelInterval( cropInterval, downsampledVoxelSize );

        RandomAccessibleInterval crop =
                Views.interval( rai, voxelCropInterval );

        writeImage( imageType, crop, tempDir );
    }

    private void writeImage(Transformer.ImageType imageType, RandomAccessibleInterval rai,  File tempDir ) {
        // TODO - generalise to not just 8-bit? e.g. what happens if I pass a 16bit to this? Does it convert to 8bit
        // sensibly or just clip?
        ImagePlus imp = ImageJFunctions.wrapUnsignedByte( rai, "towrite" );
        System.out.println(imp.getBitDepth());
        MetaImage_Writer writer = new MetaImage_Writer();

        String filenameWithExtension = imageType.name() + ".mhd";
        writer.save( imp, tempDir.getAbsolutePath(), filenameWithExtension );
    }

    public void writeImage( Transformer.ImageType imageType, File tempDir ) {
        RandomAccessibleInterval rai = transformer.getRAI( imageType, 0 );
        writeImage( imageType, rai, tempDir );
    }

    public void writeImage( Transformer.ImageType imageType, RealInterval cropInterval, File tempDir ) {
        writeImage( imageType, cropInterval, 0, tempDir );
    }

    public void writeImage( Transformer.ImageType imageType, int level, File tempDir ) {
        RandomAccessibleInterval rai = transformer.getRAI( imageType, level );
        writeImage( imageType, rai, tempDir );
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

}
