package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

/** Response from POST /api/subscription/create-order/{planId} */
public class SubscriptionOrder {

    @SerializedName("orderId")
    private String orderId;

    @SerializedName("key")
    private String key;

    @SerializedName("amount")
    private double amount;

    @SerializedName("planId")
    private int planId;

    @SerializedName("receipt")
    private String receipt;

    public String getOrderId() { return orderId; }
    public String getKey() { return key; }
    public double getAmount() { return amount; }
    public int getPlanId() { return planId; }
    public String getReceipt() { return receipt; }
}
