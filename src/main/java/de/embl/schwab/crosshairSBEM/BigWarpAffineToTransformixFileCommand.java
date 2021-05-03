package de.embl.schwab.crosshairSBEM;

import de.embl.cba.metaimage_io.MetaImage_Reader;
import de.embl.cba.metaimage_io.MetaImage_Writer;
import ij.IJ;
import ij.ImagePlus;
import itc.converters.BigWarpAffineToElastixAffineTransform3D;
import itc.transforms.elastix.ElastixAffineTransform3D;
import itc.transforms.elastix.ElastixTransform;
import itc.utilities.ParsingUtils;
import loci.common.services.ServiceFactory;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import net.imglib2.realtransform.AffineTransform3D;
import ome.units.UNITS;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import java.io.File;


// based on itc converters of same name, but changed to handle reading mhd files

@Plugin(type = Command.class, menuPath = "Plugins>Registration>Elastix>Utils>Big Warp Affine Transform to Transformix File" )
public class BigWarpAffineToTransformixFileCommand implements Command
{
    public static final String MILLIMETER = "millimeter";
    public static final String MICROMETER = "micrometer";
    public static final String NANOMETER = "nanometer";

    @Parameter( label = "Target image dimensions (from image file)" )
    public File targetImageFile;

    @Parameter( label = "Big warp affine transform" )
    public String affineTransformString = "1.0 0.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0 1.0 0.0";

    @Parameter( label = "Big warp affine transform units", choices = { MILLIMETER, MICROMETER, NANOMETER }  )
    public String affineTransformUnit;

    @Parameter( label = "Transformix transformation output file", style = "save" )
    public File transformationOutputFile;

    @Parameter( label = "Interpolation", choices = { ElastixTransform.FINAL_LINEAR_INTERPOLATOR, ElastixTransform.FINAL_NEAREST_NEIGHBOR_INTERPOLATOR } )
    public String interpolation = ElastixTransform.FINAL_LINEAR_INTERPOLATOR;

    private Double[] voxelSpacingsMillimeter;
    private Integer[] dimensionsPixels;
    private Integer bitDepth;

    public void run()
    {
        setTargetImageProperties( targetImageFile );

        AffineTransform3D affineTransform3D = new AffineTransform3D();
        affineTransform3D.set( affineStringAsDoubles( affineTransformString ) );

        final ElastixAffineTransform3D elastixAffineTransform3D =
                new BigWarpAffineToElastixAffineTransform3D().convert(
                        affineTransform3D,
                        voxelSpacingsMillimeter,
                        dimensionsPixels,
                        bitDepth,
                        interpolation,
                        affineTransformUnit );

        elastixAffineTransform3D.save( transformationOutputFile.getAbsolutePath() );
    }

    public static double[] affineStringAsDoubles( String affineString )
    {
        affineString = affineString.replace( "3d-affine: (", "" );
        affineString = affineString.replace( ")", "" );
        affineString = affineString.replace( "(", "" );
        if ( affineString.contains( "," ))
            return ParsingUtils.delimitedStringToDoubleArray( affineString, "," );
        else
            return ParsingUtils.delimitedStringToDoubleArray( affineString, " " );
    }


    private void setTargetImageProperties( File file )
    {
        // TODO - check this picks up spacing properly. Do mm units get written to .mhd?
        if (FilenameUtils.getExtension(file.getAbsolutePath()).equals("mhd")) {

            ImagePlus image = IJ.openImage( file.getAbsolutePath() );

            // MetaImage_Reader reader = new MetaImage_Reader();
            // String dir = file.getParent();
            // String imageName = FileNameUtils.getBaseName( file.getAbsolutePath()) + ".mhd";
            // ImagePlus image = reader.load(dir, imageName, true);

            voxelSpacingsMillimeter = new Double[3];
            voxelSpacingsMillimeter[0] = image.getCalibration().pixelWidth;
            voxelSpacingsMillimeter[1] = image.getCalibration().pixelHeight;
            voxelSpacingsMillimeter[2] = image.getCalibration().pixelDepth;

            dimensionsPixels = new Integer[3];
            int[] imageDimensions = image.getDimensions();
            for (int i=0; i< imageDimensions.length; i++) {
                dimensionsPixels[i] = imageDimensions[i];
            }

            bitDepth = image.getBitDepth();

        } else {
            // create OME-XML metadata store
            ServiceFactory factory = null;
            try {
                factory = new ServiceFactory();
                OMEXMLService service = factory.getInstance(OMEXMLService.class);
                IMetadata meta = service.createOMEXMLMetadata();

                // create format reader
                IFormatReader reader = new ImageReader();
                reader.setMetadataStore(meta);

                // initialize file
                reader.setId(file.getAbsolutePath());
                reader.setSeries(0);

                String unit = meta.getPixelsPhysicalSizeX(0).unit().getSymbol();
                voxelSpacingsMillimeter = new Double[3];
                voxelSpacingsMillimeter[0] = meta.getPixelsPhysicalSizeX(0).value(UNITS.MILLIMETRE).doubleValue();
                voxelSpacingsMillimeter[1] = meta.getPixelsPhysicalSizeX(0).value(UNITS.MILLIMETRE).doubleValue();
                voxelSpacingsMillimeter[2] = meta.getPixelsPhysicalSizeX(0).value(UNITS.MILLIMETRE).doubleValue();

                dimensionsPixels = new Integer[3];
                dimensionsPixels[0] = meta.getPixelsSizeX(0).getValue();
                dimensionsPixels[1] = meta.getPixelsSizeY(0).getValue();
                dimensionsPixels[2] = meta.getPixelsSizeZ(0).getValue();

                bitDepth = meta.getPixelsSignificantBits(0).getValue();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
