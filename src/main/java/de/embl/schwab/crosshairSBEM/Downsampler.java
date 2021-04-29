package de.embl.schwab.crosshairSBEM;

public class Downsampler {

    private Transformer transformer;

    public Downsampler( Transformer transformer ) { this.transformer = transformer; }

    public String[] getLevelsArray( Transformer.ImageType imageType ) {
        int numLevels = transformer.getSourceNumberOfLevels( imageType );

        String[] resolutionLevels = new String[numLevels];
        for ( int i = 0; i < numLevels; i++ ) {
            resolutionLevels[i] = Integer.toString( i );
        }

        return resolutionLevels;
    }

}
