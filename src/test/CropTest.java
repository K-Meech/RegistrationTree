import de.embl.schwab.crosshairSBEM.Cropper;
import de.embl.schwab.crosshairSBEM.Transformer;
import net.imagej.ImageJ;

import java.io.File;

public class CropTest {
    public static void main( String[] args ) {

        File fixedImage = new File("C:\\Users\\meechan\\Documents\\sample_register_images\\similarity3d\\mri-stack.xml");
        File movingImage = new File("C:\\Users\\meechan\\Documents\\sample_register_images\\similarity3d\\rotated-scaled.xml");
        Transformer transformer = new Transformer( movingImage, fixedImage );
        Cropper cropper = new Cropper( transformer );
        // cropper.createTransformedRealBoxSelectionDialog(Transformer.ImageType.FIXED);
        transformer.getElastixManager().writeCroppedAndDownsampledImages();

    }
}
