package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.agrirent.R;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.SessionManager;
import com.example.agrirent.models.BookingResponseDto;
import com.example.agrirent.models.OrderResponseDto;
import com.google.android.material.card.MaterialCardView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DashboardActivity extends BaseActivity {

    private SessionManager session;
    private ImageView btnBack, btnLogout;
    private TextView tvGreeting, tvUserSub, tvEarningsCount, tvBookingsCount, tvOrdersCount;
    private android.view.View cardManageEquipment, cardManageProducts;
    private com.google.android.material.card.MaterialCardView cardVerifyKYC, cardComplaints, cardMyReviews;
    private android.view.View cardMaintenance;
    private com.example.agrirent.views.SparklineView sparklineRevenue;
    private com.google.android.material.chip.ChipGroup cgSeries, cgTime;
    private TextView tvAnalyticsTitle, tvGrowthPercent, tvInsightSummary, tvReceivedLabel;
    private android.view.View llReceivedGrid;
    private com.google.android.material.card.MaterialCardView cardReceivedComplaints, cardReceivedReviews;
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<BookingResponseDto> ownerBookings = new java.util.ArrayList<>();
    private List<OrderResponseDto> sellerOrders = new java.util.ArrayList<>();
    private List<BookingResponseDto> myBookings = new java.util.ArrayList<>();
    private List<OrderResponseDto> myOrders = new java.util.ArrayList<>();

    private double totalRevenueBookings = 0;
    private double totalRevenueOrders = 0;
    private double totalSpend = 0;

    private boolean isOwner = false;
    private boolean isSeller = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        session = new SessionManager(this);

        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvUserSub = findViewById(R.id.tvUserSub);
        tvEarningsCount = findViewById(R.id.tvEarningsCount);
        tvBookingsCount = findViewById(R.id.tvBookingsCount);
        tvOrdersCount = findViewById(R.id.tvOrdersCount);
        cardManageEquipment = findViewById(R.id.cardManageEquipment);
        cardManageProducts = findViewById(R.id.cardManageProducts);
        
        cardVerifyKYC = findViewById(R.id.cardVerifyKYC);
        cardComplaints = findViewById(R.id.cardComplaints);
        cardMyReviews = findViewById(R.id.cardMyReviews);

        sparklineRevenue = findViewById(R.id.sparklineRevenue);
        cgSeries = findViewById(R.id.cgSeries);
        cgTime = findViewById(R.id.cgTime);
        tvAnalyticsTitle = findViewById(R.id.tvAnalyticsTitle);
        tvGrowthPercent = findViewById(R.id.tvGrowthPercent);
        tvInsightSummary = findViewById(R.id.tvInsightSummary);
        tvReceivedLabel = findViewById(R.id.tvReceivedLabel);
        llReceivedGrid = findViewById(R.id.llReceivedGrid);
        cardReceivedComplaints = findViewById(R.id.cardReceivedComplaints);
        cardReceivedReviews = findViewById(R.id.cardReceivedReviews);

        // Personalized Greeting
        String name = session.getUserName();
        if (name != null && !name.isEmpty()) {
            tvGreeting.setText("Hello, " + name + "!");
        }

        // Logout
        btnLogout.setOnClickListener(v -> {
            session.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Handle Back Navigation
        btnBack.setOnClickListener(v -> finish());

        // Parse Roles
        parseRoles();
        
        // Setup UI feedback based on roles
        setupUI();

        // Setup Swipe to Refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                fetchDashboardStats();
                new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }

        // Setup Card Listeners
        if (cardManageEquipment != null) {
            cardManageEquipment.setOnClickListener(v -> {
                if (isOwner) {
                    android.content.Intent intent = new android.content.Intent(DashboardActivity.this, ManageEquipmentActivity.class);
                    startActivity(intent);
                } else {
                    showAccessDialog(
                            "Equipment Owner access is required to manage fleet listings.",
                            "View Plans",
                            new Intent(DashboardActivity.this, SubscriptionActivity.class));
                }
            });
        }
        
        if (cardManageProducts != null) {
            cardManageProducts.setOnClickListener(v -> {
                if (isSeller) {
                    Intent intent = new Intent(DashboardActivity.this, ManageProductActivity.class);
                    startActivity(intent);
                } else {
                    showAccessDialog(
                            "Seller access is required to manage product inventory and orders.",
                            "View Plans",
                            new Intent(DashboardActivity.this, SubscriptionActivity.class));
                }
            });
        }

        // Setup Real-World Insights
        setupInsights();
        
        // Initial Fetch
        fetchDashboardStats();
    }

    private void setupInsights() {
        // Keep a stable baseline while network data is loading.
        if (sparklineRevenue != null) {
            sparklineRevenue.setData(new float[]{0f, 0f, 0f, 0f, 0f, 0f, 0f});
        }

        // Support hub actions
        if (cardVerifyKYC != null) {
            cardVerifyKYC.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, MySubscriptionsActivity.class)));
        }

        if (cardComplaints != null) {
            cardComplaints.setOnClickListener(v -> {
                startActivity(new android.content.Intent(DashboardActivity.this, ComplaintListActivity.class));
            });
        }

        if (cardMyReviews != null) {
            cardMyReviews.setOnClickListener(v -> {
                startActivity(new android.content.Intent(DashboardActivity.this, MyReviewsActivity.class));
            });
        }

        if (cardReceivedComplaints != null) {
            cardReceivedComplaints.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, IncomingComplaintsActivity.class));
            });
        }

        if (cardReceivedReviews != null) {
            cardReceivedReviews.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, IncomingReviewsActivity.class));
            });
        }

        if (cardMaintenance != null) {
            cardMaintenance.setOnClickListener(v -> {
                if (isOwner) {
                    startActivity(new Intent(DashboardActivity.this, ManageEquipmentActivity.class));
                } else if (isSeller) {
                    startActivity(new Intent(DashboardActivity.this, ManageProductActivity.class));
                } else {
                    showAccessDialog(
                            "Upgrade your plan to unlock the maintenance center and inventory tools.",
                            "Upgrade Now",
                            new Intent(DashboardActivity.this, SubscriptionActivity.class));
                }
            });
        }

        // 3. Setup Chip Listeners
        if (cgSeries != null) {
            cgSeries.setOnCheckedChangeListener((group, checkedId) -> updateRevenueUI());
        }
        if (cgTime != null) {
            cgTime.setOnCheckedChangeListener((group, checkedId) -> updateRevenueUI());
        }
    }

    private void fetchDashboardStats() {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);

        // 1. EARNINGS: Fetch Owner Booking Requests
        api.getOwnerRequests().enqueue(new Callback<List<BookingResponseDto>>() {
            @Override
            public void onResponse(Call<List<BookingResponseDto>> call, Response<List<BookingResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ownerBookings = response.body();
                    updateRevenueUI();
                }
            }
            @Override public void onFailure(Call<List<BookingResponseDto>> call, Throwable t) {}
        });

        // 2. EARNINGS: Fetch Seller Product Orders
        api.getSellerOrders().enqueue(new Callback<List<OrderResponseDto>>() {
            @Override
            public void onResponse(Call<List<OrderResponseDto>> call, Response<List<OrderResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sellerOrders = response.body();
                    updateRevenueUI();
                }
            }
            @Override public void onFailure(Call<List<OrderResponseDto>> call, Throwable t) {}
        });

        // 3. SPEND: Fetch User's own Bookings
        api.getMyBookings().enqueue(new Callback<List<BookingResponseDto>>() {
            @Override
            public void onResponse(Call<List<BookingResponseDto>> call, Response<List<BookingResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    myBookings = response.body();
                    tvBookingsCount.setText(String.valueOf(myBookings.size()));
                    updateRevenueUI();
                }
            }
            @Override public void onFailure(Call<List<BookingResponseDto>> call, Throwable t) {}
        });

        // 4. SPEND: Fetch User's own Product Orders
        api.getMyProductOrders().enqueue(new Callback<List<OrderResponseDto>>() {
            @Override
            public void onResponse(Call<List<OrderResponseDto>> call, Response<List<OrderResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    myOrders = response.body();
                    tvOrdersCount.setText(String.valueOf(myOrders.size()));
                    updateRevenueUI();
                }
            }
            @Override public void onFailure(Call<List<OrderResponseDto>> call, Throwable t) {}
        });
    }

    private void updateRevenueUI() {
        double displayTotal = 0;
        String title = "Total Earnings";
        int checkedSeries = cgSeries != null ? cgSeries.getCheckedChipId() : R.id.chipAll;
        int checkedTime = cgTime != null ? cgTime.getCheckedChipId() : R.id.chipWeek;

        List<TrendPoint> trendPoints = new java.util.ArrayList<>();
        long cutoffTime = getTimeCutoff(checkedTime);

        if (checkedSeries == R.id.chipSpend) {
            title = "Total Spending";
            for (BookingResponseDto b : myBookings) {
                if (isWithinTime(b.getStartDate(), cutoffTime)) {
                    displayTotal += b.getTotalPrice();
                    addTrendPoint(trendPoints, b.getStartDate(), b.getTotalPrice());
                }
            }
            for (OrderResponseDto o : myOrders) {
                if (isWithinTime(o.getOrderDate(), cutoffTime)) {
                    displayTotal += o.getTotalPrice();
                    addTrendPoint(trendPoints, o.getOrderDate(), o.getTotalPrice());
                }
            }
        } else if (checkedSeries == R.id.chipEquip) {
            title = "Equip. Revenue";
            for (BookingResponseDto b : ownerBookings) {
                if (b.getStatus() != null) {
                    String stat = b.getStatus().trim().toLowerCase();
                    if ((stat.contains("accept") || stat.contains("approve")) && isWithinTime(b.getStartDate(), cutoffTime)) {
                        displayTotal += b.getTotalPrice();
                        addTrendPoint(trendPoints, b.getStartDate(), b.getTotalPrice());
                    }
                }
            }
        } else if (checkedSeries == R.id.chipProd) {
            title = "Prod. Revenue";
            for (OrderResponseDto o : sellerOrders) {
                if (o.getStatus() != null) {
                    String stat = o.getStatus().trim().toLowerCase();
                    if (stat.contains("deliver") && isWithinTime(o.getOrderDate(), cutoffTime)) {
                        displayTotal += o.getTotalPrice();
                        addTrendPoint(trendPoints, o.getOrderDate(), o.getTotalPrice());
                    }
                }
            }
        } else {
            // Total Earnings (All)
            title = "Total Earnings";
            for (BookingResponseDto b : ownerBookings) {
                if (b.getStatus() != null) {
                    String stat = b.getStatus().trim().toLowerCase();
                    if ((stat.contains("accept") || stat.contains("approve")) && isWithinTime(b.getStartDate(), cutoffTime)) {
                        displayTotal += b.getTotalPrice();
                        addTrendPoint(trendPoints, b.getStartDate(), b.getTotalPrice());
                    }
                }
            }
            for (OrderResponseDto o : sellerOrders) {
                if (o.getStatus() != null) {
                    String stat = o.getStatus().trim().toLowerCase();
                    if (stat.contains("deliver") && isWithinTime(o.getOrderDate(), cutoffTime)) {
                        displayTotal += o.getTotalPrice();
                        addTrendPoint(trendPoints, o.getOrderDate(), o.getTotalPrice());
                    }
                }
            }
        }

        List<Float> trendData = toOrderedTrend(trendPoints);

        if (tvAnalyticsTitle != null) tvAnalyticsTitle.setText(title);
        if (tvEarningsCount != null) tvEarningsCount.setText("₹" + String.format("%.0f", displayTotal));

        if (tvGrowthPercent != null) {
            float growth = 0f;
            if (trendData.size() >= 2) {
                float first = trendData.get(0);
                float last = trendData.get(trendData.size() - 1);
                if (Math.abs(first) > 0.001f) {
                    growth = ((last - first) / Math.abs(first)) * 100f;
                } else if (Math.abs(last) > 0.001f) {
                    growth = 100f;
                }
            }

            String growthText;
            if (trendData.size() < 2) {
                growthText = "No trend yet";
            } else {
                growthText = (growth >= 0 ? "+" : "") + String.format("%.1f", growth) + "% trend";
            }
            tvGrowthPercent.setText(growthText);
            tvGrowthPercent.setTextColor(growth >= 0 ? 0xFF0df26c : 0xFFFF5252);
        }

        updateInsightSummary();
        
        if (sparklineRevenue != null) {
            if (trendData.isEmpty()) trendData.add(0f);
            float[] floats = new float[Math.max(5, trendData.size())];
            for (int i = 0; i < trendData.size() && i < floats.length; i++) floats[i] = trendData.get(i);
            sparklineRevenue.setData(floats);
            
            if (checkedSeries == R.id.chipSpend) {
                sparklineRevenue.setChartColor(0xFFFF5252); // Red
            } else {
                sparklineRevenue.setChartColor(0xFF0df26c); // Neon Green
            }
        }
    }

    private void addTrendPoint(List<TrendPoint> points, String dateStr, double amount) {
        long ts = parseDateToMillis(dateStr);
        if (ts <= 0) ts = System.currentTimeMillis();
        points.add(new TrendPoint(ts, (float) amount));
    }

    private List<Float> toOrderedTrend(List<TrendPoint> points) {
        points.sort((a, b) -> Long.compare(a.timeMillis, b.timeMillis));
        List<Float> ordered = new java.util.ArrayList<>();
        for (TrendPoint p : points) {
            ordered.add(p.amount);
        }
        // Keep latest 14 points for readability.
        if (ordered.size() > 14) {
            ordered = ordered.subList(ordered.size() - 14, ordered.size());
        }
        return ordered;
    }

    private long parseDateToMillis(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return -1;
        String clean = dateStr.trim();

        String[] formats = new String[] {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        };

        for (String f : formats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(f, java.util.Locale.US);
                sdf.setLenient(true);
                java.util.Date date = sdf.parse(clean);
                if (date != null) return date.getTime();
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    private long getTimeCutoff(int chipId) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            if (chipId == R.id.chipMonth) {
                cal.add(java.util.Calendar.MONTH, -1);
            } else if (chipId == R.id.chipYear) {
                cal.add(java.util.Calendar.YEAR, -1);
            } else {
                cal.add(java.util.Calendar.DAY_OF_YEAR, -7); // Defaults to week correctly even if -1
            }
            return cal.getTimeInMillis();
    }

    private boolean isWithinTime(String dateStr, long cutoff) {
        long ts = parseDateToMillis(dateStr);
        return ts > 0 && ts >= cutoff;
    }

    private static class TrendPoint {
        final long timeMillis;
        final float amount;

        TrendPoint(long timeMillis, float amount) {
            this.timeMillis = timeMillis;
            this.amount = amount;
        }
    }

    private void parseRoles() {
        String roles = session.getUserRoles();
        if (roles != null) {
            String lowerRoles = roles.toLowerCase();
            isOwner = lowerRoles.contains("equipment owner") || lowerRoles.contains("owner") || lowerRoles.contains("equipmentowner");
            isSeller = lowerRoles.contains("seller");
        }
    }

    private void showAccessDialog(String message, String actionLabel, Intent actionIntent) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Access Required")
                .setMessage(message)
                .setPositiveButton(actionLabel, (dialog, which) -> startActivity(actionIntent))
                .setNegativeButton("Not Now", null)
                .show();
    }

    private int calculateTrustScore() {
        int score = 50;
        if (isOwner) score += 12;
        if (isSeller) score += 12;
        if (session.hasActiveSubscription()) score += 15;
        if (!ownerBookings.isEmpty()) score += 6;
        if (!sellerOrders.isEmpty()) score += 6;
        if (!myBookings.isEmpty()) score += 4;
        if (!myOrders.isEmpty()) score += 4;
        return Math.min(score, 100);
    }

    private void updateInsightSummary() {
        if (tvInsightSummary == null) return;

        int approvedBookings = 0;
        for (BookingResponseDto booking : ownerBookings) {
            String status = booking.getStatus() != null ? booking.getStatus().toLowerCase() : "";
            if (status.contains("accept") || status.contains("approve") || status.contains("deliver")) {
                approvedBookings++;
            }
        }

        int deliveredOrders = 0;
        int canceledOrders = 0;
        for (OrderResponseDto order : sellerOrders) {
            String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
            if (status.contains("deliver")) deliveredOrders++;
            if (status.contains("cancel")) canceledOrders++;
        }

        float bookingConversion = ownerBookings.isEmpty() ? 0f : (approvedBookings * 100f / ownerBookings.size());
        float orderConversion = sellerOrders.isEmpty() ? 0f : (deliveredOrders * 100f / sellerOrders.size());
        tvInsightSummary.setText(String.format(java.util.Locale.getDefault(),
                "Trust %d/100  •  Bookings %d%%  •  Orders %d%%  •  Canceled %d",
                calculateTrustScore(), Math.round(bookingConversion), Math.round(orderConversion), canceledOrders));
    }

    private void setupUI() {
        if (tvUserSub != null) {
            String roleLabel;
            if (isOwner && isSeller) {
                roleLabel = "Owner + Seller";
            } else if (isOwner) {
                roleLabel = "Equipment Owner";
            } else if (isSeller) {
                roleLabel = "Seller";
            } else {
                roleLabel = "Farmer";
            }
            tvUserSub.setText(roleLabel + " • Trust " + calculateTrustScore() + "/100");
        }

        updateInsightSummary();

        if (!isOwner && cardManageEquipment != null) {
            cardManageEquipment.setAlpha(0.6f);
            ImageView ivEquipArrow = findViewById(R.id.ivEquipArrow);
            if (ivEquipArrow != null) {
                ivEquipArrow.setImageResource(R.drawable.ic_lock);
                ivEquipArrow.setColorFilter(android.graphics.Color.parseColor("#E74C3C"));
            }
        }
        
        if (!isSeller && cardManageProducts != null) {
            cardManageProducts.setAlpha(0.6f);
            ImageView ivProdArrow = findViewById(R.id.ivProdArrow);
            if (ivProdArrow != null) {
                ivProdArrow.setImageResource(R.drawable.ic_lock);
                ivProdArrow.setColorFilter(android.graphics.Color.parseColor("#E74C3C"));
            }
        }

        // Show Received Feedbacks only for Sellers/Owners
        if (isOwner || isSeller) {
            if (tvReceivedLabel != null) tvReceivedLabel.setVisibility(android.view.View.VISIBLE);
            if (llReceivedGrid != null) llReceivedGrid.setVisibility(android.view.View.VISIBLE);
        } else {
            if (tvReceivedLabel != null) tvReceivedLabel.setVisibility(android.view.View.GONE);
            if (llReceivedGrid != null) llReceivedGrid.setVisibility(android.view.View.GONE);
        }
    }
}
