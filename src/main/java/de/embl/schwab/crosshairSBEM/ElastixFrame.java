package de.embl.schwab.crosshairSBEM;

import de.embl.cba.elastixwrapper.commandline.ElastixCaller;
import de.embl.cba.elastixwrapper.commandline.settings.ElastixSettings;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.DefaultElastixParametersCreator;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import ij.gui.GenericDialog;
import org.scijava.log.StderrLogService;
import org.scijava.plugin.Parameter;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ElastixFrame extends JFrame {

    // TODO - remove these file deafults
    private String elastixDirectory = "C:\\Users\\meechan\\Documents\\elastix-4.9.0-win64";
    private String tmpDir = "C:\\Users\\meechan\\Documents\\temp\\CrosshairElastixTesting";
    private ElastixParameters.TransformationType transformationType = ElastixParameters.TransformationType.Euler;
    private String bSplineGridSpacing = "50,50,50";
    private int numIterations = 1000;
    private int numSpatialSamples = 10000;
    private String gaussianSmoothingSigmas = "10,10,10";
    private String finalResampler = ElastixParameters.FINAL_RESAMPLER_LINEAR;
    private ArrayList<String> fixedImageFilePaths;
    private ArrayList<String> movingImageFilePaths;

    Transformer transformer;
        public ElastixFrame(Transformer transformer) {
            this.transformer = transformer;
            this.fixedImageFilePaths = new ArrayList<>();
            this.movingImageFilePaths = new ArrayList<>();
            this.getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            createElastixFields();
            this.pack();
            this.show();
        }

        public void createElastixFields() {
            final GenericDialog gd = new GenericDialog("Elastix settings...");
            gd.addDirectoryField("Elastix installation directory", elastixDirectory);
            gd.addDirectoryField("Temporary directory", tmpDir);
            String[] sourceNames = new String[transformer.getSourceNames().size()];
            transformer.getSourceNames().toArray(sourceNames);
            gd.addChoice("Fixed Image", sourceNames, sourceNames[0]);
            gd.addChoice("Moving image", sourceNames, sourceNames[1]);
            String[] transformationTypes = new String[]{ElastixParameters.TRANSLATION,
                    ElastixParameters.EULER,
                    ElastixParameters.SIMILARITY,
                    ElastixParameters.AFFINE,
                    ElastixParameters.SPLINE};
            gd.addChoice("Transformation type", transformationTypes, transformationType.name() );
            gd.addStringField("Grid spacing for BSpline transformation [voxels]", bSplineGridSpacing);
            gd.addNumericField("Number of iterations", numIterations);
            gd.addNumericField("Number of spatial samples", numSpatialSamples );
            gd.addStringField("Gaussian smoothing sigma [voxels]", gaussianSmoothingSigmas );
            String[] resamplers = new String[]{ElastixParameters.FINAL_RESAMPLER_LINEAR,
                    ElastixParameters.FINAL_RESAMPLER_NEAREST_NEIGHBOR};
            gd.addChoice("Final resampler", resamplers, finalResampler );
            gd.showDialog();

            if (!gd.wasCanceled()) {
                elastixDirectory = gd.getNextString();
                tmpDir = gd.getNextString();
                int fixedSourceIndex = gd.getNextChoiceIndex();
                int movingSourceIndex = gd.getNextChoiceIndex();
                String transformationTypeString = gd.getNextChoice();

                switch (transformationTypeString) {
                    case ElastixParameters.TRANSLATION:
                        transformationType = ElastixParameters.TransformationType.Translation;
                        break;
                    case ElastixParameters.EULER:
                        transformationType = ElastixParameters.TransformationType.Euler;
                        break;
                    case ElastixParameters.SIMILARITY:
                        transformationType = ElastixParameters.TransformationType.Similarity;
                        break;
                    case ElastixParameters.AFFINE:
                        transformationType = ElastixParameters.TransformationType.Affine;
                        break;
                    case ElastixParameters.SPLINE:
                        transformationType = ElastixParameters.TransformationType.BSpline;
                        break;
                }


                bSplineGridSpacing = gd.getNextString();
                numIterations = (int) gd.getNextNumber();
                numSpatialSamples = (int) gd.getNextNumber();
                gaussianSmoothingSigmas = gd.getNextString();

                int[] sourceIndices = new int[]{fixedSourceIndex, movingSourceIndex};
                String[] names = new String[]{"fixed", "moving"};
                transformer.cropAndWrite(sourceIndices, tmpDir, names );

                fixedImageFilePaths.add( new File( tmpDir, names[0] + ".mhd" ).getAbsolutePath() );
                movingImageFilePaths.add( new File( tmpDir, names[1] + ".mhd").getAbsolutePath() );

                triggerElastix();
            }

        }

        public void triggerElastix() {
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

            String parameterFilePath = new File(tmpDir, "elastixParameters.txt").getAbsolutePath();
            defaultCreator.getElastixParameters(DefaultElastixParametersCreator.ParameterStyle.Default).writeParameterFile(
                    parameterFilePath );
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

            new ElastixCaller( elastixSettings ).callElastix();
        }
    }
