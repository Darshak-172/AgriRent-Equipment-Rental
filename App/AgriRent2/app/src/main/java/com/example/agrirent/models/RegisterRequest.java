package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("mobileNumber")
    private String mobileNumber;

    @SerializedName("password")
    private String password;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("otp")
    private String otp;

    public RegisterRequest(
            String fullName,
            String mobileNumber,
            String password,
            String sessionId,
            String otp
    ) {
        this.fullName = fullName;
        this.mobileNumber = mobileNumber;
        this.password = password;
        this.sessionId = sessionId;
        this.otp = otp;
    }

    // Optional getters (safe to keep)
    public String getFullName() {
        return fullName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getPassword() {
        return password;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getOtp() {
        return otp;
    }
}
