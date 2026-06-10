package com.example.agrirent.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.agrirent.R;
import com.example.agrirent.activities.ChangePasswordActivity;
import com.example.agrirent.activities.ComplaintListActivity;
import com.example.agrirent.activities.DashboardActivity;
import com.example.agrirent.activities.LoginActivity;
import com.example.agrirent.activities.MainActivity;
import com.example.agrirent.activities.SubscriptionActivity;
import com.example.agrirent.models.FcmTokenRequest;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.messaging.FirebaseMessaging;

import com.example.agrirent.models.MySubscription;
import com.example.agrirent.models.MySubscriptionResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private SessionManager session;
    private TextView tvUserName, tvUserRole, tvCurrentLanguage;
    private LinearLayout cardChangePassword;
    private LinearLayout cardSubscription;
    private LinearLayout cardDashboard;
    private LinearLayout cardMyComplaints;
    private LinearLayout cardLanguage;
    private SwitchMaterial switchDarkMode;
    private MaterialButton btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        session = new SessionManager(requireContext());

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserRole = view.findViewById(R.id.tvUserRole);
        tvCurrentLanguage = view.findViewById(R.id.tvCurrentLanguage);
        cardChangePassword = view.findViewById(R.id.cardChangePassword);
        cardSubscription   = view.findViewById(R.id.cardSubscription);
        cardDashboard      = view.findViewById(R.id.cardDashboard);
        cardMyComplaints   = view.findViewById(R.id.cardMyComplaints);
        cardLanguage       = view.findViewById(R.id.cardLanguage);
        switchDarkMode     = view.findViewById(R.id.switchDarkMode);
        btnLogout          = view.findViewById(R.id.btnLogout);

        switchDarkMode.setOnCheckedChangeListener(null);
        switchDarkMode.setChecked(session.isDarkMode());
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (session.isDarkMode() == isChecked) return; // Prevent infinite loops during view state restoration
            session.setDarkMode(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        loadUserInfo();

        // ── Subscription gate for Dashboard ─────────────────────────────
        applyDashboardGate();

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                applyDashboardGate();
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }

        cardChangePassword.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), ChangePasswordActivity.class));
        });

        cardSubscription.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), SubscriptionActivity.class));
        });

        if (cardMyComplaints != null) {
            cardMyComplaints.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), ComplaintListActivity.class)));
        }

        updateLanguageLabel(session.getLanguage());
        cardLanguage.setOnClickListener(v -> showLanguageDialog());

        btnLogout.setOnClickListener(v -> {
            session.logout();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }

    // Fetch live from server — fixes stale/empty cache for subscribed users
    private void applyDashboardGate() {
        // Apply cached state immediately (fast path — avoids blank lock on slow network)
        updateDashboardUI(session.hasActiveSubscription());

        // Then live-fetch to get the real up-to-date status
        ApiService api = ApiClient.getClient(requireContext()).create(ApiService.class);
        api.getMySubscriptions().enqueue(new Callback<MySubscriptionResponse>() {
            @Override
            public void onResponse(@NonNull Call<MySubscriptionResponse> call,
                                   @NonNull Response<MySubscriptionResponse> response) {
                if (!isAdded()) return;
                boolean hasActive = false;
                if (response.isSuccessful() && response.body() != null) {
                    java.util.List<MySubscription> subs = response.body().getData();
                    if (subs != null) {
                        for (MySubscription s : subs) {
                            if ("Active".equalsIgnoreCase(s.getStatus())) {
                                session.saveSubscription(s.getPlanName(), s.getStatus());
                                hasActive = true;
                                break;
                            }
                        }
                    }
                    if (!hasActive) session.saveSubscription("", "");
                }
                boolean finalHasActive = hasActive;
                requireActivity().runOnUiThread(() -> updateDashboardUI(finalHasActive));
            }

            @Override
            public void onFailure(@NonNull Call<MySubscriptionResponse> call, @NonNull Throwable t) {
                // Keep existing cached UI — do nothing
            }
        });
    }

    private void updateDashboardUI(boolean hasSub) {
        if (!isAdded() || cardDashboard == null) return;
        if (!hasSub) {
            cardDashboard.setAlpha(0.45f);
            cardDashboard.setOnClickListener(v -> {
                startActivity(new Intent(requireActivity(), SubscriptionActivity.class));
                android.widget.Toast.makeText(requireContext(),
                    "Subscribe to access your Dashboard", android.widget.Toast.LENGTH_SHORT).show();
            });
        } else {
            cardDashboard.setAlpha(1f);
            cardDashboard.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), DashboardActivity.class)));
        }
    }

    private void showLanguageDialog() {
        String[] labels = {"English", "हिन्दी (Hindi)", "ગુજરાતી (Gujarati)"};
        String[] codes  = {"en", "hi", "gu"};

        String current = session.getLanguage();
        int checked = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(current)) { checked = i; break; }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Language")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    String selectedCode = codes[which];
                    if (selectedCode.equals(session.getLanguage())) return; 

                    session.setLanguage(selectedCode);
                    syncLanguageToServer(selectedCode);

                    // Restart App to apply new locale
                    Intent restart = new Intent(requireActivity(), MainActivity.class);
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
                    requireContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            ApiService api = ApiClient.getClient(requireContext()).create(ApiService.class);
            api.updateFcmToken(new FcmTokenRequest(token, lang, deviceId)).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    Log.d(TAG, "Language synced to server: " + lang);
                }
                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
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
