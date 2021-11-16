import de.embl.schwab.registrationTree.ui.RegistrationCommand;

import java.io.File;

public class RegisterN5AzumiData {
    public static void main( String[] args ) {
        RegistrationCommand command = new RegistrationCommand();
        command.fixedImageXml = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\n5\\aftern5.xml");
        command.movingImageXml = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\n5\\beforen5.xml");
        command.temporaryDirectory = new File( "C:\\Users\\meechan\\Documents\\temp\\crosshairElastixTesting" );
        command.run();
    }
}
