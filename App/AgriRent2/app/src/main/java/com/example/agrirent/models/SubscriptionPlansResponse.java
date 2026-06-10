package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Response wrapper from GET /api/subscription-plans */
public class SubscriptionPlansResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("total")
    private int total;

    @SerializedName("data")
    private List<SubscriptionPlan> data;

    public String getMessage() { return message; }
    public int getTotal() { return total; }
    public List<SubscriptionPlan> getData() { return data; }
}
