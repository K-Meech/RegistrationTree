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

    JFrame frame;
    JPanel panel;
    JScrollPane treeView;
    JTree tree;
    DefaultTreeModel model;
    Transformer transformer;

    public RegistrationTree( Transformer transformer ) {

        this.transformer = transformer;

        frame = new JFrame("Registrations");
        panel = new JPanel(new BorderLayout());

        // Tree view of Spimdata
        DefaultMutableTreeNode top =
                new DefaultMutableTreeNode("Registrations:");
        model = new DefaultTreeModel(top);
        // top.model = model;

        DefaultMutableTreeNode category = new DefaultMutableTreeNode(new CrosshairAffineTransform(new AffineTransform3D(), "test1"));
        DefaultMutableTreeNode subcat = new DefaultMutableTreeNode("yo");
        DefaultMutableTreeNode category2 = new DefaultMutableTreeNode("test2");
        top.add(category);
        category.add(subcat);
        top.add(category2);

        // TODO - change icons so not different between internal and leaves - want all the intermediates to be selectable
        // and able to be shown

        tree = new JTree(model);
        // tree.setCellRenderer(new SourceAndConverterTreeCellRenderer());

        treeView = new JScrollPane(tree);

        panel.add(treeView, BorderLayout.CENTER);

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

                    IJ.log("right clik!");
                    popup.showPopupMenu(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // get the screen size as a java dimension
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // get a fixed proportion of the height and of the width
        int height = screenSize.height * 4 / 5;
        int width = screenSize.width / 6;

        // set the jframe height and width
        frame.setPreferredSize(new Dimension(width, height));

        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
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

        tree.scrollPathToVisible(new TreePath(childNode.getPath()));
    }

    public void removeRegistrationNode() {

    }
}
