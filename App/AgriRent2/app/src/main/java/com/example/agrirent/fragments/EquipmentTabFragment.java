package com.example.agrirent.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

public class EquipmentTabFragment extends Fragment {

    private EditText etSearch;
    private TextView tvCount;
    private ChipGroup chipGroupCategory;

    private LinearLayout llLoading, llEmpty;
    private RecyclerView rvAllEquipment;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ApiService apiService;
    private AllEquipmentAdapter adapter;

    private final List<Equipment> allEquipment = new ArrayList<>();
    private final List<Equipment> filteredList = new ArrayList<>();

    private int selectedCategoryId = -1; // -1 = All

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_equipment_tab, container, false);

        apiService = ApiClient.getClient(requireContext()).create(ApiService.class);

        bindViews(view);
        setupRecyclerView();
        setupSearch();

        loadEquipment();
        return view;
    }

    private void bindViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        tvCount = view.findViewById(R.id.tvCount);
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);

        llLoading = view.findViewById(R.id.llLoading);
        llEmpty = view.findViewById(R.id.llEmpty);
        rvAllEquipment = view.findViewById(R.id.rvAllEquipment);

        Chip chipAll = view.findViewById(R.id.chipAll);
        if (chipAll != null) {
            chipAll.setChipBackgroundColorResource(R.color.primary_color);
            chipAll.setTextColor(0xFFFFFFFF);
            chipAll.setChipStrokeWidth(0f);
        }

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> {
                if (rvAllEquipment != null && rvAllEquipment.getVisibility() == View.VISIBLE) {
                    return rvAllEquipment.canScrollVertically(-1);
                }
                return false;
            });
            swipeRefreshLayout.setOnRefreshListener(() -> {
                allEquipment.clear();
                filteredList.clear();
                adapter.notifyDataSetChanged();
                loadEquipment();
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }
    }

    private void setupRecyclerView() {
        if (rvAllEquipment == null) return;
        adapter = new AllEquipmentAdapter(requireContext(), filteredList);

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        layoutManager.setInitialPrefetchItemCount(4);

        rvAllEquipment.setLayoutManager(layoutManager);
        rvAllEquipment.setAdapter(adapter);
        rvAllEquipment.setHasFixedSize(true);
        rvAllEquipment.setItemViewCacheSize(12);
        rvAllEquipment.setRecycledViewPool(new androidx.recyclerview.widget.RecyclerView.RecycledViewPool());
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) { applyFilter(); }
            @Override public void afterTextChanged(Editable e) {}
        });
    }

    private void buildCategoryChipsFromEquipment() {
        int count = chipGroupCategory.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View child = chipGroupCategory.getChildAt(i);
            if (child.getId() != R.id.chipAll) {
                chipGroupCategory.removeViewAt(i);
            }
        }

        LinkedHashMap<Integer, String> seen = new LinkedHashMap<>();
        for (Equipment e : allEquipment) {
            if (e.getCategoryName() != null && !seen.containsKey(e.getCategoryId())) {
                seen.put(e.getCategoryId(), e.getCategoryName());
            }
        }
        for (java.util.Map.Entry<Integer, String> entry : seen.entrySet()) {
            Chip chip = (Chip) LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_filter_chip, chipGroupCategory, false);
            chip.setText(entry.getValue());
            chip.setTag(entry.getKey());
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
        if (llLoading == null || rvAllEquipment == null || llEmpty == null) return;
        llLoading.setVisibility(View.VISIBLE);
        rvAllEquipment.setVisibility(View.GONE);
        llEmpty.setVisibility(View.GONE);

        apiService.getAvailableEquipment(1, 20).enqueue(new Callback<PaginatedResponse<Equipment>>() {
            @Override
            public void onResponse(@NonNull Call<PaginatedResponse<Equipment>> call,
                                   @NonNull Response<PaginatedResponse<Equipment>> response) {
                if (!isAdded()) return;
                llLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    allEquipment.addAll(response.body().getData());
                    buildCategoryChipsFromEquipment();
                    setupCategoryChipListener();
                    applyFilter();
                } else {
                    showEmpty();
                }
            }
            @Override
            public void onFailure(@NonNull Call<PaginatedResponse<Equipment>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                llLoading.setVisibility(View.GONE);
                showEmpty();
            }
        });
    }

    private void applyFilter() {
        if (adapter == null || rvAllEquipment == null || llEmpty == null || tvCount == null || etSearch == null) return;
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";

        filteredList.clear();

        for (Equipment e : allEquipment) {
            boolean matchesCategory = selectedCategoryId == -1 || e.getCategoryId() == selectedCategoryId;
            boolean matchesSearch = query.isEmpty()
                    || (e.getEquipmentName() != null && e.getEquipmentName().toLowerCase().contains(query))
                    || (e.getLocation() != null && e.getLocation().toLowerCase().contains(query));
            if (matchesCategory && matchesSearch) filteredList.add(e);
        }

        int newSize = filteredList.size();
        adapter.notifyDataSetChanged();

        tvCount.setText(newSize + " found");
        if (filteredList.isEmpty()) showEmpty();
        else {
            rvAllEquipment.setVisibility(View.VISIBLE);
            llEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmpty() {
        if (rvAllEquipment == null || llEmpty == null) return;
        rvAllEquipment.setVisibility(View.GONE);
        llEmpty.setVisibility(View.VISIBLE);
    }
}
