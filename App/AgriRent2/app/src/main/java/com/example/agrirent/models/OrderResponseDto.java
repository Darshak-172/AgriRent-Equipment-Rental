package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class OrderResponseDto implements Serializable {

    @SerializedName("orderId")
    private int orderId;

    @SerializedName("productId")
    private int productId;

    @SerializedName("productName")
    private String productName;

    @SerializedName("sellerName")
    private String sellerName;

    @SerializedName("farmerName")
    private String farmerName;

    @SerializedName(value = "sellerMobile", alternate = { "SellerMobile", "seller_mobile" })
    private String sellerMobile;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("unitPrice")
    private double unitPrice;

    @SerializedName("totalPrice")
    private double totalPrice;

    @SerializedName(value = "deliveryAddress", alternate = { "address", "shippingAddress", "userAddress",
            "delivery_address" })
    private String deliveryAddress;

    @SerializedName(value = "contactNumber", alternate = { "phone", "userPhone", "mobileNumber", "mobile",
            "contact_number", "contact" })
    private String contactNumber;

    @SerializedName("status")
    private String status; // Pending, Processing, Shipped, Delivered, Canceled

    @SerializedName("paymentStatus")
    private String paymentStatus;

    @SerializedName("orderDate")
    private String orderDate;

    @SerializedName("deliveryDate")
    private String deliveryDate;

    @SerializedName(value = "productImage", alternate = {
            "imageUrl", "image", "productPhoto", "product_image", "productImageUrl",
            "equipmentImageUrl", "thumbnailUrl", "image_url", "imageurl", "imageURL",
            "photo", "productphoto", "product_photo", "imgUrl", "img_url",
            "EquipmentImageUrl", "ProductImage", "ImageUrl", "Image", "ThumbnailUrl",
            "equipment_image_url", "equipmentImage"
    })
    private String productImage;

    @SerializedName("images")
    private java.util.List<ProductItem.ProductImage> images;

    // Getters
    public int getOrderId() {
        return orderId;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public String getSellerMobile() {
        return sellerMobile;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public String getProductImage() {
        return productImage;
    }

    public java.util.List<ProductItem.ProductImage> getImages() {
        return images;
    }

    public String getImageUrl() {
        String url = null;
        if (images != null && !images.isEmpty()) {
            url = images.get(0).getImageUrl();
        } else {
            url = productImage;
        }

        if (url != null && !url.isEmpty() && !url.startsWith("http")) {
            url = com.example.agrirent.network.ApiClient.BASE_URL + (url.startsWith("/") ? url.substring(1) : url);
        }
        return url;
    }

    // Setters
    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public void setSellerMobile(String sellerMobile) {
        this.sellerMobile = sellerMobile;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }
}
