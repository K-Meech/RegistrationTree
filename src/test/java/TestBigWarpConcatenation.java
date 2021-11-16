import de.embl.schwab.registrationTree.Transformer;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransformSequence;

import java.io.File;

public class TestBigWarpConcatenation {
    public static void main( String[] args ) {

        File fixedImage = new File("C:\\Users\\meechan\\Documents\\sample_register_images\\different_xml_scales\\mri-stack.xml");
        File movingImage = new File("C:\\Users\\meechan\\Documents\\sample_register_images\\different_xml_scales\\rotated-scaled.xml");
        File tmpDir = new File("C:\\Users\\meechan\\Documents\\temp\\crosshairElastixTesting");
        Transformer transformer = new Transformer( movingImage, fixedImage, tmpDir );

        AffineTransform3D bigWarp1 = new AffineTransform3D();
        bigWarp1.set(0.9835340870089452, -0.1807227149294747, 0.0, -30.54048474543523, 0.1807227149294747, 0.9835340870089452, -0.0, -128.57652045077538, -0.0, 0.0, 0.9999999999999998, -12.999999999999991);

        AffineTransform3D bigWarp2 = new AffineTransform3D();
        bigWarp2.set(0.999942805217308, 0.0106951528339049, -0.0, -2.446036734426322, -0.0106951528339049, 0.999942805217308, 0.0, -134.57047474029105, 0.0, -0.0, 0.9999999999999998, 1.4210854715202E-14);

        // AffineTransform3D combinedTransform = new AffineTransform3D();
        // combinedTransform.preConcatenate(bigWarp1);
        // combinedTransform.preConcatenate(bigWarp2);

        AffineTransform3D combinedTransform = new AffineTransform3D();
        combinedTransform.concatenate(bigWarp1);
        combinedTransform.concatenate(bigWarp2);

        transformer.setTransform(Transformer.ImageType.MOVING, combinedTransform.inverse());

        transformer.refreshBdvWindow();

    }
}
