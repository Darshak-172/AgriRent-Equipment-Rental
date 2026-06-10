package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class RefreshRequest {

    @SerializedName("refreshToken")
    private String refreshToken;

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
