import de.embl.schwab.crosshairSBEM.ui.RegistererCommand;

import java.io.File;

public class RegisterN5AzumiData {
    public static void main( String[] args ) {
        RegistererCommand command = new RegistererCommand();
        command.fixedImageXml = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\n5\\aftern5.xml");
        command.movingImageXml = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\n5\\beforen5.xml");
        command.temporaryDirectory = new File( "C:\\Users\\meechan\\Documents\\temp\\crosshairElastixTesting" );
        command.run();
    }
}
