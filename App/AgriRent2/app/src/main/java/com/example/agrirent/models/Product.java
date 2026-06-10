package com.example.agrirent.models;

public class Product {
    private String name;
    private String price;
    private int imageRes;       // local drawable (0 if using URL)
    private String imageUrl;    // remote URL from API

    // Constructor for local drawable (legacy)
    public Product(String name, String price, int imageRes) {
        this.name = name;
        this.price = price;
        this.imageRes = imageRes;
        this.imageUrl = null;
    }

    // Constructor for API data
    public Product(String name, String price, int imageRes, String imageUrl) {
        this.name = name;
        this.price = price;
        this.imageRes = imageRes;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public String getPrice() { return price; }
    public int getImageRes() { return imageRes; }
    public String getImageUrl() { return imageUrl; }
}
