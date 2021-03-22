package de.embl.schwab.crosshairSBEM;

import bdv.ij.util.ProgressWriterIJ;
import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
// import de.embl.cba.bdv.utils.export.BdvRealSourceToVoxelImageExporter;
import de.embl.cba.metaimage_io.MetaImage_Writer;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import itc.commands.BigWarpAffineToTransformixFileCommand;
import itc.converters.AffineTransform3DToFlatString;
import itc.transforms.elastix.ElastixTransform;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.*;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.apache.commons.compress.utils.FileNameUtils;
import org.janelia.utility.ui.RepeatingReleasedEventsFixer;

import javax.swing.*;
import java.awt.*;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static de.embl.schwab.crosshairSBEM.SwingUtils.TEXT_FIELD_HEIGHT;
import static de.embl.schwab.crosshairSBEM.SwingUtils.getButton;

// TODO - is soruce index consistent

public class Transformer {

    public enum TransformType {
        BigWarp,
        Elastix,
        Manual
    }

    // String[] sourcePaths = new String[] {"C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack.xml",
    //         "C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack-rotated.xml" };
    String[] sourcePaths = new String[] {"C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack.xml",
            "C:\\Users\\meechan\\Documents\\sample_register_images\\zebra.xml" };
    ArrayList<SpimData> spimSources = new ArrayList<>();
    ArrayList<String> sourceNames = new ArrayList<>();
    ArrayList<TransformedSource<?>> transformedSources = new ArrayList<>();

    String tempDir = "C:\\Users\\meechan\\Documents\\main.java.de.embl.schwab.crosshairSBEM.temp\\exportTest";

    BdvHandle bdv;
    BigWarp bw;
    int fixedSourceIndex;
    int movingSourceIndex;

