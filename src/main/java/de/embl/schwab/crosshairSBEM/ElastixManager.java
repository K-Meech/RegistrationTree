package de.embl.schwab.crosshairSBEM;

import bdv.tools.transformation.TransformedSource;
import de.embl.cba.elastixwrapper.commandline.ElastixCaller;
import de.embl.cba.elastixwrapper.commandline.settings.ElastixSettings;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.DefaultElastixParametersCreator;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import de.embl.schwab.crosshairSBEM.ui.CropperUI;
import de.embl.schwab.crosshairSBEM.ui.DownsamplingUI;
import de.embl.schwab.crosshairSBEM.ui.ElastixUI;
import ij.IJ;
import ij.ImagePlus;
import itc.converters.*;
import itc.transforms.elastix.*;
import itc.utilities.TransformUtils;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.log.StderrLogService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static itc.utilities.Units.MILLIMETER;

public class ElastixManager {
    // TODO - remove these file deafults
    public String elastixDirectory = "C:\\Users\\meechan\\Documents\\elastix-4.9.0-win64";
    public String tmpDir = "C:\\Users\\meechan\\Documents\\temp\\CrosshairElastixTesting";
    public ElastixParameters.TransformationType transformationType = ElastixParameters.TransformationType.Euler;
    public String bSplineGridSpacing = "50,50,50";
    public int numIterations = 1000;
    public int numSpatialSamples = 10000;
    public String gaussianSmoothingSigmas = "10,10,10";
    public String finalResampler = ElastixParameters.FINAL_RESAMPLER_LINEAR;
    public ArrayList<String> fixedImageFilePaths;
    public ArrayList<String> movingImageFilePaths;

    // Strings in elastix transform files (Transform "")
    private final String TRANSLATION = "TranslationTransform";
    private final String EULER = "EulerTransform";
    private final String SIMILARITY = "SimilarityTransform";
    private final String AFFINE = "AffineTransform";
    private final String SPLINE = "BSplineTransform";


    // TODO - add back translation support? Doesn't appear to be class in itc converters for this?
    private String[] supportedTransforms = new String[] { EULER, SIMILARITY, AFFINE };
    private String parameterFilePath;

    private Transformer transformer;

    public ElastixManager( Transformer transformer ) {
        this.fixedImageFilePaths = new ArrayList<>();
        this.movingImageFilePaths = new ArrayList<>();
        this.transformer = transformer;
    }

    public void openElastix() {
        new ElastixUI( this );
    }

    private void createElastixParameterFile() {
        // TODO - actually read the bitdepth form image
        // TODO - warn  only supporting single channel images here
        Map<Integer, Integer> fixedToMovingChannel = new HashMap<>();
        fixedToMovingChannel.put(1, 1);
        DefaultElastixParametersCreator defaultCreator = new DefaultElastixParametersCreator(
                DefaultElastixParametersCreator.ParameterStyle.Default,
                transformationType,
                numIterations,
                Integer.toString(numSpatialSamples),
                gaussianSmoothingSigmas,
                bSplineGridSpacing,
                finalResampler,
                8,
                fixedToMovingChannel,
                new double[] {1.0,3.0,1.0,1.0,1.0,1.0} // defaults from tischi's elastix plugin
        );

        parameterFilePath = new File(tmpDir, "elastixParameters.txt").getAbsolutePath();
        defaultCreator.getElastixParameters(DefaultElastixParametersCreator.ParameterStyle.Default).writeParameterFile(
                parameterFilePath );
    }

    private ElastixSettings createElastixSettings() {
        ElastixSettings elastixSettings = new ElastixSettings();
        elastixSettings.fixedImageFilePaths = fixedImageFilePaths;
        elastixSettings.movingImageFilePaths = movingImageFilePaths;
        elastixSettings.tmpDir = tmpDir;
        // TODO - read sensible default for this
        elastixSettings.numWorkers = 1;
        elastixSettings.parameterFilePath = parameterFilePath;
        elastixSettings.headless = false;
        elastixSettings.logService = new StderrLogService();
        elastixSettings.elastixDirectory = elastixDirectory;
        elastixSettings.initialTransformationFilePath = "";
        return elastixSettings;
    }

