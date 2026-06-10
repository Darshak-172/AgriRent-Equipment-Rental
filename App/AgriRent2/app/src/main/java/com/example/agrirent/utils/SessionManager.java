package com.example.agrirent.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils; // Import needed for joining the list

import java.util.List; // Import needed for List

public class SessionManager {

    private static final String PREF_NAME = "AgriRentSession";

    // 🔐 AUTH
    private static final String KEY_TOKEN = "JWT_TOKEN";                 // 15 min
    private static final String KEY_REFRESH_TOKEN = "REFRESH_TOKEN";     // 30 days
    private static final String KEY_USER_NAME = "USER_NAME";
    private static final String KEY_USER_MOBILE = "USER_MOBILE";
    private static final String KEY_USER_ID = "USER_ID";

    // 🆕 ADDED: Key for Roles
    private static final String KEY_USER_ROLES = "USER_ROLES";

    // 🆕 SUBSCRIPTION TYPE CACHE
    private static final String KEY_SUB_PLAN_NAME = "SUB_PLAN_NAME";
    private static final String KEY_SUB_STATUS    = "SUB_STATUS";

    // 🌍 LANGUAGE & THEME
    private static final String KEY_LANGUAGE = "LANGUAGE";
    private static final String KEY_LANGUAGE_SELECTED = "LANGUAGE_SELECTED";
    private static final String KEY_IS_DARK_MODE = "IS_DARK_MODE";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // =========================
    // 🔐 AUTH METHODS
    // =========================

    /** Save tokens after LOGIN / OTP LOGIN / REFRESH */
    public void saveAuth(String jwtToken, String refreshToken) {
        editor.putString(KEY_TOKEN, jwtToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.commit(); // Use commit() for immediate persistence
    }

    /** Save username (already language-specific from backend) */
    public void saveUserName(String userName) {
        editor.putString(KEY_USER_NAME, userName);
        editor.commit(); // Use commit() for immediate persistence
    }

    /** Save mobile number */
    public void saveUserMobile(String mobile) {
        editor.putString(KEY_USER_MOBILE, mobile);
        editor.commit();
    }

    /** Save user ID */
    public void saveUserId(String userId) {
        editor.putString(KEY_USER_ID, userId);
        editor.commit();
    }

    // 🆕 NEW METHOD: Save Roles (List -> String)
    public void saveUserRoles(List<String> roles) {
        String rolesString = "";
        if (roles != null && !roles.isEmpty()) {
            // Converts ["Farmer", "Admin"] to "Farmer, Admin"
            rolesString = TextUtils.join(", ", roles);
        }
        editor.putString(KEY_USER_ROLES, rolesString);
        editor.commit(); // Use commit() for immediate persistence
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public String getRefreshToken() {
        return pref.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "");
    }

    public String getUserMobile() {
        return pref.getString(KEY_USER_MOBILE, "N/A");
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, "");
    }

    // 🆕 NEW METHOD: Get Roles
    public String getUserRoles() {
        return pref.getString(KEY_USER_ROLES, "");
    }

    // =========================
    // 📋 SUBSCRIPTION CACHE
    // =========================

    /**
     * Call this after loading MySubscription from the server.
     * planName examples: "Equipment Listing", "Product Listing"
     * status examples:   "Active", "Expired"
     */
    public void saveSubscription(String planName, String status) {
        editor.putString(KEY_SUB_PLAN_NAME, planName != null ? planName : "");
        editor.putString(KEY_SUB_STATUS,    status    != null ? status    : "");
        editor.apply();
    }

    /** Returns true if the user has at least one Active subscription */
    public boolean hasActiveSubscription() {
        return "Active".equalsIgnoreCase(pref.getString(KEY_SUB_STATUS, ""));
    }

    /** Returns true if the user has the "Owner" role (Equipment Subscription) */
    public boolean hasEquipmentSubscription() {
        String roles = getUserRoles();
        return roles != null && roles.contains("Owner");
    }

    /** Returns true if the user has the "Seller" role (Product Subscription) */
    public boolean hasProductSubscription() {
        String roles = getUserRoles();
        return roles != null && roles.contains("Seller");
    }

    public String getSubscriptionPlanName() {
        return pref.getString(KEY_SUB_PLAN_NAME, "");
    }

    /** Auto-login check → ONLY refresh token matters */
    public boolean hasRefreshToken() {
        String token = getRefreshToken();
        return token != null && !token.isEmpty();
    }

    // =========================
    // 🌍 LANGUAGE METHODS
    // =========================

    public void setLanguage(String language) {
        editor.putString(KEY_LANGUAGE, language);
        editor.apply();
    }

    public String getLanguage() {
        return pref.getString(KEY_LANGUAGE, "en");
    }

    public void setLanguageSelected(boolean selected) {
        editor.putBoolean(KEY_LANGUAGE_SELECTED, selected);
        editor.apply();
    }

    public boolean isLanguageSelected() {
        return pref.getBoolean(KEY_LANGUAGE_SELECTED, false);
    }

    public void setDarkMode(boolean isDark) {
        editor.putBoolean(KEY_IS_DARK_MODE, isDark);
        editor.apply();
    }

    public boolean isDarkMode() {
        // Defaults to system setting originally, or false for light mode
        return pref.getBoolean(KEY_IS_DARK_MODE, false);
    }

    // =========================
    // 🚪 LOGOUT (SAFE)
    // =========================

    /**
     * Logout but KEEP language selection
     */
    public void logout() {

        String language = getLanguage();
        boolean languageSelected = isLanguageSelected();

        editor.clear();

        editor.putString(KEY_LANGUAGE, language);
        editor.putBoolean(KEY_LANGUAGE_SELECTED, languageSelected);
        // Keep subscription cache on logout? No — clear it.
        editor.apply();
    }
}