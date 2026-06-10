package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiCategory {

    @SerializedName("categoryId")
    private int categoryId;

    @SerializedName("name")
    private String name;

    @SerializedName("status")
    private String status;

    @SerializedName("type")
    private String type;

    @SerializedName("subCategories")
    private List<ApiSubCategory> subCategories;

    public int getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getType() { return type; }
    public List<ApiSubCategory> getSubCategories() { return subCategories; }

    public static class ApiSubCategory {
        @SerializedName("subCategoryId")
        private int subCategoryId;

        @SerializedName("categoryId")
        private int categoryId;

        @SerializedName("subCategoryName")
        private String subCategoryName;

        @SerializedName("status")
        private String status;

        public int getSubCategoryId() { return subCategoryId; }
        public int getCategoryId() { return categoryId; }
        public String getSubCategoryName() { return subCategoryName; }
        public String getStatus() { return status; }
    }
}