    private void exportElastixResultToCrosshair() {
        File transformResultFile = new File( tmpDir, "TransformParameters.0.txt" );
        try {
            ElastixTransform elastixTransform = ElastixTransform.load( transformResultFile );
            int nDimensions = elastixTransform.FixedImageDimension;
            boolean contains = Arrays.stream(supportedTransforms).anyMatch(elastixTransform.Transform::equals);
            if ( contains ) {

                AffineTransform3D bdvTransform = null;
                switch( elastixTransform.Transform ) {
                    case EULER:
                        // TODO - scaling stuff - how to fix?
                        //TODO - throw error forunexepcted dimesnios?
                        if ( nDimensions == 2 ) {
                            bdvTransform = ElastixEuler2DToAffineTransform3D.convert((ElastixEulerTransform2D) elastixTransform);
                        } else if ( nDimensions == 3 ) {
                            bdvTransform = ElastixEuler3DToAffineTransform3D.convert((ElastixEulerTransform3D) elastixTransform);
                        }

                        // the elastix transform is in mm units, we convert to what was used for rest of images (microns)
                        // bdvTransform = TransformUtils.scaleAffineTransform3DUnits( bdvTransform, new double[]{ 1000, 1000, 1000 } );
                        // System.out.println(new AffineTransform3DToFlatString().convert(bdvTransform).getString());
                        break;
                    case SIMILARITY:
                        if ( nDimensions == 2 ) {
                            bdvTransform = ElastixSimilarity2DToAffineTransform3D.convert( (ElastixSimilarityTransform2D) elastixTransform );
                        } else if ( nDimensions == 3 ) {
                            bdvTransform = ElastixSimilarity3DToAffineTransform3D.convert( (ElastixSimilarityTransform3D) elastixTransform );
                        }
                        break;
                    case AFFINE:
                        if ( nDimensions == 2 ) {
                            bdvTransform = ElastixAffine2DToAffineTransform3D.convert( (ElastixAffineTransform2D) elastixTransform );
                        } else if ( nDimensions == 3 ) {
                            bdvTransform = ElastixAffine3DToAffineTransform3D.convert((ElastixAffineTransform3D) elastixTransform);
                        }
                        break;
                }

                transformer.showSource( bdvTransform );

            } else {
                //TODO - error?
                IJ.log( "Transform type unsupported in Crosshair!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int downsampleFixedImage() {
        return new DownsamplingUI( transformer.getDownsampler() ).chooseSourceLevel(Transformer.ImageType.FIXED);
    }

    public int downsampleMovingImage() {
        return new DownsamplingUI( transformer.getDownsampler() ).chooseSourceLevel(Transformer.ImageType.MOVING);
    }

    public String cropFixedImage() {
            return new CropperUI( transformer.getCropper() ).cropDialog(Transformer.ImageType.FIXED);
    }

    public String cropMovingImage() {
        return new CropperUI( transformer.getCropper() ).cropDialog(Transformer.ImageType.MOVING);
    }

    public void writeImages( String fixedCropName, String movingCropName, int fixedLevel, int movingLevel ) {

        fixedImageFilePaths = new ArrayList<>();
        movingImageFilePaths = new ArrayList<>();

        Exporter exporter = transformer.getExporter();
        if ( fixedCropName == null ) {
            exporter.writeImage( Transformer.ImageType.FIXED, fixedLevel, new File(tmpDir) );
            fixedImageFilePaths.add(
                    new File( tmpDir, exporter.makeImageName(Transformer.ImageType.FIXED, fixedLevel) + ".mhd" ).getAbsolutePath() );
        } else {
            exporter.writeImage( Transformer.ImageType.FIXED, fixedCropName, fixedLevel, new File(tmpDir) );
            fixedImageFilePaths.add(
                    new File( tmpDir, exporter.makeImageName(Transformer.ImageType.FIXED, fixedLevel, fixedCropName) + ".mhd" ).getAbsolutePath() );
        }

        if ( movingCropName == null ) {
            exporter.writeImage( Transformer.ImageType.MOVING, movingLevel, new File(tmpDir) );
            movingImageFilePaths.add(
                    new File( tmpDir, exporter.makeImageName(Transformer.ImageType.MOVING, movingLevel) + ".mhd" ).getAbsolutePath() );
        } else {
            exporter.writeImage( Transformer.ImageType.MOVING, movingCropName, movingLevel, new File(tmpDir) );
            movingImageFilePaths.add(
                    new File( tmpDir, exporter.makeImageName(Transformer.ImageType.MOVING, movingLevel, movingCropName) + ".mhd" ).getAbsolutePath() );
        }
    }

    public void writeInitialTransformixFile( String fixedCropName, String movingCropName ) {

        Cropper cropper = transformer.getCropper();
        AffineTransform3D fullTransform = transformer.getUi().getTree().getFullTransformOfLastSelectedNode();

        // Handle any crops. Recall fullTransform is given from fixed to moving space, so we have to translate
        // to the new fixed image crop, then do the fullTransform from the nodes, then translate by the negative of
        // the new moving image crop
        if ( fixedCropName != null ) {
            RealInterval cropInterval = cropper.getImageCropInterval( Transformer.ImageType.FIXED, fixedCropName );
            AffineTransform3D translationFixedCrop = new AffineTransform3D();
            translationFixedCrop.translate( cropInterval.minAsDoubleArray() );
            fullTransform.preConcatenate( translationFixedCrop );
        }

        if ( movingCropName != null ) {
            RealInterval cropInterval = cropper.getImageCropInterval( Transformer.ImageType.MOVING, movingCropName );
            double[] cropMin = cropInterval.minAsDoubleArray();
            for (int i = 0; i< cropMin.length; i++) {
                cropMin[i] = -1*cropMin[i];
            }
            AffineTransform3D translationMovingCrop = new AffineTransform3D();
            translationMovingCrop.translate( cropMin );
            fullTransform.concatenate( translationMovingCrop );
        }

        ImagePlus fixedImage = transformer.getExporter().getLastFixedImageWritten();
        Double[] voxelSpacingsMillimeter = new Double[3];
        voxelSpacingsMillimeter[0] = fixedImage.getCalibration().pixelWidth;
        voxelSpacingsMillimeter[1] = fixedImage.getCalibration().pixelHeight;
        voxelSpacingsMillimeter[2] = fixedImage.getCalibration().pixelDepth;

        // TODO - warn only works with 3D, no time
        Integer[] dimensionsPixels = new Integer[3];
        for (int i = 0; i<3; i++) {
            dimensionsPixels[i] = fixedImage.getDimensions()[i];
        }

        int bitDepth = fixedImage.getBitDepth();

        // We write directly with whatever spatial units are currently used. No conversion to mm.
        // Elastix will assume they are in mm, but this is no problem as long as all images use the same units
        // and we don't do any scaling on saving and loading
        final ElastixAffineTransform3D elastixAffineTransform3D =
                new BigWarpAffineToElastixAffineTransform3D().convert(
                        fullTransform,
                        voxelSpacingsMillimeter,
                        dimensionsPixels,
                        bitDepth,
                        ElastixTransform.FINAL_LINEAR_INTERPOLATOR,
                        MILLIMETER );

        elastixAffineTransform3D.save( new File(tmpDir, "initialTransform.txt").getAbsolutePath() );
    }

    public void callElastix() {
        createElastixParameterFile();
        new ElastixCaller( createElastixSettings() ).callElastix();

        // TODO - check it waits for finish
        exportElastixResultToCrosshair();
    }
}
