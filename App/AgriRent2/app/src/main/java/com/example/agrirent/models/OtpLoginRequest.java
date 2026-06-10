package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class OtpLoginRequest {

    @SerializedName("mobileNumber")
    private String mobileNumber;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("otp")
    private String otp;

    public OtpLoginRequest(
            String mobileNumber,
            String sessionId,
            String otp
    ) {
        // 🇮🇳 Always ensure +91 prefix
        if (!mobileNumber.startsWith("+91")) {
            this.mobileNumber = "+91" + mobileNumber;
        } else {
            this.mobileNumber = mobileNumber;
        }

        this.sessionId = sessionId;
        this.otp = otp;
    }

    // Optional getters (good practice)
    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getOtp() {
        return otp;
    }
}
