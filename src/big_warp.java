import bdv.export.ProgressWriter;
import bdv.ij.util.ProgressWriterIJ;
import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.viewer.Interpolation;
import de.embl.cba.bdp2.boundingbox.BoundingBoxDialog;
import bdv.tools.boundingbox.TransformedBoxSelectionDialog;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.export.BdvRealSourceToVoxelImageExporter;
import de.embl.cba.bdv.utils.io.ProgressWriterBdv;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import org.janelia.utility.ui.RepeatingReleasedEventsFixer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import static bdv.viewer.Interpolation.NLINEAR;

public class big_warp {

    String pathToFixed = "C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack.xml";
    String pathToMoving = "C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack-rotated.xml";
    BigWarp bw;
    BdvStackSource bdvFixed;
    BdvStackSource bdvMoving;
    TransformedSource<?> fixedSource;
    TransformedSource<?> movingSource;
    double[] fixedDimensions;
    double[] movingDimensions;

    public void run() {

        SpimData xraySource = null;
        SpimData emSource = null;
        try {
            emSource = new XmlIoSpimData().load(pathToFixed);
            xraySource = new XmlIoSpimData().load(pathToMoving);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }

//         final LazySpimSource emSource = new LazySpimSource("em", pathToFixed);
// //        final LazySpimSource xraySource = new LazySpimSource("xray", "Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\original_hdf5\\high_res_flip_z_bigwarped.xml");
//         final LazySpimSource xraySource = new LazySpimSource("xray", pathToMoving);

        fixedDimensions = new double[3];
        movingDimensions = new double[3];

        // Allows opening of hdf5 images in big warp
        // BigWarpBdvCommand bwcommand = new BigWarpBdvCommand();
        // bwcommand.fixedImageXml = new File("C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack.xml");
        // bwcommand.movingImageXml = new File("C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack-rotated.xml");
        // bwcommand.run();


        JFrame testInterface = new JFrame();
        JPanel content = new JPanel();
        testInterface.setContentPane(content);

        ActionListener generalListener = new GeneralListener();

        JButton openBigwarpButton = new JButton("Open Bigwarp");
        openBigwarpButton.setActionCommand("open_bigwarp");
        openBigwarpButton.addActionListener(generalListener);

        JButton displayBigwarpTransform = new JButton("Display Bigwarp");
        displayBigwarpTransform.setActionCommand("display_bigwarp");
        displayBigwarpTransform.addActionListener(generalListener);

        JButton invertBigwarpTransform = new JButton("Invert Display");
        invertBigwarpTransform.setActionCommand("invert_display");
        invertBigwarpTransform.addActionListener(generalListener);

        JButton cropDialogB = new JButton("crop");
        cropDialogB.setActionCommand("crop_dialog");
        cropDialogB.addActionListener(generalListener);

        content.add(openBigwarpButton);
        content.add(displayBigwarpTransform);
        content.add(invertBigwarpTransform);
        content.add(cropDialogB);

        testInterface.pack();
        testInterface.show();

        bdvFixed = BdvFunctions.show(emSource).get(0);
        bdvMoving = BdvFunctions.show(xraySource, BdvOptions.options().addTo(bdvFixed) ).get(0);
        fixedSource = (TransformedSource<?>) ((SourceAndConverter<?>) bdvFixed.getSources().get(0)).getSpimSource();
        movingSource = (TransformedSource<?>) ((SourceAndConverter<?>) bdvMoving.getSources().get(0)).getSpimSource();

        bdvFixed.setDisplayRange(0, 255);
        bdvMoving.setDisplayRange(0, 255);

        emSource.getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().dimensions(fixedDimensions);
        xraySource.getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().dimensions(movingDimensions);


        // emSource.getSequenceDescription().get
        // emSource.getVoxelDimensions().dimensions(fixedDimensions);
        // xraySource.getVoxelDimensions().dimensions(movingDimensions);





        // final TransformedSource< ? > movingSource = new TransformedSource<>( movingSpimData );

        // fixed transform
        // final TransformedSource< ? > source = ( TransformedSource< ? > ) bdvStackSource.getSources().get( 0 );
        // source.setFixedTransform( transform );



        // minor goal
        // run bigwarp and vis result in bigdataviewer




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

    class GeneralListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("open_bigwarp")) {
                openBigwarp();
            } else if (e.getActionCommand().equals("display_bigwarp")) {
                displayBigwarp();
            } else if (e.getActionCommand().equals("invert_display")) {
            invert();
            } else if (e.getActionCommand().equals("crop_dialog")) {
                new Thread( () -> {
                    TransformedBoxSelectionDialog.Result result = cropDialog();
                    RandomAccessibleInterval rai = fixedSource.getSource(0, 0);
                    // export stuff https://github.com/tischi/imagej-utils/blob/9d29c1dbb5bfde784f964e29956877d2d4ddc915/src/main/java/de/embl/cba/bdv/utils/export/BdvRealSourceToVoxelImageExporter.java#L305
                    // example of usage https://github.com/tischi/imagej-utils/blob/4ebabd30be230c5fb49674fb78c57cc98d8dab16/src/test/java/explore/ExploreExportSourcesFromBdv.java
                    ArrayList<Integer> sourceIndices = new ArrayList<>();
                    sourceIndices.add(0);
                    double[] outputVoxelSpacings = new double[] {1,1,1};
                    BdvRealSourceToVoxelImageExporter bdvExport = new BdvRealSourceToVoxelImageExporter<>(bdvFixed.getBdvHandle(),
                            sourceIndices, result.getInterval(), 0, 0,
                            NLINEAR, outputVoxelSpacings, BdvRealSourceToVoxelImageExporter.ExportModality.SaveAsTiffVolumes,
                            BdvRealSourceToVoxelImageExporter.ExportDataType.UnsignedByte, Runtime.getRuntime().availableProcessors(), new ProgressWriterIJ());
                    bdvExport.setOutputDirectory("C:\\Users\\meechan\\Documents\\temp\\exportTest");
                    bdvExport.export();
                } ).start();
            }
        }
    }

    private void openBigwarp () {
        try {
            SpimData fixedSpimData = new XmlIoSpimData().load(pathToFixed);
            SpimData movingSpimData = new XmlIoSpimData().load(pathToMoving);
            (new RepeatingReleasedEventsFixer()).install();
            BigWarp.BigWarpData<?> bigWarpData = BigWarpInit.createBigWarpData(movingSpimData, fixedSpimData);
            bw = new BigWarp(bigWarpData, "Big Warp", new ProgressWriterIJ());
            bw.getViewerFrameP().getViewerPanel().requestRepaint();
            bw.getViewerFrameQ().getViewerPanel().requestRepaint();
            bw.getLandmarkFrame().repaint();
            bw.setMovingSpimData(movingSpimData, new File(pathToMoving));
        } catch (SpimDataException var4) {
            var4.printStackTrace();
        }
    }

    private void displayBigwarp() {
        AffineTransform3D bigWarp = bw.affine3d();
        fixedSource.setFixedTransform(bigWarp);

        AffineTransform3D identity = new AffineTransform3D();
        identity.identity();
        movingSource.setFixedTransform(identity);
        bdvFixed.getBdvHandle().getViewerPanel().requestRepaint();
    }

    private void invert() {
        // AffineTransform3D bigWarp = bw.affine3d();
        // bigWarp.inverse();
        AffineTransform3D bigWarp = bw.getMovingToFixedTransformAsAffineTransform3D();
        movingSource.setFixedTransform(bigWarp);

        AffineTransform3D identity = new AffineTransform3D();
        identity.identity();
        fixedSource.setFixedTransform(identity);
        bdvMoving.getBdvHandle().getViewerPanel().requestRepaint();
    }

    // TODO - make crop dialog deal with transforms, so always crops in real pixel orientation for writing out
    private TransformedBoxSelectionDialog.Result cropDialog() {


        final AffineTransform3D boxTransform = new AffineTransform3D();
        boxTransform.set( fixedDimensions[0], 0,0  );
        boxTransform.set( fixedDimensions[1], 1,1  );
        boxTransform.set( fixedDimensions[2], 2,2  );


        return BdvFunctions.selectBox(
                bdvFixed,
                boxTransform,
                Intervals.createMinMax(10,10,10,100,100,100),
                Intervals.createMinMax(0,0,0,200,200,200),
                BoxSelectionOptions.options()
                        .title( "select crop" )
        );
    }

    // private void setInitialInterval( boolean calibrated )
    // {
    //     final FinalRealInterval viewerBoundingInterval = BdvUtils.getViewerGlobalBoundingInterval( bdvFixed.getBdvHandle() );
    //     double[] initialCenter = new double[ 3 ];
    //     double[] initialSize = new double[ 3 ];
    //
    //     for (int d = 0; d < 3; d++)
    //     {
    //         initialCenter[ d ] = ( viewerBoundingInterval.realMax( d ) + viewerBoundingInterval.realMin( d ) ) / 2.0;
    //         initialSize[ d ] = ( viewerBoundingInterval.realMax( d ) - viewerBoundingInterval.realMin( d ) ) / 2.0;
    //
    //         if ( ! calibrated )
    //         {
    //             initialCenter[ d ] /= image.getVoxelDimensions()[ d ];
    //             initialSize[ d ] /= image.getVoxelDimensions()[ d ];
    //         }
    //     }
    //
    //     // TODO: improve this: take whole range in the smaller direction (up or down..)
    //     initialSize[ DimensionOrder.Z ] = image.getRai().dimension( DimensionOrder.Z ) / 10;
    //
    //     if ( calibrated )
    //         initialSize[ DimensionOrder.Z ] *= image.getVoxelDimensions()[ DimensionOrder.Z ];
    //
    //     initialSize[ DimensionOrder.Z ] = (int) Math.max( initialSize[ DimensionOrder.Z ],
    //             Math.ceil( image.getVoxelDimensions()[ DimensionOrder.Z ] ) );
    //
    //     double[] minBB = new double[]{
    //             initialCenter[ X ] - initialSize[ X ] / 2,
    //             initialCenter[ Y ] - initialSize[ Y ] / 2,
    //             initialCenter[ Z ] - initialSize[ Z ] / 2 };
    //
    //     double[] maxBB = new double[]{
    //             initialCenter[ X ] + initialSize[ X ] / 2,
    //             initialCenter[ Y ] + initialSize[ Y ] / 2,
    //             initialCenter[ Z ] + initialSize[ Z ] / 2 };
    //
    //     initialInterval = Intervals.createMinMax(
    //             (long) minBB[X], (long) minBB[Y], (long) minBB[Z],
    //             (long) maxBB[X], (long) maxBB[Y], (long) maxBB[Z]);
    // }
    //
    // private void setRangeInterval( boolean calibrated )
    // {
    //     min = new int[ 4 ];
    //     max = new int[ 4 ];
    //
    //     setRangeXYZ( image, calibrated );
    //     setRangeT( image );
    //
    //     rangeInterval = Intervals.createMinMax(
    //             min[X], min[Y], min[Z],
    //             max[X], max[Y], max[Z]);
    // }
    //
    // private void setRangeT( Image< R > image )
    // {
    //     min[T] = (int) image.getRai().min( DimensionOrder.T );
    //     max[T] = (int) image.getRai().max( DimensionOrder.T );
    // }
    //
    // private void setRangeXYZ( Image< R > image, boolean calibrated )
    // {
    //     for (int d = 0; d < 3; d++)
    //     {
    //         min[ d ] = (int) ( image.getRai().min( d ) );
    //         max[ d ] = (int) ( image.getRai().max( d ) );
    //
    //         if ( calibrated )
    //         {
    //             min[ d ] *= image.getVoxelDimensions()[ d ];
    //             max[ d ] *= image.getVoxelDimensions()[ d ];
    //         }
    //     }
    // }

    // from big data processor
    // public FinalInterval getVoxelIntervalXYZCTViaDialog( )
    // {
    //     BoundingBoxDialog boundingBoxDialog = new BoundingBoxDialog( bdvFixed.getBdvHandle(), image );
    //     boundingBoxDialog.showVoxelBoxAndWaitForResult();
    //     return boundingBoxDialog.getVoxelSelectionInterval();
    //
    //     TransformedBoxSelectionDialog crop = new TransformedBoxSelectionDialog()
    //     BoundingBoxDialog boundingBoxDialog = new BoundingBoxDialog( bdvFixed.getBdvHandle(), image );
    //     boundingBoxDialog.showVoxelBoxAndWaitForResult();
    //     return boundingBoxDialog.getVoxelSelectionInterval();
    // }



        // something like https://github.com/bigdataviewer/bigdataviewer-core/blob/master/src/main/java/bdv/tools/transformation/ManualTransformationEditor.java#L153
        // saving transformed sources https://github.com/bigdataviewer/bigdataviewer-core/blob/b59d7babb0b212ccde7473295d23e10c54fc61e6/src/main/java/bdv/tools/transformation/ManualTransformation.java#L87

        // if ( bw.numDimensions() )
        // affinetransform2d or affinetransform3d
        // bw.affine()
        // AffineTransform3D transform = bw.affine3d();

    public static void main( String[] args )
    {
        new big_warp().run();
    }


}
