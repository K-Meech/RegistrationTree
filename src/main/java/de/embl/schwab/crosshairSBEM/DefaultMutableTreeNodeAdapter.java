package de.embl.schwab.crosshairSBEM;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

// based on https://stackoverflow.com/questions/53997112/how-to-serialize-defaultmutabletreenode-java-to-json
public class DefaultMutableTreeNodeAdapter implements JsonSerializer<DefaultMutableTreeNode>, JsonDeserializer<DefaultMutableTreeNode> {

    public DefaultMutableTreeNodeAdapter() {
    }

    @Override
    public DefaultMutableTreeNode deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        // double[] rowPackedCopy = (double[])jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("affinetransform3d"), double[].class);
        // AffineTransform3D at3d = new AffineTransform3D();
        // at3d.set(rowPackedCopy);
        // return at3d;
        //
        // DefaultMutableTreeNode node = new DefaultMutableTreeNode();
        // while (in.hasNext()) {
        //     switch (in.nextName()) {
        //         case "allowsChildren":
        //             node.setAllowsChildren(in.nextBoolean());
        //             break;
        //         case "userObject":
        //             node.setUserObject(gson.fromJson(in, AffineTransform3D.class));
        //             break;
        //         case "children":
        //             in.beginArray();
        //             while (in.hasNext()) {
        //                 node.add(read(in)); // recursion!
        //                 // this did also set the parent of the child-node
        //             }
        //             in.endArray();
        //             break;
        //         default:
        //             in.skipValue();
        //             break;
        //     }
        // }
        //
        // return node;
        return null;
    }

    @Override
    public JsonElement serialize(DefaultMutableTreeNode node, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.add("allowsChildren", jsonSerializationContext.serialize(node.getAllowsChildren()));
        obj.add("userObject", jsonSerializationContext.serialize(node.getUserObject()));

        if (node.getChildCount() > 0) {
            obj.add("children", jsonSerializationContext.serialize(Collections.list(node.children()))); // recursion!
        }
        // No need to write node.getParent(), it would lead to infinite recursion.
        return obj;
    }

    public Class<? extends DefaultMutableTreeNode> getAdapterClass() {
        return DefaultMutableTreeNode.class;
    }
}
