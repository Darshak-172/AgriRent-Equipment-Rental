package com.example.agrirent.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Handler;
import android.os.Looper;

import com.example.agrirent.R;
import com.example.agrirent.adapters.OrderAdapter;
import com.example.agrirent.models.OrderResponseDto;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersActivity extends BaseActivity {

    private RecyclerView rvOrders;
    private LinearLayout llEmptyState;
    private ImageButton btnBack;
    private SwipeRefreshLayout swipeRefreshLayout;

    private OrderAdapter adapter;
    private List<OrderResponseDto> orderList = new ArrayList<>();
    private boolean isSeller = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        SharedPreferences prefs = getSharedPreferences("AgriRentPrefs", Context.MODE_PRIVATE);
        String role = prefs.getString("Role", "");
        isSeller = "Seller".equals(role);

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void initViews() {
        rvOrders = findViewById(R.id.rvOrders);
        llEmptyState = findViewById(R.id.llEmptyState);
        btnBack = findViewById(R.id.btnBack);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadOrders();
                new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }

        btnBack.setOnClickListener(v -> onBackPressed());

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(this, orderList, isSeller);
        rvOrders.setAdapter(adapter);
    }

    private void loadOrders() {
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        Call<List<OrderResponseDto>> call;

        if (isSeller) {
            call = apiService.getSellerOrders();
        } else {
            call = apiService.getMyProductOrders();
        }

        call.enqueue(new Callback<List<OrderResponseDto>>() {
            @Override
            public void onResponse(Call<List<OrderResponseDto>> call, Response<List<OrderResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    orderList.clear();
                    orderList.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (orderList.isEmpty()) {
                        rvOrders.setVisibility(View.GONE);
                        llEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvOrders.setVisibility(View.VISIBLE);
                        llEmptyState.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(OrdersActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<OrderResponseDto>> call, Throwable t) {
                Toast.makeText(OrdersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
