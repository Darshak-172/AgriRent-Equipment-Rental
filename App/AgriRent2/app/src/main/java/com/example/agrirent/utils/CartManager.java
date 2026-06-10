package com.example.agrirent.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.agrirent.models.CartItem;
import com.example.agrirent.models.ProductItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static final String PREF_NAME = "AgriRentCartPrefs";
    private static final String KEY_CART = "cart_items";
    
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public CartManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void addToCart(ProductItem product, int quantity) {
        List<CartItem> cartItems = getCartItems();
        boolean exists = false;
        
        for (CartItem item : cartItems) {
            if (item.getProduct().getProductId() == product.getProductId()) {
                item.setQuantity(item.getQuantity() + quantity);
                exists = true;
                break;
            }
        }
        
        if (!exists) {
            cartItems.add(new CartItem(product, quantity));
        }
        
        saveCartItems(cartItems);
    }

    public void updateQuantity(int productId, int quantity) {
        List<CartItem> cartItems = getCartItems();
        for (CartItem item : cartItems) {
            if (item.getProduct().getProductId() == productId) {
                item.setQuantity(quantity);
                break;
            }
        }
        saveCartItems(cartItems);
    }

    public void removeFromCart(int productId) {
        List<CartItem> cartItems = getCartItems();
        for (int i = 0; i < cartItems.size(); i++) {
            if (cartItems.get(i).getProduct().getProductId() == productId) {
                cartItems.remove(i);
                break;
            }
        }
        saveCartItems(cartItems);
    }

    public List<CartItem> getCartItems() {
        String json = sharedPreferences.getString(KEY_CART, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<CartItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void clearCart() {
        sharedPreferences.edit().remove(KEY_CART).apply();
    }

    public int getCartCount() {
        return getCartItems().size();
    }

    public double getCartTotal() {
        List<CartItem> items = getCartItems();
        double total = 0;
        for (CartItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    private void saveCartItems(List<CartItem> cartItems) {
        String json = gson.toJson(cartItems);
        sharedPreferences.edit().putString(KEY_CART, json).apply();
    }
}
