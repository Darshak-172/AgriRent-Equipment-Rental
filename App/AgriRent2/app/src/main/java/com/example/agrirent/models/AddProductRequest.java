package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class AddProductRequest {

    @SerializedName("productName")
    private String productName;

    @SerializedName("description")
    private String description;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("price")
    private double price;

    @SerializedName("stock")
    private int stock;

    @SerializedName("unit")
    private String unit;

    @SerializedName("location")
    private String location;

    @SerializedName("subCategoryId")
    private int subCategoryId;

    @SerializedName("imageUrls")
    private java.util.List<String> imageUrls;

    @SerializedName("category")
    private String category;

    public AddProductRequest(String productName, String description, double price,
                             int stock, String unit, String location, int subCategoryId) {
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.unit = unit;
        this.location = location;
        this.subCategoryId = subCategoryId;
    }

    public void setImageUrls(java.util.List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void setCategory(String category) { this.category = category; }

    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getImageUrl() { return imageUrl; }

    public String getProductName() { return productName; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public String getUnit() { return unit; }
    public String getLocation() { return location; }
    public int getSubCategoryId() { return subCategoryId; }
    public java.util.List<String> getImageUrls() { return imageUrls; }
    public String getCategory() { return category; }
}
