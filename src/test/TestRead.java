import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import de.embl.schwab.crosshairSBEM.Cropper;
import de.embl.schwab.crosshairSBEM.Transformer;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

import java.io.File;

public class TestRead {
    public static void main( String[] args ) {

        String fixedImagePath = "C:\\Users\\meechan\\Documents\\temp\\testing_new_proj_creator\\blah.xml";
        String movingImagePath = "C:\\Users\\meechan\\Documents\\sample_register_images\\different_xml_scales\\rotated-scaled.xml";


        try {
            SpimData fixedSpimData = new XmlIoSpimData().load( fixedImagePath );
            SpimData movingSpimData = new XmlIoSpimData().load( movingImagePath );
            // BdvStackSource bdvStackSource = BdvFunctions.show(fixedSpimData).get(0);
            // bdvStackSource.setDisplayRange(0, 255);
            // BdvFunctions.show(movingSpimData,  BdvOptions.options().addTo(bdvStackSource)).get(0).setDisplayRange(0, 255);

            BdvStackSource bdvStackSource = BdvFunctions.show(movingSpimData).get(0);
            bdvStackSource.setDisplayRange(0, 255);
            BdvFunctions.show(fixedSpimData,  BdvOptions.options().addTo(bdvStackSource)).get(0).setDisplayRange(0, 255);

            System.out.println("yo");
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }
}
