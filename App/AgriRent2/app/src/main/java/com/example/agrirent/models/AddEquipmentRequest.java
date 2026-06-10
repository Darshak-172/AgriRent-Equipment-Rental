package com.example.agrirent.models;

public class AddEquipmentRequest {
    private String equipmentName;
    private String description;
    private String location;
    private double hourlyPrice;
    private double dailyPrice;
    private Integer subCategoryId;
    private java.util.List<String> imageUrls;

    public AddEquipmentRequest(String equipmentName, String description, String location, double hourlyPrice, double dailyPrice, Integer subCategoryId) {
        this.equipmentName = equipmentName;
        this.description = description;
        this.location = location;
        this.hourlyPrice = hourlyPrice;
        this.dailyPrice = dailyPrice;
        this.subCategoryId = subCategoryId;
    }

    public void setImageUrls(java.util.List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
