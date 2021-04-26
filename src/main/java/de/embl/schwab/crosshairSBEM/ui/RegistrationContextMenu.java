package de.embl.schwab.crosshairSBEM.ui;

import de.embl.schwab.crosshairSBEM.CrosshairAffineTransform;
import de.embl.schwab.crosshairSBEM.Transformer;
import ij.gui.GenericDialog;
import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegistrationContextMenu {

    // based on https://github.com/bigdataviewer/bigdataviewer-playground/blob/e6b93d7d2ac4cb490a9c2a19b813fbe96e640ea5/src/main/java/sc/fiji/bdvpg/scijava/services/ui/SourceAndConverterPopupMenu.java#L49

    JPopupMenu popup;
    RegistrationTree tree;
    Transformer transformer;

    public RegistrationContextMenu( RegistrationTree tree, Transformer transformer ) {
        this.transformer = transformer;
        popup = new JPopupMenu();
        this.tree = tree;
        populateActions();
    }

    public void addPopupLine() {
        popup.addSeparator();
    }

    public void addPopupAction(String actionName, ActionListener actionListener) {

        JMenuItem menuItem = new JMenuItem(actionName);
        menuItem.addActionListener( actionListener );
        popup.add(menuItem);
    }

    private void populateActions() {

        // show source with transform in BDV

        // remove source with transform from BDV

        // delete source with transform (and all that are lower in tree)

        // add transform (can then choose bigwarp or manual or elastix)
        ActionListener addListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater( () ->
                {
                    addTransformDialog();
                } );
            }
        };
        addPopupAction("Add new transform", addListener);

    }

    public void showPopupMenu(Component component, int x, int y ) {
        popup.show(component, x, y);
    }

    public void addTransformDialog () {
        final GenericDialog gd = new GenericDialog( "Add a new transformation..." );
        String[] transformTypes = new String[] {Transformer.TransformType.BigWarp.toString(), Transformer.TransformType.Elastix.toString(),
                Transformer.TransformType.Manual.toString(), Transformer.TransformType.AffineString.toString() };
        gd.addChoice( "Transformation Type", transformTypes, transformTypes[0]);
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            Transformer.TransformType transformType = Transformer.TransformType.valueOf( gd.getNextChoice() );

            switch( transformType ) {
                case BigWarp:
                    transformer.openBigwarp();
                    // TODO - only if transform was done, and button was pushed
                    tree.addRegistrationNode(new CrosshairAffineTransform(new AffineTransform3D(), "test1"),
                            tree.tree.getSelectionPath());
                    break;
                case Elastix:
                    new ElastixUI( transformer );
                    break;
                case Manual:
                    break;
                case AffineString:
                    break;
            }
        }
    }

}
