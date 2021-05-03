package de.embl.schwab.crosshairSBEM;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
// import de.embl.cba.bdv.utils.export.BdvRealSourceToVoxelImageExporter;
import de.embl.schwab.crosshairSBEM.ui.ElastixUI;
import de.embl.schwab.crosshairSBEM.ui.RegistrationTree;
import de.embl.schwab.crosshairSBEM.ui.Ui;
import itc.commands.BigWarpAffineToTransformixFileCommand;
import itc.converters.AffineTransform3DToFlatString;
import itc.transforms.elastix.ElastixTransform;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import org.apache.commons.compress.utils.FileNameUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static de.embl.schwab.crosshairSBEM.SwingUtils.getButton;

// TODO - is soruce index consistent
/**
 * Stores information on fixed and moving sources
 * Entry point for all transformers
 */
public class Transformer {

    public enum TransformType {
        BigWarp,
        Elastix,
        Manual,
        AffineString
    }

    public enum ImageType {
        FIXED,
        MOVING
    }

    public enum ViewSpace {
        FIXED,
        MOVING
    }

    private SpimData fixedSpimData;
    private BdvStackSource fixedSource;
    private SpimData movingSpimData;
    private BdvStackSource movingSource;

    private TransformedSource<?> fixedTransformedSource;
    private TransformedSource<?> movingTransformedSource;

    private File fixedImage;
    private File movingImage;

    private Ui ui;
    private BigWarpManager bigWarpManager;
    private ElastixManager elastixManager;
    private Cropper cropper;
    private Downsampler downsampler;
    private Exporter exporter;

    private ViewSpace viewSpace = ViewSpace.FIXED;
    ArrayList<String> sourceNames = new ArrayList<>();
    ArrayList<TransformedSource<?>> transformedSources = new ArrayList<>();

    BdvHandle bdv;
    BigWarp bw;
    int fixedSourceIndex;
    int movingSourceIndex;

