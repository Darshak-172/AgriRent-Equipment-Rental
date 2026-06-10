package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Handler;
import android.os.Looper;

import com.example.agrirent.R;
import com.example.agrirent.adapters.AllEquipmentAdapter;
import java.util.LinkedHashMap;
import com.example.agrirent.models.Equipment;
import com.example.agrirent.models.PaginatedResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AllEquipmentActivity extends BaseActivity {

    private ImageView btnBack;
    private EditText etSearch;
    private TextView tvCount;
    private ChipGroup chipGroupCategory;
    private SwipeRefreshLayout swipeRefreshLayout;

    private LinearLayout llLoading, llEmpty;
    private RecyclerView rvAllEquipment;

    private ApiService apiService;
    private AllEquipmentAdapter adapter;

    private final List<Equipment> allEquipment = new ArrayList<>();
    private final List<Equipment> filteredList = new ArrayList<>();

    private int selectedCategoryId = -1; // -1 = All
    
    // Pagination state
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;
    private boolean isLoadingNextPage = false;
    private boolean isLastPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_equipment);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        bindViews();
        setupRecyclerView();
        setupSearch();

        loadEquipment();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        etSearch = findViewById(R.id.etSearch);
        tvCount = findViewById(R.id.tvCount);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                currentPage = 1;
                isLastPage = false;
                loadEquipment();
                new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }

        llLoading = findViewById(R.id.llLoading);
        llEmpty = findViewById(R.id.llEmpty);
        rvAllEquipment = findViewById(R.id.rvAllEquipment);

        btnBack.setOnClickListener(v -> finish());

        // Style the default "All" chip (checked by default)
        Chip chipAll = findViewById(R.id.chipAll);
        if (chipAll != null) {
            chipAll.setChipBackgroundColorResource(R.color.primary_color);
            chipAll.setTextColor(0xFFFFFFFF);
            chipAll.setChipStrokeWidth(0f);
        }
    }

    private void setupRecyclerView() {
        adapter = new AllEquipmentAdapter(this, filteredList);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        layoutManager.setInitialPrefetchItemCount(4); // pre-load 4 items during idle

        rvAllEquipment.setLayoutManager(layoutManager);
        rvAllEquipment.setAdapter(adapter);
        rvAllEquipment.setHasFixedSize(true);        // avoids re-measuring entire list
        rvAllEquipment.setItemViewCacheSize(12);      // keep 12 views in offscreen cache
        rvAllEquipment.setRecycledViewPool(new androidx.recyclerview.widget.RecyclerView.RecycledViewPool());

        rvAllEquipment.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && !isLoadingNextPage && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadNextPage();
                    }
                }
            }
        });
    }

    private void loadNextPage() {
        isLoadingNextPage = true;
        currentPage++;
        loadEquipment();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) { applyFilter(); }
            @Override public void afterTextChanged(Editable e) {}
        });
    }

    /**
     * Build filter chips using only the categories present in the loaded equipment list.
     * This avoids showing unrelated categories (e.g. "Fruits") from the global API.
     */
    private void buildCategoryChipsFromEquipment() {
        // Collect unique categories preserving insertion order
        LinkedHashMap<Integer, String> seen = new LinkedHashMap<>();
        for (Equipment e : allEquipment) {
            if (e.getCategoryName() != null && !seen.containsKey(e.getCategoryId())) {
                seen.put(e.getCategoryId(), e.getCategoryName());
            }
        }
        for (java.util.Map.Entry<Integer, String> entry : seen.entrySet()) {
            Chip chip = (Chip) LayoutInflater.from(this)
                    .inflate(R.layout.item_filter_chip, chipGroupCategory, false);
            chip.setText(entry.getValue());
            chip.setTag(entry.getKey());
            applyChipStyle(chip);
            chipGroupCategory.addView(chip);
        }
    }

    private void setupCategoryChipListener() {
        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            // Refresh all chip styles based on check state
            for (int i = 0; i < group.getChildCount(); i++) {
                View v = group.getChildAt(i);
                if (v instanceof Chip) applyChipStyle((Chip) v);
            }
            if (checkedIds.isEmpty()) {
                selectedCategoryId = -1;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipAll) {
                    selectedCategoryId = -1;
                } else {
                    View chip = group.findViewById(checkedId);
                    if (chip != null && chip.getTag() instanceof Integer) {
                        selectedCategoryId = (int) chip.getTag();
                    }
                }
            }
            applyFilter();
        });
    }

    private void applyChipStyle(Chip chip) {
        if (chip.isChecked()) {
            chip.setChipBackgroundColorResource(R.color.primary_color);
            chip.setTextColor(0xFFFFFFFF);
            chip.setChipStrokeWidth(0f);
        } else {
            chip.setChipBackgroundColorResource(R.color.card_bg);
            chip.setTextColor(0xFF555555);
            chip.setChipStrokeWidth(android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 1f,
                    getResources().getDisplayMetrics()));
            chip.setChipStrokeColorResource(R.color.border_color);
        }
    }

    private void loadEquipment() {
        if (currentPage == 1 && (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing())) {
            llLoading.setVisibility(View.VISIBLE);
            rvAllEquipment.setVisibility(View.GONE);
            llEmpty.setVisibility(View.GONE);
        }

        apiService.getAvailableEquipment(currentPage, PAGE_SIZE).enqueue(new Callback<PaginatedResponse<Equipment>>() {
            @Override
            public void onResponse(@NonNull Call<PaginatedResponse<Equipment>> call,
                                   @NonNull Response<PaginatedResponse<Equipment>> response) {
                if (currentPage == 1) {
                    llLoading.setVisibility(View.GONE);
                }
                isLoadingNextPage = false;
        
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    
                    if (currentPage == 1) {
                        allEquipment.clear();
                        int count = chipGroupCategory.getChildCount();
                        if (count > 1) {
                            chipGroupCategory.removeViews(1, count - 1);
                        }
                    }
                    
                    allEquipment.addAll(response.body().getData());
                    
                    if (response.body().getPagination() != null) {
                        isLastPage = currentPage >= response.body().getPagination().getTotalPages();
                    } else {
                        isLastPage = response.body().getData().isEmpty() || response.body().getData().size() < PAGE_SIZE;
                    }

                    if (currentPage == 1) {
                        buildCategoryChipsFromEquipment(); // populate chips with real equipment categories
                        setupCategoryChipListener();       // attach listener after chips are built
                    }
                    applyFilter();
                } else if (currentPage == 1) {
                    showEmpty();
                }
            }
            @Override
            public void onFailure(@NonNull Call<PaginatedResponse<Equipment>> call, @NonNull Throwable t) {
                if (currentPage == 1) {
                    llLoading.setVisibility(View.GONE);
                    showEmpty();
                }
                isLoadingNextPage = false;
            }
        });
    }

    private void applyFilter() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";

        int oldSize = filteredList.size();
        filteredList.clear();
        if (oldSize > 0) adapter.notifyItemRangeRemoved(0, oldSize);

        for (Equipment e : allEquipment) {
            boolean matchesCategory = selectedCategoryId == -1 || e.getCategoryId() == selectedCategoryId;
            boolean matchesSearch = query.isEmpty()
                    || (e.getEquipmentName() != null && e.getEquipmentName().toLowerCase().contains(query))
                    || (e.getLocation() != null && e.getLocation().toLowerCase().contains(query));
            if (matchesCategory && matchesSearch) filteredList.add(e);
        }

        int newSize = filteredList.size();
        if (newSize > 0) adapter.notifyItemRangeInserted(0, newSize);

        tvCount.setText(newSize + " found");
        if (filteredList.isEmpty()) showEmpty();
        else {
            rvAllEquipment.setVisibility(View.VISIBLE);
            llEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmpty() {
        rvAllEquipment.setVisibility(View.GONE);
        llEmpty.setVisibility(View.VISIBLE);
    }
}
