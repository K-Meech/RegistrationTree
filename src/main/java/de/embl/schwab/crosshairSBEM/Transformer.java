package de.embl.schwab.crosshairSBEM;

import bdv.img.RenamableSource;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import de.embl.schwab.crosshairSBEM.registrationNodes.RegistrationNode;
import de.embl.schwab.crosshairSBEM.ui.BdvBehaviours;
import de.embl.schwab.crosshairSBEM.ui.Ui;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores information on fixed and moving sources
 * Entry point for all transformers
 */
public class Transformer {

    public static final String FIXEDSOURCENAME = "FIXED";
    public static final String MOVINGSOURCENAME = "MOVING";

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

    // if loaded from xml only
    private SpimData fixedSpimData;
    private SpimData movingSpimData;
    private File fixedImageXml;
    private File movingImageXml;

    // if loaded directly from Source, or from xml
    private BdvStackSource fixedSource;
    private BdvStackSource renamedFixedSource;
    private BdvStackSource movingSource;
    private BdvStackSource renamedMovingSource;

    private double[] fixedImageFullResolutionVoxelSize;
    private double[] movingImageFullResolutionVoxelSize;
    private String fixedImageUnit;
    private String movingImageUnit;

    private TransformedSource<?> fixedTransformedSource;
    private TransformedSource<?> movingTransformedSource;

    private File tempDir;

    private Ui ui;
    private BigWarpManager bigWarpManager;
    private ElastixManager elastixManager;
    private Cropper cropper;
    private Downsampler downsampler;
    private Exporter exporter;

    private ViewSpace viewSpace = ViewSpace.FIXED;

    BdvHandle bdv;

    private ArrayList<RegistrationNode> currentlyDisplayedNodes;

