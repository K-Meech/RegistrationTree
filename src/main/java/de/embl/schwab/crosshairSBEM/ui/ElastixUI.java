package de.embl.schwab.crosshairSBEM.ui;

import de.embl.cba.elastixwrapper.commandline.ElastixCaller;
import de.embl.cba.elastixwrapper.commandline.settings.ElastixSettings;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.DefaultElastixParametersCreator;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import de.embl.schwab.crosshairSBEM.ElastixManager;
import de.embl.schwab.crosshairSBEM.Transformer;
import ij.gui.GenericDialog;
import org.scijava.log.StderrLogService;
import org.scijava.plugin.Parameter;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ElastixUI {



    Transformer transformer;
    ElastixManager elastixManager;
        public ElastixUI(Transformer transformer) {
            this.transformer = transformer;
            this.elastixManager = new ElastixManager();
            createElastixFields();
        }

        public void createElastixFields() {
            final GenericDialog gd = new GenericDialog("Elastix settings...");
            gd.addDirectoryField("Elastix installation directory", elastixManager.elastixDirectory);
            gd.addDirectoryField("Temporary directory", elastixManager.tmpDir);
            String[] sourceNames = new String[transformer.getSourceNames().size()];
            transformer.getSourceNames().toArray(sourceNames);
            gd.addChoice("Fixed Image", sourceNames, sourceNames[0]);
            gd.addChoice("Moving image", sourceNames, sourceNames[1]);
            String[] transformationTypes = new String[]{
                    ElastixParameters.EULER,
                    ElastixParameters.SIMILARITY,
                    ElastixParameters.AFFINE,
                    ElastixParameters.SPLINE};
            gd.addChoice("Transformation type", transformationTypes, elastixManager.transformationType.name() );
            gd.addStringField("Grid spacing for BSpline transformation [voxels]", elastixManager.bSplineGridSpacing);
            gd.addNumericField("Number of iterations", elastixManager.numIterations);
            gd.addNumericField("Number of spatial samples", elastixManager.numSpatialSamples );
            gd.addStringField("Gaussian smoothing sigma [voxels]", elastixManager.gaussianSmoothingSigmas );
            String[] resamplers = new String[]{ElastixParameters.FINAL_RESAMPLER_LINEAR,
                    ElastixParameters.FINAL_RESAMPLER_NEAREST_NEIGHBOR};
            gd.addChoice("Final resampler", resamplers, elastixManager.finalResampler );
            gd.showDialog();

            if (!gd.wasCanceled()) {
                setParametersInElastixManager( gd );
                elastixManager.callElastix();
            }

        }

        public void setParametersInElastixManager( GenericDialog gd ) {
            elastixManager.elastixDirectory = gd.getNextString();
            elastixManager.tmpDir = gd.getNextString();
            int fixedSourceIndex = gd.getNextChoiceIndex();
            int movingSourceIndex = gd.getNextChoiceIndex();
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

            int[] sourceIndices = new int[]{fixedSourceIndex, movingSourceIndex};
            String[] names = new String[]{"fixed", "moving"};
            transformer.cropAndWrite(sourceIndices, elastixManager.tmpDir, names );

            elastixManager.fixedImageFilePaths.add( new File( elastixManager.tmpDir, names[0] + ".mhd" ).getAbsolutePath() );
            elastixManager.movingImageFilePaths.add( new File( elastixManager.tmpDir, names[1] + ".mhd").getAbsolutePath() );
        }


    }
