package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.JsonAdapter;
import java.io.Serializable;
import java.util.List;

public class ProductItem implements Serializable {

    @SerializedName("productId")
    private int productId;

    @SerializedName("productName")
    private String productName;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category;

    @SerializedName("price")
    private double price;

    @SerializedName("stock")
    private int stock;

    @SerializedName("unit")
    private String unit;

    @SerializedName("location")
    private String location;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("images")
    private List<ProductImage> images;

    // The public API returns seller as a String, but my-products returns it as an object.
    // We use a custom TypeAdapter to handle both.
    @SerializedName("seller")
    @JsonAdapter(SellerAdapter.class)
    private SellerDto sellerDto;

    @SerializedName("sellerMobile")
    private String sellerMobile;

    @SerializedName("status")
    private String status;

    @SerializedName("subCategoryId")
    private int subCategoryId;

    @SerializedName("averageRating")
    private double averageRating;

    @SerializedName("reviewCount")
    private int reviewCount;

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public double getAverageRating() { return averageRating; }
    public int getReviewCount() { return reviewCount; }

    public String getSeller() { 
        return sellerDto != null ? sellerDto.getFullName() : null; 
    }
    
    public String getSellerMobile() { 
        if (sellerMobile != null && !sellerMobile.isEmpty()) return sellerMobile;
        return sellerDto != null ? sellerDto.getMobileNumber() : null;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSubCategoryId() { return subCategoryId; }
    public void setSubCategoryId(int subCategoryId) { this.subCategoryId = subCategoryId; }
    public List<ProductImage> getImages() { return images; }
    public void setImages(List<ProductImage> images) { this.images = images; }

    public String getFirstImageUrl() {
        if (images != null && !images.isEmpty() && images.get(0).getImageUrl() != null) {
            return images.get(0).getImageUrl();
        }
        return imageUrl;
    }

    public static class SellerAdapter extends com.google.gson.TypeAdapter<SellerDto> {
        @Override
        public void write(com.google.gson.stream.JsonWriter out, SellerDto value) throws java.io.IOException {
            if (value == null) { out.nullValue(); return; }
            out.value(value.getFullName());
        }

        @Override
        public SellerDto read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) { in.nextNull(); return null; }
            if (in.peek() == com.google.gson.stream.JsonToken.STRING) {
                SellerDto dto = new SellerDto();
                dto.fullName = in.nextString();
                return dto;
            }
            SellerDto dto = new SellerDto();
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

    public static class SellerDto implements Serializable {
        private int userId;
        private String fullName;
        private String mobileNumber;

        public int getUserId() { return userId; }
        public String getFullName() { return fullName; }
        public String getMobileNumber() { return mobileNumber; }
    }

    public static class ProductImage implements Serializable {
        @SerializedName("imageId")
        private int imageId;
        @SerializedName("imageUrl")
        private String imageUrl;

        public String getImageUrl() { return imageUrl; }
    }
}
