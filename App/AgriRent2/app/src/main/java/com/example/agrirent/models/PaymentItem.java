package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model representing a single payment record from the /api/payment/history endpoint.
 */
public class PaymentItem {

    @SerializedName("paymentId")
    private int paymentId;

    @SerializedName("subscriptionId")
    private int subscriptionId;

    @SerializedName("razorpayPaymentId")
    private String razorpayPaymentId;

    @SerializedName("razorpayOrderId")
    private String razorpayOrderId;

    @SerializedName("amount")
    private double amount;

    @SerializedName("currency")
    private String currency;

    @SerializedName("status")
    private String status;

    @SerializedName("paymentDate")
    private String paymentDate;

    @SerializedName("receiptNumber")
    private String receiptNumber;

    @SerializedName("paymentMethod")
    private String paymentMethod;

    @SerializedName("verificationDate")
    private String verificationDate;

    @SerializedName("planName")
    private String planName;

    @SerializedName("durationDays")
    private int durationDays;

    @SerializedName("errorMessage")
    private String errorMessage;

    // Getters
    public int getPaymentId() { return paymentId; }
    public int getSubscriptionId() { return subscriptionId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getPaymentDate() { return paymentDate; }
    public String getReceiptNumber() { return receiptNumber; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getVerificationDate() { return verificationDate; }
    public String getPlanName() { return planName; }
    public int getDurationDays() { return durationDays; }
    public String getErrorMessage() { return errorMessage; }
}
