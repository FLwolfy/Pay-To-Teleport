package com.flwolfy.paytp.data.warp;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class PayTpWarpPermissionAdapter
    implements JsonSerializer<PayTpWarpPermission>,
    JsonDeserializer<PayTpWarpPermission> {

  @Override
  public JsonElement serialize(
      PayTpWarpPermission src,
      Type typeOfSrc,
      JsonSerializationContext context
  ) {
    return new JsonPrimitive(src.getLevel());
  }

  @Override
  public PayTpWarpPermission deserialize(
      JsonElement json,
      Type typeOfT,
      JsonDeserializationContext context
  ) throws JsonParseException {
    return PayTpWarpPermission.fromLevel(json.getAsInt());
  }
}
