package de.embl.schwab.crosshairSBEM.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.schwab.crosshairSBEM.DefaultMutableTreeNodeAdapter;
import de.embl.schwab.crosshairSBEM.Transformer;
import ij.gui.GenericDialog;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.serializers.AffineTransform3DAdapter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import static de.embl.schwab.crosshairSBEM.StringUtils.tidyString;

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
            addPopupAction("Delete registration node", deleteListener);

            // export in a certain space
            ActionListener exportListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread( () -> {
                        exportDialog();
                    }).start();
                }
            };
            addPopupAction("Export to bdv xml", exportListener);
        }

        // print transform (for node) or for whole chain

        // export current chain to xml file (bigstitcher style)

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
                        saveCurrentStateToJson( jsonPath );
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
        // TODO - enforce that all transform names are unique
        final GenericDialog gd = new GenericDialog( "Add a new transformation..." );
        String[] transformTypes = new String[] {Transformer.TransformType.BigWarp.toString(), Transformer.TransformType.Elastix.toString(),
                Transformer.TransformType.Manual.toString(), Transformer.TransformType.AffineString.toString() };
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

    private void loadCurrentStateFromJson( String jsonPath ) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(AffineTransform3D.class, new AffineTransform3DAdapter())
                .registerTypeAdapter(DefaultMutableTreeNode.class, new DefaultMutableTreeNodeAdapter( transformer.getCropper() ) )
                .setPrettyPrinting()
                .create();

        try(InputStream inputStream = new FileInputStream( jsonPath );
            JsonReader reader = new JsonReader( new InputStreamReader(inputStream, "UTF-8")) ) {

            transformer.removeAllCurrentSources();
            DefaultMutableTreeNode newTopNode = gson.fromJson(reader, DefaultMutableTreeNode.class);

            DefaultTreeModel treeModel = (DefaultTreeModel) tree.tree.getModel();
            treeModel.setRoot( newTopNode );
            treeModel.reload();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void saveCurrentStateToJson( String jsonPath ) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(AffineTransform3D.class, new AffineTransform3DAdapter())
                .registerTypeAdapter(DefaultMutableTreeNode.class, new DefaultMutableTreeNodeAdapter() )
                .setPrettyPrinting()
                .create();
        Object topNode = tree.tree.getModel().getRoot();

        try(OutputStream outputStream = new FileOutputStream( jsonPath );
            JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8")) ) {
            writer.setIndent("	");
            gson.toJson(topNode, DefaultMutableTreeNode.class,  writer);
        } catch (IOException exception) {
            exception.printStackTrace();
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
