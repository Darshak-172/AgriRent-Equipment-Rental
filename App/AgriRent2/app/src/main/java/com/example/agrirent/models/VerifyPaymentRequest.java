package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

/** Request body for POST /api/subscription/verify-payment */
public class VerifyPaymentRequest {

    @SerializedName("planId")
    private int planId;

    @SerializedName("paymentId")
    private String paymentId;

    @SerializedName("orderId")
    private String orderId;

    @SerializedName("signature")
    private String signature;

    public VerifyPaymentRequest(int planId, String paymentId, String orderId, String signature) {
        this.planId = planId;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.signature = signature;
    }
}
