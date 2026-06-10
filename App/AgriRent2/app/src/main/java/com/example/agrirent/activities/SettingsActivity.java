package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.agrirent.R;
import com.example.agrirent.models.FcmTokenRequest;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.LocaleHelper;
import com.example.agrirent.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity";

    private SessionManager session;
    private TextView tvUserName, tvUserRole, tvCurrentLanguage;
    private MaterialCardView cardChangePassword;
    private MaterialCardView cardSubscription;
    private MaterialCardView cardDashboard;
    private MaterialCardView cardLanguage;
    private SwitchMaterial switchDarkMode;
    private MaterialButton btnLogout;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize session manager
        session = new SessionManager(this);

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserRole = findViewById(R.id.tvUserRole);
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage);
        cardChangePassword = findViewById(R.id.cardChangePassword);
        cardSubscription   = findViewById(R.id.cardSubscription);
        cardDashboard      = findViewById(R.id.cardDashboard);
        cardLanguage       = findViewById(R.id.cardLanguage);
        switchDarkMode     = findViewById(R.id.switchDarkMode);
        btnLogout          = findViewById(R.id.btnLogout);

        // Configure Dark Mode Switch
        switchDarkMode.setChecked(session.isDarkMode());
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            session.setDarkMode(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Load user info
        loadUserInfo();

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Change Password
        cardChangePassword.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ChangePasswordActivity.class));
        });

        // Subscription
        cardSubscription.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, SubscriptionActivity.class));
        });

        // Dashboard
        cardDashboard.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, DashboardActivity.class));
        });

        // Language
        updateLanguageLabel(session.getLanguage());
        cardLanguage.setOnClickListener(v -> showLanguageDialog());

        // Logout
        btnLogout.setOnClickListener(v -> {
            session.logout();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showLanguageDialog() {
        String[] labels = {"English", "हिन्दी (Hindi)", "ગુજરાતી (Gujarati)"};
        String[] codes  = {"en", "hi", "gu"};

        String current = session.getLanguage();
        int checked = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(current)) { checked = i; break; }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    String selectedCode = codes[which];
                    if (selectedCode.equals(session.getLanguage())) return; // no change

                    session.setLanguage(selectedCode);
                    syncLanguageToServer(selectedCode);

                    // Clear full back stack and restart app from the main shell
                    // so every Activity gets the new locale via BaseActivity.attachBaseContext
                    Intent restart = new Intent(SettingsActivity.this, MainActivity.class);
                    restart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(restart);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateLanguageLabel(String code) {
        String label;
        switch (code) {
            case "hi": label = "हिन्दी (Hindi)"; break;
            case "gu": label = "ગુજરાતી (Gujarati)"; break;
            default:   label = "English"; break;
        }
        tvCurrentLanguage.setText(label);
    }

    private void syncLanguageToServer(String lang) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token == null) return;
            String deviceId = android.provider.Settings.Secure.getString(
                    getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            ApiService api = ApiClient.getClient(this).create(ApiService.class);
            api.updateFcmToken(new FcmTokenRequest(token, lang, deviceId)).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    Log.d(TAG, "Language synced to server: " + lang);
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e(TAG, "Language sync failed: " + t.getMessage());
                }
            });
        });
    }

    private void loadUserInfo() {
        String name = session.getUserName();
        String roles = session.getUserRoles();

        if (name != null && !name.isEmpty()) {
            tvUserName.setText(name);
        } else {
            tvUserName.setText("User");
        }

        if (roles != null && !roles.isEmpty()) {
            tvUserRole.setText(roles);
        } else {
            tvUserRole.setText("Member");
        }
    }
}
