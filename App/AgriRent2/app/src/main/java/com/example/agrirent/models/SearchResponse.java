package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SearchResponse {

    @SerializedName("data")
    private List<SearchResult> data;

    @SerializedName("pagination")
    private Pagination pagination;

    public List<SearchResult> getData() {
        return data;
    }

    public void setData(List<SearchResult> data) {
        this.data = data;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public static class Pagination {
        @SerializedName("page")
        private int page;

        @SerializedName("pageSize")
        private int pageSize;

        @SerializedName("totalItems")
        private int totalItems;

        @SerializedName("totalPages")
        private int totalPages;

        @SerializedName("hasNextPage")
        private boolean hasNextPage;

        @SerializedName("hasPreviousPage")
        private boolean hasPreviousPage;

        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public boolean isHasNextPage() { return hasNextPage; }
        public boolean isHasPreviousPage() { return hasPreviousPage; }
    }
}
