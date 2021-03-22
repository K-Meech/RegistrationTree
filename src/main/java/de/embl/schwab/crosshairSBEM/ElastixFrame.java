package de.embl.schwab.crosshairSBEM;

import de.embl.cba.elastixwrapper.commandline.settings.ElastixSettings;
import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import ij.gui.GenericDialog;
import org.scijava.plugin.Parameter;

import javax.swing.*;
import java.io.File;

public class ElastixFrame extends JFrame {

    private String elastixDirectory;
    private String tmpDir;
    private String transformationType = ElastixParameters.EULER;
    private String bSplineGridSpacing = "50,50,50";
    private int numIterations = 1000;
    private int numSpatialSamples = 10000;
    private String gaussianSmoothingSigmas = "10,10,10";
    private String finalResampler = ElastixParameters.FINAL_RESAMPLER_LINEAR;

    Transformer transformer;
        public ElastixFrame(Transformer transformer) {
            this.transformer = transformer;
            this.getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            createElastixFields();
            this.pack();
            this.show();
        }

        public void createElastixFields() {
            final GenericDialog gd = new GenericDialog("Elastix settings...");
            gd.addDirectoryField("Elastix installation directory", "");
            gd.addDirectoryField("Temporary directory", "");
            String[] sourceNames = new String[transformer.getSourceNames().size()];
            transformer.getSourceNames().toArray(sourceNames);
            gd.addChoice("Fixed Image", sourceNames, sourceNames[0]);
            gd.addChoice("Moving image", sourceNames, sourceNames[1]);
            String[] transformationTypes = new String[]{ElastixParameters.TRANSLATION,
                    ElastixParameters.EULER,
                    ElastixParameters.SIMILARITY,
                    ElastixParameters.AFFINE,
                    ElastixParameters.SPLINE};
            gd.addChoice("Transformation type", transformationTypes, transformationType );
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
                transformationType = gd.getNextChoice();
                bSplineGridSpacing = gd.getNextString();
                numIterations = (int) gd.getNextNumber();
                numSpatialSamples = (int) gd.getNextNumber();
                gaussianSmoothingSigmas = gd.getNextString();

                int[] sourceIndices = new int[]{fixedSourceIndex, movingSourceIndex};
                String[] names = new String[]{sourceNames[fixedSourceIndex], sourceNames[movingSourceIndex]};
                transformer.cropAndWrite(sourceIndices, tmpDir, names );
            }

        }
    }
