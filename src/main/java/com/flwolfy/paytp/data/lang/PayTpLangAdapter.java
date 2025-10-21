package com.flwolfy.paytp.data.lang;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class PayTpLangAdapter implements JsonSerializer<PayTpLang>, JsonDeserializer<PayTpLang> {
  @Override
  public JsonElement serialize(PayTpLang src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.getLangKey());
  }

  @Override
  public PayTpLang deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return PayTpLang.fromKey(json.getAsString());
  }
}
