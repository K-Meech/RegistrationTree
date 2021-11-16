package de.embl.schwab.registrationTree.ui;

import de.embl.schwab.registrationTree.Transformer;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import java.io.File;

import static de.embl.schwab.registrationTree.SwingUtils.resetCrossPlatformSwingLookAndFeel;

@Plugin(type = Command.class, menuPath = "Plugins>RegistrationTree>Register Bdv Files" )
public class RegistrationCommand implements Command {

    @Parameter
    public File movingImageXml;
    @Parameter
    public File fixedImageXml;

    @Parameter(style="directory")
    public File temporaryDirectory;

    @Override
    public void run() {
        resetCrossPlatformSwingLookAndFeel();
        new Transformer( movingImageXml, fixedImageXml, temporaryDirectory );
        }

    public static void main( String[] args ) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }
}
