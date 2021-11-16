package de.embl.schwab.registrationTree.temp;

import de.embl.cba.bdv.utils.sources.LazySpimSource;
import net.imglib2.RandomAccessibleInterval;

public class test_downsampled_writer {

    public static void main( String[] args ) {
        final LazySpimSource emSource = new LazySpimSource("em", "Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\original_hdf5\\1_overviews.xml");
        final LazySpimSource xraySource = new LazySpimSource("xray", "Z:\\Kimberly\\Projects\\Targeting_SBEM\\Data\\Derived\\65.9_was_mislabelled_as_65.6\\original_hdf5\\high_res_flip_z_bigwarped.xml");

        RandomAccessibleInterval rai = emSource.getSource(0,1);

    }
}
