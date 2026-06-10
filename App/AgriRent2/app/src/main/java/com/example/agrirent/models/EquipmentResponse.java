package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EquipmentResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<Equipment> data;

    @SerializedName("pagination")
    private Pagination pagination;

    @SerializedName("_meta")
    private Meta meta;

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<Equipment> getData() {
        return data;
    }

    public void setData(List<Equipment> data) {
        this.data = data;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    // Nested Pagination class
    public static class Pagination {
        @SerializedName("page")
        private int page;

        @SerializedName("limit")
        private int limit;

        @SerializedName("hasNextPage")
        private boolean hasNextPage;

        @SerializedName("hasPreviousPage")
        private boolean hasPreviousPage;

        // Getters and Setters
        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public boolean isHasNextPage() {
            return hasNextPage;
        }

        public void setHasNextPage(boolean hasNextPage) {
            this.hasNextPage = hasNextPage;
        }

        public boolean isHasPreviousPage() {
            return hasPreviousPage;
        }

        public void setHasPreviousPage(boolean hasPreviousPage) {
            this.hasPreviousPage = hasPreviousPage;
        }
    }

    // Nested Meta class
    public static class Meta {
        @SerializedName("language")
        private String language;

        @SerializedName("cached")
        private boolean cached;

        @SerializedName("responseTime")
        private String responseTime;

        // Getters and Setters
        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public boolean isCached() {
            return cached;
        }

        public void setCached(boolean cached) {
            this.cached = cached;
        }

        public String getResponseTime() {
            return responseTime;
        }

        public void setResponseTime(String responseTime) {
            this.responseTime = responseTime;
        }
    }
}
