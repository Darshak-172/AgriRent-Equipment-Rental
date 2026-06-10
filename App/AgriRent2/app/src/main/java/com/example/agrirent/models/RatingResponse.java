package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class RatingResponse {

    @SerializedName("ratingId")
    private int ratingId;

    @SerializedName("ratingValue")
    private int ratingValue;

    @SerializedName("comment")
    private String comment;

    // Used when fetching equipment/product reviews (userName field from backend)
    @SerializedName("userName")
    private String userName;

    // Used when fetching all reviews admin endpoint (userFullName field)
    @SerializedName("userFullName")
    private String userFullName;

    @SerializedName("userProfilePicture")
    private String userProfilePicture;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("targetId")
    private int targetId;

    @SerializedName("targetType")
    private String targetType;

    private String targetName; // Local field

    public int getRatingId() { return ratingId; }
    public int getRatingValue() { return ratingValue; }
    public String getComment() { return comment; }

    // Return whichever name field is populated
    public String getUserFullName() {
        if (userFullName != null && !userFullName.isEmpty()) return userFullName;
        if (userName != null && !userName.isEmpty()) return userName;
        return "You";
    }

    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getUserProfilePicture() { return userProfilePicture; }
    public String getCreatedAt() { return createdAt; }
    public int getTargetId() { return targetId; }
    public String getTargetType() { return targetType; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
}
