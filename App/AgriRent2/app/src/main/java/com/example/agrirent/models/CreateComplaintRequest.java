package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class CreateComplaintRequest {

    @SerializedName("targetId")
    private int targetId;

    @SerializedName("targetType")
    private String targetType; // "Equipment" or "Product"

    @SerializedName("description")
    private String description;

    public CreateComplaintRequest(int targetId, String targetType, String description) {
        this.targetId = targetId;
        this.targetType = targetType;
        this.description = description;
    }

    public int getTargetId() { return targetId; }
    public String getTargetType() { return targetType; }
    public String getDescription() { return description; }
}
