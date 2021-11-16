package de.embl.schwab.registrationTree.temp;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import itc.converters.AffineTransform3DToFlatString;
import itc.converters.ElastixEuler3DToAffineTransform3D;
import itc.converters.ElastixSimilarity2DToAffineTransform3D;
import itc.converters.ElastixSimilarity3DToAffineTransform3D;
import itc.transforms.elastix.ElastixEulerTransform3D;
import itc.transforms.elastix.ElastixSimilarityTransform2D;
import itc.transforms.elastix.ElastixSimilarityTransform3D;
import itc.transforms.elastix.ElastixTransform;
import itc.utilities.TransformUtils;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import net.imglib2.realtransform.AffineTransform3D;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class test_similarity_reader_2d {

    public static void main( String[] args )
    {
        BdvHandle bdv;
        BdvStackSource bdvStack;

        try {
            ArrayList<TransformedSource> transformedSources = new ArrayList<>();
            final SpimData fixed = new XmlIoSpimData().load( "C:\\Users\\meechan\\Documents\\sample_register_images\\similarity2d\\mri-stack-2d.xml");
            final SpimData moving = new XmlIoSpimData().load("C:\\Users\\meechan\\Documents\\sample_register_images\\similarity2d\\rotated_10_2d.xml");
            final SpimData corrected = new XmlIoSpimData().load("C:\\Users\\meechan\\Documents\\sample_register_images\\similarity2d\\mri-stack-2d.xml");

            bdv = null;

            for(SpimData source: new SpimData[]{fixed, moving, corrected}) {
                BdvStackSource bdvStackSource;
                if (bdv == null) {
                    bdvStackSource = BdvFunctions.show(source).get(0);
                    bdv = bdvStackSource.getBdvHandle();
                } else {
                    bdvStackSource = BdvFunctions.show(source, BdvOptions.options().addTo(bdv)).get(0);
                }
                bdvStackSource.setDisplayRange(0, 255);
                transformedSources.add( (TransformedSource<?>) ((SourceAndConverter<?>) bdvStackSource.getSources().get(0)).getSpimSource() );
            }

            ElastixTransform elastixTransform = ElastixTransform.load( new File("C:\\Users\\meechan\\Documents\\temp\\elatix_checks\\similarityimpl\\similarity_proper_test_2d\\TransformParameters.0.txt" ));
            AffineTransform3D bdvTransform = ElastixSimilarity2DToAffineTransform3D.convert( (ElastixSimilarityTransform2D) elastixTransform);

            TransformedSource transformedSource = transformedSources.get(2);
            transformedSource.setFixedTransform( bdvTransform );
            bdv.getViewerPanel().requestRepaint();

        } catch (IOException | SpimDataException e) {
            e.printStackTrace();
        }

    }
}
