package com.example.agrirent.models;

public class CategoryModel {
    private String name;
    private int iconRes;

    public CategoryModel(String name, int iconRes) {
        this.name = name;
        this.iconRes = iconRes;
    }

    public String getName() {
        return name;
    }

    public int getIconRes() {
        return iconRes;
    }
}
