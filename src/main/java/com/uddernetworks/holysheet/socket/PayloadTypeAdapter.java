package com.uddernetworks.holysheet.socket;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class PayloadTypeAdapter extends TypeAdapter<PayloadType> {

    @Override
    public void write(JsonWriter out, PayloadType value) throws IOException {
        out.beginObject()
                .name("type")
                .value(value.getType())
                .endObject();
    }

    @Override
    public PayloadType read(JsonReader in) throws IOException {
        return PayloadType.fromType(in.nextInt()).orElse(null);
    }
}