    public Transformer( File movingImageXml, File fixedImageXml, File tempDir ) {
        try {
            loadSources(movingImageXml, fixedImageXml);
            initialiseParameters( tempDir );
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }

    public Transformer( Source movingImage, Source fixedImage, double[] fixedImageFullResolutionVoxelSize,
                        double[] movingImageFullResolutionVoxelSize, String fixedImageUnit,
                        String movingImageUnit, File tempDir ) {
        loadSources( movingImage, fixedImage, fixedImageFullResolutionVoxelSize, movingImageFullResolutionVoxelSize,
                fixedImageUnit, movingImageUnit );
        initialiseParameters( tempDir );
    }

    private void initialiseParameters( File tempDir ) {
        ui = new Ui( this );
        this.tempDir = tempDir;
        bigWarpManager = new BigWarpManager( this );
        elastixManager = new ElastixManager( this, tempDir );
        cropper = new Cropper( this );
        downsampler = new Downsampler( this );
        exporter = new Exporter( this, cropper );
        currentlyDisplayedNodes = new ArrayList<>();
        new BdvBehaviours(this );

        // position windows
        Window viewFrame = SwingUtilities.getWindowAncestor(bdv.getViewerPanel());
        Point treeLocation = ui.getLocationOnScreen();
        viewFrame.setLocation(
                treeLocation.x + ui.getWidth(),
                treeLocation.y );
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

    public Map<String, RegistrationNode> getDisplayedNodeNamesToNodes() {
        Map<String, RegistrationNode> displayedNodeNamesToNodes = new HashMap<>();
        for ( RegistrationNode node: currentlyDisplayedNodes ) {
            displayedNodeNamesToNodes.put( node.getName(), node );
        }
        return displayedNodeNamesToNodes;
    }

    private void loadSources( Source movingImage, Source fixedImage, double[] fixedImageFullResolutionVoxelSize,
                              double[] movingImageFullResolutionVoxelSize, String fixedImageUnit,
                              String movingImageUnit ) {
        fixedSource = BdvFunctions.show( fixedImage );
        bdv = fixedSource.getBdvHandle();
        movingSource = BdvFunctions.show( movingImage, BdvOptions.options().addTo(bdv) );

        this.fixedImageFullResolutionVoxelSize = fixedImageFullResolutionVoxelSize;
        this.fixedImageUnit = fixedImageUnit;
        this.movingImageFullResolutionVoxelSize = movingImageFullResolutionVoxelSize;
        this.movingImageUnit = movingImageUnit;

        loadSources( fixedSource, movingSource );
    }

    private void loadSources( File movingImageXml, File fixedImageXml ) throws SpimDataException {
        this.fixedImageXml = fixedImageXml;
        this.movingImageXml = movingImageXml;

        fixedSpimData = new XmlIoSpimData().load( fixedImageXml.getAbsolutePath() );
        movingSpimData = new XmlIoSpimData().load( movingImageXml.getAbsolutePath() );

        fixedImageFullResolutionVoxelSize = getFullResolutionSourceVoxelSize( fixedSpimData );
        movingImageFullResolutionVoxelSize = getFullResolutionSourceVoxelSize( movingSpimData );
        fixedImageUnit = getSourceUnit( fixedSpimData );
        movingImageUnit = getSourceUnit( movingSpimData );

        fixedSource = BdvFunctions.show(fixedSpimData).get(0);
        bdv = fixedSource.getBdvHandle();
        movingSource = BdvFunctions.show(movingSpimData, BdvOptions.options().addTo(bdv)).get(0);

        loadSources( fixedSource, movingSource );
    }

    private void loadSources( BdvStackSource fixedSource, BdvStackSource movingSource ) {
        fixedTransformedSource = (TransformedSource<?>) ((SourceAndConverter<?>) fixedSource.getSources().get(0)).getSpimSource();
        movingTransformedSource = (TransformedSource<?>) ((SourceAndConverter<?>) movingSource.getSources().get(0)).getSpimSource();

        // for display purposes, wrap into renameable sources, and remove the originals
        // TODO - generalise brightness
        renamedFixedSource = BdvFunctions.show( new RenamableSource( getSource(ImageType.FIXED), FIXEDSOURCENAME ), BdvOptions.options().addTo(bdv));
        renamedFixedSource.setDisplayRange(0, 255);
        renamedMovingSource = BdvFunctions.show( new RenamableSource( getSource(ImageType.MOVING), MOVINGSOURCENAME ), BdvOptions.options().addTo(bdv));
        renamedMovingSource.setDisplayRange(0, 255);
        fixedSource.removeFromBdv();
        movingSource.removeFromBdv();
    }

    private double[] getFullResolutionSourceVoxelSize( SpimData spimData ) {
        double[] sourceDimensions = new double[3];
        spimData.getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().dimensions(sourceDimensions);
        return sourceDimensions;
    }

    private double[] getSourceVoxelSize( double[] fullResolutionVoxelSize, BdvStackSource bdvStackSource,  int level ) {
        return getSourceVoxelSize( fullResolutionVoxelSize, getSource(bdvStackSource), level );
    }

    // only used for writing masks! (as currently we don't support loading masks from Source, only from xml)
    public double[] getSourceVoxelSize( SpimData spimData, Source spimSource, int level ) {
        double[] fullResolutionVoxelSize = getFullResolutionSourceVoxelSize( spimData );
        return getSourceVoxelSize( fullResolutionVoxelSize, spimSource, level );
    }

    public double[] getSourceVoxelSize( double[] fullResolutionVoxelSize, Source spimSource, int level ) {
        long[] fullResolutionVoxelDimensions = getSourceVoxelDimensions( spimSource, 0 );

        if ( level == 0 ) {
            return fullResolutionVoxelSize;
        } else {
            long[] downsampledResolutionVoxelDimensions = getSourceVoxelDimensions( spimSource, level );
            double[] downsampledResolutionVoxelSize = new double[3];
            for ( int i = 0; i< fullResolutionVoxelDimensions.length; i++ ) {
                downsampledResolutionVoxelSize[i] = fullResolutionVoxelSize[i] *
                        ( (double) fullResolutionVoxelDimensions[i] / (double) downsampledResolutionVoxelDimensions[i] );
            }
            return downsampledResolutionVoxelSize;
        }
    }

    public double[] getSourceVoxelSize( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return fixedImageFullResolutionVoxelSize;
        } else {
            return movingImageFullResolutionVoxelSize;
        }
    }

    public double[] getSourceVoxelSize( ImageType imageType, int level ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceVoxelSize( getSourceVoxelSize( imageType ), fixedSource, level );
        } else {
            return getSourceVoxelSize( getSourceVoxelSize( imageType ), movingSource, level );
        }
    }

