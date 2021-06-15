package de.embl.schwab.crosshairSBEM;

import net.imglib2.Interval;

public class Downsampler {

    private Transformer transformer;

    public Downsampler( Transformer transformer ) { this.transformer = transformer; }

    public String[] getLevelsArray( Transformer.ImageType imageType ) {
        int numLevels = transformer.getSourceNumberOfLevels( imageType );

        String[] resolutionLevels = new String[numLevels];
        for ( int i = 0; i < numLevels; i++ ) {
            long[] sourceDimensions = transformer.getSourceVoxelDimensions( imageType, i );

            StringBuilder resolutionLevelString = new StringBuilder( Integer.toString(i) );
            for ( int j = 0; j<sourceDimensions.length; j++) {
                resolutionLevelString.append("-").append( sourceDimensions[j] );
            }

            resolutionLevels[i] = resolutionLevelString.toString();
        }

        return resolutionLevels;
    }

    public String[] getLevelsArray( Transformer.ImageType imageType, String cropName ) {
        int numLevels = transformer.getSourceNumberOfLevels( imageType );

        String[] resolutionLevels = new String[numLevels];
        for ( int i = 0; i < numLevels; i++ ) {
            Interval interval = transformer.getCropper().getImageCropIntervalVoxelSpace( imageType, cropName, i );

            StringBuilder resolutionLevelString = new StringBuilder( Integer.toString(i) );
            for ( int j = 0; j<interval.numDimensions(); j++) {
                resolutionLevelString.append("-").append(interval.dimension(j));
            }

            resolutionLevels[i] = resolutionLevelString.toString();
        }

        return resolutionLevels;
    }

}
