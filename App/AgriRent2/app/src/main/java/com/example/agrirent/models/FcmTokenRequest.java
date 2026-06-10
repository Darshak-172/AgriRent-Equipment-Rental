package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class FcmTokenRequest {
    @SerializedName("fcmToken")
    private String fcmToken;

    @SerializedName("language")
    private String language;

    @SerializedName("deviceId")
    private String deviceId;

    // Updated constructor to accept 3 arguments
    public FcmTokenRequest(String fcmToken, String language, String deviceId) {
        this.fcmToken = fcmToken;
        this.language = language != null ? language : "en";
        this.deviceId = deviceId;
    }

    // Getters and Setters
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
