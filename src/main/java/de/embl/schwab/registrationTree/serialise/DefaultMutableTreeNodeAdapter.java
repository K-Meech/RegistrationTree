package de.embl.schwab.registrationTree.serialise;

import com.google.gson.*;
import de.embl.schwab.registrationTree.Cropper;
import de.embl.schwab.registrationTree.registrationNodes.BigWarpRegistrationNode;
import de.embl.schwab.registrationTree.registrationNodes.ElastixRegistrationNode;
import de.embl.schwab.registrationTree.registrationNodes.RegistrationNode;

import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;

// based on https://stackoverflow.com/questions/53997112/how-to-serialize-defaultmutabletreenode-java-to-json
public class DefaultMutableTreeNodeAdapter implements JsonSerializer<DefaultMutableTreeNode>, JsonDeserializer<DefaultMutableTreeNode> {

    private Cropper cropper;

    public DefaultMutableTreeNodeAdapter() {
    }

    public DefaultMutableTreeNodeAdapter( Cropper cropper ) {
        this.cropper = cropper;
    }

    @Override
    public DefaultMutableTreeNode deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        JsonObject jobject = jsonElement.getAsJsonObject();
        Iterator<String> keys = jobject.keySet().iterator();
        DefaultMutableTreeNode node = new DefaultMutableTreeNode();

        // remove any old crops, and replace with those read from the file
        if ( cropper != null ) {
            cropper.removeAllCrops();
        }

        while (keys.hasNext()) {
            switch (keys.next()) {
                case "allowsChildren":
                    node.setAllowsChildren( jobject.get("allowsChildren").getAsBoolean() );
                    break;
                case "userObject":
                    JsonObject userObject = jobject.get("userObject").getAsJsonObject();
                    if ( userObject.has("elastixParameters")) {
                        ElastixRegistrationNode elastixNode = jsonDeserializationContext.deserialize(
                                userObject, ElastixRegistrationNode.class);
                        node.setUserObject( elastixNode );
                        if ( cropper != null ) {
                            if (elastixNode.fixedCrop != null && elastixNode.fixedCrop.size() > 0) {
                                cropper.addFixedImageCrops(elastixNode.fixedCrop);
                            }

                            if (elastixNode.movingCrop != null && elastixNode.movingCrop.size() > 0) {
                                cropper.addMovingImageCrops(elastixNode.movingCrop);
                            }
                        }

                    } else if ( userObject.has("movingLandmarks") ) {
                        node.setUserObject(jsonDeserializationContext.deserialize(
                                userObject, BigWarpRegistrationNode.class));
                    } else {
                        node.setUserObject(jsonDeserializationContext.deserialize(
                                userObject, RegistrationNode.class));
                    }
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
