import de.embl.schwab.registrationTree.Cropper;
import de.embl.schwab.registrationTree.Transformer;
import net.imglib2.realtransform.InvertibleRealTransformSequence;

import java.io.File;

public class Bdvplaygroundtest {
    public static void main( String[] args ) {

        File fixedImage = new File("C:\\Users\\meechan\\Documents\\sample_register_images\\similarity3d\\mri-stack.xml");
        File movingImage = new File("C:\\Users\\meechan\\Documents\\sample_register_images\\similarity3d\\rotated-scaled.xml");
        File tempDir = new File( "C:\\Users\\meechan\\Documents\\temp\\crosshairElastixTesting" );
        Transformer transformer = new Transformer( movingImage, fixedImage, tempDir );

        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();

    }
}
