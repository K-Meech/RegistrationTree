import de.embl.schwab.registrationTree.ui.RegistererCommand;
import net.imagej.ImageJ;

import java.io.File;

public class RegisterAzumiData {
    public static void main( String[] args ) {
        RegistererCommand command = new RegistererCommand();
        command.fixedImageXml = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\after.xml");
        command.movingImageXml = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\before.xml");
        command.temporaryDirectory = new File( "C:\\Users\\meechan\\Documents\\temp\\crosshairElastixTesting" );
        command.run();
    }

}
