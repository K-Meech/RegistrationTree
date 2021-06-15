package de.embl.schwab.crosshairSBEM.ui;

import de.embl.schwab.crosshairSBEM.BigWarpManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import static de.embl.schwab.crosshairSBEM.SwingUtils.TEXT_FIELD_HEIGHT;
import static de.embl.schwab.crosshairSBEM.SwingUtils.getButton;

public class BigWarpUI {

    private BigWarpManager bigWarpManager;

    public BigWarpUI( BigWarpManager bigWarpManager ) {

        this.bigWarpManager = bigWarpManager;

        JFrame menu = new JFrame();
        menu.addWindowListener( createWindowListener() );
        menu.setTitle( "Crosshair - Bigwarp menu");
        menu.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

        JPanel panel = new JPanel();
        panel.setBorder( BorderFactory.createEmptyBorder(0, 10, 10, 10) );
        JButton exportCrosshairButton = getButton( "export current transform to Crosshair", new Dimension( 300, TEXT_FIELD_HEIGHT ));
        exportCrosshairButton.setBackground( new Color(240, 128, 128));
        panel.add(exportCrosshairButton);
        menu.getContentPane().add(panel);

        exportCrosshairButton.addActionListener( e ->
        {
            new Thread( () -> {
                bigWarpManager.exportBigWarpToCrosshair();
                menu.dispatchEvent(new WindowEvent(menu, WindowEvent.WINDOW_CLOSING));
            } ).start();
        } );

        menu.pack();
        Point bdvWindowLocation = bigWarpManager.getViewerFrameQLocation();
        int bdvWindowHeight = bigWarpManager.getViewerFrameQHeight();

        menu.setLocation(bdvWindowLocation.x, bdvWindowLocation.y + bdvWindowHeight);
        menu.show();
    }

    public WindowListener createWindowListener() {
        WindowListener windowListener = new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {}

            @Override
            public void windowClosing(WindowEvent e) {}

            @Override
            public void windowClosed(WindowEvent e) {
                bigWarpManager.closeAllBigWarpWindows();
            }

            @Override
            public void windowIconified(WindowEvent e) {}

            @Override
            public void windowDeiconified(WindowEvent e) {}

            @Override
            public void windowActivated(WindowEvent e) {}

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        };
        return windowListener;
    }


}
