package de.embl.schwab.crosshairSBEM;

import com.google.gson.*;
import de.embl.schwab.crosshairSBEM.registrationNodes.RegistrationNode;

import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;

// based on https://stackoverflow.com/questions/53997112/how-to-serialize-defaultmutabletreenode-java-to-json
public class DefaultMutableTreeNodeAdapter implements JsonSerializer<DefaultMutableTreeNode>, JsonDeserializer<DefaultMutableTreeNode> {

    public DefaultMutableTreeNodeAdapter() {
    }

    @Override
    public DefaultMutableTreeNode deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        JsonObject jobject = jsonElement.getAsJsonObject();
        Iterator<String> keys = jobject.keySet().iterator();
        DefaultMutableTreeNode node = new DefaultMutableTreeNode();

        while (keys.hasNext()) {
            switch (keys.next()) {
                case "allowsChildren":
                    node.setAllowsChildren( jobject.get("allowsChildren").getAsBoolean() );
                    break;
                case "userObject":
                    node.setUserObject(jsonDeserializationContext.deserialize( jobject.get("userObject").getAsJsonObject(), RegistrationNode.class));
                    break;
                case "children":
                    Iterator<JsonElement> children = jobject.get("children").getAsJsonArray().iterator();
                    while (children.hasNext()) {
                        node.add( jsonDeserializationContext.deserialize( children.next(), DefaultMutableTreeNode.class )); // recursion!
                    }
                    break;
                default:
                    break;
            }
        }

        return node;
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
