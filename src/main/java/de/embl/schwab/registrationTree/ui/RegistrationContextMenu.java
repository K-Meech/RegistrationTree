package de.embl.schwab.registrationTree.ui;

import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.schwab.registrationTree.Transformer;
import ij.gui.GenericDialog;
import mpicbg.spim.data.SpimDataException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import static de.embl.schwab.registrationTree.StringUtils.tidyString;

public class RegistrationContextMenu {

    // based on https://github.com/bigdataviewer/bigdataviewer-playground/blob/e6b93d7d2ac4cb490a9c2a19b813fbe96e640ea5/src/main/java/sc/fiji/bdvpg/scijava/services/ui/SourceAndConverterPopupMenu.java#L49

    JPopupMenu popup;
    RegistrationTree tree;
    Transformer transformer;

    public RegistrationContextMenu( RegistrationTree tree, Transformer transformer ) {
        this.transformer = transformer;
        this.tree = tree;
    }

    public void addPopupLine() {
        popup.addSeparator();
    }

    public void addPopupAction(String actionName, ActionListener actionListener) {

        JMenuItem menuItem = new JMenuItem(actionName);
        menuItem.addActionListener( actionListener );
        popup.add(menuItem);
    }

    private void populateActions( boolean isRoot ) {

        if ( !isRoot ) {
            // show source with transform in BDV
            ActionListener showListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread(() -> {
                        transformer.showSource(tree.getSelectedNode());
                    }).start();
                }
            };
            addPopupAction("Show in Bdv", showListener);

            // remove source with transform from BDV
            ActionListener hideListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread(() -> {
                        transformer.removeSource(tree.getSelectedNode());
                    }).start();
                }
            };
            addPopupAction("Hide from Bdv", hideListener);

            // delete source with transform (and all that are lower in tree)
            ActionListener deleteListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread(() -> {
                        tree.removeSelectedRegistrationNode();
                    }).start();
                }
            };
            addPopupAction("Delete transform node", deleteListener);

            // If there is no spimdata (i.e. it was loaded directly from Source, and not from an xml), then we can't
            // export to an xml
            if ( transformer.getSpimData( Transformer.ImageType.FIXED ) != null ) {
                // export in a certain space
                ActionListener exportListener = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        new Thread(() -> {
                            exportDialog();
                        }).start();
                    }
                };
                addPopupAction("Export to bdv xml", exportListener);
            }

            // print transform of selected node
            ActionListener printNodeTransformListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread(() -> {
                        tree.printTransformOfSelectedRegistrationNode();
                    }).start();
                }
            };
            addPopupAction("Print transform of this node", printNodeTransformListener);

            // print full transform of selected node
            ActionListener printFullNodeTransformListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread(() -> {
                        tree.printFullTransformOfSelectedRegistrationNode();
                    }).start();
                }
            };
            addPopupAction("Print full transform of path to this node", printFullNodeTransformListener);
        }

        // TODO - print transform (for node) or for whole chain

        // load tree from json
        ActionListener loadTreeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread( () -> {
                    String jsonPath = chooseJson();
                    if ( jsonPath != null ) {
                        loadCurrentStateFromJson( jsonPath );
                    }
                }).start();
            }
        };
        addPopupAction("Load registration tree", loadTreeListener);

        // save tree
        ActionListener saveTreeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread( () -> {
                    String jsonPath = chooseSaveLocationDialog();
                    if ( jsonPath != null ) {
                        new RegistrationTreeParser().saveTreeJson( tree, jsonPath );
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
        addPopupAction("Add new transform node", addListener);

    }

    public void showPopupMenu(Component component, int x, int y, boolean isRoot ) {
        popup = new JPopupMenu();
        populateActions( isRoot );
        popup.show(component, x, y);
    }

    public void addTransformDialog () {
        final GenericDialog gd = new GenericDialog( "Add a new transformation..." );
        // TODO - add manual or affine string options
        String[] transformTypes = new String[] {Transformer.TransformType.BigWarp.toString(), Transformer.TransformType.Elastix.toString()};
        gd.addChoice( "Transformation Type", transformTypes, transformTypes[0]);
        gd.addStringField("Transform name", "");
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            Transformer.TransformType transformType = Transformer.TransformType.valueOf( gd.getNextChoice() );
            String transformName = tidyString( gd.getNextString() );

            if ( transformName != null ) {
                switch (transformType) {
                    case BigWarp:
                        tree.updateLastSelectedNode();
                        transformer.getBigWarpManager().openBigwarpAtSelectedNode(transformName);
                        break;
                    case Elastix:
                        tree.updateLastSelectedNode();
                        transformer.getElastixManager().openElastix(transformName);
                        break;
                    case Manual:
                        break;
                    case AffineString:
                        break;
                }
            }
        }
    }

    public void exportDialog () {
        final GenericDialog gd = new GenericDialog( "Export transform to bdv xml..." );
        gd.addMessage("FIXED space, will export a bdv xml for the moving image that matches it to the fixed image. \n" +
                "MOVING space, will export a bdv xml for the fixed image that matches it to the moving image.");
        String[] viewSpaces = new String[] {Transformer.ViewSpace.FIXED.toString(),
                Transformer.ViewSpace.MOVING.toString() };
        gd.addChoice( "Export in space:", viewSpaces, viewSpaces[0]);
        gd.addFileField("Bdv xml location",
                FileAndUrlUtils.combinePath( System.getProperty("user.home"), "export.xml") );
        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            Transformer.ViewSpace viewSpace = Transformer.ViewSpace.valueOf( gd.getNextChoice() );
            String xmlPath = gd.getNextString();

            if (!xmlPath.endsWith(".xml")) {
                xmlPath += ".xml";
            }

            try {
                transformer.writeBdvXml(viewSpace, tree.getSelectedNode(), xmlPath);
            } catch (SpimDataException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadCurrentStateFromJson( String jsonPath ) {
        try {
            transformer.removeAllCurrentSources();
            DefaultMutableTreeNode newTopNode = new RegistrationTreeParser().parseTreeJson( jsonPath, transformer );
            DefaultTreeModel treeModel = (DefaultTreeModel) tree.tree.getModel();
            treeModel.setRoot( newTopNode );
            treeModel.reload();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String chooseSaveLocationDialog() {
        String jsonPath = null;
        final JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setDialogTitle("Choose json location:");
        jFileChooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
        if (jFileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            jsonPath = jFileChooser.getSelectedFile().getAbsolutePath();

            if (!jsonPath.endsWith(".json")) {
                jsonPath += ".json";
            }
        }

        return jsonPath;
    }

    private String chooseJson() {
        String jsonPath = null;
        final JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setDialogTitle("Choose json:");
        jFileChooser.setFileFilter(new FileNameExtensionFilter("json", "json"));
        if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            jsonPath = jFileChooser.getSelectedFile().getAbsolutePath();

            if (!jsonPath.endsWith(".json")) {
                return null;
            }
        }

        return jsonPath;
    }

}
