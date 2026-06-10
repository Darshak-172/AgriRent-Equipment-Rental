package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

/**
 * Response from POST /api/subscription/verify-payment
 * {success, message, token, roles[], validFrom, validTill}
 */
public class VerifyPaymentResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("token")
    private String token;

    @SerializedName("roles")
    private java.util.List<String> roles;

    @SerializedName("validFrom")
    private String validFrom;

    @SerializedName("validTill")
    private String validTill;

    public boolean isSuccess()      { return success; }
    public String getMessage()      { return message; }
    public String getToken()        { return token; }
    public java.util.List<String> getRoles() { return roles; }
    public String getValidFrom()    { return validFrom; }
    public String getValidTill()    { return validTill; }
}
