package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class RefreshResponse {

    @SerializedName("token")
    private String token;

    @SerializedName("refreshToken")
    private String refreshToken;

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
