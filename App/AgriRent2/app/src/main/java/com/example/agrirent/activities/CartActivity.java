package com.example.agrirent.activities;

import android.Manifest;
import com.example.agrirent.utils.SessionManager;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrirent.R;
import com.example.agrirent.adapters.CartAdapter;
import com.example.agrirent.models.CartItem;
import com.example.agrirent.models.CreateOrderRequest;
import com.example.agrirent.models.GenericResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.CartManager;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends BaseActivity implements CartAdapter.CartUpdateListener {

    private RecyclerView rvCartItems;
    private TextView tvSubtotal, tvTotalAmount;
    private View llEmptyCart, cardSummary, bottomBar;
    private android.widget.Button btnCheckout;
    private TextInputEditText etDeliveryAddress;
    private ProgressDialog progressDialog;
    
    private CartManager cartManager;
    private SessionManager session;
    private CartAdapter adapter;
    private List<CartItem> cartItems = new ArrayList<>();

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartManager = new CartManager(this);
        initViews();
        setupRecyclerView();
        updateUI();
    }

    private void initViews() {
        rvCartItems = findViewById(R.id.rvCartItems);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        llEmptyCart = findViewById(R.id.llEmptyCart);
        cardSummary = findViewById(R.id.cardSummary);
        bottomBar = findViewById(R.id.bottomBar);
        btnCheckout = findViewById(R.id.btnCheckout);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClearCart).setOnClickListener(v -> {
            cartManager.clearCart();
            updateUI();
        });

        findViewById(R.id.btnExplore).setOnClickListener(v -> {
            // Usually returns to home/marketplace
            finish();
        });

        btnCheckout.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            } else if (validateInput()) {
                startOrderPlacementProcess();
            }
        });

        findViewById(R.id.llAutoDetectLocation).setOnClickListener(v -> detectLocation());

        session = new SessionManager(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Placing Orders...");
        progressDialog.setCancelable(false);
        
        etDeliveryAddress = findViewById(R.id.etDeliveryAddress);
    }

    private boolean validateInput() {
        String address = etDeliveryAddress.getText().toString().trim();

        if (address.isEmpty()) {
            etDeliveryAddress.setError("Address required");
            etDeliveryAddress.requestFocus();
            return false;
        }

        // Check for own products
        for (CartItem item : cartItems) {
            if (item.getProduct().getSellerMobile() != null && 
                item.getProduct().getSellerMobile().equals(session.getUserMobile())) {
                Toast.makeText(this, "You cannot order your own product: " + item.getProduct().getProductName(), Toast.LENGTH_LONG).show();
                return false;
            }
        }

        return true;
    }

    private void startOrderPlacementProcess() {
        progressDialog.show();
        placeNextOrder(0);
    }

    private void placeNextOrder(int index) {
        if (index >= cartItems.size()) {
            progressDialog.dismiss();
            cartManager.clearCart();
            startActivity(new Intent(this, OrderSuccessActivity.class));
            finish();
            return;
        }

        CartItem item = cartItems.get(index);
        String contact = session.getUserMobile();
        if (contact == null || contact.equals("N/A") || contact.isEmpty()) {
            contact = "+910000000000";
        } else {
            // Strip any existing country-code prefix then prepend +91
            contact = contact.trim();
            if (contact.startsWith("+91")) {
                // Already correct format — keep as is
            } else if (contact.startsWith("91") && contact.length() == 12) {
                contact = "+" + contact;
            } else if (contact.startsWith("0") && contact.length() == 11) {
                contact = "+91" + contact.substring(1);
            } else {
                // Plain 10-digit number
                contact = "+91" + contact;
            }
        }

        CreateOrderRequest request = new CreateOrderRequest(
                item.getProduct().getProductId(),
                item.getQuantity(),
                etDeliveryAddress.getText().toString().trim(),
                contact
        );

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.placeProductOrder(request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    placeNextOrder(index + 1);
                } else {
                    progressDialog.dismiss();
                    String errMsg = "Failed to place order for " + item.getProduct().getProductName();
                    try {
                        if (response.errorBody() != null) {
                            errMsg += ": " + response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(CartActivity.this, errMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(CartActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void detectLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                Toast.makeText(this, "Could not detect location. Please enable GPS.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                etDeliveryAddress.setText(addresses.get(0).getAddressLine(0));
                Toast.makeText(this, "Location detected!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e("Cart", "Geocoder failed", e);
        } finally {
            progressDialog.dismiss();
            progressDialog.setMessage("Placing Orders...");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            detectLocation();
        }
    }

    private void setupRecyclerView() {
        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(this, cartItems, this);
        rvCartItems.setAdapter(adapter);
    }

    private void updateUI() {
        cartItems.clear();
        List<CartItem> rawItems = cartManager.getCartItems();
        // Sort by seller name for grouping (perfect view)
        Collections.sort(rawItems, (a, b) -> {
            String s1 = a.getProduct().getSeller();
            String s2 = b.getProduct().getSeller();
            if (s1 == null) return 1;
            if (s2 == null) return -1;
            return s1.compareTo(s2);
        });
        cartItems.addAll(rawItems);
        adapter.notifyDataSetChanged();
        updateTotals();
    }

    private void updateTotals() {
        if (cartItems.isEmpty()) {
            rvCartItems.setVisibility(View.GONE);
            llEmptyCart.setVisibility(View.VISIBLE);
            cardSummary.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
        } else {
            rvCartItems.setVisibility(View.VISIBLE);
            llEmptyCart.setVisibility(View.GONE);
            cardSummary.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            
            double total = cartManager.getCartTotal();
            tvSubtotal.setText("₹" + String.format("%.0f", total));
            tvTotalAmount.setText("₹" + String.format("%.0f", total));
        }
    }

    @Override
    public void onQuantityChanged(int productId, int quantity) {
        cartManager.updateQuantity(productId, quantity);
        updateTotals(); // Only update totals, don't refresh the whole list
    }

    @Override
    public void onItemDeleted(int productId) {
        cartManager.removeFromCart(productId);
        updateUI();
    }

    @Override
    public void onValidationChanged(boolean isValid) {
        if (btnCheckout != null) {
            btnCheckout.setEnabled(isValid);
            btnCheckout.setAlpha(isValid ? 1.0f : 0.5f);
        }
    }
}
