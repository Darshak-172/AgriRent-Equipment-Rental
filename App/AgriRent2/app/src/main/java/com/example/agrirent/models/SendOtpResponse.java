package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class SendOtpResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("sessionId")
    private String sessionId;

    public String getMessage() {
        return message;
    }

    public String getSessionId() {
        return sessionId;
    }
}
