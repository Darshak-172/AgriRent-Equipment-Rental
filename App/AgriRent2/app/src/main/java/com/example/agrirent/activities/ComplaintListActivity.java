package com.example.agrirent.activities;

import android.content.Intent;
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
import com.example.agrirent.adapters.ComplaintAdapter;
import com.example.agrirent.models.ComplaintResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.Map;

public class ComplaintListActivity extends AppCompatActivity {

    private RecyclerView rvComplaints;
    private ComplaintAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout llEmptyState;
    private ExtendedFloatingActionButton fabNewComplaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvComplaints = findViewById(R.id.rvComplaints);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);
        fabNewComplaint = findViewById(R.id.fabNewComplaint);

        rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ComplaintAdapter(new ArrayList<>());
        rvComplaints.setAdapter(adapter);

        fabNewComplaint.setOnClickListener(v -> {
            startActivity(new Intent(ComplaintListActivity.this, CreateComplaintActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchComplaints();
    }

    private void fetchComplaints() {
        progressBar.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getMyComplaints().enqueue(new Callback<List<ComplaintResponse>>() {
            @Override
            public void onResponse(Call<List<ComplaintResponse>> call, Response<List<ComplaintResponse>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<ComplaintResponse> complaints = response.body();
                    if (complaints.isEmpty()) {
                        llEmptyState.setVisibility(View.VISIBLE);
                        rvComplaints.setVisibility(View.GONE);
                    } else {
                        llEmptyState.setVisibility(View.GONE);
                        rvComplaints.setVisibility(View.VISIBLE);
                        adapter.setComplaints(complaints);
                        resolveItemNames(complaints);
                    }
                } else {
                    llEmptyState.setVisibility(View.VISIBLE);
                    Toast.makeText(ComplaintListActivity.this, "Failed to load complaints", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ComplaintResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                llEmptyState.setVisibility(View.VISIBLE);
                Toast.makeText(ComplaintListActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resolveItemNames(List<ComplaintResponse> complaints) {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        
        for (ComplaintResponse complaint : complaints) {
            final int targetId = complaint.getTargetId();
            final String targetType = complaint.getTransactionType();

            if (targetId <= 0 || targetType == null || complaint.getTargetName() != null) continue;

            if ("Equipment".equalsIgnoreCase(targetType)) {
                api.getEquipmentById(targetId).enqueue(new Callback<com.example.agrirent.models.EquipmentNameResponse>() {
                    @Override
                    public void onResponse(Call<com.example.agrirent.models.EquipmentNameResponse> call, Response<com.example.agrirent.models.EquipmentNameResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            complaint.setTargetName(response.body().getEquipmentName());
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
                            complaint.setTargetName(response.body().getProductName());
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
