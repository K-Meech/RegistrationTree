package de.embl.schwab.crosshairSBEM.ui;

import de.embl.schwab.crosshairSBEM.Downsampler;
import de.embl.schwab.crosshairSBEM.Transformer;
import ij.gui.GenericDialog;

public class DownsamplingUI {

    private Downsampler downsampler;

    public DownsamplingUI( Downsampler downsampler ) {
        this.downsampler = downsampler;
    }

    private int chooseSourceLevel( Transformer.ImageType imageType ) throws RuntimeException {
        final GenericDialog gd = new GenericDialog( "Choose resolution level..." );

        String[] resolutionLevels = downsampler.getLevelsArray( imageType );
        gd.addChoice("Level:", resolutionLevels, resolutionLevels[0]);
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            return gd.getNextChoiceIndex();
        } else {
            throw new RuntimeException();
        }
    }
}
