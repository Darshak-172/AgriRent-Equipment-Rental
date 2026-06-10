package com.example.agrirent.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.agrirent.R;
import com.example.agrirent.adapters.OrderAdapter;
import com.example.agrirent.models.OrderResponseDto;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductOrdersTabFragment extends Fragment {

    private SessionManager session;
    private ApiService apiService;
    private OrderAdapter adapter;

    private RecyclerView rvOrders;
    private ProgressBar pbLoading;
    private LinearLayout llEmptyState;
    private TabLayout tabLayoutProductOrders;
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<OrderResponseDto> orderList = new ArrayList<>();
    private boolean isSellerRole = false;
    private boolean isSellerView = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_orders_tab, container, false);

        session = new SessionManager(requireContext());
        apiService = ApiClient.getClient(requireContext()).create(ApiService.class);
        isSellerRole = session.getUserRoles().contains("Seller");
        
        isSellerView = false;

        initViews(view);
        setupTabs();
        loadOrders();

        return view;
    }

    private void initViews(View view) {
        rvOrders = view.findViewById(R.id.rvOrders);
        pbLoading = view.findViewById(R.id.pbLoading);
        llEmptyState = view.findViewById(R.id.llEmptyState);
        tabLayoutProductOrders = view.findViewById(R.id.tabLayoutProductOrders);

        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrderAdapter(requireContext(), orderList, isSellerView);
        rvOrders.setAdapter(adapter);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> {
                if (rvOrders != null && rvOrders.getVisibility() == View.VISIBLE) {
                    return rvOrders.canScrollVertically(-1);
                }
                return false;
            });
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadOrders();
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }
    }

    private void setupTabs() {
        if (isSellerRole) {
            tabLayoutProductOrders.setVisibility(View.VISIBLE);
            TabLayout.Tab tabMyOrders = tabLayoutProductOrders.newTab().setText("My Orders");
            TabLayout.Tab tabReceived = tabLayoutProductOrders.newTab().setText("Received Orders");
            
            tabLayoutProductOrders.addTab(tabMyOrders);
            tabLayoutProductOrders.addTab(tabReceived);

            tabLayoutProductOrders.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    isSellerView = (tab.getPosition() == 1);
                    loadOrders();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        } else {
            tabLayoutProductOrders.setVisibility(View.GONE);
        }
    }

    private void loadOrders() {
        showLoading();

        Call<List<OrderResponseDto>> call = isSellerView ? apiService.getSellerOrders() : apiService.getMyProductOrders();

        call.enqueue(new Callback<List<OrderResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<OrderResponseDto>> call, @NonNull Response<List<OrderResponseDto>> response) {
                if (!isAdded()) return;
                hideLoading();
                
                if (response.isSuccessful() && response.body() != null) {
                    orderList.clear();
                    orderList.addAll(response.body());
                    
                    // Update adapter state
                    adapter = new OrderAdapter(requireContext(), orderList, isSellerView);
                    rvOrders.setAdapter(adapter);

                    if (orderList.isEmpty()) {
                        showEmptyState();
                    } else {
                        rvOrders.setVisibility(View.VISIBLE);
                        llEmptyState.setVisibility(View.GONE);
                    }
                } else {
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<OrderResponseDto>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                hideLoading();
                showEmptyState();
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading() {
        pbLoading.setVisibility(View.VISIBLE);
        rvOrders.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        pbLoading.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        llEmptyState.setVisibility(View.VISIBLE);
        rvOrders.setVisibility(View.GONE);
    }
}
