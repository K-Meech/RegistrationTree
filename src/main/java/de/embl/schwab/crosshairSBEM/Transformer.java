package de.embl.schwab.crosshairSBEM;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
// import de.embl.cba.bdv.utils.export.BdvRealSourceToVoxelImageExporter;
import de.embl.schwab.crosshairSBEM.ui.ElastixUI;
import de.embl.schwab.crosshairSBEM.ui.RegistrationTree;
import de.embl.schwab.crosshairSBEM.ui.Ui;
import itc.commands.BigWarpAffineToTransformixFileCommand;
import itc.converters.AffineTransform3DToFlatString;
import itc.transforms.elastix.ElastixTransform;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import org.apache.commons.compress.utils.FileNameUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static de.embl.schwab.crosshairSBEM.SwingUtils.getButton;

// TODO - is soruce index consistent
/**
 * Stores information on fixed and moving sources
 * Entry point for all transformers
 */
public class Transformer {

    public enum TransformType {
        BigWarp,
        Elastix,
        Manual,
        AffineString
    }

    public enum ImageType {
        FIXED,
        MOVING
    }

    public enum ViewSpace {
        FIXED,
        MOVING
    }

    private SpimData fixedSpimData;
    private BdvStackSource fixedSource;
    private SpimData movingSpimData;
    private BdvStackSource movingSource;

    private TransformedSource<?> fixedTransformedSource;
    private TransformedSource<?> movingTransformedSource;

    private File fixedImage;
    private File movingImage;

    private Ui ui;
    private BigWarpManager bigWarpManager;
    private ElastixManager elastixManager;
    private Cropper cropper;

    private ViewSpace viewSpace = ViewSpace.FIXED;

    // String[] sourcePaths = new String[] {"C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack.xml",
    //         "C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack-rotated.xml" };
    String[] sourcePaths = new String[] {"C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack.xml",
            "C:\\Users\\meechan\\Documents\\sample_register_images\\zebra.xml" };
    ArrayList<SpimData> spimSources = new ArrayList<>();
    ArrayList<String> sourceNames = new ArrayList<>();
    ArrayList<TransformedSource<?>> transformedSources = new ArrayList<>();

    String tempDir = "C:\\Users\\meechan\\Documents\\main.java.de.embl.schwab.crosshairSBEM.temp.temp\\exportTest";

    BdvHandle bdv;
    BigWarp bw;
    int fixedSourceIndex;
    int movingSourceIndex;

