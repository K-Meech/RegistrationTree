package de.embl.schwab.registrationTree.ui;

import de.embl.schwab.registrationTree.registrationNodes.RegistrationNode;
import de.embl.schwab.registrationTree.Transformer;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Enumeration;

public class RegistrationTree {
    // based on bdv playground's tree
    // https://github.com/bigdataviewer/bigdataviewer-playground/blob/e6b93d7d2ac4cb490a9c2a19b813fbe96e640ea5/src/main/java/sc/fiji/bdvpg/scijava/widget/SwingSourceAndConverterListWidget.java
    // https://github.com/bigdataviewer/bigdataviewer-playground/blob/e6b93d7d2ac4cb490a9c2a19b813fbe96e640ea5/src/main/java/sc/fiji/bdvpg/scijava/services/ui/SourceAndConverterServiceUI.java#L149
    // https://docs.oracle.com/javase/tutorial/uiswing/components/tree.html

    JPanel panel;
    JScrollPane treeView;
    JTree tree;
    DefaultTreeModel model;
    Transformer transformer;
    RegistrationContextMenu popup;

    TreePath lastSelectedNode;
    TreePath lastAddedNode;
    TreePath rootNode;

    public RegistrationTree( Transformer transformer ) {
        this.transformer = transformer;

        // Tree view of Spimdata
        // This is just left at the identity transform. The transform in the xml is already present in the loaded data.
        // All remaining transforms are put on top as the fixed transform in a Transformed Source
        DefaultMutableTreeNode top =
                new DefaultMutableTreeNode(new RegistrationNode(new AffineTransform3D(), new AffineTransform3D(), "XmlTransform"));
        model = new DefaultTreeModel(top);

        tree = new JTree(model);

        rootNode = new TreePath( top.getPath() );

        popup =  new RegistrationContextMenu(this, transformer );

        // Shows Popup on right click
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                // Right Click -> popup
                if (SwingUtilities.isRightMouseButton(e)) {
                    int selRow = tree.getRowForLocation(e.getX(), e.getY());
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    if ( selPath != null && selRow > -1 ) {
                        tree.setSelectionPath(selPath);
                        tree.setSelectionRow(selRow);
                        popup.showPopupMenu(e.getComponent(), e.getX(), e.getY(), isRoot(selPath));
                    }
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


    public void addRegistrationNodeAtLastSelection( RegistrationNode regNode ) {
        addRegistrationNode( regNode, lastSelectedNode );
    }

    public void addRegistrationNode( RegistrationNode regNode, TreePath parentPath ) {

        AffineTransform3D fullTransform = getFullTransform( parentPath, regNode.getAffine() );
        regNode.setFullTransform( fullTransform );

        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode( regNode );

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

    public void addRegistrationNodeAtRoot( RegistrationNode regNode ) {
        addRegistrationNode( regNode, rootNode );
    }

    private boolean isRoot( TreePath path ) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        return node.isRoot();
    }

    public RegistrationNode getSelectedNode() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
        return (RegistrationNode) node.getUserObject();
    }

    public RegistrationNode getLastAddedNode() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastAddedNode.getLastPathComponent();
        return (RegistrationNode) node.getUserObject();
    }

    public RegistrationNode getLastSelectedNode() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastSelectedNode.getLastPathComponent();
        return (RegistrationNode) node.getUserObject();
    }

    public AffineTransform3D getFullTransform( TreePath path ) {
        AffineTransform3D fullTransform = new AffineTransform3D();
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        fullTransform.set( ((RegistrationNode) currentNode.getUserObject()).getFullTransform() );
        return fullTransform;
    }

    public AffineTransform3D getFullTransform( TreePath parentPath, AffineTransform3D childTransform ) {
        AffineTransform3D fullTransform = getFullTransform( parentPath );
        fullTransform.preConcatenate( childTransform );

        return fullTransform;
    }

    public RegistrationContextMenu getPopup() {
        return popup;
    }

    public String getTransformNameFromTreePath(TreePath path ) {
        DefaultMutableTreeNode lastNodeInPath = (DefaultMutableTreeNode) path.getLastPathComponent();
        String name = ((RegistrationNode) lastNodeInPath.getUserObject()).getName();
        return name;
    }

    private void removeSource( DefaultMutableTreeNode node ) {
        RegistrationNode regNode = ((RegistrationNode) node.getUserObject());
        transformer.removeSource( regNode );
    }

    private void removeNodeAndChildrenFromBdv( DefaultMutableTreeNode node ) {
        removeSource( node );
        Enumeration children = node.children();
            while( children.hasMoreElements() ) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                removeNodeAndChildrenFromBdv( child );
            }
    }

    public void removeSelectedRegistrationNode() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
        if ( !node.isRoot() ) {
            if (node.getChildCount() > 0) {
                if (continueDialog()) {
                    model.removeNodeFromParent(node);
                    removeNodeAndChildrenFromBdv(node);
                }
            } else {
                model.removeNodeFromParent(node);
                removeNodeAndChildrenFromBdv(node);
            }
        }
    }

    public void printTransformOfSelectedRegistrationNode() {
        RegistrationNode node = getSelectedNode();
        IJ.log("Affine transform (fixed to moving) for node " + node.getName() + ":");
        IJ.log( Arrays.toString( node.getAffine().getRowPackedCopy() ));
    }

    public void printFullTransformOfSelectedRegistrationNode() {
        RegistrationNode node = getSelectedNode();
        IJ.log("Full affine transform (fixed to moving) for path to node " + node.getName() + ":");
        IJ.log( Arrays.toString( node.getFullTransform().getRowPackedCopy() ));
    }

    private boolean continueDialog() {
        int result = JOptionPane.showConfirmDialog(null,
                "All children of this node will also be deleted. Continue?", "Delete node and all children?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            return true;
        } else {
            return false;
        }
    }
}
