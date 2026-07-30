package com.flwolfy.paytp.data.script;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class PayTpScriptAdapter extends TypeAdapter<PayTpScript> {

  @Override
  public void write(JsonWriter writer, PayTpScript script) throws IOException {
    writer.value(script.source());
  }

  @Override
  public PayTpScript read(JsonReader reader) throws IOException {
    return new PayTpScript(reader.nextString());
  }
}
