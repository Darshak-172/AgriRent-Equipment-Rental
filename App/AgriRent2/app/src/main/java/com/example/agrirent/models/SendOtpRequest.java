package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class SendOtpRequest {

    @SerializedName("mobileNumber")
    private String mobileNumber;

    public SendOtpRequest(String mobileNumber) {
        // 🇮🇳 Always send +91 prefix
        if (!mobileNumber.startsWith("+91")) {
            this.mobileNumber = "+91" + mobileNumber;
        } else {
            this.mobileNumber = mobileNumber;
        }
    }

    public String getMobileNumber() {
        return mobileNumber;
    }
}