    public Transformer( File movingImage, File fixedImage ) {
        try {
            ui = new Ui( this );
            loadSources(movingImage, fixedImage);
            this.fixedImage = fixedImage;
            this.movingImage = movingImage;
            bigWarpManager = new BigWarpManager( this );
            elastixManager = new ElastixManager( this );
            cropper = new Cropper( this );
            downsampler = new Downsampler( this );
            exporter = new Exporter( this, cropper );
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getSourceNames() {
        return sourceNames;
    }

    public BigWarpManager getBigWarpManager() {
        return bigWarpManager;
    }

    public ElastixManager getElastixManager() {
        return elastixManager;
    }

    public ViewSpace getViewSpace() {
        return viewSpace;
    }

    public Ui getUi() {
        return ui;
    }

    public Cropper getCropper() {
        return cropper;
    }

    public Downsampler getDownsampler() {
        return downsampler;
    }

    public Exporter getExporter() {
        return exporter;
    }

    public BdvHandle getBdv() {
        return bdv;
    }

    public void refreshBdvWindow() {
        bdv.getViewerPanel().requestRepaint();
    }

    public AffineTransform3D getBaseTransform( ImageType imageType ) {
        AffineTransform3D affine = new AffineTransform3D();
        Source source = null;
        if ( imageType == ImageType.FIXED ) {
            source = getSource( ImageType.FIXED );

        } else {
            source = getSource( ImageType.MOVING );
        }

        source.getSourceTransform(0, 0, affine);
        return affine;
    }

    public void setTransform( ImageType imageType, AffineTransform3D affine ) {
        if ( imageType == ImageType.FIXED ) {
            fixedTransformedSource.setFixedTransform(affine);
        } else {
            movingTransformedSource.setFixedTransform( affine );
        }
    }

    private void loadSources( File movingImage, File fixedImage ) throws SpimDataException {

        String fixedImagePath = fixedImage.getAbsolutePath();
        String movingImagePath = movingImage.getAbsolutePath();
        fixedSpimData = new XmlIoSpimData().load( fixedImagePath );
        movingSpimData = new XmlIoSpimData().load( movingImagePath );

        String fixedSourceName = FileNameUtils.getBaseName( fixedImagePath );
        String movingSourceName = FileNameUtils.getBaseName( movingImagePath );

        // TODO -rename the source somehow so appears nicely in bdv pullout (how is this so difficult to find?)

        fixedSource = BdvFunctions.show(fixedSpimData).get(0);
        bdv = fixedSource.getBdvHandle();

        Window viewFrame = SwingUtilities.getWindowAncestor(bdv.getViewerPanel());
        Point treeLocation = ui.getLocationOnScreen();
        viewFrame.setLocation(
                treeLocation.x + ui.getWidth(),
                 treeLocation.y );

        fixedSource.setDisplayRange(0, 255);
        fixedTransformedSource = (TransformedSource<?>) ((SourceAndConverter<?>) fixedSource.getSources().get(0)).getSpimSource();

        movingSource = BdvFunctions.show(movingSpimData, BdvOptions.options().addTo(bdv)).get(0);
        movingSource.setDisplayRange(0, 255);
        movingTransformedSource = (TransformedSource<?>) ((SourceAndConverter<?>) movingSource.getSources().get(0)).getSpimSource();
    }

    private double[] getFullResolutionSourceVoxelSize( SpimData spimData ) {
        double[] sourceDimensions = new double[3];
        spimData.getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().dimensions(sourceDimensions);
        return sourceDimensions;
    }

    private double[] getSourceVoxelSize( SpimData spimData, BdvStackSource bdvStackSource,  int level ) {
        long[] fullResolutionVoxelDimensions = getSourceVoxelDimensions( bdvStackSource, 0 );
        double[] fullResolutionVoxelSize = getFullResolutionSourceVoxelSize( spimData );

        if ( level == 0 ) {
            return fullResolutionVoxelSize;
        } else {
            long[] downsampledResolutionVoxelDimensions = getSourceVoxelDimensions( bdvStackSource, level );
            double[] downsampledResolutionVoxelSize = new double[3];
            for ( int i = 0; i< fullResolutionVoxelDimensions.length; i++ ) {
                downsampledResolutionVoxelSize[i] = fullResolutionVoxelSize[i] *
                        ( (double) fullResolutionVoxelDimensions[i] / (double) downsampledResolutionVoxelDimensions[i] );
            }
            return downsampledResolutionVoxelSize;
        }
    }

    private String getSourceUnit( SpimData spimData ) {
        return spimData.getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().unit();
    }

    private long[] getSourceVoxelDimensions( BdvStackSource bdvStackSource, int level ) {

        Source spimSource = ((SourceAndConverter<?>) bdvStackSource.getSources().get(0) ).getSpimSource();

        // TODO - warn doesn't support time series
        long[] dimensions = new long[3];
        spimSource.getSource( 0, level ).dimensions( dimensions );
        return dimensions;
    }

    private int getSourceNumberOfLevels( BdvStackSource bdvStackSource ) {
        Source spimSource = ((SourceAndConverter<?>) bdvStackSource.getSources().get(0) ).getSpimSource();
        return spimSource.getNumMipmapLevels();
    }

    public Source getSource( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return ((SourceAndConverter<?>) fixedSource.getSources().get(0) ).getSpimSource();
        } else {
            return ((SourceAndConverter<?>) movingSource.getSources().get(0) ).getSpimSource();
        }
    }

    public SpimData getSpimData( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return fixedSpimData;
        } else {
            return movingSpimData;
        }
    }

