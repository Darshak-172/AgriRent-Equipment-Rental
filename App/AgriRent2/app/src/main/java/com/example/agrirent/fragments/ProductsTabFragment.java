package com.example.agrirent.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

public class ProductsTabFragment extends Fragment {

    private EditText etSearch;
    private TextView tvCount;
    private ChipGroup chipGroupCategory;
    private LinearLayout llLoading, llEmpty;
    private RecyclerView rvAllProducts;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ApiService apiService;
    private ProductItemAdapter adapter;

    private final List<ProductItem> allProducts  = new ArrayList<>();
    private final List<ProductItem> filteredList = new ArrayList<>();

    private String selectedCategory = null; // null = All

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_products_tab, container, false);

        apiService = ApiClient.getClient(requireContext()).create(ApiService.class);

        bindViews(view);
        setupRecyclerView();
        setupSearch();
        loadProducts();

        return view;
    }

    private void bindViews(View view) {
        etSearch        = view.findViewById(R.id.etSearch);
        tvCount         = view.findViewById(R.id.tvCount);
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
        llLoading       = view.findViewById(R.id.llLoading);
        llEmpty         = view.findViewById(R.id.llEmpty);
        rvAllProducts   = view.findViewById(R.id.rvAllProducts);

        Chip chipAll = view.findViewById(R.id.chipAll);
        if (chipAll != null) applyChipStyle(chipAll);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> {
                if (rvAllProducts != null && rvAllProducts.getVisibility() == View.VISIBLE) {
                    return rvAllProducts.canScrollVertically(-1);
                }
                return false;
            });
            swipeRefreshLayout.setOnRefreshListener(() -> {
                allProducts.clear();
                filteredList.clear();
                adapter.notifyDataSetChanged();
                loadProducts();
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }
    }

    private void setupRecyclerView() {
        if (rvAllProducts == null) return;
        adapter = new ProductItemAdapter(requireContext(), filteredList);
        GridLayoutManager lm = new GridLayoutManager(requireContext(), 2);
        lm.setInitialPrefetchItemCount(4);
        rvAllProducts.setLayoutManager(lm);
        rvAllProducts.setAdapter(adapter);
        rvAllProducts.setHasFixedSize(true);
        rvAllProducts.setItemViewCacheSize(10);
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) { applyFilter(); }
            @Override public void afterTextChanged(Editable e) {}
        });
    }

    private void loadProducts() {
        if (llLoading == null || rvAllProducts == null || llEmpty == null) return;
        llLoading.setVisibility(View.VISIBLE);
        rvAllProducts.setVisibility(View.GONE);
        llEmpty.setVisibility(View.GONE);

        apiService.getProducts(1, 20).enqueue(new Callback<PaginatedResponse<ProductItem>>() {
            @Override
            public void onResponse(@NonNull Call<PaginatedResponse<ProductItem>> call,
                                   @NonNull Response<PaginatedResponse<ProductItem>> response) {
                if (!isAdded()) return;
                llLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    allProducts.addAll(response.body().getData());
                    buildCategoryChipsFromProducts();
                    setupCategoryChipListener();
                    applyFilter();
                } else {
                    showEmpty();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PaginatedResponse<ProductItem>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                llLoading.setVisibility(View.GONE);
                showEmpty();
            }
        });
    }

    private void buildCategoryChipsFromProducts() {
        int count = chipGroupCategory.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View child = chipGroupCategory.getChildAt(i);
            if (child.getId() != R.id.chipAll) {
                chipGroupCategory.removeViewAt(i);
            }
        }

        LinkedHashMap<String, Boolean> seen = new LinkedHashMap<>();
        for (ProductItem p : allProducts) {
            if (p.getCategory() != null && !seen.containsKey(p.getCategory())) {
                seen.put(p.getCategory(), true);
            }
        }
        for (String cat : seen.keySet()) {
            Chip chip = (Chip) LayoutInflater.from(requireContext())
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
        if (adapter == null || rvAllProducts == null || llEmpty == null || tvCount == null || etSearch == null) return;
        String query = etSearch.getText() != null
                ? etSearch.getText().toString().trim().toLowerCase() : "";

        filteredList.clear();

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
        adapter.notifyDataSetChanged();

        tvCount.setText(newSize + " found");
        if (filteredList.isEmpty()) showEmpty();
        else {
            rvAllProducts.setVisibility(View.VISIBLE);
            llEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmpty() {
        if (rvAllProducts == null || llEmpty == null) return;
        rvAllProducts.setVisibility(View.GONE);
        llEmpty.setVisibility(View.VISIBLE);
    }
}
