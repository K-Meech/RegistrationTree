package de.embl.schwab.crosshairSBEM.ui;

import de.embl.schwab.crosshairSBEM.Transformer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Ui {

    private JFrame frame;
    private JPanel contentPanel;
    private JPanel treePanel;
    private JPanel viewPanel;
    private RegistrationTree tree;
    private Transformer transformer;

    public Ui( Transformer transformer ) {
        // TODO make loading and saving tree buttons in panel rather than in menu, right clickmenu should be things
        // that are node specific
        this.transformer = transformer;
        tree = new RegistrationTree( transformer );

        frame = new JFrame("Registrations");
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.PAGE_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        contentPanel.setOpaque(true);
        frame.setContentPane(contentPanel);
        treePanel = tree.createTreePanel();
        viewPanel = createViewPanel();

        // get the screen size as a java dimension
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // get a fixed proportion of the height and of the width
        int height = screenSize.height * 4 / 5;
        int width = screenSize.width / 6;

        // set the jframe height and width
        frame.setPreferredSize(new Dimension(width, height));

        contentPanel.add(treePanel);
        contentPanel.add(viewPanel);
        frame.pack();
        frame.setVisible(true);
    }

    public Transformer getTransformer() {
        return transformer;
    }

    public RegistrationTree getTree() {
        return tree;
    }

    private JPanel createViewPanel() {
        JPanel viewPanel = new JPanel();
        viewPanel.setLayout(new GridLayout(1, 2));

        ActionListener viewListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO - fill
                if (e.getActionCommand().equals("fixed")) {

                } else if (e.getActionCommand().equals("moving")) {

                }
            }
        };

        JButton viewFixed = new JButton("View Fixed Space");
        viewFixed.setActionCommand("fixed");
        viewFixed.addActionListener(viewListener);
        viewPanel.add(viewFixed);

        JButton viewMoving = new JButton("View Moving Space");
        viewMoving.setActionCommand("moving");
        viewMoving.addActionListener(viewListener);
        viewPanel.add(viewMoving);

        return viewPanel;

    }

    public Point getLocationOnScreen() {
        return frame.getLocationOnScreen();
    }

    public int getWidth() {
        return frame.getWidth();
    }

}
