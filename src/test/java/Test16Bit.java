import de.embl.schwab.registrationTree.ui.RegistererCommand;

import java.io.File;

public class Test16Bit {
    public static void main( String[] args ) {
        RegistererCommand command = new RegistererCommand();
        command.fixedImageXml = new File("C:\\Users\\meechan\\Documents\\temp\\test_sbem\\test_n5\\first_100_proper_z_spacing\\ov000_0.xml");
        command.movingImageXml = new File("C:\\Users\\meechan\\Documents\\test_images\\sbem_run\\x-ray\\Platy-89_02_tomo.xml");
        command.temporaryDirectory = new File( "C:\\Users\\meechan\\Documents\\temp\\crosshairElastixTesting" );
        command.run();
    }
}
