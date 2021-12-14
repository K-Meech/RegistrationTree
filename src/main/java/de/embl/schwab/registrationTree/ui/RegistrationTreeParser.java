package de.embl.schwab.registrationTree.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.embl.schwab.registrationTree.Transformer;
import de.embl.schwab.registrationTree.serialise.DefaultMutableTreeNodeAdapter;
import de.embl.schwab.registrationTree.serialise.RealIntervalAdapter;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.AffineTransform3DAdapter;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.*;

public class RegistrationTreeParser {

    // providing a transformer, will mean that the cropper will be auto-filled with any crops from the json
    public DefaultMutableTreeNode parseTreeJson( String jsonPath, Transformer transformer ) throws IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(AffineTransform3D.class, new AffineTransform3DAdapter())
                .registerTypeAdapter(RealInterval.class, new RealIntervalAdapter())
                .registerTypeAdapter(DefaultMutableTreeNode.class, new DefaultMutableTreeNodeAdapter( transformer.getCropper() ) )
                .setPrettyPrinting()
                .create();

        // remove any old crops, and replace with those read from the file
        if ( transformer.getCropper() != null ) {
            transformer.getCropper().removeAllCrops();
        }

        return parseTreeJson( gson, jsonPath );
    }

    public DefaultMutableTreeNode parseTreeJson( String jsonPath ) throws IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(AffineTransform3D.class, new AffineTransform3DAdapter())
                .registerTypeAdapter(RealInterval.class, new RealIntervalAdapter())
                .registerTypeAdapter(DefaultMutableTreeNode.class, new DefaultMutableTreeNodeAdapter() )
                .setPrettyPrinting()
                .create();

        return parseTreeJson( gson, jsonPath );
    }

    private DefaultMutableTreeNode parseTreeJson( Gson gson, String jsonPath ) throws IOException {
        try(InputStream inputStream = new FileInputStream( jsonPath );
            JsonReader reader = new JsonReader( new InputStreamReader(inputStream, "UTF-8")) ) {
            DefaultMutableTreeNode treeNode = gson.fromJson(reader, DefaultMutableTreeNode.class);
            return treeNode;
        }
    }

    public void saveTreeJson( RegistrationTree registrationTree, String jsonPath ) {
        // TODO - somehow record which fixed and moving image were used too
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(AffineTransform3D.class, new AffineTransform3DAdapter())
                .registerTypeAdapter(DefaultMutableTreeNode.class, new DefaultMutableTreeNodeAdapter() )
                .setPrettyPrinting()
                .create();
        Object topNode = registrationTree.tree.getModel().getRoot();

        try(OutputStream outputStream = new FileOutputStream( jsonPath );
            JsonWriter writer = new JsonWriter( new OutputStreamWriter(outputStream, "UTF-8")) ) {
            writer.setIndent("	");
            gson.toJson(topNode, DefaultMutableTreeNode.class,  writer);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

}
