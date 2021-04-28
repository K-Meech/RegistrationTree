package de.embl.schwab.crosshairSBEM.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import de.embl.schwab.crosshairSBEM.CrosshairAffineTransform;
import de.embl.schwab.crosshairSBEM.DefaultMutableTreeNodeAdapter;
import de.embl.schwab.crosshairSBEM.Transformer;
import ij.gui.GenericDialog;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.serializers.AffineTransform3DAdapter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

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

        // print transform (for node) or for whole chain

        // export current chain to xml file (bigstitcher style)

        // save tree
        ActionListener saveTreeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread( () -> {
                    Gson gson = new GsonBuilder()
                            .registerTypeAdapter(AffineTransform3D.class, new AffineTransform3DAdapter())
                            .registerTypeAdapter(DefaultMutableTreeNode.class, new DefaultMutableTreeNodeAdapter() )
                            .setPrettyPrinting()
                            .create();
                    Object topNode = tree.tree.getModel().getRoot();

                    try(OutputStream outputStream = new FileOutputStream( "C:\\Users\\meechan\\Documents\\temp\\still_testing\\test.json" );
                        JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8")) ) {
                        writer.setIndent("	");
                        gson.toJson(topNode, DefaultMutableTreeNode.class,  writer);
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }).start();
            }
        };
        addPopupAction("Save registration tree", saveTreeListener);


        // add transform (can then choose bigwarp or manual or elastix)
        ActionListener addListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Note: putting SwingUtilities.invokeLater here also resulted in the crop box hanging, and never
                // becoming visible
                new Thread( () -> {
                        addTransformDialog();
                }).start();
            }
        };
        addPopupAction("Add new transform", addListener);

    }

    public void showPopupMenu(Component component, int x, int y ) {
        popup.show(component, x, y);
    }

    public void addTransformDialog () {
        // TODO - enforce that all transform names are unique
        final GenericDialog gd = new GenericDialog( "Add a new transformation..." );
        String[] transformTypes = new String[] {Transformer.TransformType.BigWarp.toString(), Transformer.TransformType.Elastix.toString(),
                Transformer.TransformType.Manual.toString(), Transformer.TransformType.AffineString.toString() };
        gd.addChoice( "Transformation Type", transformTypes, transformTypes[0]);
        gd.addStringField("Transform name", "");
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            Transformer.TransformType transformType = Transformer.TransformType.valueOf( gd.getNextChoice() );
            String transformName = gd.getNextString();

            switch( transformType ) {
                case BigWarp:
                    transformer.openBigwarp();
                    // TODO - only if transform was done, and button was pushed
                    // TODO - put actual transform here
                    tree.addRegistrationNode(new CrosshairAffineTransform(new AffineTransform3D(), transformName),
                            tree.tree.getSelectionPath());
                    break;
                case Elastix:
                    transformer.openElastix();
                    // TODO - only if transform was done, and button was pushed
                    // TODO - put actual transform here
                    tree.addRegistrationNode(new CrosshairAffineTransform(new AffineTransform3D(), transformName),
                            tree.tree.getSelectionPath());
                    break;
                case Manual:
                    break;
                case AffineString:
                    break;
            }
        }
    }

}
