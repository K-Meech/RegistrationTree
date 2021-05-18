package de.embl.schwab.crosshairSBEM.ui;

import de.embl.schwab.crosshairSBEM.CrosshairAffineTransform;
import de.embl.schwab.crosshairSBEM.Transformer;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RegistrationTree {
    // based on bdv playground's tree
    // https://github.com/bigdataviewer/bigdataviewer-playground/blob/e6b93d7d2ac4cb490a9c2a19b813fbe96e640ea5/src/main/java/sc/fiji/bdvpg/scijava/widget/SwingSourceAndConverterListWidget.java
    // https://github.com/bigdataviewer/bigdataviewer-playground/blob/e6b93d7d2ac4cb490a9c2a19b813fbe96e640ea5/src/main/java/sc/fiji/bdvpg/scijava/services/ui/SourceAndConverterServiceUI.java#L149
    // https://docs.oracle.com/javase/tutorial/uiswing/components/tree.html

    // TODO - make so can load a transformation tree from file
    JFrame frame;
    JPanel panel;
    JScrollPane treeView;
    JTree tree;
    DefaultTreeModel model;
    Transformer transformer;

    TreePath lastSelectedNode;
    TreePath lastAddedNode;

    public RegistrationTree( Transformer transformer ) {
        this.transformer = transformer;

        // Tree view of Spimdata
        // This is just left at the identity transform. The transform in the xml is already present in the loaded data.
        // All remaining transforms are put on top as the fixed transform in a Transformed Source
        DefaultMutableTreeNode top =
                new DefaultMutableTreeNode(new CrosshairAffineTransform(new AffineTransform3D(), "XmlTransform"));
        model = new DefaultTreeModel(top);

        tree = new JTree(model);

        RegistrationContextMenu popup =  new RegistrationContextMenu(this, transformer );

        // Shows Popup on right click
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                // Right Click -> popup
                if (SwingUtilities.isRightMouseButton(e)) {
                    int selRow = tree.getRowForLocation(e.getX(), e.getY());
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    tree.setSelectionPath(selPath);
                    if (selRow>-1){
                        tree.setSelectionRow(selRow);
                    }

                    popup.showPopupMenu(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

    }

    public JPanel createTreePanel() {
        panel = new JPanel(new BorderLayout());
        treeView = new JScrollPane(tree);
        panel.add(treeView, BorderLayout.CENTER);
        return panel;
    }

    public void updateLastSelectedNode() {
        lastSelectedNode = tree.getSelectionPath();
    }


    public void addRegistrationNodeAtLastSelection( CrosshairAffineTransform affine ) {
        addRegistrationNode( affine, lastSelectedNode );
    }

    public void addRegistrationNode( CrosshairAffineTransform affine, TreePath parentPath ) {
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode( affine );

        DefaultMutableTreeNode parentNode= null;
        if (parentPath != null) {
            parentNode = (DefaultMutableTreeNode)
                    (parentPath.getLastPathComponent());
        }


        model.insertNodeInto(childNode, parentNode,
                parentNode.getChildCount());

        lastAddedNode = new TreePath(childNode.getPath());
        tree.scrollPathToVisible(lastAddedNode);
    }

    public CrosshairAffineTransform getFullTransformOfSelectedNode() {
        TreePath path = tree.getSelectionPath();
        return new CrosshairAffineTransform( getFullTransformOfTreePath( path ), getTransformNameFromTreePath( path ));
    }

    public CrosshairAffineTransform getFullTransformOfLastAddedNode() {
        return new CrosshairAffineTransform( getFullTransformOfTreePath( lastAddedNode ), getTransformNameFromTreePath( lastAddedNode ) );
    }

    public AffineTransform3D getFullTransformOfLastSelectedNode() { return getFullTransformOfTreePath( lastSelectedNode ); }

    public AffineTransform3D getFullTransformOfTreePath( TreePath path ) {
        // concatenate transforms up tree dependent on fixed or moving view
        Object[] pathNodes = path.getPath();
        AffineTransform3D fullTransform = new AffineTransform3D();

        // skip root node
        for (int i = 1; i< pathNodes.length; i++) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) pathNodes[i];
            AffineTransform3D nodeTransform = ((CrosshairAffineTransform) currentNode.getUserObject()).getAffine();
            fullTransform.preConcatenate(nodeTransform);
        }

        return fullTransform;
    }

    public String getTransformNameFromTreePath( TreePath path ) {
        DefaultMutableTreeNode lastNodeInPath = (DefaultMutableTreeNode) path.getLastPathComponent();
        String name = ((CrosshairAffineTransform) lastNodeInPath.getUserObject()).getName();
        return name;
    }

    // TODO
    public void removeRegistrationNode() {

    }
}
