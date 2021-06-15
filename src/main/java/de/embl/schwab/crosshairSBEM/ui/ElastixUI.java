package de.embl.schwab.crosshairSBEM.ui;

import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import de.embl.schwab.crosshairSBEM.ElastixManager;
import de.embl.schwab.crosshairSBEM.Transformer;
import ij.gui.GenericDialog;

import java.io.File;

public class ElastixUI {

    private ElastixManager elastixManager;

        // TODO - make it optional whether to overwrite images
        // Keep elastix metadata from all runs i.e. elastixparameters an transformix files

        public ElastixUI( ElastixManager elastixManager ) {
            this.elastixManager = elastixManager;
            createElastixParameterDialog();
        }

        public void createElastixParameterDialog() {
            final GenericDialog gd = new GenericDialog("Elastix settings...");
            gd.addDirectoryField("Elastix installation directory", elastixManager.elastixDirectory);

            String[] transformationTypes = new String[]{
                    ElastixParameters.EULER,
                    ElastixParameters.SIMILARITY,
                    ElastixParameters.AFFINE,
                    // TODO - can we support spline?
                    // ElastixParameters.SPLINE
            };
            gd.addChoice("Transformation type", transformationTypes, elastixManager.transformationType.name() );
            gd.addStringField("Grid spacing for BSpline transformation [voxels]", elastixManager.bSplineGridSpacing);
            gd.addNumericField("Number of iterations", elastixManager.numIterations);
            gd.addNumericField("Number of spatial samples", elastixManager.numSpatialSamples );
            gd.addStringField("Gaussian smoothing sigma [voxels]", elastixManager.gaussianSmoothingSigmas );
            String[] resamplers = new String[]{ElastixParameters.FINAL_RESAMPLER_LINEAR,
                    ElastixParameters.FINAL_RESAMPLER_NEAREST_NEIGHBOR};
            gd.addChoice("Final resampler", resamplers, elastixManager.finalResampler );
            gd.addCheckbox("Crop Fixed Image", true );
            gd.addCheckbox("Crop Moving Image", true);
            gd.addCheckbox( "Downsample Fixed Image", true);
            gd.addCheckbox( "Downsample Moving Image", true );
            gd.showDialog();

            if (!gd.wasCanceled()) {
                setParametersInElastixManager( gd );

                String fixedCropName = null;
                String movingCropName = null;
                // crop fixed image
                if ( gd.getNextBoolean() ) {
                    fixedCropName = elastixManager.cropFixedImage();
                }

                // crop moving image
                if ( gd.getNextBoolean() ) {
                    movingCropName = elastixManager.cropMovingImage();
                }

                int fixedLevel = 0;
                int movingLevel = 0;
                // downsample fixed image
                if ( gd.getNextBoolean() ) {
                    fixedLevel = elastixManager.downsampleFixedImage();
                }

                // downsample moving image
                if ( gd.getNextBoolean() ) {
                    movingLevel = elastixManager.downsampleMovingImage();
                }

                elastixManager.writeImages( fixedCropName, movingCropName, fixedLevel, movingLevel );
                elastixManager.writeInitialTransformixFile( fixedCropName, movingCropName, fixedLevel, movingLevel );
                elastixManager.callElastix();
                elastixManager.exportElastixResultToCrosshair( fixedCropName, movingCropName, fixedLevel, movingLevel );
            }

        }

        public void setParametersInElastixManager( GenericDialog gd ) {
            // TODO -make sure no settings persist between runs
            elastixManager.elastixDirectory = gd.getNextString();
            String transformationTypeString = gd.getNextChoice();

            switch (transformationTypeString) {
                case ElastixParameters.TRANSLATION:
                    elastixManager.transformationType = ElastixParameters.TransformationType.Translation;
                    break;
                case ElastixParameters.EULER:
                    elastixManager.transformationType = ElastixParameters.TransformationType.Euler;
                    break;
                case ElastixParameters.SIMILARITY:
                    elastixManager.transformationType = ElastixParameters.TransformationType.Similarity;
                    break;
                case ElastixParameters.AFFINE:
                    elastixManager.transformationType = ElastixParameters.TransformationType.Affine;
                    break;
                case ElastixParameters.SPLINE:
                    elastixManager.transformationType = ElastixParameters.TransformationType.BSpline;
                    break;
            }


            elastixManager.bSplineGridSpacing = gd.getNextString();
            elastixManager.numIterations = (int) gd.getNextNumber();
            elastixManager.numSpatialSamples = (int) gd.getNextNumber();
            elastixManager.gaussianSmoothingSigmas = gd.getNextString();


        }


    }
