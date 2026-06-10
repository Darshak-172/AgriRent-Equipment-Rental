package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class CreateRatingRequest {
    @SerializedName("targetId")
    private int targetId;
    
    @SerializedName("targetType")
    private String ratingType;
    
    @SerializedName("ratingValue")
    private int ratingValue;
    
    @SerializedName("comment")
    private String comment;

    public CreateRatingRequest(int targetId, String ratingType, int ratingValue, String comment) {
        this.targetId = targetId;
        this.ratingType = ratingType;
        this.ratingValue = ratingValue;
        this.comment = comment;
    }

    public int getTargetId() { return targetId; }
    public String getRatingType() { return ratingType; }
    public int getRatingValue() { return ratingValue; }
    public String getComment() { return comment; }
}
