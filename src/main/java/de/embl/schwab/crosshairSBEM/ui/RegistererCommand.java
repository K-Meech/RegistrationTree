package de.embl.schwab.crosshairSBEM.ui;

import de.embl.schwab.crosshairSBEM.Transformer;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>Registerer>Register Bdv Files" )
public class RegistererCommand implements Command {

    @Parameter
    public File movingImage;
    @Parameter
    public File fixedImage;

    @Override
    public void run() {
            new Transformer( movingImage, fixedImage );
        }

    public static void main( String[] args ) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }
}
