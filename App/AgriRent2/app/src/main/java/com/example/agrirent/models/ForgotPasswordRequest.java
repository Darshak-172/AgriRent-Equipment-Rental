package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class ForgotPasswordRequest {

    @SerializedName("mobileNumber")
    private String mobileNumber;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("otp")
    private String otp;

    @SerializedName("newPassword")
    private String newPassword;

    public ForgotPasswordRequest(
            String mobileNumber,
            String sessionId,
            String otp,
            String newPassword
    ) {
        this.mobileNumber = mobileNumber;
        this.sessionId = sessionId;
        this.otp = otp;
        this.newPassword = newPassword;
    }
}
