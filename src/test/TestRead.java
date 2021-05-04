import de.embl.schwab.crosshairSBEM.Cropper;
import de.embl.schwab.crosshairSBEM.Transformer;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;

public class TestRead {
    public static void main( String[] args ) {

        ImagePlus imp = IJ.openImage("C:\\Users\\meechan\\Documents\\sample_register_images\\different_xml_scales\\mri-stack.tif");
        System.out.println("yo");

    }
}
