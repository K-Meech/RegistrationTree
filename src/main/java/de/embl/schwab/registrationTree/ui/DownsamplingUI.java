package de.embl.schwab.registrationTree.ui;

import de.embl.schwab.registrationTree.Downsampler;
import de.embl.schwab.registrationTree.Transformer;
import ij.gui.GenericDialog;

public class DownsamplingUI {

    private Downsampler downsampler;

    public DownsamplingUI( Downsampler downsampler ) {
        this.downsampler = downsampler;
    }

    private Integer levelChoiceDialog( Transformer.ImageType imageType, String[] resolutionStrings ) {
        final GenericDialog gd = new GenericDialog( "Choose resolution level of " + imageType.name() );

        gd.addChoice("Resolution level of " + imageType.name(), resolutionStrings, resolutionStrings[0]);
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            return gd.getNextChoiceIndex();
        } else {
            return null;
        }
    }

    public Integer chooseSourceLevel( Transformer.ImageType imageType ) {
        String[] resolutionLevels = downsampler.getLevelsArray( imageType );
        return levelChoiceDialog( imageType, resolutionLevels );
    }

    public Integer chooseSourceLevel( Transformer.ImageType imageType, String cropName ) {
        String[] resolutionLevels = downsampler.getLevelsArray( imageType, cropName );
        return levelChoiceDialog( imageType, resolutionLevels );
    }
}
