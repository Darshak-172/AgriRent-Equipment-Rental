package com.example.agrirent.models;

import java.io.Serializable;

public class CartItem implements Serializable {
    private ProductItem product;
    private int quantity;

    public CartItem(ProductItem product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public ProductItem getProduct() {
        return product;
    }

    public void setProduct(ProductItem product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getSubtotal() {
        return product.getPrice() * quantity;
    }
}
