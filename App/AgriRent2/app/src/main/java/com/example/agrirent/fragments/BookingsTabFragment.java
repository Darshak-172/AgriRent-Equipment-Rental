package com.example.agrirent.fragments;

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
import com.example.agrirent.adapters.BookingAdapter;
import com.example.agrirent.models.BookingResponseDto;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingsTabFragment extends Fragment implements BookingAdapter.BookingActionListener {

    private SessionManager session;
    private ApiService apiService;
    private BookingAdapter adapter;

    private RecyclerView rvBookings;
    private ProgressBar pbLoading;
    private LinearLayout llEmptyState;
    private TabLayout tabLayoutBookings;
    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean isOwnerRole = false;
    private boolean isOwnerView = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookings_tab, container, false);

        session = new SessionManager(requireContext());
        apiService = ApiClient.getClient(requireContext()).create(ApiService.class);
        isOwnerRole = session.getUserRoles().contains("Owner") || session.getUserRoles().contains("EquipmentOwner");
        
        isOwnerView = false;

        initViews(view);
        setupTabs();
        loadBookings();

        return view;
    }

    private void initViews(View view) {
        rvBookings = view.findViewById(R.id.rvBookings);
        pbLoading = view.findViewById(R.id.pbLoading);
        llEmptyState = view.findViewById(R.id.llEmptyState);
        tabLayoutBookings = view.findViewById(R.id.tabLayoutBookings);

        rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> {
                if (rvBookings != null && rvBookings.getVisibility() == View.VISIBLE) {
                    return rvBookings.canScrollVertically(-1);
                }
                return false;
            });
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadBookings();
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }
    }

    private void setupTabs() {
        if (isOwnerRole) {
            tabLayoutBookings.setVisibility(View.VISIBLE);
            TabLayout.Tab tabMyBookings = tabLayoutBookings.newTab().setText("My Bookings");
            TabLayout.Tab tabRequests = tabLayoutBookings.newTab().setText("Received Requests");
            
            tabLayoutBookings.addTab(tabMyBookings);
            tabLayoutBookings.addTab(tabRequests);

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

    private void loadBookings() {
        showLoading();

        adapter = new BookingAdapter(requireContext(), new ArrayList<>(), isOwnerView, this);
        rvBookings.setAdapter(adapter);

        Call<List<BookingResponseDto>> call = isOwnerView ? apiService.getOwnerRequests() : apiService.getMyBookings();

        call.enqueue(new Callback<List<BookingResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<BookingResponseDto>> call, @NonNull Response<List<BookingResponseDto>> response) {
                if (!isAdded()) return;
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
            public void onFailure(@NonNull Call<List<BookingResponseDto>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                hideLoading();
                showEmptyState();
                Toast.makeText(requireContext(), "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading() {
        pbLoading.setVisibility(View.VISIBLE);
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
        loadBookings();
    }
}
