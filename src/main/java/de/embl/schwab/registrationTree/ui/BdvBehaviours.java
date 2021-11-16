package de.embl.schwab.registrationTree.ui;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.schwab.registrationTree.Transformer;
import de.embl.schwab.registrationTree.registrationNodes.RegistrationNode;
import ij.gui.GenericDialog;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BdvBehaviours {
    private Transformer transformer;
    private BdvHandle bdvHandle;

    public BdvBehaviours ( Transformer transformer ) {
        this.transformer = transformer;
        this.bdvHandle = transformer.getBdv();

        installBehaviours();
    }

    private void chooseSourceDialog() {
        GenericDialog gd = new GenericDialog("Choose a source:");
        List<String> sources = new ArrayList<>();
        sources.add( Transformer.FIXEDSOURCENAME );
        sources.add( Transformer.MOVINGSOURCENAME );

        Map<String, RegistrationNode> displayedNodeNamesToNodes = transformer.getDisplayedNodeNamesToNodes();
        sources.addAll(displayedNodeNamesToNodes.keySet() );

        String[] sourceNames = sources.toArray(new String[0]);
        gd.addChoice("Source:", sourceNames, sourceNames[0]);

        gd.showDialog();

        if ( !gd.wasCanceled() ) {
            String sourceName = gd.getNextChoice();
            if ( sourceName.equals( Transformer.FIXEDSOURCENAME) ) {
                transformer.focus( Transformer.ImageType.FIXED );
            } else if ( sourceName.equals( Transformer.MOVINGSOURCENAME )) {
                transformer.focus( Transformer.ImageType.MOVING );
            } else {
                transformer.focus( displayedNodeNamesToNodes.get( sourceName ));
            }
        }
    }

    private void installBehaviours() {
        final Behaviours behaviours = new Behaviours(new InputTriggerConfig());
        behaviours.install( bdvHandle.getTriggerbindings(), "Registration" );

        BdvPopupMenus.addAction(bdvHandle, "Focus on source", ( x, y ) ->
        {
            chooseSourceDialog();
        });

    }
}
