package com.planify.frontend.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.planify.frontend.models.auth.MemberInfo;

import java.io.IOException;

public class MemberInfoAdapter extends TypeAdapter<MemberInfo> {
    @Override
    public void write(JsonWriter out, MemberInfo member) throws IOException {
        out.beginObject();
        out.name("name").value(member.getName());
        out.name("email").value(member.getEmail()); // or whatever the field is
        out.endObject();
    }

    @Override
    public MemberInfo read(JsonReader in) throws IOException {
        String name = null;
        String email = null;
        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "name" -> name = in.nextString();
                case "email" -> email = in.nextString();
                default -> in.skipValue();
            }
        }
        in.endObject();
        return new MemberInfo(name, email);
    }
}