package de.embl.schwab.crosshairSBEM;

import de.embl.cba.metaimage_io.MetaImage_Writer;
import ij.ImagePlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.view.Views;

import java.io.File;

public class Exporter {

    private Transformer transformer;
    private Cropper cropper;

    // TODO - check for bad names in all dialogs e.g. spaces

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

    public void writeImage(Transformer.ImageType imageType, String cropName, int level, File tempDir ) {
        // export stuff https://github.com/tischi/imagej-utils/blob/9d29c1dbb5bfde784f964e29956877d2d4ddc915/src/main/java/de/embl/cba/bdv/utils/export/BdvRealSourceToVoxelImageExporter.java#L305
        // example of usage https://github.com/tischi/imagej-utils/blob/4ebabd30be230c5fb49674fb78c57cc98d8dab16/src/test/java/explore/ExploreExportSourcesFromBdv.java

        // TODO - warn that time series are not supported
        RandomAccessibleInterval rai = transformer.getRAI( imageType, level );
        Interval voxelCropInterval = cropper.getImageCropIntervalVoxelSpace( imageType, cropName, level );

        // NOT necessary??? As now we use a voxel interval
        // same as big data processor here: https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/process/crop/CropDialog.java#L89
        // i.e. get voxel size at that level, and use it to get a voxel interval
        // double[] downsampledVoxelSize = transformer.getSourceVoxelSize( imageType, level );
        // Interval voxelCropInterval = toVoxelInterval( cropInterval, downsampledVoxelSize );

        RandomAccessibleInterval crop =
                Views.interval( rai, voxelCropInterval );

        writeImage( imageType, crop, transformer.getSourceVoxelSize(imageType, level),
                makeImageName(imageType, level, cropName), tempDir );
    }

    public void writeImage( Transformer.ImageType imageType, File tempDir ) {
        RandomAccessibleInterval rai = transformer.getRAI( imageType, 0 );
        writeImage( imageType, rai, transformer.getSourceVoxelSize(imageType, 0),
                makeImageName( imageType, 0 ), tempDir );
    }

    public void writeImage( Transformer.ImageType imageType, String cropName, File tempDir ) {
        writeImage( imageType, cropName, 0,  tempDir );
    }

    public void writeImage( Transformer.ImageType imageType, int level, File tempDir ) {
        RandomAccessibleInterval rai = transformer.getRAI( imageType, level );
        writeImage( imageType, rai,  transformer.getSourceVoxelSize(imageType, level),
                makeImageName( imageType, level ), tempDir );
    }

    private void writeImage(Transformer.ImageType imageType,
                            RandomAccessibleInterval rai, double[] voxelSize, String imageName, File tempDir ) {
        // TODO - generalise to not just 8-bit? e.g. what happens if I pass a 16bit to this? Does it convert to 8bit
        // sensibly or just clip?
        ImagePlus imp = ImageJFunctions.wrapUnsignedByte(rai, "towrite");
        imp.getCalibration().pixelWidth = voxelSize[0];
        imp.getCalibration().pixelHeight = voxelSize[1];
        imp.getCalibration().pixelDepth = voxelSize[2];

        // we keep this as a generic unit name, as otherwise the metaimage writer recognises this and tries
        // to convert to mm (often in somewhat unexpected ways)
        imp.getCalibration().setUnit( "physical_units" );
        System.out.println(imp.getBitDepth());

        MetaImage_Writer writer = new MetaImage_Writer();

        String filenameWithExtension = imageName + ".mhd";
        writer.save(imp, tempDir.getAbsolutePath(), filenameWithExtension);
    }

    private boolean imageExists( String imageName, File tempDir ) {
        return new File(tempDir, imageName + ".mhd").exists();
    }



}
