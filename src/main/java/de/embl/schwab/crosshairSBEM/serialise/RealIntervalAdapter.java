package de.embl.schwab.crosshairSBEM.serialise;

import com.google.gson.*;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import java.lang.reflect.Type;

public class RealIntervalAdapter implements JsonDeserializer<RealInterval> {

    public RealIntervalAdapter() {
    }

    @Override
    public RealInterval deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jObject = jsonElement.getAsJsonObject();
        double[] min = (double[]) jsonDeserializationContext.deserialize(jObject.get("min"), double[].class );
        double[] max = (double[]) jsonDeserializationContext.deserialize(jObject.get("max"), double[].class );
        return  new FinalRealInterval( min, max );
    }

    public Class<? extends RealInterval> getAdapterClass() {
        return RealInterval.class;
    }
}
