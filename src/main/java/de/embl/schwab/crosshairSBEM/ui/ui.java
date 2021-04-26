package de.embl.schwab.crosshairSBEM.ui;

import de.embl.schwab.crosshairSBEM.Transformer;
import ij.gui.GenericDialog;

import javax.swing.*;
import java.awt.*;

import static de.embl.schwab.crosshairSBEM.SwingUtils.*;

public class ui extends JFrame {



    Transformer transformer;
    int maxTranformNumber = 0;

    public ui () {
        this.getContentPane().setLayout( new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS ) );
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        addUneditableTransformPanel( 1, "physical units transform (from bdv xml)" );
        // addTranformPanel( 2, "test" );

        // this.transformer = new Transformer();

        this.pack();
        this.show();
    }

    public void updateFrame() {
        this.pack();
    }

    private void addUneditableTransformPanel( int transformNumber, String name ) {
        final JPanel horizontalLayoutPanel = horizontalLayoutPanel();

        JComboBox transformCombo = createTranformComboBox( name );

        horizontalLayoutPanel.add(getJLabel(Integer.toString( transformNumber ), 60, 10));
        horizontalLayoutPanel.add( transformCombo );
        // blank label so aligns with editable panels
        horizontalLayoutPanel.add(getJLabel("", BUTTON_DIMENSION.width*2, BUTTON_DIMENSION.height));
        horizontalLayoutPanel.setAlignmentX( Component.LEFT_ALIGNMENT );
        transformCombo.setEditable(false);
        transformCombo.setEnabled(false);

        maxTranformNumber += 1;

        this.getContentPane().add(horizontalLayoutPanel);
    }

    // private void addTranformPanel( int transformNumber, String name ) {
    //     final JPanel horizontalLayoutPanel = horizontalLayoutPanel();
    //
    //     final JButton visibilityButton = getButton("V");
    //     final JButton addTransformButton = getButton("+");
    //
    //     JComboBox transformCombo = createTranformComboBox( name );
    //     // addButton.addActionListener( e ->
    //     // {
    //     //     new Thread( () -> { addDatasetDialog(); } ).start();
    //     // } );
    //     //
    //     addTransformButton.addActionListener( e ->
    //     {
    //         new Thread( () -> { addTransformDialog(); } ).start();
    //     } );
    //
    //     horizontalLayoutPanel.add(getJLabel(Integer.toString( transformNumber ), 60, 10));
    //     horizontalLayoutPanel.add( transformCombo );
    //     horizontalLayoutPanel.add( visibilityButton );
    //     horizontalLayoutPanel.add( addTransformButton );
    //     horizontalLayoutPanel.setAlignmentX( Component.LEFT_ALIGNMENT );
    //
    //     maxTranformNumber += 1;
    //
    //     this.getContentPane().add(horizontalLayoutPanel);
    // }

    // public void addTransformDialog () {
    //     final GenericDialog gd = new GenericDialog( "Add a new transformation..." );
    //     String[] transformTypes = new String[] {Transformer.TransformType.BigWarp.toString(), Transformer.TransformType.Elastix.toString(),
    //             Transformer.TransformType.Manual.toString() };
    //     gd.addChoice( "Transformation Type", transformTypes, transformTypes[0]);
    //     gd.showDialog();
    //
    //     if ( !gd.wasCanceled() ) {
    //         Transformer.TransformType transformType = Transformer.TransformType.valueOf( gd.getNextChoice() );
    //
    //         switch( transformType ) {
    //             case BigWarp:
    //                 // transformer.getBigWarpManager().openBigwarp();
    //                 addTranformPanel( maxTranformNumber + 1, "bigwarp" );
    //                 updateFrame();
    //                 break;
    //             case Elastix:
    //                 new ElastixUI( transformer );
    //                 break;
    //             case Manual:
    //                 break;
    //         }
    //     }
    // }

    private JComboBox createTranformComboBox( String name ) {
        String[] transformNames = new String[1];
        transformNames[0] = name;
        JComboBox transformComboBox = new JComboBox<>( transformNames );
        transformComboBox.setSelectedItem( transformNames[0] );
        setComboBoxDimensions( transformComboBox );
        return transformComboBox;
        // datasetComboBox.addActionListener( new SyncImageAndDatasetComboBox() );
    }



    public static void main( String[] args )
    {
        new ui();
    }

}
