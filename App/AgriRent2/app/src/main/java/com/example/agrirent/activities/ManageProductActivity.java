package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Handler;
import android.os.Looper;

import com.example.agrirent.R;
import com.example.agrirent.adapters.ManageProductAdapter;
import com.example.agrirent.models.ProductItem;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageProductActivity extends BaseActivity implements ManageProductAdapter.OnProductActionClickListener {

    private ImageView btnBack;
    private RecyclerView rvProducts;
    private LinearLayout llLoading;
    private LinearLayout llEmpty;
    private ExtendedFloatingActionButton fabAddProduct;
    private SwipeRefreshLayout swipeRefreshLayout;
    private android.widget.TextView tvAllCount, tvActiveCount, tvPendingCount, tvPausedCount, tvBlockedCount;
    private LinearLayout llFilterAll, llFilterActive, llFilterPending, llFilterPaused, llFilterBlocked;

    private ManageProductAdapter adapter;
    private List<ProductItem> allProductList = new ArrayList<>();
    private List<ProductItem> productList = new ArrayList<>();
    private String currentFilter = "All";

    private final androidx.activity.result.ActivityResultLauncher<Intent> addProductLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    fetchMyProducts();
                }
            }
    );

    private final androidx.activity.result.ActivityResultLauncher<Intent> editProductLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    fetchMyProducts();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_product);

        btnBack = findViewById(R.id.btnBack);
        rvProducts = findViewById(R.id.rvProducts);
        llLoading = findViewById(R.id.llLoading);
        llEmpty = findViewById(R.id.llEmpty);
        fabAddProduct = findViewById(R.id.fabAddProduct);
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
                fetchMyProducts();
                new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }

        btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManageProductAdapter(this, productList, this);
        rvProducts.setAdapter(adapter);

        applyFilter();

        fabAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(ManageProductActivity.this, AddProductActivity.class);
            addProductLauncher.launch(intent);
        });

        fetchMyProducts();
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

        productList.clear();
        for (ProductItem p : allProductList) {
            String status = p.getStatus() != null ? p.getStatus().trim() : "";
            if ("All".equals(currentFilter)) {
                productList.add(p);
            } else if (currentFilter.equalsIgnoreCase(status)) {
                productList.add(p);
            } else if ("Inactive".equalsIgnoreCase(currentFilter) && "Paused".equalsIgnoreCase(status)) {
                productList.add(p);
            } else if ("Pending".equalsIgnoreCase(currentFilter) && "Pending".equalsIgnoreCase(status)) {
                productList.add(p);
            } else if ("Blocked".equalsIgnoreCase(currentFilter) && "Blocked".equalsIgnoreCase(status)) {
                productList.add(p);
            }
        }
        adapter.notifyDataSetChanged();
        
        if (productList.isEmpty()) {
            llEmpty.setVisibility(View.VISIBLE);
            rvProducts.setVisibility(View.GONE);
        } else {
            llEmpty.setVisibility(View.GONE);
            rvProducts.setVisibility(View.VISIBLE);
        }
    }

    private void fetchMyProducts() {
        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            llLoading.setVisibility(View.VISIBLE);
        }
        rvProducts.setVisibility(View.GONE);
        llEmpty.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.getMyProducts().enqueue(new Callback<List<ProductItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<ProductItem>> call, @NonNull Response<List<ProductItem>> response) {
                llLoading.setVisibility(View.GONE);

                if (response.code() == 401) {
                    Toast.makeText(ManageProductActivity.this,
                            "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                    llEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<ProductItem> list = response.body();

                    allProductList.clear();
                    allProductList.addAll(list);

                    updateStats();
                    applyFilter();
                } else {
                    llEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(ManageProductActivity.this,
                            "Failed to load products (code: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ProductItem>> call, @NonNull Throwable t) {
                llLoading.setVisibility(View.GONE);
                llEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(ManageProductActivity.this,
                        "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditClick(ProductItem product) {
        Intent intent = new Intent(this, EditProductActivity.class);
        intent.putExtra("product", product);
        editProductLauncher.launch(intent);
    }

    @Override
    public void onToggleStatusClick(ProductItem product, int position, View itemView) {
        // Subtle feedback animation
        itemView.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(150)
                .withEndAction(() -> {
                    itemView.animate().scaleX(1f).scaleY(1f).setDuration(150);
                    performToggleStatus(product, position);
                });
    }

    private void performToggleStatus(ProductItem product, int position) {
        String targetStatus = "Active".equalsIgnoreCase(product.getStatus()) ? "Blocked" : "Active";
        
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.toggleProductStatus(product.getProductId()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    product.setStatus(targetStatus);
                    updateStats();
                    applyFilter();
                    Toast.makeText(ManageProductActivity.this, "Product " + targetStatus, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ManageProductActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Toast.makeText(ManageProductActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteClick(ProductItem product, int position, View itemView) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + product.getProductName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Exit animation
                    itemView.animate()
                            .translationX(itemView.getWidth())
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction(() -> deleteProductApi(product, position));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProductApi(ProductItem product, int position) {
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.deleteProduct(product.getProductId()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    allProductList.remove(product);
                    updateStats();
                    applyFilter();
                    Toast.makeText(ManageProductActivity.this, "Product deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ManageProductActivity.this, "Failed to delete product", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Toast.makeText(ManageProductActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats() {
        int active = 0, blocked = 0, pending = 0, paused = 0;
        for (ProductItem p : allProductList) {
            String status = p.getStatus() != null ? p.getStatus().trim() : "";
            if ("Active".equalsIgnoreCase(status)) active++;
            else if ("Blocked".equalsIgnoreCase(status)) blocked++;
            else if ("Pending".equalsIgnoreCase(status)) pending++;
            else if ("Inactive".equalsIgnoreCase(status) || "Paused".equalsIgnoreCase(status)) paused++;
        }
        if (tvAllCount != null) tvAllCount.setText("All (" + allProductList.size() + ")");
        if (tvActiveCount != null) tvActiveCount.setText(active + " Active");
        if (tvBlockedCount != null) tvBlockedCount.setText(blocked + " Blocked");
        if (tvPendingCount != null) tvPendingCount.setText(pending + " Pending");
        if (tvPausedCount != null) tvPausedCount.setText(paused + " Paused");
    }
}
