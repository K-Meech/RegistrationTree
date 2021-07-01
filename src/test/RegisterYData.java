import de.embl.schwab.crosshairSBEM.ui.RegistererCommand;

import java.io.File;

public class RegisterYData {
    public static void main( String[] args ) {
        RegistererCommand command = new RegistererCommand();
        command.fixedImageXml = new File("C:\\Users\\meechan\\Documents\\temp\\y_data\\20210122-K1-post_Rec_vertical_flip.xml");
        command.movingImageXml = new File("C:\\Users\\meechan\\Documents\\temp\\y_data\\20210118_k1_1um_2k_rec_vertical_flip.xml");
        command.temporaryDirectory = new File( "C:\\Users\\meechan\\Documents\\temp\\y_data\\register_with_mask" );
        command.run();
    }
}
