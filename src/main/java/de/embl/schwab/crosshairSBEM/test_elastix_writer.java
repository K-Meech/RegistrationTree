package de.embl.schwab.crosshairSBEM;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import itc.commands.BigWarpAffineToTransformixFileCommand;
import itc.converters.AffineTransform3DToFlatString;
import itc.transforms.elastix.ElastixTransform;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.realtransform.AffineTransform3D;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class test_elastix_writer {


    public static void main( String[] args )
    {
        final LazySpimSource emSource = new LazySpimSource("em", "Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\original_hdf5\\1_overviews.xml");
        final LazySpimSource xraySource = new LazySpimSource("xray", "Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\original_hdf5\\high_res_flip_z_bigwarped.xml");

        // total transform, except for the unit scaling
        AffineTransform3D totalTransform = new AffineTransform3D();
        totalTransform.identity();

        try {
            SpimDataMinimal spimDataMinimal = new XmlIoSpimDataMinimal().load("Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\original_hdf5\\high_res_flip_z_bigwarped.xml");
            List<ViewRegistration> viewRegistrations = spimDataMinimal.getViewRegistrations().getViewRegistrationsOrdered();
            if (viewRegistrations.size() == 1) {
                List<ViewTransform> viewTransforms = viewRegistrations.get(0).getTransformList();
//                for (int i = 0; i<viewTransforms.size() - 1; i++) {
                for (int i = 0; i<viewTransforms.size() - 1; i++) {
                    System.out.println(i);
                    totalTransform.concatenate( viewTransforms.get(i).asAffine3D() );
                }

            } else {
                System.out.println( " More than one view registration!" );
            }

        } catch (SpimDataException e) {
            e.printStackTrace();
        }

//        AffineTransform3D totalTransform = new AffineTransform3D();
//        xraySource.getSourceTransform(0, 0, totalTransform);

        BigWarpAffineToTransformixFileCommand bw = new BigWarpAffineToTransformixFileCommand();
//        AffineTransform3D xrayTransform = new AffineTransform3D();
//        xraySource.getSourceTransform(0, 0, xrayTransform);
//        bw.affineTransformString = xrayTransform.toString();
        bw.affineTransformString = new AffineTransform3DToFlatString().convert(totalTransform).getString();
        bw.affineTransformUnit = "micrometer";
        bw.interpolation = ElastixTransform.FINAL_LINEAR_INTERPOLATOR;
        bw.transformationOutputFile = new File("Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\targeting_test\\elastix_flipped_xray_to_em\\initialTransform.txt");
        bw.targetImageFile = new File("Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\targeting_test\\elastix_flipped_xray_to_em\\065_9_high_res.tif");
        bw.run();

        // recall elastix works in mm, so the spacing listed in the transform file will be in mm
    }
}
