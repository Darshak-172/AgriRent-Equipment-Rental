package com.example.agrirent.models;

public class ChangePasswordRequest {

    private String mobileNumber;
    private String sessionId;
    private String otp;
    private String oldPassword;
    private String newPassword;

    public ChangePasswordRequest(
            String mobileNumber,
            String sessionId,
            String otp,
            String oldPassword,
            String newPassword
    ) {
        this.mobileNumber = mobileNumber;
        this.sessionId = sessionId;
        this.otp = otp;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}
