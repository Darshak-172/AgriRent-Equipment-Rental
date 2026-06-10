package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class ProductNameResponse {
    @SerializedName("productId")
    private int productId;

    @SerializedName("productName")
    private String productName;

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
}
