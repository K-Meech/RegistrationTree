package de.embl.schwab.crosshairSBEM.ui;

import de.embl.schwab.crosshairSBEM.CrosshairAffineTransform;
import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegistrationContextMenu {

    // based on https://github.com/bigdataviewer/bigdataviewer-playground/blob/e6b93d7d2ac4cb490a9c2a19b813fbe96e640ea5/src/main/java/sc/fiji/bdvpg/scijava/services/ui/SourceAndConverterPopupMenu.java#L49

    JPopupMenu popup;
    RegistrationTree tree;

    public RegistrationContextMenu( RegistrationTree tree ) {
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

        // show source with transform

        // remove source with transform

        // add transform (can then choose bigwarp or manual or elastix)
        ActionListener addListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater( () ->
                {
                    tree.addRegistrationNode(new CrosshairAffineTransform(new AffineTransform3D(), "test1"),
                            tree.tree.getSelectionPath());
                } );
            }
        };
        addPopupAction("Add new transform", addListener);

    }

    public void showPopupMenu(Component component, int x, int y ) {
        popup.show(component, x, y);
    }

}
