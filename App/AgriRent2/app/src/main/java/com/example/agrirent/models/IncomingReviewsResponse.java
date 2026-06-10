package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class IncomingReviewsResponse {
    @SerializedName("averageRating")
    private double averageRating;

    @SerializedName("totalReviews")
    private int totalReviews;

    @SerializedName("reviews")
    private List<RatingResponse> reviews;

    public double getAverageRating() { return averageRating; }
    public int getTotalReviews() { return totalReviews; }
    public List<RatingResponse> getReviews() { return reviews; }
}
