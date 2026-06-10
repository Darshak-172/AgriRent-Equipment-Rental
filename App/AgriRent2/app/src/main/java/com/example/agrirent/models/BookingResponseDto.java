package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class BookingResponseDto implements Serializable {

    @SerializedName("bookingId")
    private int bookingId;

    @SerializedName("equipmentId")
    private int equipmentId;

    @SerializedName("equipmentName")
    private String equipmentName;

    @SerializedName("equipmentImageUrl")
    private String equipmentImageUrl;

    @SerializedName("equipmentLocation")
    private String equipmentLocation;

    @SerializedName("equipmentPrice")
    private double equipmentPrice;

    @SerializedName("farmerName")
    private String farmerName;

    @SerializedName("farmerMobile")
    private String farmerMobile;

    @SerializedName("ownerName")
    private String ownerName;

    @SerializedName("ownerMobile")
    private String ownerMobile;

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("totalPrice")
    private double totalPrice;

    @SerializedName("status")
    private String status;

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public int getEquipmentId() { return equipmentId; }
    public void setEquipmentId(int equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getEquipmentImageUrl() { return equipmentImageUrl; }
    public void setEquipmentImageUrl(String equipmentImageUrl) { this.equipmentImageUrl = equipmentImageUrl; }

    public String getEquipmentLocation() { return equipmentLocation; }
    public void setEquipmentLocation(String equipmentLocation) { this.equipmentLocation = equipmentLocation; }

    public double getEquipmentPrice() { return equipmentPrice; }
    public void setEquipmentPrice(double equipmentPrice) { this.equipmentPrice = equipmentPrice; }

    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }

    public String getFarmerMobile() { return farmerMobile; }
    public void setFarmerMobile(String farmerMobile) { this.farmerMobile = farmerMobile; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerMobile() { return ownerMobile; }
    public void setOwnerMobile(String ownerMobile) { this.ownerMobile = ownerMobile; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
