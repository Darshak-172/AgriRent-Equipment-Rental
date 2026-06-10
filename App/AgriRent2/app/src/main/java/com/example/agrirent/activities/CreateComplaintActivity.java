package com.example.agrirent.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.agrirent.R;
import com.example.agrirent.models.BookingResponseDto;
import com.example.agrirent.models.CreateComplaintRequest;
import com.example.agrirent.models.OrderResponseDto;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateComplaintActivity extends AppCompatActivity {

    private AutoCompleteTextView autocompleteItem;
    private TextInputEditText editTitle, editDescription;
    private MaterialButton btnSubmitComplaint;
    private View progressBar;

    private List<BookingResponseDto> myBookings = new ArrayList<>();
    private List<OrderResponseDto> myOrders = new ArrayList<>();

    private List<TransactionItem> transactionItems = new ArrayList<>();
    private TransactionItem selectedTransaction = null;

    // Hold full list for dedup reference
    private java.util.HashSet<String> seenKeys = new java.util.HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_complaint);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        autocompleteItem = findViewById(R.id.autocompleteItem);
        editTitle = findViewById(R.id.editTitle);
        editDescription = findViewById(R.id.editDescription);
        btnSubmitComplaint = findViewById(R.id.btnSubmitComplaint);
        progressBar = findViewById(R.id.progressBar);

        // Fetch user data securely
        fetchUserTransactions();

        autocompleteItem.setOnItemClickListener((parent, view, position, id) -> {
            selectedTransaction = transactionItems.get(position);
        });

        btnSubmitComplaint.setOnClickListener(v -> submitComplaint());
    }

    private void fetchUserTransactions() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = ApiClient.getClient(this).create(ApiService.class);

        // 1. Fetch Bookings
        api.getMyBookings().enqueue(new Callback<List<BookingResponseDto>>() {
            @Override
            public void onResponse(Call<List<BookingResponseDto>> call, Response<List<BookingResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    myBookings = response.body();
                }
                
                // 2. Fetch Orders after Bookings complete
                api.getMyProductOrders().enqueue(new Callback<List<OrderResponseDto>>() {
                    @Override
                    public void onResponse(Call<List<OrderResponseDto>> call, Response<List<OrderResponseDto>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            myOrders = response.body();
                        }
                        populateDropdown();
                    }

                    @Override
                    public void onFailure(Call<List<OrderResponseDto>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        populateDropdown();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<BookingResponseDto>> call, Throwable t) {
                // Failsafe, continue to orders
                api.getMyProductOrders().enqueue(new Callback<List<OrderResponseDto>>() {
                    @Override
                    public void onResponse(Call<List<OrderResponseDto>> call, Response<List<OrderResponseDto>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            myOrders = response.body();
                        }
                        populateDropdown();
                    }

                    @Override
                    public void onFailure(Call<List<OrderResponseDto>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CreateComplaintActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void populateDropdown() {
        transactionItems.clear();
        seenKeys.clear();

        for (BookingResponseDto b : myBookings) {
            String name = b.getEquipmentName() != null ? b.getEquipmentName().trim() : "Unknown Equipment";
            int equipId = b.getEquipmentId();
            if (equipId > 0 && seenKeys.add("E_" + equipId)) {
                transactionItems.add(new TransactionItem(equipId, "Equipment", "Equipment: " + name));
            }
        }

        for (OrderResponseDto o : myOrders) {
            String name = o.getProductName() != null ? o.getProductName().trim() : "Unknown Product";
            int prodId = o.getProductId();
            if (prodId > 0 && seenKeys.add("P_" + prodId)) {
                transactionItems.add(new TransactionItem(prodId, "Product", "Product: " + name));
            }
        }

        List<String> displayNames = new ArrayList<>();
        for (TransactionItem item : transactionItems) displayNames.add(item.displayName);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, displayNames);
        autocompleteItem.setAdapter(adapter);

        // Automatically select the first if available
        if (!displayNames.isEmpty()) {
            autocompleteItem.setText(displayNames.get(0), false);
            selectedTransaction = transactionItems.get(0);
        } else {
            autocompleteItem.setHint("No previous transactions found");
            autocompleteItem.setEnabled(false);
            btnSubmitComplaint.setEnabled(false);
        }
    }

    private void submitComplaint() {
        if (selectedTransaction == null) {
            autocompleteItem.setError("Please select an item");
            return;
        }
        
        String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
        String desc = editDescription.getText() != null ? editDescription.getText().toString().trim() : "";

        if (title.isEmpty()) {
            editTitle.setError("Title required");
            return;
        }

        if (desc.isEmpty()) {
            editDescription.setError("Description required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmitComplaint.setEnabled(false);

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        // Combine title + description into one description field for backend
        String fullDescription = title + ": " + desc;
        CreateComplaintRequest req = new CreateComplaintRequest(
                selectedTransaction.targetId,
                selectedTransaction.targetType,
                fullDescription);

        api.submitComplaint(req).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(CreateComplaintActivity.this, "Complaint reported successfully. Our team will review it.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    btnSubmitComplaint.setEnabled(true);
                    Toast.makeText(CreateComplaintActivity.this, "Failed to submit. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSubmitComplaint.setEnabled(true);
                Toast.makeText(CreateComplaintActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class TransactionItem {
        int targetId;
        String targetType;
        String displayName;

        TransactionItem(int targetId, String targetType, String name) {
            this.targetId = targetId;
            this.targetType = targetType;
            this.displayName = name;
        }
    }
}
