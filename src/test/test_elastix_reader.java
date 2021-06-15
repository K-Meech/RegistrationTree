package de.embl.schwab.crosshairSBEM.temp;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import itc.commands.BigWarpAffineToTransformixFileCommand;
import itc.converters.AffineTransform3DToFlatString;
import itc.converters.ElastixEuler3DToAffineTransform3D;
import itc.transforms.elastix.ElastixEulerTransform3D;
import itc.transforms.elastix.ElastixTransform;
import itc.utilities.TransformUtils;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewTransform;
import net.imglib2.realtransform.AffineTransform3D;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class test_elastix_reader {
    public static void main( String[] args )
    {
        try {
            ElastixTransform elastixTransform = ElastixTransform.load( new File("Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\targeting_test\\elastix_flipped_xray_to_em_downsampled\\elastix\\TransformParameters.0.txt" ));
            AffineTransform3D bdvTransform = ElastixEuler3DToAffineTransform3D.convert( (ElastixEulerTransform3D) elastixTransform);

            // the elastix transform is in mm units, we convert to what was used for rest of images (microns)
            bdvTransform = TransformUtils.scaleAffineTransform3DUnits( bdvTransform, new double[]{ 1000, 1000, 1000 } );
            System.out.println(new AffineTransform3DToFlatString().convert(bdvTransform).getString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
