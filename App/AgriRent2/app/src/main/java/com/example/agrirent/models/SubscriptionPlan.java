package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

/**
 * Maps to GET /api/subscription-plans → data[]
 * Fields: planId, planName, durationDays, price, maxEquipment, maxProducts
 */
public class SubscriptionPlan {

    @SerializedName("planId")
    private int planId;

    @SerializedName("planName")
    private String planName;

    @SerializedName("durationDays")
    private int durationDays;

    @SerializedName("price")
    private double price;

    @SerializedName("maxEquipment")
    private int maxEquipment;

    @SerializedName("maxProducts")
    private int maxProducts;

    // Getters
    public int getPlanId()         { return planId; }
    public String getPlanName()    { return planName; }
    public int getDurationDays()   { return durationDays; }
    public double getPrice()       { return price; }
    public int getMaxEquipment()   { return maxEquipment; }
    public int getMaxProducts()    { return maxProducts; }
}
