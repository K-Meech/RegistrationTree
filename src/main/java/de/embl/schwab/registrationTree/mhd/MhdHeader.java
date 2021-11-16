package de.embl.schwab.registrationTree.mhd;

public class MhdHeader {
    public String objectType;
    public int nDims;
    public boolean binaryData;
    public boolean binaryDataByteOrderMSB;
    public int[] dimSize;
    public double[] elementSize;
    public String elementType;
    public String elementDataFile;
}
