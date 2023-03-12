package com.ppm.integration.agilesdk.connector.octane.client;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class RemoveNullListDeserializer<Permission> implements JsonDeserializer<Permission> {

    @Override
    public Permission deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext arg2)
            throws JsonParseException
    {
        JsonArray jsonArray1 = new JsonArray();
        JsonArray jsonArray = json.getAsJsonObject().get("data").getAsJsonArray();
        for (int i = 0; i < jsonArray.size(); i++)
        {
            JsonElement jsonElement = jsonArray.get(i);
            if (!jsonElement.getAsJsonObject().get("logical_name").toString().contains("perm.view.spm")) {

                continue;
            }
            jsonArray1.add(jsonElement);
        }


        json.getAsJsonObject().add("data", jsonArray1);
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json, typeOfT);
    }
}

   

