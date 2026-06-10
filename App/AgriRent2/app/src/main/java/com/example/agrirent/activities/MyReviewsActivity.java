package com.example.agrirent.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrirent.R;
import com.example.agrirent.adapters.RatingAdapter;
import com.example.agrirent.models.RatingResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.agrirent.utils.SessionManager;

public class MyReviewsActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private RatingAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout llEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reviews);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvReviews = findViewById(R.id.rvReviews);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        // We initialize the adapter with an empty list and false for isCurrentUser context
        // but for "My Reviews", we could pass false as we only want to show the list, not edit.
        adapter = new RatingAdapter(new ArrayList<>());
        rvReviews.setAdapter(adapter);

        fetchMyReviews();
    }

    private void fetchMyReviews() {
        progressBar.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getMyRatings().enqueue(new Callback<List<RatingResponse>>() {
            @Override
            public void onResponse(Call<List<RatingResponse>> call, Response<List<RatingResponse>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<RatingResponse> reviews = response.body();
                    if (reviews.isEmpty()) {
                        llEmptyState.setVisibility(View.VISIBLE);
                        rvReviews.setVisibility(View.GONE);
                    } else {
                        llEmptyState.setVisibility(View.GONE);
                        rvReviews.setVisibility(View.VISIBLE);
                        
                        // Set current user name from session
                        SessionManager session = new SessionManager(MyReviewsActivity.this);
                        String fullName = session.getUserName();
                        
                        for (RatingResponse r : reviews) {
                            if (fullName != null && !fullName.isEmpty()) {
                                r.setUserFullName(fullName);
                            }
                        }
                        
                        adapter.updateRatings(reviews);
                        resolveReviewItemNames(reviews);
                    }
                } else {
                    llEmptyState.setVisibility(View.VISIBLE);
                    Toast.makeText(MyReviewsActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<RatingResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                llEmptyState.setVisibility(View.VISIBLE);
                Toast.makeText(MyReviewsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resolveReviewItemNames(List<RatingResponse> reviews) {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        for (RatingResponse review : reviews) {
            final int targetId = review.getTargetId();
            final String targetType = review.getTargetType();

            if (targetId <= 0 || targetType == null || review.getTargetName() != null) continue;

            if ("Equipment".equalsIgnoreCase(targetType)) {
                api.getEquipmentById(targetId).enqueue(new Callback<com.example.agrirent.models.EquipmentNameResponse>() {
                    @Override
                    public void onResponse(Call<com.example.agrirent.models.EquipmentNameResponse> call, Response<com.example.agrirent.models.EquipmentNameResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            review.setTargetName(response.body().getEquipmentName());
                            adapter.notifyDataSetChanged();
                        }
                    }
                    @Override
                    public void onFailure(Call<com.example.agrirent.models.EquipmentNameResponse> call, Throwable t) {}
                });
            } else if ("Product".equalsIgnoreCase(targetType)) {
                api.getProductById(targetId).enqueue(new Callback<com.example.agrirent.models.ProductNameResponse>() {
                    @Override
                    public void onResponse(Call<com.example.agrirent.models.ProductNameResponse> call, Response<com.example.agrirent.models.ProductNameResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            review.setTargetName(response.body().getProductName());
                            adapter.notifyDataSetChanged();
                        }
                    }
                    @Override
                    public void onFailure(Call<com.example.agrirent.models.ProductNameResponse> call, Throwable t) {}
                });
            }
        }
    }
}
