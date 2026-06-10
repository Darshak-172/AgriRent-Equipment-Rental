package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

/**
 * Maps to GET /api/subscription/my-subscription → data[]
 * Fields: subscriptionId, planId, planName, startDate, endDate, status, paymentId
 */
public class MySubscription {

    @SerializedName("subscriptionId")
    private int subscriptionId;

    @SerializedName("planId")
    private int planId;

    @SerializedName("planName")
    private String planName;

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("status")
    private String status;

    @SerializedName("paymentId")
    private String paymentId;

    // Getters
    public int getSubscriptionId()  { return subscriptionId; }
    public int getPlanId()          { return planId; }
    public String getPlanName()     { return planName; }
    public String getStartDate()    { return startDate; }
    public String getEndDate()      { return endDate; }
    public String getStatus()       { return status; }
    public String getPaymentId()    { return paymentId; }
}