    public Transformer( File movingImage, File fixedImage ) {
        try {
            ui = new Ui( this );
            loadSources(movingImage, fixedImage);
            this.fixedImage = fixedImage;
            this.movingImage = movingImage;
            bigWarpManager = new BigWarpManager( this );
            elastixManager = new ElastixManager( this );
            cropper = new Cropper( this );
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getSourceNames() {
        return sourceNames;
    }

    public BigWarpManager getBigWarpManager() {
        return bigWarpManager;
    }

    public ElastixManager getElastixManager() {
        return elastixManager;
    }

    public ViewSpace getViewSpace() {
        return viewSpace;
    }

    public Cropper getCropper() {
        return cropper;
    }

    public BdvHandle getBdv() {
        return bdv;
    }

    public void refreshBdvWindow() {
        bdv.getViewerPanel().requestRepaint();
    }

    public AffineTransform3D getTransform( ImageType imageType ) {
        AffineTransform3D affine = new AffineTransform3D();
        if ( imageType == ImageType.FIXED ) {
            fixedTransformedSource.getFixedTransform(affine);
        } else {
            movingTransformedSource.getFixedTransform(affine);
        }
        return affine;
    }

    public void setTransform( ImageType imageType, AffineTransform3D affine ) {
        if ( imageType == ImageType.FIXED ) {
            fixedTransformedSource.setFixedTransform(affine);
        } else {
            movingTransformedSource.setFixedTransform( affine );
        }
    }

    public void run() {



        // TODO - set names of sources to be root of filename?

//         final LazySpimSource emSource = new LazySpimSource("em", pathToFixed);
// //        final LazySpimSource xraySource = new LazySpimSource("xray", "Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\original_hdf5\\high_res_flip_z_bigwarped.xml");
//         final LazySpimSource xraySource = new LazySpimSource("xray", pathToMoving);

        // fixedDimensions = new double[3];
        // movingDimensions = new double[3];

        // Allows opening of hdf5 images in big warp
        // BigWarpBdvCommand bwcommand = new BigWarpBdvCommand();
        // bwcommand.fixedImageXml = new File("C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack.xml");
        // bwcommand.movingImageXml = new File("C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack-rotated.xml");
        // bwcommand.run();

        //
        // JFrame testInterface = new JFrame();
        // JPanel content = new JPanel();
        // testInterface.setContentPane(content);
        //
        // ActionListener generalListener = new GeneralListener();
        //
        // JButton openBigwarpButton = new JButton("Open Bigwarp");
        // openBigwarpButton.setActionCommand("open_bigwarp");
        // openBigwarpButton.addActionListener(generalListener);
        //
        // JButton displayBigwarpTransform = new JButton("Display Bigwarp");
        // displayBigwarpTransform.setActionCommand("display_bigwarp");
        // displayBigwarpTransform.addActionListener(generalListener);
        //
        // JButton invertBigwarpTransform = new JButton("Invert Display");
        // invertBigwarpTransform.setActionCommand("invert_display");
        // invertBigwarpTransform.addActionListener(generalListener);
        //
        // JButton cropDialogB = new JButton("crop");
        // cropDialogB.setActionCommand("crop_dialog");
        // cropDialogB.addActionListener(generalListener);
        //
        // content.add(openBigwarpButton);
        // content.add(displayBigwarpTransform);
        // content.add(invertBigwarpTransform);
        // content.add(cropDialogB);
        //
        // testInterface.pack();
        // testInterface.show();


        // minor goal
        // crop in micron coords both images, always in their own image space - remember the values


        // open bigwarp programatically, have them save their landmarks then exit.
        // Can then open those landmarks and based on selected transform, show image in that way

        // can get affine transform from the landmarks sortof directly like here:
        //https://github.com/saalfeldlab/bigwarp/blob/master/scripts/Bigwarp_affinePart.groovy

        // here transform type is changed
        //https://github.com/saalfeldlab/bigwarp/blob/a194507e53b875cfa076c5977f6145bb244b11a6/src/main/java/bdv/gui/TransformTypeSelectDialog.java#L17

        // affine exprot
        //https://github.com/saalfeldlab/bigwarp/blob/86b78a1967732e2f689f5f00f6a98fa3d9f2fcbf/src/main/java/bigwarp/BigWarpActions.java#L1047

        // getting the affine and printing it is all in top bigWarp class
        // https://github.com/saalfeldlab/bigwarp/blob/72abfa1940da656f1f41691be511e9e1023ebb85/src/main/java/bigwarp/BigWarp.java#L1226

        // changes the transform display here
        // https://github.com/saalfeldlab/bigwarp/blob/a194507e53b875cfa076c5977f6145bb244b11a6/src/main/java/bigwarp/BigWarp.java#L2477

        // opening bigwarp from RAI?

        // heart of the manual transformer
    }

    public void openBigwarp() {
        bigWarpManager.openBigwarp(movingSpimData, fixedSpimData, movingImage.getAbsolutePath());
    }

    public void openElastix() {
        new ElastixUI( elastixManager );
    }

    private void showSource( SpimData source ) {

    }

    private void loadSources( File movingImage, File fixedImage ) throws SpimDataException {

        String fixedImagePath = fixedImage.getAbsolutePath();
        String movingImagePath = movingImage.getAbsolutePath();
        fixedSpimData = new XmlIoSpimData().load( fixedImagePath );
        movingSpimData = new XmlIoSpimData().load( movingImagePath );

        String fixedSourceName = FileNameUtils.getBaseName( fixedImagePath );
        String movingSourceName = FileNameUtils.getBaseName( movingImagePath );

        // TODO -rename the source somehow so appears nicely in bdv pullout (how is this so difficult to find?)

        fixedSource = BdvFunctions.show(fixedSpimData).get(0);
        bdv = fixedSource.getBdvHandle();

        Window viewFrame = SwingUtilities.getWindowAncestor(bdv.getViewerPanel());
        Point treeLocation = ui.getLocationOnScreen();
        viewFrame.setLocation(
                treeLocation.x + ui.getWidth(),
                 treeLocation.y );

        fixedSource.setDisplayRange(0, 255);
        fixedTransformedSource = (TransformedSource<?>) ((SourceAndConverter<?>) fixedSource.getSources().get(0)).getSpimSource();

        movingSource = BdvFunctions.show(movingSpimData, BdvOptions.options().addTo(bdv)).get(0);
        movingSource.setDisplayRange(0, 255);
        movingTransformedSource = (TransformedSource<?>) ((SourceAndConverter<?>) movingSource.getSources().get(0)).getSpimSource();
    }

    private double[] getFullResolutionSourceVoxelSize( SpimData spimData ) {
        double[] sourceDimensions = new double[3];
        spimData.getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().dimensions(sourceDimensions);
        return sourceDimensions;
    }

    private double[] getSourceVoxelSize( SpimData spimData, BdvStackSource bdvStackSource,  int level ) {
        long[] fullResolutionVoxelDimensions = getSourceVoxelDimensions( bdvStackSource, 0 );
        double[] fullResolutionVoxelSize = getFullResolutionSourceVoxelSize( spimData );

        if ( level == 0 ) {
            return fullResolutionVoxelSize;
        } else {
            long[] downsampledResolutionVoxelDimensions = getSourceVoxelDimensions( bdvStackSource, level );
            double[] downsampledResolutionVoxelSize = new double[3];
            for ( int i = 0; i< fullResolutionVoxelDimensions.length; i++ ) {
                downsampledResolutionVoxelSize[i] = fullResolutionVoxelSize[i] *
                        ( (double) fullResolutionVoxelDimensions[i] / (double) downsampledResolutionVoxelDimensions[i] );
            }
            return downsampledResolutionVoxelSize;
        }
    }

    private String getSourceUnit( SpimData spimData ) {
        return spimData.getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().unit();
    }

    private long[] getSourceVoxelDimensions( BdvStackSource bdvStackSource, int level ) {

        Source spimSource = ((SourceAndConverter<?>) bdvStackSource.getSources().get(0) ).getSpimSource();

        // TODO - warn doesn't support time series
        long[] dimensions = new long[3];
        spimSource.getSource( 0, level ).dimensions( dimensions );
        return dimensions;
    }

    private int getSourceNumberOfLevels( BdvStackSource bdvStackSource ) {
        Source spimSource = ((SourceAndConverter<?>) bdvStackSource.getSources().get(0) ).getSpimSource();
        return spimSource.getNumMipmapLevels();
    }

    public Source getSource( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return ((SourceAndConverter<?>) fixedSource.getSources().get(0) ).getSpimSource();
        } else {
            return ((SourceAndConverter<?>) movingSource.getSources().get(0) ).getSpimSource();
        }
    }
    public double[] getSourceVoxelSize( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return getFullResolutionSourceVoxelSize(fixedSpimData);
        } else {
            return getFullResolutionSourceVoxelSize(movingSpimData);
        }
    }

