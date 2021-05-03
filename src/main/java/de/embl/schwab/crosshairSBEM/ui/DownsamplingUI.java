package de.embl.schwab.crosshairSBEM.ui;

import de.embl.schwab.crosshairSBEM.Downsampler;
import de.embl.schwab.crosshairSBEM.Transformer;
import ij.gui.GenericDialog;

public class DownsamplingUI {

    private Downsampler downsampler;

    public DownsamplingUI( Downsampler downsampler ) {
        this.downsampler = downsampler;
    }

    public int chooseSourceLevel( Transformer.ImageType imageType ) throws RuntimeException {
        final GenericDialog gd = new GenericDialog( "Choose resolution level of " + imageType.name() );

        String[] resolutionLevels = downsampler.getLevelsArray( imageType );
        gd.addChoice("Resolution level of " + imageType.name(), resolutionLevels, resolutionLevels[0]);
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            return gd.getNextChoiceIndex();
        } else {
            throw new RuntimeException();
        }
    }
}
