import bdv.ij.BigWarpBdvCommand;
import bdv.ij.util.ProgressWriterIJ;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.utility.ui.RepeatingReleasedEventsFixer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class big_warp {

    String pathToFixed = "C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack.xml";
    String pathToMoving = "C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack-rotated.xml";
    BigWarp bw;
    BdvStackSource bdv;

    public void run() {
        final LazySpimSource emSource = new LazySpimSource("em", pathToFixed);
//        final LazySpimSource xraySource = new LazySpimSource("xray", "Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\original_hdf5\\high_res_flip_z_bigwarped.xml");
        final LazySpimSource xraySource = new LazySpimSource("xray", pathToMoving);



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

        content.add(openBigwarpButton);
        content.add(displayBigwarpTransform);

        testInterface.pack();
        testInterface.show();

        bdv = BdvFunctions.show(emSource, 1);
        BdvFunctions.show(xraySource, 1, BdvOptions.options().addTo( bdv ) );
        bdv.setDisplayRange(0, 255);





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
        TransformedSource< ? > source = (TransformedSource< ? >) ((SourceAndConverter< ? >) bdv.getSources().get(0)).getSpimSource();
        source.setFixedTransform(bigWarp);
        bdv.getBdvHandle().getViewerPanel().requestRepaint();



        // something like https://github.com/bigdataviewer/bigdataviewer-core/blob/master/src/main/java/bdv/tools/transformation/ManualTransformationEditor.java#L153
        // saving transformed sources https://github.com/bigdataviewer/bigdataviewer-core/blob/b59d7babb0b212ccde7473295d23e10c54fc61e6/src/main/java/bdv/tools/transformation/ManualTransformation.java#L87

        // if ( bw.numDimensions() )
        // affinetransform2d or affinetransform3d
        // bw.affine()
        // AffineTransform3D transform = bw.affine3d();
        //
    }

    public static void main( String[] args )
    {
        new big_warp().run();
    }


}
