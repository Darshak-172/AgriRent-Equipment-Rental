package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ComplaintResponse implements Serializable {

    @SerializedName("complaintId")
    private int id;

    @SerializedName("description")
    private String description;

    @SerializedName("targetId")
    private int targetId;

    @SerializedName("targetType")
    private String targetType;

    @SerializedName("status")
    private String status;

    @SerializedName("resolutionNote")
    private String resolutionNote;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("resolvedAt")
    private String resolvedAt;

    @SerializedName("filedBy")
    private String filedBy;

    private String targetName; // Local field for resolved item name

    // Getters
    public int getComplaintId() { return id; }
    public int getId() { return id; }
    public String getFiledBy() { return filedBy; }
    public String getDescription() { return description; }
    // Use description as both subject and description since backend only has description
    public String getSubject() { return description; }
    public int getTargetId() { return targetId; }
    public String getTransactionId() { return String.valueOf(targetId); }
    public String getTransactionType() { return targetType; }
    public String getStatus() { return status; }
    public String getResolutionNote() { return resolutionNote; }
    public String getCreatedAt() { return createdAt; }
    public String getResolvedAt() { return resolvedAt; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
}
