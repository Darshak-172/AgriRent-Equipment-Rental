package com.example.agrirent.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.agrirent.R;
import com.example.agrirent.models.CreateOrderRequest;
import com.example.agrirent.models.GenericResponse;
import com.example.agrirent.models.ProductItem;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaceOrderActivity extends BaseActivity {

    private ImageView ivProductImage;
    private TextView tvProductName, tvProductPrice, tvAvailableStock, tvTotalPrice, tvAutoDetectLocation;
    private TextInputEditText etQuantity, etContactNumber, etDeliveryAddress;
    private MaterialButton btnConfirmOrder;
    private ImageButton btnBack;

    private ProductItem product;
    private ProgressDialog progressDialog;
    
    // Default logged in user number just as a fallback, would normally get this from AuthManager
    private String defaultContactNumber = "9999999999"; 
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);

        product = (ProductItem) getIntent().getSerializableExtra("product");
        if (product == null) {
            Toast.makeText(this, "Product details not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupData();
        setupListeners();
    }

    private void initViews() {
        ivProductImage = findViewById(R.id.ivProductImage);
        tvProductName = findViewById(R.id.tvProductName);
        tvProductPrice = findViewById(R.id.tvProductPrice);
        tvAvailableStock = findViewById(R.id.tvAvailableStock);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvAutoDetectLocation = findViewById(R.id.tvAutoDetectLocation);

        etQuantity = findViewById(R.id.etQuantity);
        etContactNumber = findViewById(R.id.etContactNumber);
        etDeliveryAddress = findViewById(R.id.etDeliveryAddress);

        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);
        btnBack = findViewById(R.id.btnBack);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing...");
        progressDialog.setCancelable(false);
    }

    private void setupData() {
        tvProductName.setText(product.getProductName());
        tvProductPrice.setText("₹" + product.getPrice() + " / " + product.getUnit());
        tvAvailableStock.setText("Available: " + product.getStock() + " " + product.getUnit());

        if (product.getFirstImageUrl() != null && !product.getFirstImageUrl().isEmpty()) {
            Glide.with(this)
                 .load(product.getFirstImageUrl())
                 .placeholder(R.drawable.ic_agrirent_logo)
                 .centerCrop()
                 .into(ivProductImage);
        }

        etQuantity.setText("1");
        etContactNumber.setText(defaultContactNumber); // Might integrate AuthManager in real scenario
        updateTotalPrice();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        etQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTotalPrice();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        tvAutoDetectLocation.setOnClickListener(v -> detectLocation());

        btnConfirmOrder.setOnClickListener(v -> {
            if (validateInput()) {
                placeOrder();
            }
        });
    }

    private void updateTotalPrice() {
        String qtyStr = etQuantity.getText().toString().trim();
        if (qtyStr.isEmpty()) {
            tvTotalPrice.setText("₹0");
            return;
        }

        try {
            int quantity = Integer.parseInt(qtyStr);
            if (quantity > product.getStock()) {
                etQuantity.setError("Cannot exceed available stock");
                tvTotalPrice.setText("₹0");
            } else if (quantity <= 0) {
                etQuantity.setError("Quantity must be at least 1");
                tvTotalPrice.setText("₹0");
            } else {
                etQuantity.setError(null);
                double total = quantity * product.getPrice();
                tvTotalPrice.setText("₹" + total);
            }
        } catch (NumberFormatException e) {
            tvTotalPrice.setText("₹0");
        }
    }

    private boolean validateInput() {
        String qtyStr = etQuantity.getText().toString().trim();
        String address = etDeliveryAddress.getText().toString().trim();

        if (qtyStr.isEmpty() || Integer.parseInt(qtyStr) <= 0 || Integer.parseInt(qtyStr) > product.getStock()) {
            Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (address.isEmpty()) {
            etDeliveryAddress.setError("Delivery address is required");
            return false;
        }

        return true;
    }
    
    private void detectLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED 
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        progressDialog.setMessage("Detecting location...");
        progressDialog.show();
        
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                getAddressFromLocation(location.getLatitude(), location.getLongitude());
            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Could not get current location. Please ensure location services are enabled.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);
                etDeliveryAddress.setText(fullAddress);
                Toast.makeText(this, "Location detected!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e("PlaceOrder", "Geocoder failed", e);
            Toast.makeText(this, "Failed to resolve address", Toast.LENGTH_SHORT).show();
        } finally {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                detectLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void placeOrder() {
        progressDialog.setMessage("Placing Order...");
        progressDialog.show();

        int quantity = Integer.parseInt(etQuantity.getText().toString().trim());
        String contact = etContactNumber.getText().toString().trim();
        if (!contact.startsWith("+91")) {
            contact = "+91" + contact;
        }
        String address = etDeliveryAddress.getText().toString().trim();

        CreateOrderRequest request = new CreateOrderRequest(
                product.getProductId(),
                quantity,
                address,
                contact
        );

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        Call<GenericResponse> call = apiService.placeProductOrder(request);

        call.enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(PlaceOrderActivity.this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PlaceOrderActivity.this, "Failed to place order. Try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(PlaceOrderActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
