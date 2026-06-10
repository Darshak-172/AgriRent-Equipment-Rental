package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("mobileNumber")
    private String mobileNumber;

    @SerializedName("password")
    private String password;

    public LoginRequest(String mobileNumber, String password) {

        // 🇮🇳 Always enforce +91
        if (!mobileNumber.startsWith("+91")) {
            this.mobileNumber = "+91" + mobileNumber;
        } else {
            this.mobileNumber = mobileNumber;
        }

        this.password = password;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getPassword() {
        return password;
    }
}
