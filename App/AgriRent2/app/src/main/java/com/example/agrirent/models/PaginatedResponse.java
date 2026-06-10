package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class PaginatedResponse<T> implements Serializable {

    @SerializedName("data")
    private List<T> data;

    @SerializedName("pagination")
    private Pagination pagination;

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public static class Pagination implements Serializable {
        @SerializedName("page")
        private int page;

        @SerializedName("pageSize")
        private int pageSize;

        @SerializedName("totalItems")
        private int totalItems;

        @SerializedName("totalPages")
        private int totalPages;

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }

        public int getTotalItems() { return totalItems; }
        public void setTotalItems(int totalItems) { this.totalItems = totalItems; }

        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }
}
