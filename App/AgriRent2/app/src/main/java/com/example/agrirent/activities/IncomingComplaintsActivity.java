package com.example.agrirent.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.agrirent.R;
import com.example.agrirent.adapters.IncomingComplaintAdapter;
import com.example.agrirent.models.ComplaintResponse;
import com.example.agrirent.models.MessageResponse;
import com.example.agrirent.models.ResolutionRequest;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingComplaintsActivity extends BaseActivity {

    private RecyclerView rvComplaints;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private IncomingComplaintAdapter adapter;
    private List<ComplaintResponse> complaints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_complaints);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvComplaints = findViewById(R.id.rvComplaints);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncomingComplaintAdapter(this, complaints, this::showResolveDialog);
        rvComplaints.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(this::fetchComplaints);
        fetchComplaints();
    }

    private void fetchComplaints() {
        if (!swipeRefreshLayout.isRefreshing()) progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getIncomingComplaints().enqueue(new Callback<List<ComplaintResponse>>() {
            @Override
            public void onResponse(Call<List<ComplaintResponse>> call, Response<List<ComplaintResponse>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    complaints.clear();
                    complaints.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    if (complaints.isEmpty()) tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(IncomingComplaintsActivity.this, "Failed to load complaints", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ComplaintResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(IncomingComplaintsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showResolveDialog(ComplaintResponse complaint) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_resolve_complaint, null);
        EditText etNote = dialogView.findViewById(R.id.etResolutionNote);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Resolve Complaint")
                .setView(dialogView)
                .setPositiveButton("Resolve", (dialog, which) -> {
                    String note = etNote.getText().toString().trim();
                    resolveComplaint(complaint.getComplaintId(), note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resolveComplaint(int id, String note) {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.resolveComplaint(id, new ResolutionRequest(note)).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(IncomingComplaintsActivity.this, "Complaint resolved", Toast.LENGTH_SHORT).show();
                    fetchComplaints();
                } else {
                    Toast.makeText(IncomingComplaintsActivity.this, "Failed to resolve", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(IncomingComplaintsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
