package com.example.agrirent.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Handler;
import android.os.Looper;

import com.example.agrirent.R;
import com.example.agrirent.adapters.ManageEquipmentAdapter;
import com.example.agrirent.models.Equipment;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageEquipmentActivity extends BaseActivity implements ManageEquipmentAdapter.OnEquipmentActionClickListener {

    private ImageView btnBack;
    private RecyclerView rvEquipment;
    private LinearLayout llLoading;
    private LinearLayout llEmpty;
    private ExtendedFloatingActionButton fabAddEquipment;
    private SwipeRefreshLayout swipeRefreshLayout;
    private android.widget.TextView tvAllCount, tvActiveCount, tvPendingCount, tvPausedCount, tvBlockedCount;
    private LinearLayout llFilterAll, llFilterActive, llFilterPending, llFilterPaused, llFilterBlocked;

    private ManageEquipmentAdapter adapter;
    private List<Equipment> allEquipmentList = new ArrayList<>();
    private List<Equipment> equipmentList = new ArrayList<>();
    private String currentFilter = "All";

    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> addEquipmentLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    fetchMyEquipment();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_equipment);

        btnBack = findViewById(R.id.btnBack);
        rvEquipment = findViewById(R.id.rvEquipment);
        llLoading = findViewById(R.id.llLoading);
        llEmpty = findViewById(R.id.llEmpty);
        fabAddEquipment = findViewById(R.id.fabAddEquipment);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tvAllCount = findViewById(R.id.tvAllCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvPausedCount = findViewById(R.id.tvPausedCount);
        tvBlockedCount = findViewById(R.id.tvBlockedCount);

        llFilterAll = findViewById(R.id.llFilterAll);
        llFilterActive = findViewById(R.id.llFilterActive);
        llFilterPending = findViewById(R.id.llFilterPending);
        llFilterPaused = findViewById(R.id.llFilterPaused);
        llFilterBlocked = findViewById(R.id.llFilterBlocked);
        
        setupFilterClicks();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                fetchMyEquipment();
                new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }

        btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        rvEquipment.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManageEquipmentAdapter(this, equipmentList, this);
        rvEquipment.setAdapter(adapter);

        applyFilter();

        fabAddEquipment.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(ManageEquipmentActivity.this, AddEquipmentActivity.class);
            addEquipmentLauncher.launch(intent);
        });

        fetchMyEquipment();
    }

    private void setupFilterClicks() {
        View.OnClickListener listener = v -> {
            if (v.getId() == R.id.llFilterAll) currentFilter = "All";
            else if (v.getId() == R.id.llFilterActive) currentFilter = "Active";
            else if (v.getId() == R.id.llFilterPending) currentFilter = "Pending";
            else if (v.getId() == R.id.llFilterPaused) currentFilter = "Inactive";
            else if (v.getId() == R.id.llFilterBlocked) currentFilter = "Blocked";
            applyFilter();
        };

        if (llFilterAll != null) llFilterAll.setOnClickListener(listener);
        if (llFilterActive != null) llFilterActive.setOnClickListener(listener);
        if (llFilterPending != null) llFilterPending.setOnClickListener(listener);
        if (llFilterPaused != null) llFilterPaused.setOnClickListener(listener);
        if (llFilterBlocked != null) llFilterBlocked.setOnClickListener(listener);
    }

    private void applyFilterUI(LinearLayout ll, android.widget.TextView tv, boolean isSelected) {
        if (ll == null || tv == null) return;
        if (isSelected) {
            ll.setBackgroundResource(R.drawable.bg_chip_solid_white);
            tv.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
        } else {
            ll.setBackgroundResource(R.drawable.bg_chip_translucent_white);
            tv.setTextColor(android.graphics.Color.WHITE);
        }
    }

    private void applyFilter() {
        applyFilterUI(llFilterAll, tvAllCount, "All".equals(currentFilter));
        applyFilterUI(llFilterActive, tvActiveCount, "Active".equals(currentFilter));
        applyFilterUI(llFilterPending, tvPendingCount, "Pending".equals(currentFilter));
        applyFilterUI(llFilterPaused, tvPausedCount, "Inactive".equals(currentFilter));
        applyFilterUI(llFilterBlocked, tvBlockedCount, "Blocked".equals(currentFilter));

        equipmentList.clear();
        for (Equipment e : allEquipmentList) {
            String status = e.getStatus() != null ? e.getStatus().trim() : "";
            if ("All".equals(currentFilter)) {
                equipmentList.add(e);
            } else if (currentFilter.equalsIgnoreCase(status)) {
                equipmentList.add(e);
            } else if ("Inactive".equalsIgnoreCase(currentFilter) && "Paused".equalsIgnoreCase(status)) {
                equipmentList.add(e);
            } else if ("Pending".equalsIgnoreCase(currentFilter) && "Pending".equalsIgnoreCase(status)) {
                equipmentList.add(e);
            } else if ("Blocked".equalsIgnoreCase(currentFilter) && "Blocked".equalsIgnoreCase(status)) {
                equipmentList.add(e);
            }
        }
        adapter.notifyDataSetChanged();
        
        if (equipmentList.isEmpty()) {
            llEmpty.setVisibility(View.VISIBLE);
            rvEquipment.setVisibility(View.GONE);
        } else {
            llEmpty.setVisibility(View.GONE);
            rvEquipment.setVisibility(View.VISIBLE);
        }
    }

    private void fetchMyEquipment() {
        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            llLoading.setVisibility(View.VISIBLE);
        }
        rvEquipment.setVisibility(View.GONE);
        llEmpty.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.getMyEquipments().enqueue(new Callback<List<Equipment>>() {
            @Override
            public void onResponse(@NonNull Call<List<Equipment>> call, @NonNull Response<List<Equipment>> response) {
                llLoading.setVisibility(View.GONE);

                if (response.code() == 401) {
                    Toast.makeText(ManageEquipmentActivity.this,
                            "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                    llEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                if (response.code() == 403) {
                    Toast.makeText(ManageEquipmentActivity.this,
                            "Access denied. Equipment Owner role required.", Toast.LENGTH_LONG).show();
                    llEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<Equipment> list = response.body();

                    allEquipmentList.clear();
                    allEquipmentList.addAll(list);

                    updateStats();
                    applyFilter();
                } else {
                    llEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(ManageEquipmentActivity.this,
                            "Failed to load equipment (code: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Equipment>> call, @NonNull Throwable t) {
                llLoading.setVisibility(View.GONE);
                llEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(ManageEquipmentActivity.this,
                        "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditClick(Equipment equipment) {
        android.content.Intent intent = new android.content.Intent(this, EditEquipmentActivity.class);
        intent.putExtra("equipment", equipment);
        addEquipmentLauncher.launch(intent);
    }

    @Override
    public void onToggleStatusClick(Equipment equipment, int position, View itemView) {
        // Subtle feedback animation
        itemView.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(150)
                .withEndAction(() -> {
                    itemView.animate().scaleX(1f).scaleY(1f).setDuration(150);
                    performToggleStatus(equipment, position);
                });
    }

    private void performToggleStatus(Equipment equipment, int position) {
        String targetStatus = "Active".equalsIgnoreCase(equipment.getStatus()) ? "Blocked" : "Active";
        
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.toggleEquipmentStatus(equipment.getEquipmentId()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    equipment.setStatus(targetStatus);
                    updateStats();
                    applyFilter();
                    Toast.makeText(ManageEquipmentActivity.this, "Equipment " + targetStatus, Toast.LENGTH_SHORT).show();
                } else {
                    String errorMessage = "Failed to update status";
                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            org.json.JSONObject jsonObject = new org.json.JSONObject(errorJson);
                            if (jsonObject.has("message")) {
                                errorMessage = jsonObject.getString("message");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(ManageEquipmentActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Toast.makeText(ManageEquipmentActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteClick(Equipment equipment, int position, View itemView) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Delete Equipment")
                .setMessage("Are you sure you want to delete " + equipment.getEquipmentName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Exit animation
                    itemView.animate()
                            .translationX(itemView.getWidth())
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction(() -> deleteEquipmentApi(equipment, position));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEquipmentApi(Equipment equipment, int position) {
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.deleteEquipment(equipment.getEquipmentId()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    allEquipmentList.remove(equipment);
                    updateStats();
                    applyFilter();
                    Toast.makeText(ManageEquipmentActivity.this, "Equipment deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ManageEquipmentActivity.this, "Failed to delete equipment", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Toast.makeText(ManageEquipmentActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats() {
        int active = 0, blocked = 0, pending = 0, paused = 0;
        for (Equipment e : allEquipmentList) {
            String status = e.getStatus() != null ? e.getStatus().trim() : "";
            if ("Active".equalsIgnoreCase(status)) active++;
            else if ("Blocked".equalsIgnoreCase(status)) blocked++;
            else if ("Pending".equalsIgnoreCase(status)) pending++;
            else if ("Inactive".equalsIgnoreCase(status) || "Paused".equalsIgnoreCase(status)) paused++;
        }
        if (tvAllCount != null) tvAllCount.setText("All (" + allEquipmentList.size() + ")");
        if (tvActiveCount != null) tvActiveCount.setText(active + " Active");
        if (tvBlockedCount != null) tvBlockedCount.setText(blocked + " Blocked");
        if (tvPendingCount != null) tvPendingCount.setText(pending + " Pending");
        if (tvPausedCount != null) tvPausedCount.setText(paused + " Paused");
    }
}
