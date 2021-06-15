package de.embl.schwab.crosshairSBEM.mhd;

import java.io.*;

public class MhdHeaderParser {

    private String mhdFilePath;

    public MhdHeaderParser( String mhdFilePath ) {
        this.mhdFilePath = mhdFilePath;
    }

    public MhdHeader parseHeader() {
        MhdHeader mhdHeader = new MhdHeader();
        try( BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(mhdFilePath))); ) {

            String line;
            while ( (line = in.readLine()) != null ) {
                String[] splitLine = line.split(" ");
                String elementName = splitLine[0];
                String lastElement = splitLine[splitLine.length - 1];

                if ( elementName.equals("ObjectType") ){
                    mhdHeader.objectType = lastElement;
                } else if ( elementName.equals("NDims") ) {
                    mhdHeader.nDims = Integer.parseInt( lastElement );
                } else if ( elementName.equals("BinaryData") ) {
                    mhdHeader.binaryData = Boolean.parseBoolean( lastElement );
                } else if ( elementName.equals( "BinaryDataByteOrderMSB") ) {
                    mhdHeader.binaryDataByteOrderMSB = Boolean.parseBoolean( lastElement );
                } else if ( elementName.equals("DimSize") ) {
                    int[] dimSize = new int[splitLine.length - 2 ];
                    for ( int i=2; i< splitLine.length; i++ ) {
                        dimSize[i - 2] = Integer.parseInt( splitLine[i] );
                    }
                    mhdHeader.dimSize = dimSize;
                } else if ( elementName.equals("ElementSize") ) {
                    double[] elementSize = new double[splitLine.length - 2 ];
                    for ( int i=2; i< splitLine.length; i++ ) {
                        elementSize[i - 2] = Double.parseDouble( splitLine[i] );
                    }
                    mhdHeader.elementSize = elementSize;
                } else if ( elementName.equals("ElementType") ) {
                    mhdHeader.elementType = lastElement;
                } else if ( elementName.equals("ElementDataFile") ) {
                    mhdHeader.elementDataFile = lastElement;
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return mhdHeader;
    }
}
