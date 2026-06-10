package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Handler;
import android.os.Looper;

import com.example.agrirent.R;
import com.example.agrirent.adapters.BookingAdapter;
import com.example.agrirent.models.BookingResponseDto;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.LocaleHelper;
import com.example.agrirent.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingsActivity extends BaseActivity implements BookingAdapter.BookingActionListener {

    private SessionManager session;
    private ApiService apiService;
    private BookingAdapter adapter;

    private RecyclerView rvBookings;
    private ProgressBar pbLoading;
    private LinearLayout llEmptyState;
    private BottomNavigationView bottomNavigation;
    private TabLayout tabLayoutBookings;
    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean isOwnerRole = false;
    private boolean isOwnerView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        session = new SessionManager(this);
        LocaleHelper.applyLanguage(this, session.getLanguage());
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        isOwnerRole = session.getUserRoles().contains("Owner") || session.getUserRoles().contains("EquipmentOwner");
        
        // Default to showing "My Bookings" (Requests I made)
        isOwnerView = false;

        initViews();
        setupTabs();
        setupBottomNavigation();
        loadBookings();
    }

    private void initViews() {
        rvBookings = findViewById(R.id.rvBookings);
        pbLoading = findViewById(R.id.pbLoading);
        llEmptyState = findViewById(R.id.llEmptyState);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tabLayoutBookings = findViewById(R.id.tabLayoutBookings);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadBookings();
                new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        // The adapter is recreated dynamically when toggling views.
    }

    private void setupTabs() {
        if (isOwnerRole) {
            tabLayoutBookings.setVisibility(View.VISIBLE);
            tabLayoutBookings.addTab(tabLayoutBookings.newTab().setText("My Bookings"));
            tabLayoutBookings.addTab(tabLayoutBookings.newTab().setText("Received Requests"));

            tabLayoutBookings.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    isOwnerView = (tab.getPosition() == 1);
                    loadBookings();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        } else {
            tabLayoutBookings.setVisibility(View.GONE);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_bookings);
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_bookings) {
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_equipment) {
                startActivity(new Intent(this, AllEquipmentActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_add_listing) {
                Toast.makeText(this, "Add Listing coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_products) {
                Toast.makeText(this, "Products page coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void loadBookings() {
        showLoading();

        // Recreate adapter when switching views
        adapter = new BookingAdapter(this, new ArrayList<>(), isOwnerView, this);
        rvBookings.setAdapter(adapter);

        Call<List<BookingResponseDto>> call = isOwnerView ? apiService.getOwnerRequests() : apiService.getMyBookings();

        call.enqueue(new Callback<List<BookingResponseDto>>() {
            @Override
            public void onResponse(Call<List<BookingResponseDto>> call, Response<List<BookingResponseDto>> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    List<BookingResponseDto> list = response.body();
                    if (list.isEmpty()) {
                        showEmptyState();
                    } else {
                        showBookings(list);
                    }
                } else {
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<List<BookingResponseDto>> call, Throwable t) {
                hideLoading();
                showEmptyState();
                Toast.makeText(BookingsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading() {
        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            pbLoading.setVisibility(View.VISIBLE);
        }
        rvBookings.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        pbLoading.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        llEmptyState.setVisibility(View.VISIBLE);
        rvBookings.setVisibility(View.GONE);
    }

    private void showBookings(List<BookingResponseDto> list) {
        llEmptyState.setVisibility(View.GONE);
        rvBookings.setVisibility(View.VISIBLE);
        adapter.updateData(list);
    }

    @Override
    public void onActionSuccess() {
        loadBookings(); // Reload list after accept/reject
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_bookings);
    }
}
