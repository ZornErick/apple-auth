package com.zorn.jwt;

import org.json.JSONObject;

import java.util.Base64;

public class JwtUtility {
    public static JSONObject decodeJwt(String jwt) throws IllegalArgumentException {
        String[] chunks = jwt.split("\\.");
        if (chunks.length != 3) {
            throw new IllegalArgumentException("Invalid JWT Format");
        }

        Base64.Decoder decoder = Base64.getUrlDecoder();
        String payload = new String(decoder.decode(chunks[1]));

        return new JSONObject(payload);
    }
}
