package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class CreateOrderRequest {

    @SerializedName("productId")
    private int productId;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("deliveryAddress")
    private String deliveryAddress;

    @SerializedName("contactNumber")
    private String contactNumber;

    public CreateOrderRequest(int productId, int quantity, String deliveryAddress, String contactNumber) {
        this.productId = productId;
        this.quantity = quantity;
        this.deliveryAddress = deliveryAddress;
        this.contactNumber = contactNumber;
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
}
