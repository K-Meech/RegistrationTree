package de.embl.schwab.crosshairSBEM;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import de.embl.cba.bdv.utils.sources.LazySpimSource;

public class test_loader {

    public static void main( String[] args )
    {
        final LazySpimSource emSource = new LazySpimSource("em", "Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\original_hdf5\\1_overviews.xml");
//        final LazySpimSource xraySource = new LazySpimSource("xray", "Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\original_hdf5\\high_res_flip_z_bigwarped.xml");
        final LazySpimSource xraySource = new LazySpimSource("xray", "Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\original_hdf5\\high_res_flip_z_bigwarped_elastix.xml");

        BdvStackSource bdvStackSource = BdvFunctions.show(emSource, 1);
        BdvFunctions.show(xraySource, 1, BdvOptions.options().addTo( bdvStackSource ) );
        bdvStackSource.setDisplayRange(0, 255);

        // flipping about z literally does that i.e. all z coordinates now negative, not quite what we wanted
    }
}
