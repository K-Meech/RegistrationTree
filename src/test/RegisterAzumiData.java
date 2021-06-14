import de.embl.schwab.crosshairSBEM.ui.RegistererCommand;
import net.imagej.ImageJ;

import java.io.File;

public class RegisterAzumiData {
    public static void main( String[] args ) {
        RegistererCommand command = new RegistererCommand();
        command.fixedImage = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\after.xml");
        command.movingImage = new File("C:\\Users\\meechan\\Documents\\temp\\azumi_data\\before.xml");
        command.run();
    }

}
