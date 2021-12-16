package de.embl.schwab.registrationTree;

import bdv.viewer.Source;
import de.embl.cba.metaimage_io.MetaImage_Writer;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimData;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealUnsignedByteConverter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import java.io.File;

public class Exporter {

    // NOTE: all images are written with 1, 1, 1 voxel spacing in the mhd file header. This is because all
    // scaling info is being incorporated directly into the initial transform (this makes it easier to compensate
    // for any other base transforms in the xml like rotations or translations)

    private Transformer transformer;
    private Cropper cropper;

    public Exporter( Transformer transformer, Cropper cropper ) {
        this.transformer = transformer;
        this.cropper = cropper;
    }

    public String makeImageName (Transformer.ImageType imageType, int level) {
        return imageType.name() + "_" + String.valueOf( level );
    }

    public String makeImageName (Transformer.ImageType imageType, int level, String cropName ) {
        return makeImageName( imageType, level ) + "_" + cropName;
    }

    public String makeMaskName( Transformer.ImageType imageType, int level ) {
        return imageType.name() + "_MASK" + "_" + String.valueOf( level );
    }

    public String makeMaskName( Transformer.ImageType imageType, int level, String cropName ) {
        return makeMaskName( imageType, level ) + "_" + cropName;
    }

    public void writeImage(Transformer.ImageType imageType, String cropName, int level, File tempDir ) {
        // export stuff https://github.com/tischi/imagej-utils/blob/9d29c1dbb5bfde784f964e29956877d2d4ddc915/src/main/java/de/embl/cba/bdv/utils/export/BdvRealSourceToVoxelImageExporter.java#L305
        // example of usage https://github.com/tischi/imagej-utils/blob/4ebabd30be230c5fb49674fb78c57cc98d8dab16/src/test/java/explore/ExploreExportSourcesFromBdv.java

        RandomAccessibleInterval rai = transformer.getRAI( imageType, level );
        Interval voxelCropInterval = cropper.getImageCropIntervalVoxelSpace( imageType, cropName, level );

        RandomAccessibleInterval crop =
                Views.interval( rai, voxelCropInterval );

        writeImage( crop, new double[]{1, 1, 1},
                makeImageName(imageType, level, cropName), tempDir );
    }

    public void writeImage( Transformer.ImageType imageType, File tempDir ) {
        RandomAccessibleInterval rai = transformer.getRAI( imageType, 0 );
        writeImage( rai, new double[]{1, 1, 1},
                makeImageName( imageType, 0 ), tempDir );
    }

    public void writeImage( Transformer.ImageType imageType, String cropName, File tempDir ) {
        writeImage( imageType, cropName, 0,  tempDir );
    }

    public void writeImage( Transformer.ImageType imageType, int level, File tempDir ) {
        RandomAccessibleInterval rai = transformer.getRAI( imageType, level );
        writeImage( rai,  new double[]{1, 1, 1},
                makeImageName( imageType, level ), tempDir );
    }

    private boolean sourcesHaveSameDimensions( RandomAccessibleInterval rai, Transformer.ImageType imageType, int level ) {
        long[] maskDimensions = rai.dimensionsAsLongArray();
        long[] imageDimensions = transformer.getSourceVoxelDimensions( imageType, level );

        if ( maskDimensions.length != imageDimensions.length ) {
            return false;
        }

        for ( int i=0; i<maskDimensions.length; i++ ) {
            if ( maskDimensions[i] != imageDimensions[i] ){
                return false;
            }
        }

        return true;
    }

    // used for writing masks - writes the spim source/data using the crops etc defined for the given image type
    public void writeMask( Transformer.ImageType imageType, SpimData spimData, Source spimSource,
                          String cropName, int level, File tempDir ) {
        RandomAccessibleInterval rai = transformer.getRAI( spimSource, level );
        // TODO - this currently assumes the original image (e.g. fixed) and the mask have exactly the same pyramid
        // i.e. exactly the same voxel sizes and dimensions on the same levels. Would be good to generalise this in future.
        if ( !sourcesHaveSameDimensions( rai, imageType, level ) ) {
            throw new UnsupportedOperationException( "original image and mask do not have the same voxel dimensions at level: " + level );
        }
        Interval voxelCropInterval = cropper.getImageCropIntervalVoxelSpace( imageType, cropName, level );

        RandomAccessibleInterval crop =
                Views.interval( rai, voxelCropInterval );

        writeImage( crop, new double[]{1, 1, 1},
                makeMaskName(imageType, level, cropName), tempDir );
    }

    public void writeMask( Transformer.ImageType imageType, SpimData spimData, Source spimSource,
                           int level, File tempDir ) {
        RandomAccessibleInterval rai = transformer.getRAI( spimSource, level );
        // TODO - this currently assumes the original image (e.g. fixed) and the mask have exactly the same pyramid
        // i.e. exactly the same voxel sizes and dimensions on the same levels. Would be good to generalise this in future.
        if ( !sourcesHaveSameDimensions( rai, imageType, level ) ) {
            throw new UnsupportedOperationException( "original image and mask do not have the same voxel dimensions at level: " + level );
        }
        writeImage( rai,  new double[]{1, 1, 1},
                makeMaskName( imageType, level ), tempDir );
    }

    private void writeImage( RandomAccessibleInterval rai, double[] voxelSize, String imageName, File tempDir ) {
        // TODO - generalise to any bit depth? Would need to specify different max in the converter i.e. remove
        // hardcoded 65535
        Object imageType = Util.getTypeFromInterval(rai);
        if ( !(imageType instanceof UnsignedByteType) && !(imageType instanceof UnsignedShortType)) {
            IJ.log("Stopping... only 8-bit or 16-bit images are supported");
        } else {
            RandomAccessibleInterval<UnsignedByteType> unsignedByteRai;
            if (imageType instanceof UnsignedByteType) {
                unsignedByteRai = rai;
            } else {
                // rescale 16-bit to 8-bit
                unsignedByteRai = Converters.convert(
                        rai,
                        new RealUnsignedByteConverter(0, 65535),
                        new UnsignedByteType());
            }

            ImagePlus imp = ImageJFunctions.wrapUnsignedByte(unsignedByteRai, "towrite");

            if (imp.getNFrames() > 1) {
                IJ.log("Stopping... time series are not supported");
            } else {
                imp.getCalibration().pixelWidth = voxelSize[0];
                imp.getCalibration().pixelHeight = voxelSize[1];
                imp.getCalibration().pixelDepth = voxelSize[2];

                // we keep this as a generic unit name, as otherwise the metaimage writer recognises this and tries
                // to convert to mm (often in somewhat unexpected ways)
                imp.getCalibration().setUnit("physical_units");

                MetaImage_Writer writer = new MetaImage_Writer();

                String filenameWithExtension = imageName + ".mhd";
                writer.save(imp, tempDir.getAbsolutePath(), filenameWithExtension);
            }
        }
    }

    private boolean imageExists( String imageName, File tempDir ) {
        return new File(tempDir, imageName + ".mhd").exists();
    }

}
