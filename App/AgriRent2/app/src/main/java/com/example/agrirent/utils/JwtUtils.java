package com.example.agrirent.utils;

import android.util.Base64;

import org.json.JSONObject;

public class JwtUtils {

    public static String getUserName(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "";

            String payload = parts[1];

            byte[] decodedBytes =
                    Base64.decode(payload, Base64.URL_SAFE);

            String decodedPayload =
                    new String(decodedBytes);

            JSONObject json = new JSONObject(decodedPayload);

            // ASP.NET maps ClaimTypes.Name -> "unique_name"
            return json.optString("unique_name", "");

        } catch (Exception e) {
            return "";
        }
    }
}
