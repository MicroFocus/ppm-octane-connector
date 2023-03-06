package com.ppm.integration.agilesdk.connector.octane.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * @Author YanFeng
 * @Date 2/25/2023
 * @Description
 */

public class LogicNameDeserializer implements JsonDeserializer<PermissionLogicName> {
    @Override
    public PermissionLogicName deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        String enumString = json.getAsString();
        try {
            return PermissionLogicName.decode(enumString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
