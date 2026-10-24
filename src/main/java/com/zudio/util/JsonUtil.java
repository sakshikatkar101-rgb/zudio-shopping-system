package com.zudio.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .registerTypeAdapter(Timestamp.class, new JsonSerializer<Timestamp>() {
                @Override
                public JsonElement serialize(Timestamp src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == null) return JsonNull.INSTANCE;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                    return new JsonPrimitive(sdf.format(src));
                }
            })
            .registerTypeAdapter(Timestamp.class, new JsonDeserializer<Timestamp>() {
                @Override
                public Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    try {
                        return new Timestamp(json.getAsLong());
                    } catch (Exception e) {
                        return new Timestamp(System.currentTimeMillis());
                    }
                }
            })
            .create();

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static <T> T fromJson(String json, Type typeOfT) {
        return gson.fromJson(json, typeOfT);
    }

    public static JsonObject parseObject(String json) {
        if (json == null || json.isBlank()) return new JsonObject();
        return JsonParser.parseString(json).getAsJsonObject();
    }
}
