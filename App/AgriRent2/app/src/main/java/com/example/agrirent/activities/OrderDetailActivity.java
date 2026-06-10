package com.example.agrirent.activities;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.agrirent.R;
import com.example.agrirent.models.OrderResponseDto;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.android.material.button.MaterialButton;
import android.widget.ImageView;
import android.net.Uri;
import android.content.Intent;
import android.util.Log;
import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.util.HashMap;
import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailActivity extends BaseActivity {

    private OrderResponseDto order;
    private boolean isSellerView;

    private TextView tvOrderStatus, tvOrderId, tvOrderDate;
    private TextView tvDetailsProductName, tvDetailsQuantity, tvDetailsUnitPrice, tvDetailsTotal;
    private TextView tvContactPartyLabel, tvContactPartyName, tvContactNumber, tvDeliveryAddress;
    private TextView tvStatusTimeline;
    private ImageView ivProductPhoto;

    private LinearLayout llSellerActions, llBuyerActions;
    private MaterialButton btnReject, btnApprove, btnMarkDelivered, btnCancelOrder;
    private ImageButton btnBack;
    
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnWhatsApp, btnCallParty;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        order = (OrderResponseDto) getIntent().getSerializableExtra("order");
        isSellerView = getIntent().getBooleanExtra("isSellerView", false);

        if (order == null) {
            Toast.makeText(this, "Order details not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupData();
        setupListeners();
    }

    private void initViews() {
        tvOrderStatus = findViewById(R.id.tvOrderStatus);
        tvOrderId = findViewById(R.id.tvOrderId);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        tvDetailsProductName = findViewById(R.id.tvDetailsProductName);
        tvDetailsQuantity = findViewById(R.id.tvDetailsQuantity);
        tvDetailsUnitPrice = findViewById(R.id.tvDetailsUnitPrice);
        tvDetailsTotal = findViewById(R.id.tvDetailsTotal);

        tvContactPartyLabel = findViewById(R.id.tvContactPartyLabel);
        tvContactPartyName = findViewById(R.id.tvContactPartyName);
        tvContactNumber = findViewById(R.id.tvContactNumber);
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);
        tvStatusTimeline = findViewById(R.id.tvStatusTimeline);
        ivProductPhoto = findViewById(R.id.ivProductPhoto);

        llSellerActions = findViewById(R.id.llSellerActions);
        llBuyerActions = findViewById(R.id.llBuyerActions);

        btnReject = findViewById(R.id.btnReject);
        btnApprove = findViewById(R.id.btnApprove);
        btnMarkDelivered = findViewById(R.id.btnMarkDelivered);
        btnCancelOrder = findViewById(R.id.btnCancelOrder);
        btnBack = findViewById(R.id.btnBack);
        btnWhatsApp = findViewById(R.id.btnWhatsApp);
        btnCallParty = findViewById(R.id.btnCallParty);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating...");
        progressDialog.setCancelable(false);
    }

    private void setupData() {
        updateStatusUI(order.getStatus());
        tvOrderId.setText("Order ID: #" + order.getOrderId());

        try {
            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy  h:mm a", Locale.getDefault());
            Date date = apiFormat.parse(order.getOrderDate());
            if (date != null) {
                tvOrderDate.setText("Placed on: " + displayFormat.format(date));
            }
        } catch (ParseException | NullPointerException e) {
            tvOrderDate.setText("Placed on: " + order.getOrderDate());
        }

        tvDetailsProductName.setText(order.getProductName());
        tvDetailsQuantity.setText(String.valueOf(order.getQuantity()));
        tvDetailsUnitPrice.setText("₹" + order.getUnitPrice());
        tvDetailsTotal.setText("₹" + order.getTotalPrice());

        tvDeliveryAddress.setText(order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "N/A");

        if (ivProductPhoto != null) {
            Glide.with(this)
                    .load(order.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(ivProductPhoto);
        }

        if (isSellerView) {
            tvContactPartyLabel.setText("Buyer Name");
            tvContactPartyName.setText(order.getFarmerName() != null ? order.getFarmerName() : "N/A");
            tvContactNumber.setText(order.getContactNumber() != null ? order.getContactNumber() : "N/A");

            // Seller Actions: Pending -> Approve/Reject, Approved/Processing/Shipped -> Mark Delivered
            String status = order.getStatus().toLowerCase();
            if (status.equals("pending")) {
                llSellerActions.setVisibility(View.VISIBLE);
                btnReject.setVisibility(View.VISIBLE);
                btnApprove.setVisibility(View.VISIBLE);
                btnMarkDelivered.setVisibility(View.GONE);
            } else if (status.equals("approved") || status.equals("processing") || status.equals("shipped")) {
                llSellerActions.setVisibility(View.VISIBLE);
                btnReject.setVisibility(View.GONE);
                btnApprove.setVisibility(View.GONE);
                btnMarkDelivered.setVisibility(View.VISIBLE);
            } else {
                llSellerActions.setVisibility(View.GONE);
            }
        } else {
            tvContactPartyLabel.setText("Seller Name");
            tvContactPartyName.setText(order.getSellerName() != null ? order.getSellerName() : "N/A");
            tvContactNumber.setText(order.getSellerMobile() != null ? order.getSellerMobile() : "N/A");

            // Buyer Actions: Only show cancel if pending
            if (order.getStatus().equalsIgnoreCase("Pending")) {
                llBuyerActions.setVisibility(View.VISIBLE);
            } else {
                llBuyerActions.setVisibility(View.GONE);
            }
        }
    }

    private void updateStatusUI(String status) {
        if (status == null) status = "Unknown";
        tvOrderStatus.setText(status);
        
        switch (status.toLowerCase()) {
            case "pending":
                tvOrderStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvOrderStatus.setTextColor(Color.parseColor("#F59E0B")); // amber
                break;
            case "approved":
            case "processing":
            case "shipped":
                tvOrderStatus.setBackgroundResource(R.drawable.bg_status_approved);
                tvOrderStatus.setTextColor(Color.parseColor("#3B82F6")); // blue
                break;
            case "delivered":
                tvOrderStatus.setBackgroundResource(R.drawable.bg_status_delivered);
                tvOrderStatus.setTextColor(Color.parseColor("#10B981")); // green
                break;
            case "canceled":
            case "cancelled":
                tvOrderStatus.setBackgroundResource(R.drawable.bg_status_canceled);
                tvOrderStatus.setTextColor(Color.parseColor("#EF4444")); // red
                break;
            default:
                tvOrderStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvOrderStatus.setTextColor(Color.parseColor("#6B7280")); // gray
                break;
        }
        if (tvStatusTimeline != null) {
            tvStatusTimeline.setText(buildOrderTimeline(status));
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnApprove.setOnClickListener(v -> updateOrderStatus("Approved"));
        btnReject.setOnClickListener(v -> updateOrderStatus("Canceled"));
        btnMarkDelivered.setOnClickListener(v -> updateOrderStatus("Delivered"));

        btnCancelOrder.setOnClickListener(v -> cancelOrder());

        btnWhatsApp.setOnClickListener(v -> openWhatsApp());
        btnCallParty.setOnClickListener(v -> dialNumber());
    }

    private void openWhatsApp() {
        String number = isSellerView ? order.getContactNumber() : order.getSellerMobile();
        if (number == null || number.isEmpty()) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = "Hi, I am contacting you regarding my order #" + order.getOrderId() + " (" + order.getProductName() + ") on AgriRent.";

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=" + number.replace("+", "").replace(" ", "") + "&text=" + Uri.encode(message);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void dialNumber() {
        String number = isSellerView ? order.getContactNumber() : order.getSellerMobile();
        if (number == null || number.isEmpty()) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);
    }

    private void updateOrderStatus(String newStatus) {
        progressDialog.show();
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

        okhttp3.RequestBody statusJson = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json"),
                "\"" + newStatus + "\"");

        Call<ResponseBody> call = apiService.updateSellerOrderStatus(order.getOrderId(), statusJson);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    Toast.makeText(OrderDetailActivity.this, "Order marked as " + newStatus, Toast.LENGTH_SHORT).show();
                    order.setStatus(newStatus);
                    setupData(); // refresh UI to hide buttons and update badge
                } else {
                    String errorMsg = "Failed to update order";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    Toast.makeText(OrderDetailActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(OrderDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelOrder() {
        progressDialog.show();
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        Call<String> call = apiService.cancelProductOrder(order.getOrderId());

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    Toast.makeText(OrderDetailActivity.this, "Order canceled successfully", Toast.LENGTH_SHORT).show();
                    order.setStatus("Canceled");
                    setupData(); // refresh UI
                } else {
                    Toast.makeText(OrderDetailActivity.this, "Failed to cancel order", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(OrderDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String buildOrderTimeline(String status) {
        String normalized = status == null ? "" : status.toLowerCase();
        if (normalized.contains("deliver")) {
            return "Timeline: Placed → Approved → Shipped → Delivered";
        }
        if (normalized.contains("ship")) {
            return "Timeline: Placed → Approved → Shipped";
        }
        if (normalized.contains("process") || normalized.contains("approve")) {
            return "Timeline: Placed → Approved → Processing";
        }
        if (normalized.contains("cancel")) {
            return "Timeline: Placed → Canceled";
        }
        return "Timeline: Placed → Awaiting seller approval";
    }
}
