package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class EquipmentNameResponse {
    @SerializedName("equipmentId")
    private int equipmentId;

    @SerializedName("equipmentName")
    private String equipmentName;

    public int getEquipmentId() { return equipmentId; }
    public String getEquipmentName() { return equipmentName; }
}