    public Transformer() {
        try {
            loadSources();
        } catch (SpimDataException e) {
            e.printStackTrace();
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

    private void loadSources() throws SpimDataException {
        for ( String sourcePath : sourcePaths ) {
            SpimData source = new XmlIoSpimData().load(sourcePath);
            spimSources.add(source);
            sourceNames.add( FileNameUtils.getBaseName( sourcePath ) );

            BdvStackSource bdvStackSource;
            if ( bdv == null ) {
                bdvStackSource = BdvFunctions.show( source ).get(0);
                bdv = bdvStackSource.getBdvHandle();
            } else {
                bdvStackSource = BdvFunctions.show(source,  BdvOptions.options().addTo(bdv) ).get(0);
            }
            bdvStackSource.setDisplayRange(0, 255);

            transformedSources.add( (TransformedSource<?>) ((SourceAndConverter<?>) bdvStackSource.getSources().get(0)).getSpimSource() );

        }
    }

    private double[] getSourceVoxelSize( int sourceIndex ) {
        double[] sourceDimensions = new double[3];
        spimSources.get(sourceIndex).getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().dimensions(sourceDimensions);
        return sourceDimensions;
    }

    private String getSourceUnit( int sourceIndex ) {
        return spimSources.get(sourceIndex).getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().unit();
    }

    private long[] getSourceVoxelDimensions( int sourceIndex ) {
        return getSourceVoxelDimensionsAtLevel( sourceIndex, 0 );
    }

    private long[] getSourceVoxelDimensionsAtLevel( int sourceIndex, int level ) {
        List<SourceAndConverter<?>> sources = bdv.getViewerPanel().state().getSources();
        Source spimSource = sources.get( sourceIndex ).getSpimSource();

        long[] dimensions = new long[3];
        spimSource.getSource( 0, level ).dimensions( dimensions );
        return dimensions;
    }

    class GeneralListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("open_bigwarp")) {
                openBigwarp();
            } else if (e.getActionCommand().equals("display_bigwarp")) {
                ;
            } else if (e.getActionCommand().equals("invert_display")) {
            invert();
            } else if (e.getActionCommand().equals("crop_dialog")) {
                new Thread( () -> {
                    final GenericDialog gd = new GenericDialog( "Choose image to crop..." );
                    String[] imageNames = new String[sourceNames.size()];
                    for ( int i = 0; i < imageNames.length; i++ ) {
                        imageNames[i] = sourceNames.get(i);
                    }
                    gd.addChoice("Image to crop..", imageNames, imageNames[0]);
                    gd.showDialog();

                    if ( !gd.wasCanceled() ) {
                        int sourceIndex = gd.getNextChoiceIndex();
                        TransformedRealBoxSelectionDialog.Result result = cropDialog( sourceIndex );
                        writeCrop( result, sourceIndex );
                    }
                } ).start();
            }
        }
    }

    // saving to mhd - https://github.com/embl-cba/elastixWrapper/blob/edb37861b497747217a8e9dd9e579fd8d8a325bb/src/main/java/de/embl/cba/elastixwrapper/elastix/ElastixWrapper.java#L479
    // save to mhd AND enable choosing of name

    private int chooseSourceLevel( int sourceIndex ) throws RuntimeException {
        final GenericDialog gd = new GenericDialog( "Choose resolution level..." );
        List<SourceAndConverter<?>> sources = bdv.getViewerPanel().state().getSources();
        Source source = sources.get( sourceIndex ).getSpimSource();
        int numLevels = source.getNumMipmapLevels();

        String[] resolutionLevels = new String[numLevels];
        for ( int i = 0; i < numLevels; i++ ) {
            resolutionLevels[i] = Integer.toString( i );
        }
        gd.addChoice("Level:", resolutionLevels, resolutionLevels[0]);
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            return gd.getNextChoiceIndex();
        } else {
            throw new RuntimeException();
        }
    }

    private double[] getVoxelSizeAtLevel( int sourceIndex, int level ) {
        long[] fullResolutionVoxelDimensions = getSourceVoxelDimensions( sourceIndex );
        double[] fullResolutionVoxelSize = getSourceVoxelSize( sourceIndex );

        if ( level == 0 ) {
            return fullResolutionVoxelSize;
        } else {
            long[] downsampledResolutionVoxelDimensions = getSourceVoxelDimensionsAtLevel( sourceIndex, level );
            double[] downsampledResolutionVoxelSize = new double[3];
            for ( int i = 0; i< fullResolutionVoxelDimensions.length; i++ ) {
                downsampledResolutionVoxelSize[i] = fullResolutionVoxelSize[i] *
                        ( (double) fullResolutionVoxelDimensions[i] / (double) downsampledResolutionVoxelDimensions[i] );
            }
            return downsampledResolutionVoxelSize;
        }
    }

    public static Interval toVoxelInterval(
            RealInterval interval,
            double[] voxelSize )
    {
        final long[] min = new long[ 3 ];
        final long[] max = new long[ 3 ];

        for ( int d = 0; d < 3; d++ )
        {
            min[ d ] = Math.round( interval.realMin( d ) / voxelSize[ d ] );
            max[ d ] = Math.round( interval.realMax( d ) / voxelSize[ d ] );
        }

        return new FinalInterval( min, max );
    }

    private void writeCrop( TransformedRealBoxSelectionDialog.Result result, int sourceIndex ) {
        // export stuff https://github.com/tischi/imagej-utils/blob/9d29c1dbb5bfde784f964e29956877d2d4ddc915/src/main/java/de/embl/cba/bdv/utils/export/BdvRealSourceToVoxelImageExporter.java#L305
        // example of usage https://github.com/tischi/imagej-utils/blob/4ebabd30be230c5fb49674fb78c57cc98d8dab16/src/test/java/explore/ExploreExportSourcesFromBdv.java

        List<SourceAndConverter<?>> sources = bdv.getViewerPanel().state().getSources();
        int level = chooseSourceLevel( sourceIndex );
        // TODO - warn that time series are not supported
        RandomAccessibleInterval rai = sources.get( sourceIndex ).getSpimSource().getSource( 0, level);

        // same as big data processor here: https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/process/crop/CropDialog.java#L89
        // i.e. get voxel size at that level, and use it to get a voxel interval
        double[] downsampledVoxelSize = getVoxelSizeAtLevel( sourceIndex, level );
        Interval voxelCropInterval = toVoxelInterval( result.getInterval(), downsampledVoxelSize );

        RandomAccessibleInterval crop =
                Views.interval( rai, voxelCropInterval );

        // TODO - generalise to not just 8-bit? e.g. what happens if I pass a 16bit to this? Does it convert to 8bit
        // sensibly or just clip?
        ImagePlus imp = ImageJFunctions.wrapUnsignedByte( crop, "towrite" );
        System.out.println(imp.getBitDepth());
        MetaImage_Writer writer = new MetaImage_Writer();
        String directory = "C:\\Users\\meechan\\Documents\\main.java.de.embl.schwab.crosshairSBEM.temp\\";
        String filenameWithExtension = "test-" + level + "TODAY.mhd";
        writer.save( imp, directory, filenameWithExtension );
    }

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

    private void chooseFixedMovingDialog() {
        final GenericDialog gd = new GenericDialog( "Choose fixed and moving..." );
        String[] imageNames = new String[sourceNames.size()];
        for ( int i = 0; i < imageNames.length; i++ ) {
            imageNames[i] = sourceNames.get(i);
        }
        gd.addChoice("Fixed image..", imageNames, imageNames[0]);
        gd.addChoice("Moving image..", imageNames, imageNames[1]);
        gd.showDialog();

        // TODO - check not the same image selected in both
        if ( !gd.wasCanceled() ) {
            fixedSourceIndex = gd.getNextChoiceIndex();
            movingSourceIndex = gd.getNextChoiceIndex();
        }
    }

    public WindowListener createWindowListener() {
        WindowListener windowListener = new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {}

            @Override
            public void windowClosing(WindowEvent e) {}

            @Override
            public void windowClosed(WindowEvent e) {
                bw.closeAll();
            }

            @Override
            public void windowIconified(WindowEvent e) {}

            @Override
            public void windowDeiconified(WindowEvent e) {}

            @Override
            public void windowActivated(WindowEvent e) {}

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        };
        return windowListener;
    }

    public void exportBigWarpToCrosshair() {
        // TODO - deal with if fixed/moving same way around, or needs to be swapped
        AffineTransform3D bigWarp = bw.affine3d();
        transformedSources.get(fixedSourceIndex).setFixedTransform(bigWarp);

        AffineTransform3D identity = new AffineTransform3D();
        identity.identity();
        transformedSources.get(movingSourceIndex).setFixedTransform(identity);
        bdv.getViewerPanel().requestRepaint();
        // TODO - add transform panel too
    }

    public void crosshairBigwarpMenu() {
        JFrame menu = new JFrame();
        menu.addWindowListener( createWindowListener() );
        menu.setTitle( "Crosshair - Bigwarp menu");
        // menu.getContentPane().setLayout( new BoxLayout(menu.getContentPane(), BoxLayout.Y_AXIS ) );
        menu.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        JPanel panel = new JPanel();
        // panel.setLayout( new BoxLayout(panel, BoxLayout.Y_AXIS) );
        panel.setBorder( BorderFactory.createEmptyBorder(0, 10, 10, 10) );
        JButton exportCrosshairButton = getButton( "export current transform to Crosshair", new Dimension( 300, TEXT_FIELD_HEIGHT ));
        exportCrosshairButton.setBackground( new Color(240, 128, 128));
        panel.add(exportCrosshairButton);
        menu.getContentPane().add(panel);

        exportCrosshairButton.addActionListener( e ->
        {
            new Thread( () -> {
                exportBigWarpToCrosshair();
                menu.dispatchEvent(new WindowEvent(menu, WindowEvent.WINDOW_CLOSING));
            } ).start();
        } );

        menu.pack();
        Point bdvWindowLocation = bw.getViewerFrameQ().getLocation();
        int bdvWindowHeight = bw.getViewerFrameQ().getHeight();

        menu.setLocation(bdvWindowLocation.x, bdvWindowLocation.y + bdvWindowHeight);
        menu.show();
    }

    public void openBigwarp () {
        try {
            (new RepeatingReleasedEventsFixer()).install();
            chooseFixedMovingDialog();
            SpimData movingSpimData = spimSources.get(movingSourceIndex);
            SpimData fixedSpimData = spimSources.get(fixedSourceIndex);
            BigWarp.BigWarpData<?> bigWarpData = BigWarpInit.createBigWarpData(movingSpimData, fixedSpimData);
            bw = new BigWarp(bigWarpData, "Big Warp", new ProgressWriterIJ());
            bw.getViewerFrameP().getViewerPanel().requestRepaint();
            bw.getViewerFrameQ().getViewerPanel().requestRepaint();
            bw.getLandmarkFrame().repaint();
            bw.setMovingSpimData(movingSpimData, new File (sourcePaths[movingSourceIndex]));
            crosshairBigwarpMenu();
        } catch (SpimDataException var4) {
            var4.printStackTrace();
        }
    }



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

    private FinalRealInterval getRangeInterval(int sourceIndex )
    {
        double[] max = new double[ 3 ];

        long[] sourceVoxelDimensions = getSourceVoxelDimensions( sourceIndex );
        double[] sourceVoxelSize = getSourceVoxelSize( sourceIndex );
        for ( int i = 0; i < sourceVoxelSize.length; i++ ) {
            max[i] = sourceVoxelDimensions[i] * sourceVoxelSize[i];
        }
        return Intervals.createMinMaxReal(
                0, 0, 0,
                max[0], max[1], max[2]);
    }

    // TODO - make crop dialog deal with transforms, so always crops in real pixel orientation for writing out
    // TODO - y dim seems integer??
    private TransformedRealBoxSelectionDialog.Result cropDialog(int sourceIndex ) {

        // https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/process/crop/CropDialog.java
        //https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/process/crop/CropDialog.java#L58

        // based on calbirated real box stuff here: https://github.com/bigdataprocessor/bigdataprocessor2/blob/c3853cd56f8352749a81791f547c63816319a0bd/src/main/java/de/embl/cba/bdp2/boundingbox/BoundingBoxDialog.java#L144
        final AffineTransform3D boxTransform = new AffineTransform3D();
        transformedSources.get(sourceIndex).getFixedTransform(boxTransform);

        // set sensible initial intervals
        FinalRealInterval rangeInterval = getRangeInterval( sourceIndex );
        FinalRealInterval initialInterval = Intervals.createMinMaxReal( 0, 0, 0,
                rangeInterval.realMax(0)/2,
                rangeInterval.realMax(1)/2,
                rangeInterval.realMax(2)/2);

        TransformedRealBoxSelectionDialog.Result result =  BdvFunctions.selectRealBox(
                bdv,
                boxTransform,
                initialInterval,
                rangeInterval,
                BoxSelectionOptions.options()
                        .title( "Units: " + getSourceUnit( sourceIndex ) )
        );

        if ( result.isValid() ) {
            return result;
        } else {
            return null;
        }
    }



        // something like https://github.com/bigdataviewer/bigdataviewer-core/blob/master/src/main/java/bdv/tools/transformation/ManualTransformationEditor.java#L153
        // saving transformed sources https://github.com/bigdataviewer/bigdataviewer-core/blob/b59d7babb0b212ccde7473295d23e10c54fc61e6/src/main/java/bdv/tools/transformation/ManualTransformation.java#L87

        // if ( bw.numDimensions() )
        // affinetransform2d or affinetransform3d
        // bw.affine()
        // AffineTransform3D transform = bw.affine3d();

    public static void main( String[] args )
    {
        new Transformer().run();
    }


}
