package de.embl.schwab.crosshairSBEM;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ui extends JFrame {

    public static final int TEXT_FIELD_HEIGHT = 20;
    public static final int COMBOBOX_WIDTH = 270;
    public static final Dimension BUTTON_DIMENSION = new Dimension( 80, TEXT_FIELD_HEIGHT );

    public ui () {
        this.getContentPane().setLayout( new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS ) );
        this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        addUneditableTransformPanel( 1, "physical units transform (from bdv xml)" );
        addTranformPanel( 2 );

        this.pack();
        this.show();
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

        this.getContentPane().add(horizontalLayoutPanel);
    }

    private void addTranformPanel( int transformNumber ) {
        final JPanel horizontalLayoutPanel = horizontalLayoutPanel();

        final JButton visibilityButton = getButton("V");
        final JButton addTransformButton = getButton("+");

        JComboBox transformCombo = createTranformComboBox( "test " );
        // addButton.addActionListener( e ->
        // {
        //     new Thread( () -> { addDatasetDialog(); } ).start();
        // } );
        //
        // editButton.addActionListener( e ->
        // {
        //     new Thread( () -> { editDatasetDialog(); } ).start();
        // } );

        horizontalLayoutPanel.add(getJLabel(Integer.toString( transformNumber ), 60, 10));
        horizontalLayoutPanel.add( transformCombo );
        horizontalLayoutPanel.add( visibilityButton );
        horizontalLayoutPanel.add( addTransformButton );
        horizontalLayoutPanel.setAlignmentX( Component.LEFT_ALIGNMENT );

        this.getContentPane().add(horizontalLayoutPanel);
    }

    private JComboBox createTranformComboBox( String name ) {
        String[] transformNames = new String[1];
        transformNames[0] = name;
        JComboBox transformComboBox = new JComboBox<>( transformNames );
        transformComboBox.setSelectedItem( transformNames[0] );
        setComboBoxDimensions( transformComboBox );
        return transformComboBox;
        // datasetComboBox.addActionListener( new SyncImageAndDatasetComboBox() );
    }

    public static void setComboBoxDimensions( JComboBox< String > comboBox )
    {
        comboBox.setPreferredSize( new Dimension( 350, 20 ) );
        comboBox.setMaximumSize( new Dimension( 350, 20 ) );
    }

    public static JPanel horizontalLayoutPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout( new BoxLayout( panel, BoxLayout.LINE_AXIS ) );
        panel.setBorder( BorderFactory.createEmptyBorder(0, 10, 10, 10) );
        panel.add( Box.createHorizontalGlue() );
        panel.setAlignmentX( Component.LEFT_ALIGNMENT );

        return panel;
    }

    public static JLabel getJLabel( String text )
    {
        return getJLabel( text, 170, 10);
    }

    public static JLabel getJLabel( String text, int width, int height )
    {
        final JLabel comp = new JLabel( text );
        comp.setPreferredSize( new Dimension( width,height ) );
        comp.setHorizontalAlignment( SwingConstants.LEFT );
        comp.setHorizontalTextPosition( SwingConstants.LEFT );
        comp.setAlignmentX( Component.LEFT_ALIGNMENT );
        return comp;
    }

    public static JButton getButton( String buttonLabel )
    {
        return getButton( buttonLabel, BUTTON_DIMENSION );
    }

    public static JButton getButton( String buttonLabel, Dimension dimension )
    {
        final JButton button = new JButton( buttonLabel );
        button.setPreferredSize( dimension );
        return button;
    }

    public static void main( String[] args )
    {
        new ui();
    }

}
