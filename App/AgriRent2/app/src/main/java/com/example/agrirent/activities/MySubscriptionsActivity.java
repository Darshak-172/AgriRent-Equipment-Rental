package com.example.agrirent.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Handler;
import android.os.Looper;

import com.example.agrirent.R;
import com.example.agrirent.models.MySubscription;
import com.example.agrirent.models.MySubscriptionResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shows the user's subscription history with premium card UI.
 */
public class MySubscriptionsActivity extends BaseActivity {

    private LinearLayout    llLoading, llEmpty, llSubscriptions;
    private NestedScrollView scrollView;
    private MaterialButton  btnBuyNow;
    private SwipeRefreshLayout swipeRefreshLayout;

    private SessionManager session;
    private ApiService     api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_subscriptions);

        session = new SessionManager(this);
        api     = ApiClient.getClient(this).create(ApiService.class);

        // Back
        View ivBack = findViewById(R.id.ivBack);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        // + Get Plan
        TextView tvGet = findViewById(R.id.tvGetPlans);
        if (tvGet != null) {
            tvGet.setOnClickListener(v -> {
                startActivity(new Intent(this, SubscriptionActivity.class));
                finish();
            });
        }

        // 🧾 History
        TextView tvHistory = findViewById(R.id.tvPayHistory);
        if (tvHistory != null) {
            tvHistory.setOnClickListener(v ->
                    startActivity(new Intent(this, PaymentHistoryActivity.class)));
        }

        llLoading       = findViewById(R.id.llLoading);
        llEmpty         = findViewById(R.id.llEmpty);
        llSubscriptions = findViewById(R.id.llSubscriptions);
        scrollView      = findViewById(R.id.scrollView);
        btnBuyNow       = findViewById(R.id.btnBuyNow);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadSubscriptions();
                new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }

        if (btnBuyNow != null) {
            btnBuyNow.setOnClickListener(v -> {
                startActivity(new Intent(this, SubscriptionActivity.class));
                finish();
            });
        }

        loadSubscriptions();
    }

    private void loadSubscriptions() {
        setState("loading");

        api.getMySubscriptions().enqueue(new Callback<MySubscriptionResponse>() {
            @Override
            public void onResponse(Call<MySubscriptionResponse> call,
                                   Response<MySubscriptionResponse> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess()) {
                    List<MySubscription> list = res.body().getData();
                    if (list != null && !list.isEmpty()) {
                        buildCards(list);
                        setState("list");
                    } else {
                        setState("empty");
                    }
                } else if (res.code() == 401) {
                    Toast.makeText(MySubscriptionsActivity.this,
                            "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
                    setState("empty");
                } else {
                    Toast.makeText(MySubscriptionsActivity.this,
                            "Failed to load subscriptions (HTTP " + res.code() + ")",
                            Toast.LENGTH_SHORT).show();
                    setState("empty");
                }
            }

            @Override
            public void onFailure(Call<MySubscriptionResponse> call, Throwable t) {
                Toast.makeText(MySubscriptionsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                setState("empty");
            }
        });
    }

    private void buildCards(List<MySubscription> list) {
        llSubscriptions.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);

        for (int i = 0; i < list.size(); i++) {
            MySubscription sub = list.get(i);
            View card = inf.inflate(R.layout.item_my_subscription, llSubscriptions, false);

            // Plan name
            setText(card, R.id.tvSubPlanName,
                    sub.getPlanName() != null ? sub.getPlanName() : "Plan");

            // ID
            setText(card, R.id.tvSubId, "Subscription #" + sub.getSubscriptionId());

            // Status badge
            TextView badge = card.findViewById(R.id.tvSubStatus);
            if (badge != null) {
                boolean active = "active".equalsIgnoreCase(sub.getStatus());
                String label   = active ? "● Active" : "● " + sub.getStatus();
                int bgColor    = active ? Color.parseColor("#E8F5E9")
                                        : Color.parseColor("#FFF3E0");
                int txtColor   = active ? Color.parseColor("#2E7D32")
                                        : Color.parseColor("#E65100");
                badge.setText(label);
                badge.setTextColor(txtColor);
                badge.setBackgroundColor(Color.TRANSPARENT);
                // We keep bg_price_tag drawable; just change text/text-color
            }

            // Dates
            setText(card, R.id.tvSubStart, formatDate(sub.getStartDate()));
            setText(card, R.id.tvSubEnd,   formatDate(sub.getEndDate()));

            // Stagger animation
            final int delay = i * 80;
            card.setAlpha(0f);
            card.setTranslationY(30f);
            card.animate().alpha(1f).translationY(0f)
                    .setStartDelay(delay).setDuration(350)
                    .setInterpolator(new OvershootInterpolator(0.8f))
                    .start();

            llSubscriptions.addView(card);
        }
    }

    private void setState(String s) {
        runOnUiThread(() -> {
            if ("loading".equals(s) && (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing())) {
                if (llLoading  != null) llLoading.setVisibility(View.VISIBLE);
            } else if (!"loading".equals(s)) {
                if (llLoading  != null) llLoading.setVisibility(View.GONE);
            }
            if (llEmpty    != null) llEmpty.setVisibility("empty".equals(s)     ? View.VISIBLE : View.GONE);
            if (scrollView != null) scrollView.setVisibility("list".equals(s)   ? View.VISIBLE : View.GONE);
        });
    }

    private void setText(View root, int id, String text) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private String formatDate(String iso) {
        if (iso == null || iso.isEmpty()) return "—";
        try {
            String d  = iso.contains("T") ? iso.split("T")[0] : iso;
            String[] p  = d.split("-");
            String[] mo = {"","Jan","Feb","Mar","Apr","May","Jun",
                              "Jul","Aug","Sep","Oct","Nov","Dec"};
            return p[2] + " " + mo[Integer.parseInt(p[1])] + " " + p[0];
        } catch (Exception e) { return iso; }
    }
}
