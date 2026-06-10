package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager2.widget.ViewPager2;

import com.example.agrirent.activities.CartActivity;
import com.example.agrirent.utils.CartManager;
import com.example.agrirent.R;
import com.example.agrirent.adapters.EquipmentImageSliderAdapter;
import com.example.agrirent.models.ProductItem;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrirent.adapters.RatingAdapter;
import com.example.agrirent.models.CreateRatingRequest;
import com.example.agrirent.models.RatingResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends BaseActivity {

    private ProductItem product;
    private CartManager cartManager;
    private TextView tvCartBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("product")) {
            product = (ProductItem) intent.getSerializableExtra("product");
        }

        if (product == null) {
            Toast.makeText(this, "Product details not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cartManager = new CartManager(this);
        setupViews();
        updateCartBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
    }

    private void updateCartBadge() {
        int count = cartManager.getCartCount();
        if (tvCartBadge != null) {
            if (count > 0) {
                tvCartBadge.setVisibility(android.view.View.VISIBLE);
                tvCartBadge.setText(String.valueOf(count));
            } else {
                tvCartBadge.setVisibility(android.view.View.GONE);
            }
        }
    }

    private void setupViews() {
        ViewPager2 vpProductImages = findViewById(R.id.vpProductImages);
        TabLayout tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator);
        
        TextView tvProductName = findViewById(R.id.tvProductName);
        TextView tvProductCategory = findViewById(R.id.tvProductCategory);
        TextView tvProductStock = findViewById(R.id.tvProductStock);
        
        TextView tvAverageRating = findViewById(R.id.tvAverageRating);
        TextView tvReviewCount = findViewById(R.id.tvReviewCount);
        
        TextView tvDescription = findViewById(R.id.tvProductDescription);
        TextView tvProductPrice = findViewById(R.id.tvProductPrice);
        TextView tvProductUnit = findViewById(R.id.tvProductUnit);
        
        TextView tvLocationFull = findViewById(R.id.tvProductLocation);

        android.widget.LinearLayout llOverviewContent = findViewById(R.id.llOverviewContent);
        android.widget.LinearLayout llReviewsContent = findViewById(R.id.llReviewsContent);
        android.widget.LinearLayout llLocationContent = findViewById(R.id.llLocationContent);
        TabLayout tabLayoutSections = findViewById(R.id.tabLayoutSections);

        // Tab selection logic
        tabLayoutSections.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (llOverviewContent != null) llOverviewContent.setVisibility(android.view.View.GONE);
                if (llReviewsContent != null) llReviewsContent.setVisibility(android.view.View.GONE);
                if (llLocationContent != null) llLocationContent.setVisibility(android.view.View.GONE);

                switch (tab.getPosition()) {
                    case 0: // Overview
                        if (llOverviewContent != null) llOverviewContent.setVisibility(android.view.View.VISIBLE);
                        break;
                    case 1: // Reviews
                        if (llReviewsContent != null) llReviewsContent.setVisibility(android.view.View.VISIBLE);
                        break;
                    case 2: // Location
                        if (llLocationContent != null) llLocationContent.setVisibility(android.view.View.VISIBLE);
                        break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Rating views
        if (tvAverageRating != null) {
            tvAverageRating.setText("★ " + String.format("%.1f", product.getAverageRating()));
        }
        if (tvReviewCount != null) {
            tvReviewCount.setText("(" + product.getReviewCount() + ")");
        }

        // Basic Info
        tvProductName.setText(product.getProductName() != null ? product.getProductName() : "Unknown Product");
        tvProductCategory.setText(product.getCategory() != null ? product.getCategory() : "General");
        
        int stock = product.getStock();
        tvProductStock.setText(stock + " Units Available");
        
        android.widget.LinearLayout llStockPillContainer = findViewById(R.id.llStockPillContainer);
        TextView tvStockPill = findViewById(R.id.tvStockPill);
        if (stock <= 0) {
            tvProductStock.setText("OUT OF STOCK");
            tvProductStock.setTextColor(0xFFDC2626); // Red 600
            if (llStockPillContainer != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    llStockPillContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFEE2E2)); // Red 100
                }
            }
            if (tvStockPill != null) {
                tvStockPill.setText("OUT OF STOCK");
                tvStockPill.setTextColor(0xFFD32F2F); // Red text
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    tvStockPill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFEBEE)); // Light Red background
                }
            }
        } else {
            tvProductStock.setTextColor(0xFF059669); // Green 600
            if (llStockPillContainer != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    llStockPillContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFECFDF5)); // Green 50
                }
            }
            if (tvStockPill != null) {
                tvStockPill.setText("IN STOCK");
                tvStockPill.setTextColor(0xFF2E7D32); // Green text
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    tvStockPill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE8F5E9)); // Light Green background
                }
            }
        }

        // Description
        if (tvDescription != null) {
            tvDescription.setText(product.getDescription() != null ? product.getDescription() : "No description available.");
        }

        // Price
        tvProductPrice.setText("₹" + String.format("%.0f", product.getPrice()));
        tvProductUnit.setText(product.getUnit() != null ? "/ " + product.getUnit() : "");

        // Location
        if (tvLocationFull != null) {
            tvLocationFull.setText("📍 " + (product.getLocation() != null ? product.getLocation() : "Unknown Location"));
        }

        // Seller Info
        TextView tvSellerName = findViewById(R.id.tvSellerName);
        TextView tvSellerAvatar = findViewById(R.id.tvSellerAvatar);
        
        String sellerNameStr = product.getSeller() != null && !product.getSeller().trim().isEmpty() ? product.getSeller().trim() : "Unknown Seller";
        tvSellerName.setText(sellerNameStr);
        tvSellerAvatar.setText(sellerNameStr.substring(0, 1).toUpperCase());

        // WhatsApp action
        findViewById(R.id.btnSellerWhatsApp).setOnClickListener(v -> {
            String message = getString(R.string.whatsapp_booking_message, product.getProductName());
            String sellerPhone = product.getSellerMobile();
            
            if (sellerPhone != null && !sellerPhone.isEmpty()) {
                Intent intent1 = new Intent(Intent.ACTION_VIEW);
                intent1.setData(android.net.Uri.parse("http://api.whatsapp.com/send?phone=" + sellerPhone + "&text=" + android.net.Uri.encode(message)));
                try {
                    startActivity(intent1);
                } catch (android.content.ActivityNotFoundException e) {
                    Toast.makeText(this, "WhatsApp not installed.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Seller phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Image Slider
        List<String> imageUrls = new ArrayList<>();
        if (product.getFirstImageUrl() != null && !product.getFirstImageUrl().isEmpty()) {
            imageUrls.add(product.getFirstImageUrl());
        }

        if (!imageUrls.isEmpty()) {
            EquipmentImageSliderAdapter sliderAdapter = new EquipmentImageSliderAdapter(this, imageUrls, false);
            vpProductImages.setAdapter(sliderAdapter);

            if (imageUrls.size() > 1) {
                new TabLayoutMediator(tabLayoutIndicator, vpProductImages,
                        (tab, position) -> {}).attach();
            } else {
                tabLayoutIndicator.setVisibility(android.view.View.GONE);
            }
        } else {
            tabLayoutIndicator.setVisibility(android.view.View.GONE);
        }

        // Actions
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        android.widget.Button btnBuy = findViewById(R.id.btnBuyProduct);
        com.example.agrirent.utils.SessionManager session = new com.example.agrirent.utils.SessionManager(this);
        
        boolean isOwner = false;
        if (product.getSellerMobile() != null && product.getSellerMobile().equals(session.getUserMobile())) {
            isOwner = true;
        }

        if (isOwner) {
            btnBuy.setEnabled(false);
            btnBuy.setText("YOU OWN THIS PRODUCT");
            btnBuy.setAlpha(0.6f);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                btnBuy.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6B7280)); 
            }
        } else if (product.getStock() <= 0) {
            btnBuy.setEnabled(false);
            btnBuy.setText("OUT OF STOCK");
            btnBuy.setAlpha(0.6f);
        } else {
            btnBuy.setText("ADD TO CART");
            btnBuy.setOnClickListener(v -> {
                cartManager.addToCart(product, 1);
                updateCartBadge();
                Toast.makeText(this, "Added to cart!", Toast.LENGTH_SHORT).show();
            });
        }

        android.widget.LinearLayout llReviewForm = findViewById(R.id.llReviewForm);
        if (isOwner && llReviewForm != null) {
            llReviewForm.setVisibility(android.view.View.GONE);
        }

        tvCartBadge = findViewById(R.id.tvCartBadge);
        android.view.View cartContainer = findViewById(R.id.cartContainer);
        android.view.View btnCart = findViewById(R.id.btnCart);
        
        android.view.View.OnClickListener cartListener = v -> {
            Intent intentC = new Intent(this, CartActivity.class);
            startActivity(intentC);
        };
        
        if (cartContainer != null) cartContainer.setOnClickListener(cartListener);
        if (btnCart != null) btnCart.setOnClickListener(cartListener);

        findViewById(R.id.btnFavorite).setOnClickListener(v -> {
            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
        });
        
        findViewById(R.id.btnShare).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this product on AgriRent");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Product: " + product.getProductName() + "\nPrice: ₹" + product.getPrice() + "\nLink: agrirent://product/" + product.getProductId());
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        // Review logic
        setupReviewLogic(session);
    }

    private void setupReviewLogic(com.example.agrirent.utils.SessionManager session) {
        RecyclerView rvReviews = findViewById(R.id.rvReviews);
        TextView tvNoReviews = findViewById(R.id.tvNoReviews);
        if (rvReviews != null) rvReviews.setLayoutManager(new LinearLayoutManager(this));

        // Fetch
        fetchReviews(rvReviews, tvNoReviews, session);

        // Submit
        com.google.android.material.button.MaterialButton btnSubmitReview = findViewById(R.id.btnSubmitReview);
        if (btnSubmitReview != null) {
            btnSubmitReview.setOnClickListener(v -> {
                android.widget.RatingBar ratingBarInput = findViewById(R.id.ratingBarInput);
                com.google.android.material.textfield.TextInputEditText etReviewComment = findViewById(R.id.etReviewComment);
                int ratingValue = (int) ratingBarInput.getRating();
                String comment = etReviewComment.getText() != null ? etReviewComment.getText().toString() : "";
                
                if (ratingValue < 1) {
                    Toast.makeText(this, "Please select at least 1 star.", Toast.LENGTH_SHORT).show();
                    return;
                }

                submitReview(ratingValue, comment, rvReviews, tvNoReviews, session);
            });
        }
    }

    private void fetchReviews(RecyclerView rvReviews, TextView tvNoReviews, com.example.agrirent.utils.SessionManager session) {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getRatings("Product", product.getProductId()).enqueue(new Callback<com.example.agrirent.models.IncomingReviewsResponse>() {
            @Override
            public void onResponse(Call<com.example.agrirent.models.IncomingReviewsResponse> call, Response<com.example.agrirent.models.IncomingReviewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<RatingResponse> reviews = response.body().getReviews();
                    if (reviews == null) reviews = new java.util.ArrayList<>();
                    if (reviews.isEmpty()) {
                        tvNoReviews.setVisibility(android.view.View.VISIBLE);
                        rvReviews.setVisibility(android.view.View.GONE);
                    } else {
                        tvNoReviews.setVisibility(android.view.View.GONE);
                        rvReviews.setVisibility(android.view.View.VISIBLE);
                        rvReviews.setAdapter(new RatingAdapter(reviews));
                        
                        TextView tvAvg = findViewById(R.id.tvAverageRating);
                        TextView tvCount = findViewById(R.id.tvReviewCount);
                        if (tvAvg != null) tvAvg.setText("★ " + String.format(java.util.Locale.getDefault(), "%.1f", response.body().getAverageRating()));
                        if (tvCount != null) tvCount.setText("(" + response.body().getTotalReviews() + ")");

                        // Check if current user already submitted (to hide form)
                        String myName = session.getUserName();
                        if (myName != null) {
                            for (RatingResponse r : reviews) {
                                if (myName.equalsIgnoreCase(r.getUserFullName())) {
                                    android.widget.LinearLayout llReviewForm = findViewById(R.id.llReviewForm);
                                    if (llReviewForm != null) llReviewForm.setVisibility(android.view.View.GONE);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<com.example.agrirent.models.IncomingReviewsResponse> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Failed to load reviews.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitReview(int ratingValue, String comment, RecyclerView rvReviews, TextView tvNoReviews, com.example.agrirent.utils.SessionManager session) {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        CreateRatingRequest request = new CreateRatingRequest(product.getProductId(), "Product", ratingValue, comment);
        
        api.submitRating(request).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProductDetailActivity.this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                    android.widget.LinearLayout llReviewForm = findViewById(R.id.llReviewForm);
                    if (llReviewForm != null) llReviewForm.setVisibility(android.view.View.GONE);
                    fetchReviews(rvReviews, tvNoReviews, session); // Refresh list
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Toast.makeText(ProductDetailActivity.this, "Failed: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(ProductDetailActivity.this, "Failed to submit review.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
