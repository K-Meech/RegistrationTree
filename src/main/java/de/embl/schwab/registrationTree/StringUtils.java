package de.embl.schwab.registrationTree;

import ij.IJ;

public class StringUtils {
    public static String tidyString( String string ) {
        string = string.trim();
        String tidyString = string.replaceAll("\\s+","_");

        if ( !string.equals(tidyString) ) {
            IJ.log( "Spaces were removed from name, and replaced by _");
        }

        // check only contains alphanumerics, or _ -
        if ( !tidyString.matches("^[a-zA-Z0-9_-]+$") ) {
            IJ.log( "Names must only contain letters, numbers, _ or -. Please try again " +
                    "with a different name.");
            tidyString = null;
        }

        return tidyString;
    }
}