    private String getSourceUnit( SpimData spimData ) {
        return spimData.getSequenceDescription().getViewSetupsOrdered().get(0).getVoxelSize().unit();
    }

    private long[] getSourceVoxelDimensions( BdvStackSource bdvStackSource, int level ) {
        Source spimSource = getSource( bdvStackSource );
        return getSourceVoxelDimensions( spimSource, level );
    }

    private long[] getSourceVoxelDimensions( Source spimSource, int level ) {
        return spimSource.getSource( 0, level ).dimensionsAsLongArray();
    }

    public long[] getSourceVoxelDimensions( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceVoxelDimensions(fixedSource, 0);
        } else {
            return getSourceVoxelDimensions( movingSource, 0 );
        }
    }

    private int getSourceNumberOfLevels( BdvStackSource bdvStackSource ) {
        Source spimSource = ((SourceAndConverter<?>) bdvStackSource.getSources().get(0) ).getSpimSource();
        return spimSource.getNumMipmapLevels();
    }

    public Source getSource( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return getSource( fixedSource );
        } else {
            return getSource( movingSource );
        }
    }

    public SourceAndConverter<?> getSourceAndConverter( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return getSourceAndConverter( fixedSource );
        } else {
            return getSourceAndConverter( movingSource );
        }
    }

    private SourceAndConverter<?> getSourceAndConverter( BdvStackSource bdvStackSource ) {
        return (SourceAndConverter<?>) bdvStackSource.getSources().get(0);
    }

    private Source getSource( BdvStackSource bdvStackSource ) {
            return getSourceAndConverter( bdvStackSource ).getSpimSource();
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
            if ( fixedImageXml != null ) {
                return fixedImageXml.getAbsolutePath();
            }
        } else {
            if ( movingImageXml != null ) {
                return movingImageXml.getAbsolutePath();
            }
        }

        return null;
    }

    public String getSourceUnit( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            return fixedImageUnit;
        } else {
            return movingImageUnit;
        }
    }

    // Affine is from registration program i.e. defined as fixed to moving space
    public Source createTransformedSource( ImageType imageType, RegistrationNode regNode ) {
        TransformedSource transformedSource;
        if ( imageType == ImageType.FIXED ) {
            transformedSource = new TransformedSource(getSource(ImageType.FIXED));
            transformedSource.setFixedTransform(regNode.getFullTransform());
        } else {
            transformedSource = new TransformedSource(getSource(ImageType.MOVING));
            transformedSource.setFixedTransform(regNode.getFullTransform().inverse());
        }

        return new RenamableSource( transformedSource, regNode.getName() );
    }

    // Affine is from registration program i.e. defined as fixed to moving space
    public void showSource( RegistrationNode regNode ) {
        if ( regNode.getSrc() == null ) {
            Source transformedSource;
            if (viewSpace == Transformer.ViewSpace.MOVING) {
                // create a source with that transform and display it
                transformedSource = createTransformedSource(ImageType.FIXED, regNode);
            } else {
                transformedSource = createTransformedSource(ImageType.MOVING, regNode);
            }

            BdvStackSource stackSource = BdvFunctions.show(transformedSource, BdvOptions.options().addTo(bdv));
            // TODO - generalise brightness setting?
            stackSource.setDisplayRange(0, 255);
            regNode.setSrc(stackSource);
            currentlyDisplayedNodes.add(regNode);
            refreshBdvWindow();
        }
    }

    public void onlyShowOriginalImage( ImageType imageType ) {
        for ( RegistrationNode node: currentlyDisplayedNodes ) {
            node.getSrc().setActive( false );
        }

        if ( imageType == ImageType.FIXED ) {
            renamedFixedSource.setActive( true );
            renamedMovingSource.setActive( false );
        } else {
            renamedMovingSource.setActive( true );
            renamedFixedSource.setActive( false );
        }
    }

    public void hideOriginalImage( ImageType imageType ) {
        if ( imageType == ImageType.FIXED ) {
            renamedFixedSource.setActive( false );
        } else {
            renamedMovingSource.setActive( false );
        }
    }

    public void removeSource( RegistrationNode regNode ) {
        BdvStackSource src = regNode.getSrc();
        if ( src != null ) {
            regNode.getSrc().removeFromBdv();
            regNode.setSrc(null);
            currentlyDisplayedNodes.remove(regNode);
        }
    }

    public void removeAllCurrentSources() {
        // make new arraylist, otherwise we are modifying the list as we iterate through it
        ArrayList<RegistrationNode> currentNodes = new ArrayList<>( currentlyDisplayedNodes );
        for ( RegistrationNode regNode: currentNodes ) {
            removeSource( regNode );
        }
    }

    private void focus( SourceAndConverter<?> sourceAndConverter ) {
        final AffineTransform3D transform = new ViewerTransformAdjuster(  bdv, sourceAndConverter ).getTransform();
        new ViewerTransformChanger( bdv, transform, false, 1000 ).run();
    }

    public void focus( RegistrationNode node ) {
        focus( getSourceAndConverter( node.getSrc()) );
    }

    public void focus( ImageType imageType ) {
        SourceAndConverter<?> sourceAndConverter = getSourceAndConverter( imageType );
        focus( sourceAndConverter );
    }

    public void toggleViewSpace() {
        if ( viewSpace == ViewSpace.FIXED ) {
            viewSpace = ViewSpace.MOVING;
        } else {
            viewSpace = ViewSpace.FIXED;
        }

        // make new arraylist, otherwise we are modifying the list as we iterate through it
        ArrayList<RegistrationNode> currentNodes = new ArrayList<>( currentlyDisplayedNodes );
        for ( RegistrationNode regNode: currentNodes ) {
            removeSource( regNode );
            showSource( regNode );
        }
    }

    // TODO - add option to add each item in chain as separate view transform? Would make it more readable so people
    // could just look at the xml and see all the individual big warp / elastix etc transforms
    public void writeBdvXml(ViewSpace viewSpace, RegistrationNode regNode, String newXmlPath ) throws SpimDataException {
        SpimDataMinimal spimDataMinimal;
        if ( viewSpace == ViewSpace.FIXED ) {
            spimDataMinimal = new XmlIoSpimDataMinimal().load(movingImageXml.getAbsolutePath());
        } else {
            spimDataMinimal = new XmlIoSpimDataMinimal().load(fixedImageXml.getAbsolutePath());
        }
        // add on top of any existing transforms in the xml file
        Map<ViewId, ViewRegistration> registrations = spimDataMinimal.getViewRegistrations().getViewRegistrations();

        int numTimepoints = spimDataMinimal.getSequenceDescription().getTimePoints().size();
        int numSetups = spimDataMinimal.getSequenceDescription().getViewSetupsOrdered().size();

        AffineTransform3D regTransform = new AffineTransform3D();
        if ( viewSpace == ViewSpace.FIXED ) {
            regTransform.set(regNode.getFullTransform().inverse());
        } else {
            regTransform.set(regNode.getFullTransform());
        }

        for ( int t = 0; t < numTimepoints; ++t ) {
            for (int s = 0; s < numSetups; ++s) {
                ViewTransform viewTransform = new ViewTransformAffine( regNode.getName(), regTransform );
                registrations.get( new ViewId(t, s) ).preconcatenateTransform( viewTransform );
            }
        }

        SpimDataMinimal updatedSpimDataMinimial = new SpimDataMinimal(spimDataMinimal.getBasePath(),
                spimDataMinimal.getSequenceDescription(), new ViewRegistrations( registrations) );

        new XmlIoSpimDataMinimal().save( updatedSpimDataMinimial, newXmlPath );
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
        return getRAI( spimSource, level );
    }

    public RandomAccessibleInterval getRAI( Source spimSource, int level ) {
        return spimSource.getSource( 0, level);
    }

}
