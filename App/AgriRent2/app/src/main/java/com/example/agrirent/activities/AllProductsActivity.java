package com.example.agrirent.activities;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Handler;
import android.os.Looper;

import com.example.agrirent.R;
import com.example.agrirent.adapters.ProductItemAdapter;
import com.example.agrirent.models.PaginatedResponse;
import com.example.agrirent.models.ProductItem;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AllProductsActivity extends BaseActivity {

    private ImageView btnBack;
    private EditText etSearch;
    private TextView tvCount;
    private ChipGroup chipGroupCategory;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout llLoading, llEmpty;
    private RecyclerView rvAllProducts;

    private ApiService apiService;
    private ProductItemAdapter adapter;

    private final List<ProductItem> allProducts  = new ArrayList<>();
    private final List<ProductItem> filteredList = new ArrayList<>();

    private String selectedCategory = null; // null = All
    
    // Pagination state
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;
    private boolean isLoadingNextPage = false;
    private boolean isLastPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_products);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        bindViews();
        setupRecyclerView();
        setupSearch();
        loadProducts();
    }

    private void bindViews() {
        btnBack         = findViewById(R.id.btnBack);
        etSearch        = findViewById(R.id.etSearch);
        tvCount         = findViewById(R.id.tvCount);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        llLoading       = findViewById(R.id.llLoading);
        llEmpty         = findViewById(R.id.llEmpty);
        rvAllProducts   = findViewById(R.id.rvAllProducts);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                currentPage = 1;
                isLastPage = false;
                loadProducts();
                new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }

        btnBack.setOnClickListener(v -> finish());

        // Style the default "All" chip
        Chip chipAll = findViewById(R.id.chipAll);
        if (chipAll != null) applyChipStyle(chipAll);
    }

    private void setupRecyclerView() {
        adapter = new ProductItemAdapter(this, filteredList);
        GridLayoutManager lm = new GridLayoutManager(this, 2);
        lm.setInitialPrefetchItemCount(4);
        rvAllProducts.setLayoutManager(lm);
        rvAllProducts.setAdapter(adapter);
        rvAllProducts.setHasFixedSize(true);
        rvAllProducts.setItemViewCacheSize(10);
        
        rvAllProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && !isLoadingNextPage && !isLastPage) {
                    int visibleItemCount = lm.getChildCount();
                    int totalItemCount = lm.getItemCount();
                    int firstVisibleItemPosition = lm.findFirstVisibleItemPosition();

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
        loadProducts();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) { applyFilter(); }
            @Override public void afterTextChanged(Editable e) {}
        });
    }

    private void loadProducts() {
        if (currentPage == 1 && (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing())) {
            llLoading.setVisibility(View.VISIBLE);
            rvAllProducts.setVisibility(View.GONE);
            llEmpty.setVisibility(View.GONE);
        }

        apiService.getProducts(currentPage, PAGE_SIZE).enqueue(new Callback<PaginatedResponse<ProductItem>>() {
            @Override
            public void onResponse(@NonNull Call<PaginatedResponse<ProductItem>> call,
                                   @NonNull Response<PaginatedResponse<ProductItem>> response) {
                if (currentPage == 1) {
                    llLoading.setVisibility(View.GONE);
                }
                isLoadingNextPage = false;
                
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    
                    if (currentPage == 1) {
                        allProducts.clear();
                        int count = chipGroupCategory.getChildCount();
                        if (count > 1) {
                            chipGroupCategory.removeViews(1, count - 1);
                        }
                    }
                    
                    allProducts.addAll(response.body().getData());
                    
                    if (response.body().getPagination() != null) {
                        isLastPage = currentPage >= response.body().getPagination().getTotalPages();
                    } else {
                        isLastPage = response.body().getData().isEmpty() || response.body().getData().size() < PAGE_SIZE;
                    }

                    if (currentPage == 1) {
                        buildCategoryChipsFromProducts();
                        setupCategoryChipListener();
                    }
                    applyFilter();
                } else if (currentPage == 1) {
                    showEmpty();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PaginatedResponse<ProductItem>> call, @NonNull Throwable t) {
                if (currentPage == 1) {
                    llLoading.setVisibility(View.GONE);
                    showEmpty();
                }
                isLoadingNextPage = false;
            }
        });
    }

    /**
     * Build category chips from the actual product data — avoids showing irrelevant categories.
     */
    private void buildCategoryChipsFromProducts() {
        LinkedHashMap<String, Boolean> seen = new LinkedHashMap<>();
        for (ProductItem p : allProducts) {
            if (p.getCategory() != null && !seen.containsKey(p.getCategory())) {
                seen.put(p.getCategory(), true);
            }
        }
        for (String cat : seen.keySet()) {
            Chip chip = (Chip) LayoutInflater.from(this)
                    .inflate(R.layout.item_filter_chip, chipGroupCategory, false);
            chip.setText(cat);
            chip.setTag(cat);
            applyChipStyle(chip);
            chipGroupCategory.addView(chip);
        }
    }

    private void setupCategoryChipListener() {
        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            for (int i = 0; i < group.getChildCount(); i++) {
                View v = group.getChildAt(i);
                if (v instanceof Chip) applyChipStyle((Chip) v);
            }
            if (checkedIds.isEmpty()) {
                selectedCategory = null;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipAll) {
                    selectedCategory = null;
                } else {
                    View chip = group.findViewById(checkedId);
                    if (chip != null && chip.getTag() instanceof String) {
                        selectedCategory = (String) chip.getTag();
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

    private void applyFilter() {
        String query = etSearch.getText() != null
                ? etSearch.getText().toString().trim().toLowerCase() : "";

        int oldSize = filteredList.size();
        filteredList.clear();
        if (oldSize > 0) adapter.notifyItemRangeRemoved(0, oldSize);

        for (ProductItem p : allProducts) {
            boolean matchesCat = selectedCategory == null
                    || selectedCategory.equals(p.getCategory());
            boolean matchesSearch = query.isEmpty()
                    || (p.getProductName() != null && p.getProductName().toLowerCase().contains(query))
                    || (p.getCategory() != null && p.getCategory().toLowerCase().contains(query))
                    || (p.getLocation() != null && p.getLocation().toLowerCase().contains(query));
            if (matchesCat && matchesSearch) filteredList.add(p);
        }

        int newSize = filteredList.size();
        if (newSize > 0) adapter.notifyItemRangeInserted(0, newSize);

        tvCount.setText(newSize + " found");
        if (filteredList.isEmpty()) showEmpty();
        else {
            rvAllProducts.setVisibility(View.VISIBLE);
            llEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmpty() {
        rvAllProducts.setVisibility(View.GONE);
        llEmpty.setVisibility(View.VISIBLE);
    }
}
