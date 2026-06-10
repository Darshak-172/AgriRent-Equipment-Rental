package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class BlockedDateDto implements Serializable {

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;

    @SerializedName("reason")
    private String reason;

    /** "blocked" = owner manually blocked, "booked" = already booked by a farmer */
    @SerializedName("type")
    private String type;

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isBooked() { return "booked".equalsIgnoreCase(type); }
}