    public String getSourcePath( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return fixedImage.getAbsolutePath();
        } else {
            return movingImage.getAbsolutePath();
        }
    }

    public double[] getSourceVoxelSize( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return getFullResolutionSourceVoxelSize(fixedSpimData);
        } else {
            return getFullResolutionSourceVoxelSize(movingSpimData);
        }
    }

    public double[] getSourceVoxelSize( ImageType imageType, int level ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceVoxelSize( fixedSpimData, fixedSource, level );
        } else {
            return getSourceVoxelSize( movingSpimData, movingSource, level );
        }
    }

    public String getSourceUnit( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceUnit(fixedSpimData);
        } else {
            return getSourceUnit(movingSpimData);
        }
    }

    public long[] getSourceVoxelDimensions( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceVoxelDimensions(fixedSource, 0);
        } else {
            return getSourceVoxelDimensions( movingSource, 0 );
        }
    }

    // Affine is from registration program i.e. defined as fixed to moving space
    public TransformedSource createTransformedSource( ImageType imageType, AffineTransform3D affine ) {
        TransformedSource transformedSource;
        if ( imageType == ImageType.FIXED ) {
            transformedSource = new TransformedSource(getSource(ImageType.FIXED));
            transformedSource.setFixedTransform(affine);
        } else {
            transformedSource = new TransformedSource(getSource(ImageType.MOVING));
            transformedSource.setFixedTransform(affine.inverse());
        }

        return transformedSource;
    }

    private void addViewTransform( SpimData spimData, AffineTransform3D affine ) {
        ViewTransform newViewTransform = new ViewTransformAffine("previous_transforms", affine);
        spimData.getViewRegistrations().getViewRegistrationsOrdered().get(0).preconcatenateTransform( newViewTransform );
        spimData.getViewRegistrations().getViewRegistrationsOrdered().get(0).updateModel();
    }

    // Affine is from registration program i.e. defined as fixed to moving space
    // based on https://github.com/bigdataviewer/bigdataviewer-playground/blob/e6b93d7d2ac4cb490a9c2a19b813fbe96e640ea5/src/main/java/sc/fiji/bdvpg/sourceandconverter/transform/SourceTransformHelper.java#L249
    public SpimData getSpimData( ImageType imageType, AffineTransform3D affine ) {
        SpimData spimData = null;
        try {
            if ( imageType == ImageType.FIXED ) {
                spimData = new XmlIoSpimData().load( fixedImage.getAbsolutePath() );
                addViewTransform(spimData, affine);
            } else {
                spimData = new XmlIoSpimData().load( movingImage.getAbsolutePath() );
                addViewTransform(spimData, affine.inverse());
            }
        } catch (SpimDataException e) {
            e.printStackTrace();
        }

        // TODO - warn time not supported
        return spimData;
    }

    // Affine is from registration program i.e. defined as fixed to moving space
    public void showSource( AffineTransform3D affine ) {
        TransformedSource transformedSource;
        if (viewSpace == Transformer.ViewSpace.MOVING) {
            // create a source with that transform and display it
            transformedSource = createTransformedSource( ImageType.FIXED, affine );
        } else  {
            transformedSource = createTransformedSource( ImageType.MOVING, affine );
        }

        BdvStackSource stackSource = BdvFunctions.show(transformedSource, BdvOptions.options().addTo(bdv));
        // TODO - generalise?
        stackSource.setDisplayRange(0, 255);
        refreshBdvWindow();
    }

    public long[] getSourceVoxelDimensions( ImageType imageType, int level ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceVoxelDimensions( fixedSource, level );
        } else {
            return getSourceVoxelDimensions( movingSource, level );
        }
    }

    public int getSourceNumberOfLevels( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceNumberOfLevels( fixedSource );
        } else {
            return getSourceNumberOfLevels( movingSource );
        }
    }

    public RandomAccessibleInterval getRAI( ImageType imageType, int level ) {

        Source spimSource;
        if ( imageType == ImageType.FIXED ) {
            spimSource = ((SourceAndConverter<?>) fixedSource.getSources().get(0) ).getSpimSource();
        } else {
            spimSource = ((SourceAndConverter<?>) movingSource.getSources().get(0) ).getSpimSource();
        }
        return spimSource.getSource( 0, level);
    }

    public static void main( String[] args )
    {
    }


}
