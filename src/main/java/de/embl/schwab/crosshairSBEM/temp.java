package de.embl.schwab.crosshairSBEM;

import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;


public class temp {
    public void run() throws SpimDataException
    {
        // final SpimData spimData = new XmlIoSpimData().load( "C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack.xml" );
        //
        // final BdvStackSource< ? > bdvStackSource =
        //         BdvFunctions.show( spimData ).get( 0 );
        //
        // bdvStackSource.setDisplayRange( 0, 255 );
        //
        // final BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
        //
        // final SpimData spimData2 = new XmlIoSpimData().load( "C:\\Users\\meechan\\Documents\\sample_register_images\\mri-stack-rotated.xml" );
        //
        // BdvFunctions.show( spimData2, BdvOptions.options().addTo( bdvHandle ) ).get( 0 ).setDisplayRange( 0, 255 );
        //
        // final FinalRealInterval maximalRangeInterval = BdvUtils.getRealIntervalOfCurrentSource( bdvHandle );
        //
        // final TransformedRealBoxSelectionDialog.Result result =
        //         BdvDialogs.showBoundingBoxDialog(
        //                 bdvHandle,
        //                 maximalRangeInterval );
        //
        // final BdvRealSourceToVoxelImageExporter exporter =
        //         new BdvRealSourceToVoxelImageExporter(
        //                 bdvHandle,
        //                 BdvUtils.getVisibleSourceIndices( bdvHandle ),
        //                 result.getInterval(),
        //                 result.getMinTimepoint(),
        //                 result.getMaxTimepoint(),
        //                 Interpolation.NLINEAR,
        //                 new double[]{ 0.5, 0.5, 0.5 },
        //                 BdvRealSourceToVoxelImageExporter.ExportModality.ShowImages,
        //                 BdvRealSourceToVoxelImageExporter.ExportDataType.UnsignedByte,
        //                 Runtime.getRuntime().availableProcessors(),
        //                 new ProgressWriterIJ()
        //         );
        //
        // exporter.setOutputDirectory( "C:\\Users\\meechan\\Documents\\main.java.de.embl.schwab.crosshairSBEM.temp\\exportTest");
        //
        // exporter.export();

    }

    public static void main( String[] args ) throws SpimDataException
    {
        new ImageJ().ui().showUI();
        new temp().run();
    }
}
