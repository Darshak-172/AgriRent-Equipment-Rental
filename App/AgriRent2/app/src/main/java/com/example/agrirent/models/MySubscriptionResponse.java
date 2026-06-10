package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response wrapper for GET /api/subscription/my-subscription
 * { "success": true, "data": [...] }
 */
public class MySubscriptionResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<MySubscription> data;

    public boolean isSuccess() { return success; }
    public List<MySubscription> getData() { return data; }
}
