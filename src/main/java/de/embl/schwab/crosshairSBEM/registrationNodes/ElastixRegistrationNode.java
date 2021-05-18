package de.embl.schwab.crosshairSBEM.registrationNodes;

import de.embl.cba.elastixwrapper.wrapper.elastix.parameters.ElastixParameters;
import itc.transforms.elastix.ElastixTransform;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.Map;

public class ElastixRegistrationNode extends RegistrationNode {
    // crops in pixel space of full-resolution
    public Map<String, RealInterval> fixedCrop;
    public Map<String, RealInterval> movingCrop;

    // resolution level of bdv file, 0 == full resolution
    public int fixedDownsamplingLevel;
    public int movingDownsamplingLevel;

    public ElastixParameters elastixParameters;
    public ElastixTransform elastixTransform;

    public ElastixRegistrationNode( Map<String, RealInterval> fixedCrop, Map<String, RealInterval> movingCrop,
                                    int fixedDownsamplingLevel, int movingDownsamplingLevel, ElastixParameters elastixParameters,
                                    ElastixTransform elastixTransform, AffineTransform3D affine,
                                    String transformName ) {
        super( affine, transformName );
        this.fixedCrop = fixedCrop;
        this.movingCrop = movingCrop;
        this.fixedDownsamplingLevel = fixedDownsamplingLevel;
        this.movingDownsamplingLevel = movingDownsamplingLevel;
        this.elastixParameters = elastixParameters;
        this.elastixTransform = elastixTransform;
    }
}
