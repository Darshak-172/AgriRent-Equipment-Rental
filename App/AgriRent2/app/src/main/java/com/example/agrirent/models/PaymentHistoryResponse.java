package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response model for the paginated payment history endpoint: GET /api/payment/history
 */
public class PaymentHistoryResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("totalCount")
    private int totalCount;

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("currentPage")
    private int currentPage;

    @SerializedName("pageSize")
    private int pageSize;

    @SerializedName("data")
    private List<PaymentItem> data;

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public int getTotalCount() { return totalCount; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
    public int getPageSize() { return pageSize; }
    public List<PaymentItem> getData() { return data; }
}
