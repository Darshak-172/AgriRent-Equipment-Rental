package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class CreateBookingRequest {
    @SerializedName("equipmentId")
    private int equipmentId;
    
    @SerializedName("startDate")
    private String startDate;
    
    @SerializedName("endDate")
    private String endDate;

    public CreateBookingRequest(int equipmentId, String startDate, String endDate) {
        this.equipmentId = equipmentId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public int getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}
