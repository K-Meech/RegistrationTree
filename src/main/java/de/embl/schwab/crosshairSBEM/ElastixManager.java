package de.embl.schwab.crosshairSBEM;

import bdv.BigDataViewer;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.elastixwrapper.commandline.ElastixCaller;
import de.embl.cba.elastixwrapper.commandline.TransformixCaller;
import de.embl.cba.elastixwrapper.commandline.settings.ElastixSettings;
import de.embl.cba.elastixwrapper.commandline.settings.TransformixSettings;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.DefaultElastixParametersCreator;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import de.embl.schwab.crosshairSBEM.mhd.MhdHeader;
import de.embl.schwab.crosshairSBEM.mhd.MhdHeaderParser;
import de.embl.schwab.crosshairSBEM.registrationNodes.ElastixRegistrationNode;
import de.embl.schwab.crosshairSBEM.registrationNodes.RegistrationNode;
import de.embl.schwab.crosshairSBEM.ui.CropperUI;
import de.embl.schwab.crosshairSBEM.ui.DownsamplingUI;
import de.embl.schwab.crosshairSBEM.ui.ElastixUI;
import de.embl.schwab.crosshairSBEM.ui.RegistrationTree;
import ij.IJ;
import itc.converters.*;
import itc.transforms.elastix.*;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
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

    public String elastixDirectory;
    public String tmpDir;
    public ElastixParameters.TransformationType transformationType = ElastixParameters.TransformationType.Euler;
    public String bSplineGridSpacing = "50,50,50";
    public int numIterations = 1000;
    public int numSpatialSamples = 10000;
    public String gaussianSmoothingSigmas = "10,10,10";
    public String finalResampler = ElastixParameters.FINAL_RESAMPLER_LINEAR;
    public boolean useFixedMask = false;
    public String fixedMaskXml;
    public ArrayList<String> fixedImageFilePaths;
    public ArrayList<String> movingImageFilePaths;
    public ArrayList<String> fixedMaskFilePaths;

    // Strings in elastix transform files (Transform "")
    private final String TRANSLATION = "TranslationTransform";
    private final String EULER = "EulerTransform";
    private final String SIMILARITY = "SimilarityTransform";
    private final String AFFINE = "AffineTransform";
    private final String SPLINE = "BSplineTransform";

    private ElastixParameters elastixParameters;
    private String transformName;

    // for debugging purposes, if flipped to true it will keep all output files i.e. all transformix file runs
    private boolean debug = true;


    // TODO - add back translation support? Doesn't appear to be class in itc converters for this?
    private String[] supportedTransforms = new String[] { EULER, SIMILARITY, AFFINE };
    private String parameterFilePath;

    private Transformer transformer;

    enum CompensateDirection {
        ToElastix,
        FromElastix
    }

    public ElastixManager( Transformer transformer, File tmpDir ) {
        this.fixedImageFilePaths = new ArrayList<>();
        this.movingImageFilePaths = new ArrayList<>();
        this.transformer = transformer;
        this.tmpDir = tmpDir.getAbsolutePath();
    }

    public void openElastix( String transformName ) {
        this.transformName = transformName;
        new ElastixUI( this );
    }

    private void createElastixParameterFile() {
        // assume single channel and 8 bit (this is what we currently write all mhd images as)
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
        elastixParameters = defaultCreator.getElastixParameters( DefaultElastixParametersCreator.ParameterStyle.Default );
        elastixParameters.writeParameterFile( parameterFilePath );
    }

    private ElastixSettings createElastixSettings() {
        ElastixSettings elastixSettings = new ElastixSettings();
        elastixSettings.fixedImageFilePaths = fixedImageFilePaths;
        elastixSettings.movingImageFilePaths = movingImageFilePaths;
        if ( fixedMaskFilePaths.size() > 0 ) {
            elastixSettings.fixedMaskFilePaths = fixedMaskFilePaths;
        }
        elastixSettings.tmpDir = tmpDir;
        elastixSettings.numWorkers = Runtime.getRuntime().availableProcessors();
        elastixSettings.parameterFilePath = parameterFilePath;
        elastixSettings.headless = false;
        elastixSettings.logService = new StderrLogService();
        elastixSettings.elastixDirectory = elastixDirectory;
        elastixSettings.initialTransformationFilePath = new File(tmpDir, "initialTransform.txt").getAbsolutePath();
        return elastixSettings;
    }

    private TransformixSettings createTransformixSettings() {
        TransformixSettings transformixSettings = new TransformixSettings();
        transformixSettings.elastixDirectory = elastixDirectory;
        transformixSettings.headless = false;
        transformixSettings.movingImageFilePath = movingImageFilePaths.get(0);
        transformixSettings.logService = new StderrLogService();
        transformixSettings.numWorkers = Runtime.getRuntime().availableProcessors();;
        return transformixSettings;
    }

    public void exportElastixResultToCrosshair( String fixedCropName, String movingCropName, int fixedLevel, int movingLevel ) {
        File transformResultFile = new File( tmpDir, "TransformParameters.0.txt" );
        try {
            ElastixTransform elastixTransform = ElastixTransform.load( transformResultFile );
            int nDimensions = elastixTransform.FixedImageDimension;
            boolean contains = Arrays.stream(supportedTransforms).anyMatch(elastixTransform.Transform::equals);
            if ( contains ) {
                AffineTransform3D bdvTransform = null;
                switch( elastixTransform.Transform ) {
                    case EULER:
                        if ( nDimensions == 2 ) {
                            bdvTransform = ElastixEuler2DToAffineTransform3D.convert((ElastixEulerTransform2D) elastixTransform);
                        } else if ( nDimensions == 3 ) {
                            bdvTransform = ElastixEuler3DToAffineTransform3D.convert((ElastixEulerTransform3D) elastixTransform);
                        } else {
                            throw new UnsupportedOperationException("Unsupported number of image dimensions");
                        }
                        break;
                    case SIMILARITY:
                        if ( nDimensions == 2 ) {
                            bdvTransform = ElastixSimilarity2DToAffineTransform3D.convert( (ElastixSimilarityTransform2D) elastixTransform );
                        } else if ( nDimensions == 3 ) {
                            bdvTransform = ElastixSimilarity3DToAffineTransform3D.convert( (ElastixSimilarityTransform3D) elastixTransform );
                        } else {
                            throw new UnsupportedOperationException("Unsupported number of image dimensions");
                        }
                        break;
                    case AFFINE:
                        if ( nDimensions == 2 ) {
                            bdvTransform = ElastixAffine2DToAffineTransform3D.convert( (ElastixAffineTransform2D) elastixTransform );
                        } else if ( nDimensions == 3 ) {
                            bdvTransform = ElastixAffine3DToAffineTransform3D.convert((ElastixAffineTransform3D) elastixTransform);
                        } else {
                            throw new UnsupportedOperationException("Unsupported number of image dimensions");
                        }
                        break;
                }

                bdvTransform = compensateForCrop( fixedCropName, movingCropName, fixedLevel, movingLevel, bdvTransform, CompensateDirection.FromElastix );
                RegistrationTree tree = transformer.getUi().getTree();

                Map<String, RealInterval> fixedCrop = new HashMap<>();
                if ( fixedCropName != null ) {
                    fixedCrop.put( fixedCropName, transformer.getCropper().getImageCropRealIntervalVoxelSpace(Transformer.ImageType.FIXED, fixedCropName) );
                }

                Map<String, RealInterval> movingCrop = new HashMap<>();
                if ( movingCropName != null ) {
                    movingCrop.put( movingCropName, transformer.getCropper().getImageCropRealIntervalVoxelSpace(Transformer.ImageType.MOVING, movingCropName) );
                }

                RegistrationNode registrationNode = new ElastixRegistrationNode(fixedCrop, movingCrop, fixedLevel, movingLevel,
                        useFixedMask, fixedMaskXml, elastixParameters, elastixTransform, bdvTransform, transformName );
                tree.addRegistrationNodeAtLastSelection( registrationNode );
                transformer.showSource( tree.getLastAddedNode() );
            } else {
                throw new UnsupportedOperationException("Transform type unsupported in Crosshair!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Integer downsampleFixedImage( String fixedImageCropName ) {
        if ( fixedImageCropName == null ) {
            return new DownsamplingUI(transformer.getDownsampler()).chooseSourceLevel(Transformer.ImageType.FIXED);
        } else {
            return new DownsamplingUI(transformer.getDownsampler()).chooseSourceLevel(
                    Transformer.ImageType.FIXED, fixedImageCropName);
        }
    }

    public Integer downsampleMovingImage( String movingImageCropName ) {
        if ( movingImageCropName == null ) {
            return new DownsamplingUI(transformer.getDownsampler()).chooseSourceLevel(Transformer.ImageType.MOVING);
        } else {
            return new DownsamplingUI(transformer.getDownsampler()).chooseSourceLevel(Transformer.ImageType.MOVING,
                    movingImageCropName);
        }
    }

    public String cropFixedImage() {
            return new CropperUI( transformer.getCropper() ).cropDialog(Transformer.ImageType.FIXED);
    }

    public String cropMovingImage() {
        return new CropperUI( transformer.getCropper() ).cropDialog(Transformer.ImageType.MOVING);
    }

    private void writeImage(Transformer.ImageType imageType, String cropName, int level ) {
        Exporter exporter = transformer.getExporter();
        File imageFile;
        if ( cropName == null ) {
            imageFile = new File( tmpDir, exporter.makeImageName( imageType, level) + ".mhd" );
            if ( !imageFile.exists() ) {
                exporter.writeImage( imageType, level, new File(tmpDir));
            }

        } else {
            imageFile = new File( tmpDir, exporter.makeImageName( imageType, level, cropName ) + ".mhd" );
            if ( !imageFile.exists() ) {
                exporter.writeImage(imageType, cropName, level, new File(tmpDir));
            }
        }

        if ( imageType == Transformer.ImageType.FIXED ) {
            fixedImageFilePaths.add( imageFile.getAbsolutePath());
        } else {
            movingImageFilePaths.add( imageFile.getAbsolutePath() );
        }
    }

    public void writeMask( Transformer.ImageType imageType, String cropName, int level ) throws SpimDataException {
        if ( imageType == Transformer.ImageType.MOVING ) {
            throw new UnsupportedOperationException( "Moving masks are not yet supported!");
        }

        SpimData maskSpimData = new XmlIoSpimData().load( fixedMaskXml );
        final ArrayList<SourceAndConverter< ? >> sources = new ArrayList<>();
        BigDataViewer.initSetups( maskSpimData, new ArrayList<>(), sources );
        Source maskSource = sources.get(0).getSpimSource();

        Exporter exporter = transformer.getExporter();
        File maskFile;
        if ( cropName == null ) {
            maskFile = new File( tmpDir, exporter.makeMaskName( imageType, level) + ".mhd" );
            if ( !maskFile.exists() ) {
                exporter.writeMask( imageType, maskSpimData, maskSource, level, new File(tmpDir));
            }

        } else {
            maskFile = new File( tmpDir, exporter.makeMaskName( imageType, level, cropName ) + ".mhd" );
            if ( !maskFile.exists() ) {
                exporter.writeMask(imageType, maskSpimData, maskSource, cropName, level, new File(tmpDir));
            }
        }

        if ( imageType == Transformer.ImageType.FIXED ) {
            fixedMaskFilePaths.add( maskFile.getAbsolutePath());
        }
    }

    public void writeImages( String fixedCropName, String movingCropName, int fixedLevel, int movingLevel ) {

        fixedImageFilePaths = new ArrayList<>();
        movingImageFilePaths = new ArrayList<>();
        fixedMaskFilePaths = new ArrayList<>();

        writeImage( Transformer.ImageType.FIXED, fixedCropName, fixedLevel );
        writeImage( Transformer.ImageType.MOVING, movingCropName, movingLevel );
        if ( useFixedMask ) {
            try {
                writeMask( Transformer.ImageType.FIXED, fixedCropName, fixedLevel );
            } catch (SpimDataException e) {
                e.printStackTrace();
            }
        }
    }

    private AffineTransform3D compensateForCrop( String fixedCropName, String movingCropName, int fixedLevel,
                                                 int movingLevel, AffineTransform3D affine,
                                                 CompensateDirection cropCompensateDirection ) {

        // for toElastix: We are trying to adjust our tree transform which is from physical units fixed space
        // (with any base transforms from the xml, or original source) to the physical units moving space
        // (again with any base transforms from the xml, or original source) INTO a transform from cropped voxel fixed
        // space to cropped voxel moving space. In order to do this, we first translate our fixed crop to be at the full
        // sized voxel space position i.e. (+ fixed crop), then we do the base fixed transform (which contains the scaling
        // to physical units, along with any additional affine transforms). Then we do the affines from the tree. This brings
        // us to the full moving space. We must then do the inverse of any base transforms on the moving image to bring us to
        // moving voxel space. Then finally, we translate to the moving crop i.e. (- moving crop).

        // for fromElastix: We are trying to adjust our transform from elastix, to be one that can go on the end
        // of our chain of node transforms. i.e. so that the fixed source can go through the base transforms, then the
        // transforms from the node tree, and then this adjusted elastix transform and come to be aligned with the
        // moving image (which has its base transforms already applied). To do this, we first must do what we did on the
        // toElastix compensation that is not yet included. i.e. we must  do the inverse of the moving image base transforms,
        // then translate to the moving crop i.e. (-moving crop). Then we add the elastix transform. At this point we
        // have remade the chain of transforms that brings us the the moving cropped voxel space. Now we need to get back
        // to the full moving space (i.e. the one with the base transforms and no crop). To do this, we translate by the
        // moving crop to get to the full size moving voxel space, then we do the base transforms of the moving image to
        // get to the full physical units moving space.

        AffineTransform3D fullTransform = new AffineTransform3D();
        AffineTransform3D fixedBaseTransform = transformer.getBaseTransform( Transformer.ImageType.FIXED, fixedLevel );
        AffineTransform3D movingBaseTransform = transformer.getBaseTransform( Transformer.ImageType.MOVING, movingLevel );
        Cropper cropper = transformer.getCropper();

        if ( cropCompensateDirection == CompensateDirection.ToElastix ) {
            if ( movingCropName != null ) {
                AffineTransform3D translationMovingCrop = cropper.getCropTranslationVoxelSpace(
                        Transformer.ImageType.MOVING, true, movingCropName, movingLevel);
                fullTransform.concatenate( translationMovingCrop );
            }

            fullTransform.concatenate( movingBaseTransform.inverse() );
            fullTransform.concatenate( affine );
            fullTransform.concatenate( fixedBaseTransform );

            if ( fixedCropName != null ) {
                AffineTransform3D translationFixedCrop = cropper.getCropTranslationVoxelSpace(
                        Transformer.ImageType.FIXED, false, fixedCropName,fixedLevel );
                fullTransform.concatenate( translationFixedCrop );
            }

        } else {
            AffineTransform3D translationPositiveMovingCrop = null;
            AffineTransform3D translationNegativeMovingCrop = null;

            fullTransform.concatenate( movingBaseTransform );

            if ( movingCropName != null ) {
                translationPositiveMovingCrop = cropper.getCropTranslationVoxelSpace(
                        Transformer.ImageType.MOVING, false, movingCropName, movingLevel);
                translationNegativeMovingCrop = cropper.getCropTranslationVoxelSpace(
                        Transformer.ImageType.MOVING, true, movingCropName, movingLevel);
                fullTransform.concatenate( translationPositiveMovingCrop );
            }

            fullTransform.concatenate( affine );

            if ( translationNegativeMovingCrop != null ) {
                fullTransform.concatenate( translationNegativeMovingCrop );
            }

            fullTransform.concatenate( movingBaseTransform.inverse() );
        }

        return fullTransform;
    }

    // // for fixed crop written with physical coordinate spacing, to moving crop written with physical coordinate spacing
    // private AffineTransform3D compensateForCrop( String fixedCropName, String movingCropName, int fixedLevel,
    //                                              int movingLevel, AffineTransform3D affine,
    //                                              CompensateDirection cropCompensateDirection ) {
    //
    //     // for toElastix: We are trying to adjust our tree transform which is from full fixed space to full moving space,
    //     // to a transfrom from cropped fixed space to cropped moving space. For this, we first translate our fixed crop
    //     // to be at the full size fixed space position i.e. ( + fixed crop), then we do the affines from the tree. This brings
    //     // us to full moving space. We must then translate to the moving crop i.e. ( - moving crop)
    //
    //     // for fromElastix: With toElastix, we first compensated for teh fixed crop to bring it into the full size fixed space.
    //     // Then we did the affines from the tree, then compensated for the moving crop. To apply this in the fromElastix
    //     // scenario we don't need to compensate for teh fixed crop as we are already in the full size fixed space. We already
    //     // include the affines from the tree, so we just need to compensate for the moving crop (in the same way as toElastix).
    //     // Then do the elastix transforms. Then translate + moving Crop to get it back to the full moving space.
    //
    //     AffineTransform3D fullTransform = new AffineTransform3D();
    //     Cropper cropper = transformer.getCropper();
    //
    //     if ( cropCompensateDirection == CompensateDirection.ToElastix ) {
    //         if ( movingCropName != null ) {
    //             AffineTransform3D translationMovingCrop = cropper.getCropTranslationPhysicalSpace(
    //                     Transformer.ImageType.MOVING, true, movingCropName, movingLevel);
    //             fullTransform.concatenate( translationMovingCrop );
    //         }
    //
    //         fullTransform.concatenate( affine );
    //
    //         if ( fixedCropName != null ) {
    //             AffineTransform3D translationFixedCrop = cropper.getCropTranslationPhysicalSpace(
    //                     Transformer.ImageType.FIXED, false, fixedCropName,fixedLevel );
    //             fullTransform.concatenate( translationFixedCrop );
    //         }
    //
    //     } else {
    //         AffineTransform3D translationPositiveMovingCrop = null;
    //         AffineTransform3D translationNegativeMovingCrop = null;
    //         if ( movingCropName != null ) {
    //             translationPositiveMovingCrop = cropper.getCropTranslationPhysicalSpace(
    //                     Transformer.ImageType.MOVING, false, movingCropName, movingLevel);
    //             translationNegativeMovingCrop = cropper.getCropTranslationPhysicalSpace(
    //                     Transformer.ImageType.MOVING, true, movingCropName, movingLevel);
    //             fullTransform.concatenate( translationPositiveMovingCrop );
    //         }
    //
    //         fullTransform.concatenate( affine );
    //
    //         if ( translationNegativeMovingCrop != null ) {
    //             fullTransform.concatenate( translationNegativeMovingCrop );
    //         }
    //     }
    //
    //     return fullTransform;
    // }

    public void writeInitialTransformixFile( String fixedCropName, String movingCropName, int fixedLevel, int movingLevel ) {

        AffineTransform3D fullTransform = transformer.getUi().getTree().getLastSelectedNode().getFullTransform();
        fullTransform = compensateForCrop( fixedCropName, movingCropName, fixedLevel, movingLevel, fullTransform, CompensateDirection.ToElastix );

        File fixedImageFile;
        if ( fixedCropName != null ) {
            fixedImageFile = new File( tmpDir,
                    transformer.getExporter().makeImageName(Transformer.ImageType.FIXED, fixedLevel, fixedCropName) + ".mhd" );
        } else {
            fixedImageFile = new File( tmpDir,
                    transformer.getExporter().makeImageName(Transformer.ImageType.FIXED, fixedLevel ) + ".mhd" );
        }

        if ( fixedImageFile.exists() ) {
            MhdHeader header = new MhdHeaderParser( fixedImageFile.getAbsolutePath() ).parseHeader();

            if ( header.nDims > 3 ) {
                IJ.log( "Stopping... Only up to 3 image dimensions is supported.");
            } else if ( !header.elementType.equals("MET_UCHAR") ) {
                IJ.log( "Stopping... Only 8-bit images supported" );
            } else {
                int bitDepth = 8;

                Double[] voxelSpacingsMillimeter = new Double[ header.nDims ];
                for ( int i= 0; i< voxelSpacingsMillimeter.length; i++ ) {
                    voxelSpacingsMillimeter[i] = header.elementSize[i];
                }

                Integer[] dimensionsPixels = new Integer[ header.nDims ];
                for (int i = 0; i < dimensionsPixels.length; i++) {
                    dimensionsPixels[i] = header.dimSize[i];
                }

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
                                MILLIMETER);

                elastixAffineTransform3D.save(new File(tmpDir, "initialTransform.txt").getAbsolutePath());
            }
        }
    }

    public void callElastix() {
        createElastixParameterFile();
        new ElastixCaller( createElastixSettings() ).callElastix();

        if ( debug ) {
            callTransformix();
        }
    }

    private void callTransformix() {
        TransformixSettings transformixSettings;
        transformixSettings = createTransformixSettings();
        File initialTransformDir =  new File(tmpDir, "initialTransformTransformix");
        if (!initialTransformDir.exists()) {
            initialTransformDir.mkdirs();
        }
        transformixSettings.tmpDir = initialTransformDir.getAbsolutePath();
        transformixSettings.transformationFilePath = new File(tmpDir, "initialTransform.txt").getAbsolutePath();
        new TransformixCaller(transformixSettings).callTransformix();

        transformixSettings = createTransformixSettings();
        File fullTransformDir = new File(tmpDir, "fullTransformTransformix");
        if (!fullTransformDir.exists()) {
            fullTransformDir.mkdirs();
        }
        transformixSettings.tmpDir = fullTransformDir.getAbsolutePath();
        transformixSettings.transformationFilePath = new File(tmpDir, "TransformParameters.0.txt").getAbsolutePath();
        new TransformixCaller(transformixSettings).callTransformix();
    }
}
