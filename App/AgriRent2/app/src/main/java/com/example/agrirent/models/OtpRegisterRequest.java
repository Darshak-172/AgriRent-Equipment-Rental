package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class OtpRegisterRequest {

    @SerializedName("mobileNumber")
    private String mobileNumber;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("otp")
    private String otp;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("password")
    private String password;

    public OtpRegisterRequest(
            String mobileNumber,
            String sessionId,
            String otp,
            String fullName,
            String password
    ) {
        // 🇮🇳 enforce +91
        this.mobileNumber = mobileNumber.startsWith("+91")
                ? mobileNumber
                : "+91" + mobileNumber;

        this.sessionId = sessionId;
        this.otp = otp;
        this.fullName = fullName;
        this.password = password;
    }
}
