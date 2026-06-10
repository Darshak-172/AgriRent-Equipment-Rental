package com.example.agrirent.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrirent.R;
import com.example.agrirent.adapters.RatingAdapter;
import com.example.agrirent.models.IncomingReviewsResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingReviewsActivity extends BaseActivity {

    private TextView tvAverageRating, tvTotalReviews, tvNoReviews;
    private RatingBar ratingBar;
    private RecyclerView rvReviews;
    private ProgressBar progressBar;
    private RatingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_reviews);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        ratingBar = findViewById(R.id.ratingBar);
        rvReviews = findViewById(R.id.rvReviews);
        progressBar = findViewById(R.id.progressBar);

        rvReviews.setLayoutManager(new LinearLayoutManager(this));

        fetchIncomingReviews();
    }

    private void fetchIncomingReviews() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getIncomingReviews().enqueue(new Callback<IncomingReviewsResponse>() {
            @Override
            public void onResponse(Call<IncomingReviewsResponse> call, Response<IncomingReviewsResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    IncomingReviewsResponse data = response.body();
                    updateHeader(data);
                    if (data.getReviews() == null || data.getReviews().isEmpty()) {
                        tvNoReviews.setVisibility(View.VISIBLE);
                        rvReviews.setVisibility(View.GONE);
                    } else {
                        tvNoReviews.setVisibility(View.GONE);
                        rvReviews.setVisibility(View.VISIBLE);
                        adapter = new RatingAdapter(data.getReviews());
                        rvReviews.setAdapter(adapter);
                    }
                } else {
                    Toast.makeText(IncomingReviewsActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<IncomingReviewsResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(IncomingReviewsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateHeader(IncomingReviewsResponse data) {
        tvAverageRating.setText(String.valueOf(data.getAverageRating()));
        tvTotalReviews.setText("Based on " + data.getTotalReviews() + " reviews");
        ratingBar.setRating((float) data.getAverageRating());
    }
}
