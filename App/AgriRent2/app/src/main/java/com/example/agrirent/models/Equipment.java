package com.example.agrirent.models;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class Equipment implements Serializable {

    @SerializedName("equipmentId")
    private int equipmentId;

    @SerializedName("equipmentName")
    private String equipmentName;

    @SerializedName("description")
    private String description;

    @SerializedName("hourlyPrice")
    private double hourlyPrice;

    @SerializedName("dailyPrice")
    private double dailyPrice;

    @SerializedName("location")
    private String location;

    @SerializedName("subCategoryId")
    private int subCategoryId;

    @SerializedName("subCategoryName")
    private String subCategoryName;

    @SerializedName("categoryId")
    private int categoryId;

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName("imageCount")
    private int imageCount;

    @SerializedName("averageRating")
    private double averageRating;

    @SerializedName("reviewCount")
    private int reviewCount;

    @SerializedName("images")
    private List<EquipmentImage> images;

    // The public API returns owner as a String, but my-list returns it as an object.
    // We use a custom TypeAdapter to handle both.
    @SerializedName("owner")
    @JsonAdapter(OwnerAdapter.class)
    private OwnerDto owner;

    @SerializedName("ownerId")
    private int ownerId;

    @SerializedName("ownerMobile")
    private String ownerMobile;

    // Direct imageUrl field returned by both public and my-list endpoints
    @SerializedName("imageUrl")
    private String imageUrlDirect;

    @SerializedName("status")
    private String status;

    // =========================
    // Getters and Setters
    // =========================

    public int getEquipmentId() { return equipmentId; }
    public void setEquipmentId(int equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getHourlyPrice() { return hourlyPrice; }
    public void setHourlyPrice(double hourlyPrice) { this.hourlyPrice = hourlyPrice; }

    public double getDailyPrice() { return dailyPrice; }
    public void setDailyPrice(double dailyPrice) { this.dailyPrice = dailyPrice; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getSubCategoryId() { return subCategoryId; }
    public void setSubCategoryId(int subCategoryId) { this.subCategoryId = subCategoryId; }

    public String getSubCategoryName() { return subCategoryName; }
    public void setSubCategoryName(String subCategoryName) { this.subCategoryName = subCategoryName; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public int getImageCount() { return imageCount; }
    public void setImageCount(int imageCount) { this.imageCount = imageCount; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public List<EquipmentImage> getImages() { return images; }
    public void setImages(List<EquipmentImage> images) { this.images = images; }

    public String getOwnerName() {
        return owner != null ? owner.getFullName() : null;
    }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getOwnerMobile() {
        if (ownerMobile != null && !ownerMobile.isEmpty()) return ownerMobile;
        return owner != null ? owner.getMobileNumber() : null;
    }
    public void setOwnerMobile(String ownerMobile) { this.ownerMobile = ownerMobile; }

    public String getOwner() {
        return owner != null ? owner.getFullName() : null;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() {
        if (imageUrlDirect != null && !imageUrlDirect.isEmpty()) return imageUrlDirect;
        if (images != null && !images.isEmpty() && images.get(0).getImageUrl() != null)
            return images.get(0).getImageUrl();
        return thumbnailUrl;
    }

    // =========================
    // Inner Classes
    // =========================

    /**
     * Custom TypeAdapter that handles owner being either:
     *   - a plain string: "Harmish"  (public API)
     *   - an object: {"userId": 1, "fullName": "Harmish", "mobileNumber": "..."} (my-list API)
     */
    public static class OwnerAdapter extends TypeAdapter<OwnerDto> {
        @Override
        public void write(JsonWriter out, OwnerDto value) throws IOException {
            if (value == null) { out.nullValue(); return; }
            out.value(value.getFullName());
        }

        @Override
        public OwnerDto read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
            if (in.peek() == JsonToken.STRING) {
                // Public API: "owner": "Harmish"
                OwnerDto dto = new OwnerDto();
                dto.fullName = in.nextString();
                return dto;
            }
            // my-list API: "owner": { "userId": ..., "fullName": ..., "mobileNumber": ... }
            OwnerDto dto = new OwnerDto();
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                switch (name) {
                    case "userId": dto.userId = in.nextInt(); break;
                    case "fullName": dto.fullName = in.nextString(); break;
                    case "mobileNumber": dto.mobileNumber = in.nextString(); break;
                    default: in.skipValue(); break;
                }
            }
            in.endObject();
            return dto;
        }
    }

    public static class OwnerDto implements Serializable {
        private int userId;
        private String fullName;
        private String mobileNumber;

        public int getUserId() { return userId; }
        public String getFullName() { return fullName; }
        public String getMobileNumber() { return mobileNumber; }
    }

    public static class EquipmentImage implements Serializable {
        @SerializedName("imageId")
        private int imageId;
        @SerializedName("imageUrl")
        private String imageUrl;

        public int getImageId() { return imageId; }
        public String getImageUrl() { return imageUrl; }
    }
}
