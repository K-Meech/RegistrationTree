package de.embl.schwab.crosshairSBEM;

import net.imglib2.Interval;

public class Downsampler {

    private Transformer transformer;

    public Downsampler( Transformer transformer ) { this.transformer = transformer; }

    private double getEstimatedSizeInGigaBytes( long[] voxelDimensions ) {
        long nVoxels = 1;
        for ( long voxelDim: voxelDimensions ) {
            nVoxels = nVoxels * voxelDim;
        }

        // assume 8-bit. We currently write all elastix images in 8-bit
        // then one uncompressed voxel == 1 byte

        // round to 3 dp
        double sizeEstimate = (double) nVoxels / 1000000000.0;
        sizeEstimate = Math.round(sizeEstimate * 1000.0) / 1000.0;
        return sizeEstimate;
    }

    private String makeResolutionString( int level, long[] sourceDimensions, double sizeEstimate ) {
        StringBuilder resolutionLevelString = new StringBuilder( Integer.toString(level) );
        resolutionLevelString.append("  |  ");
        for ( int j = 0; j<sourceDimensions.length; j++) {
            if ( j != 0 ) {
                resolutionLevelString.append("-");
            }
            resolutionLevelString.append( sourceDimensions[j] );
        }

        resolutionLevelString.append("  |  ");
        resolutionLevelString.append( sizeEstimate ).append( "GB");

        return resolutionLevelString.toString();
    }

    public String[] getLevelsArray( Transformer.ImageType imageType ) {
        int numLevels = transformer.getSourceNumberOfLevels( imageType );

        String[] resolutionLevels = new String[numLevels];
        for ( int i = 0; i < numLevels; i++ ) {
            long[] sourceDimensions = transformer.getSourceVoxelDimensions( imageType, i );
            double sizeEstimate = getEstimatedSizeInGigaBytes( sourceDimensions );
            resolutionLevels[i] = makeResolutionString( i, sourceDimensions, sizeEstimate );
        }

        return resolutionLevels;
    }

    public String[] getLevelsArray( Transformer.ImageType imageType, String cropName ) {
        int numLevels = transformer.getSourceNumberOfLevels( imageType );

        String[] resolutionLevels = new String[numLevels];
        for ( int i = 0; i < numLevels; i++ ) {
            Interval interval = transformer.getCropper().getImageCropIntervalVoxelSpace( imageType, cropName, i );
            double sizeEstimate = getEstimatedSizeInGigaBytes( interval.dimensionsAsLongArray() );

            resolutionLevels[i] = makeResolutionString( i, interval.dimensionsAsLongArray(), sizeEstimate );
        }

        return resolutionLevels;
    }

}
