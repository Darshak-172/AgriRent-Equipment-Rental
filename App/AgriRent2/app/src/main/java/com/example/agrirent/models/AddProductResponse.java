package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class AddProductResponse {

    @SerializedName("productId")
    private int productId;

    @SerializedName("message")
    private String message;

    @SerializedName("success")
    private boolean success;

    public int getProductId() { return productId; }
    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
}