    public double[] getSourceVoxelSize( ImageType imageType, int level ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceVoxelSize( fixedSpimData, fixedSource, level );
        } else {
            return getSourceVoxelSize( movingSpimData, movingSource, level );
        }
    }

    public String getSourceUnit( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceUnit(fixedSpimData);
        } else {
            return getSourceUnit(movingSpimData);
        }
    }

    public long[] getSourceVoxelDimensions( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceVoxelDimensions(fixedSource, 0);
        } else {
            return getSourceVoxelDimensions( movingSource, 0 );
        }
    }

    public void addSource( Source source ) {
        BdvStackSource stackSource = BdvFunctions.show(source, BdvOptions.options().addTo(bdv));
        // TODO - generalise?
        stackSource.setDisplayRange(0, 255);
        refreshBdvWindow();
    }

    public long[] getSourceVoxelDimensions( ImageType imageType, int level ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceVoxelDimensions( fixedSource, level );
        } else {
            return getSourceVoxelDimensions( movingSource, level );
        }
    }

    public int getSourceNumberOfLevels( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceNumberOfLevels( fixedSource );
        } else {
            return getSourceNumberOfLevels( movingSource );
        }
    }

    public RandomAccessibleInterval getRAI( ImageType imageType, int level ) {

        Source spimSource;
        if ( imageType == ImageType.FIXED ) {
            spimSource = ((SourceAndConverter<?>) fixedSource.getSources().get(0) ).getSpimSource();
        } else {
            spimSource = ((SourceAndConverter<?>) movingSource.getSources().get(0) ).getSpimSource();
        }
        return spimSource.getSource( 0, level);
    }

    // class GeneralListener implements ActionListener {
    //     public void actionPerformed(ActionEvent e) {
    //         if (e.getActionCommand().equals("open_bigwarp")) {
    //             openBigwarp();
    //         } else if (e.getActionCommand().equals("display_bigwarp")) {
    //             ;
    //         } else if (e.getActionCommand().equals("invert_display")) {
    //         invert();
    //         } else if (e.getActionCommand().equals("crop_dialog")) {
    //             new Thread( () -> {
    //                 final GenericDialog gd = new GenericDialog( "Choose image to crop..." );
    //                 String[] imageNames = new String[sourceNames.size()];
    //                 for ( int i = 0; i < imageNames.length; i++ ) {
    //                     imageNames[i] = sourceNames.get(i);
    //                 }
    //                 gd.addChoice("Image to crop..", imageNames, imageNames[0]);
    //                 gd.showDialog();
    //
    //                 if ( !gd.wasCanceled() ) {
    //                     int sourceIndex = gd.getNextChoiceIndex();
    //                     TransformedRealBoxSelectionDialog.Result result = cropDialog( sourceIndex );
    //                     writeCrop( result, sourceIndex );
    //                 }
    //             } ).start();
    //         }
    //     }
    // }









    private void writeFixedTransformToTransformixFile( TransformedSource<?> fixedSource ){
        AffineTransform3D fixedTransform = new AffineTransform3D();
        fixedSource.getFixedTransform( fixedTransform );
        BigWarpAffineToTransformixFileCommand bw = new BigWarpAffineToTransformixFileCommand();
        bw.affineTransformString = new AffineTransform3DToFlatString().convert(fixedTransform).getString();
        bw.affineTransformUnit = "micrometer";
        bw.interpolation = ElastixTransform.FINAL_LINEAR_INTERPOLATOR;
        bw.transformationOutputFile = new File("Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\targeting_test\\elastix_flipped_xray_to_em\\initialTransform.txt");
        bw.targetImageFile = new File("Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\targeting_test\\elastix_flipped_xray_to_em\\065_9_high_res.tif");
        bw.run();
    }

    // CHANGE SO optionally can set names of written volumes + can write directly to mhd


    private void invert() {
        // AffineTransform3D bigWarp = bw.affine3d();
        // bigWarp.inverse();
        AffineTransform3D bigWarp = bw.getMovingToFixedTransformAsAffineTransform3D();
        transformedSources.get(movingSourceIndex).setFixedTransform(bigWarp);

        AffineTransform3D identity = new AffineTransform3D();
        identity.identity();
        transformedSources.get(fixedSourceIndex).setFixedTransform(identity);
        bdv.getViewerPanel().requestRepaint();
    }







        // something like https://github.com/bigdataviewer/bigdataviewer-core/blob/master/src/main/java/bdv/tools/transformation/ManualTransformationEditor.java#L153
        // saving transformed sources https://github.com/bigdataviewer/bigdataviewer-core/blob/b59d7babb0b212ccde7473295d23e10c54fc61e6/src/main/java/bdv/tools/transformation/ManualTransformation.java#L87

        // if ( bw.numDimensions() )
        // affinetransform2d or affinetransform3d
        // bw.affine()
        // AffineTransform3D transform = bw.affine3d();

    public static void main( String[] args )
    {
    }


}
