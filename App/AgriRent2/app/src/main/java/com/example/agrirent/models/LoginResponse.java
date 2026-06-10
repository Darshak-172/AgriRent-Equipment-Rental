package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LoginResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    public String getMessage() {
        return message;
    }

    public Data getData() {
        return data;
    }

    // =====================
    // 🔹 INNER DATA OBJECT
    // =====================
    public static class Data {

        @SerializedName("username")
        private String username;

        @SerializedName("userId")
        private String userId;

        @SerializedName("mobile")
        private String mobile;
        
        @SerializedName("roles")
        private List<String> roles;

        @SerializedName("token")
        private String token;

        @SerializedName("refreshToken")
        private String refreshToken;

        public String getUsername() {
            return username;
        }

        public String getUserId() {
            return userId;
        }

        public String getMobile() {
            return mobile;
        }

        public List<String> getRoles() {
            return roles;
        }

        public String getToken() {
            return token;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
