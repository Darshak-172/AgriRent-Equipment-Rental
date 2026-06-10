package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.agrirent.utils.SessionManager;
import com.example.agrirent.R;
import com.example.agrirent.adapters.EquipmentImageSliderAdapter;
import com.example.agrirent.models.Equipment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
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

public class EquipmentDetailActivity extends BaseActivity {

    private Equipment equipment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_detail);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("equipment")) {
            equipment = (Equipment) intent.getSerializableExtra("equipment");
        }

        if (equipment == null) {
            Toast.makeText(this, "Equipment details not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupViews();
    }

    private void setupViews() {
        ViewPager2 vpEquipmentImages = findViewById(R.id.vpEquipmentImages);
        TabLayout tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator);
        
        TextView tvAverageRating = findViewById(R.id.tvAverageRating);
        TextView tvReviewCount = findViewById(R.id.tvReviewCount);
        
        TextView tvEquipmentName = findViewById(R.id.tvEquipmentName);
        TextView tvEquipmentCategory = findViewById(R.id.tvEquipmentCategory);
        TextView tvEquipmentSubCategory = findViewById(R.id.tvEquipmentSubCategory);
        
        TextView tvDescription = findViewById(R.id.tvEquipmentDescription);
        TextView tvHourlyPrice = findViewById(R.id.tvHourlyPrice);
        TextView tvDailyPrice = findViewById(R.id.tvDailyPrice);
        TextView tvLocationFull = findViewById(R.id.tvEquipmentLocation);

        android.widget.LinearLayout llOverviewContent = findViewById(R.id.llOverviewContent);
        android.widget.LinearLayout llReviewsContent = findViewById(R.id.llReviewsContent);
        android.widget.LinearLayout llLocationContent = findViewById(R.id.llLocationContent);
        TabLayout tabLayoutSections = findViewById(R.id.tabLayoutSections);
        
        // Rating views
        if (tvAverageRating != null) {
            tvAverageRating.setText("★ " + String.format("%.1f", equipment.getAverageRating()));
        }
        if (tvReviewCount != null) {
            tvReviewCount.setText("(" + equipment.getReviewCount() + ")");
        }

        // Tab selection logic
        tabLayoutSections.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                llOverviewContent.setVisibility(android.view.View.GONE);
                llReviewsContent.setVisibility(android.view.View.GONE);
                llLocationContent.setVisibility(android.view.View.GONE);

                switch (tab.getPosition()) {
                    case 0: // Overview
                        llOverviewContent.setVisibility(android.view.View.VISIBLE);
                        break;
                    case 1: // Reviews
                        llReviewsContent.setVisibility(android.view.View.VISIBLE);
                        break;
                    case 2: // Location
                        llLocationContent.setVisibility(android.view.View.VISIBLE);
                        break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Name
        tvEquipmentName.setText(equipment.getEquipmentName());

        // Subtitle (Category & Subcategory)
        String category = equipment.getCategoryName() != null ? equipment.getCategoryName() : "General";
        tvEquipmentCategory.setText(category);
        
        android.view.View llSubCategoryPill = findViewById(R.id.llSubCategoryPillContainer);
        if (equipment.getSubCategoryName() != null && !equipment.getSubCategoryName().isEmpty()) {
            tvEquipmentSubCategory.setText(equipment.getSubCategoryName());
            if (llSubCategoryPill != null) llSubCategoryPill.setVisibility(android.view.View.VISIBLE);
            else tvEquipmentSubCategory.setVisibility(android.view.View.VISIBLE);
        } else {
            if (llSubCategoryPill != null) llSubCategoryPill.setVisibility(android.view.View.GONE);
            else tvEquipmentSubCategory.setVisibility(android.view.View.GONE);
        }
        
        TextView tvAvailablePill = findViewById(R.id.tvAvailablePill);
        if (tvAvailablePill != null) {
            String status = equipment.getStatus();
            // If status is not null and not "Active", consider it unavailable/in-use/maintenance
            if (status != null && !status.equalsIgnoreCase("Active")) {
                tvAvailablePill.setText(status.toUpperCase());
                tvAvailablePill.setTextColor(0xFFD32F2F); // Red text
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    tvAvailablePill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFEBEE)); // Light red background
                }
            } else {
                tvAvailablePill.setText("AVAILABLE");
                tvAvailablePill.setTextColor(0xFF2E7D32); // Green text
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    tvAvailablePill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE8F5E9)); // Light green background
                }
            }
        }

        // Description
        if (tvDescription != null) {
            tvDescription.setText(equipment.getDescription() != null ? equipment.getDescription() : "No description available.");
        }

        // Price
        String hourly = "₹" + String.format("%.0f", equipment.getHourlyPrice()) + "/hr";
        String daily = "₹" + String.format("%.0f", equipment.getDailyPrice()) + "/day";
        tvHourlyPrice.setText(hourly);
        tvDailyPrice.setText(daily);

        // Location
        if (tvLocationFull != null) {
            tvLocationFull.setText("📍 " + (equipment.getLocation() != null ? equipment.getLocation() : "Unknown Location"));
        }

        android.view.View cvViewOnMap = findViewById(R.id.cvViewOnMap);
        if (cvViewOnMap != null) {
            cvViewOnMap.setOnClickListener(v -> {
                String locationQuery = equipment.getLocation() != null ? equipment.getLocation() : "India";
                android.net.Uri gmmIntentUri = android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(locationQuery));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                try {
                    startActivity(mapIntent);
                } catch (android.content.ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + android.net.Uri.encode(locationQuery))));
                }
            });
        }

        TextView tvOwnerName = findViewById(R.id.tvOwnerName);
        TextView tvOwnerAvatar = findViewById(R.id.tvOwnerAvatar);
        
        // ... (existing code for tabs, etc is above)

        // Owner
        if (equipment.getOwnerName() != null && !equipment.getOwnerName().isEmpty()) {
            String owner = equipment.getOwnerName();
            tvOwnerName.setText(owner);
            tvOwnerAvatar.setText(String.valueOf(owner.charAt(0)).toUpperCase());
        } else {
            tvOwnerAvatar.setText("?");
        }

        findViewById(R.id.btnOwnerWhatsApp).setOnClickListener(v -> {
            String message = getString(R.string.whatsapp_booking_message, equipment.getEquipmentName());
            String ownerPhone = equipment.getOwnerMobile();
            
            if (ownerPhone != null && !ownerPhone.isEmpty()) {
                Intent intent1 = new Intent(Intent.ACTION_VIEW);
                intent1.setData(android.net.Uri.parse("http://api.whatsapp.com/send?phone=" + ownerPhone + "&text=" + android.net.Uri.encode(message)));
                try {
                    startActivity(intent1);
                } catch (android.content.ActivityNotFoundException e) {
                    Toast.makeText(this, "WhatsApp not installed.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Owner phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Image Slider
        List<String> imageUrls = new ArrayList<>();
        if (equipment.getImages() != null && !equipment.getImages().isEmpty()) {
            for (Equipment.EquipmentImage img : equipment.getImages()) {
                imageUrls.add(img.getImageUrl());
            }
        } else if (equipment.getThumbnailUrl() != null && !equipment.getThumbnailUrl().isEmpty()) {
            imageUrls.add(equipment.getThumbnailUrl());
        }

        if (!imageUrls.isEmpty()) {
            EquipmentImageSliderAdapter sliderAdapter = new EquipmentImageSliderAdapter(this, imageUrls);
            vpEquipmentImages.setAdapter(sliderAdapter);

            if (imageUrls.size() > 1) {
                new TabLayoutMediator(tabLayoutIndicator, vpEquipmentImages,
                        (tab, position) -> {
                            // Empty config, just need the dots
                        }).attach();
            } else {
                tabLayoutIndicator.setVisibility(android.view.View.GONE);
            }
        } else {
            tabLayoutIndicator.setVisibility(android.view.View.GONE);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Hide Book button and Review Form if this is the user's own equipment
        SessionManager session = new SessionManager(this);
        String loggedInUser = session.getUserName();
        String ownerName = equipment.getOwnerName();
        boolean isOwner = loggedInUser != null && ownerName != null &&
                loggedInUser.trim().equalsIgnoreCase(ownerName.trim());

        android.widget.LinearLayout llReviewForm = findViewById(R.id.llReviewForm);
        if (isOwner && llReviewForm != null) {
            llReviewForm.setVisibility(android.view.View.GONE);
        }

        com.google.android.material.button.MaterialButton btnBook =
                findViewById(R.id.btnBookEquipment);
        if (isOwner) {
            btnBook.setEnabled(false);
            btnBook.setText("Your Equipment");
            btnBook.setAlpha(0.5f);
        } else {
            btnBook.setOnClickListener(v -> {
                Intent scheduleIntent = new Intent(EquipmentDetailActivity.this, ScheduleBookingActivity.class);
                scheduleIntent.putExtra("equipment", equipment);
                startActivity(scheduleIntent);
            });
        }
        findViewById(R.id.btnFavorite).setOnClickListener(v -> {
             Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnShare).setOnClickListener(v -> {
             Toast.makeText(this, "Sharing...", Toast.LENGTH_SHORT).show();
        });

        // Review logic
        setupReviewLogic(session);
    }

    private void setupReviewLogic(SessionManager session) {
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

    private void fetchReviews(RecyclerView rvReviews, TextView tvNoReviews, SessionManager session) {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getRatings("Equipment", equipment.getEquipmentId()).enqueue(new Callback<com.example.agrirent.models.IncomingReviewsResponse>() {
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
                Toast.makeText(EquipmentDetailActivity.this, "Failed to load reviews.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitReview(int ratingValue, String comment, RecyclerView rvReviews, TextView tvNoReviews, SessionManager session) {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        CreateRatingRequest request = new CreateRatingRequest(equipment.getEquipmentId(), "Equipment", ratingValue, comment);
        
        api.submitRating(request).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EquipmentDetailActivity.this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                    android.widget.LinearLayout llReviewForm = findViewById(R.id.llReviewForm);
                    if (llReviewForm != null) llReviewForm.setVisibility(android.view.View.GONE);
                    fetchReviews(rvReviews, tvNoReviews, session); // Refresh list
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Toast.makeText(EquipmentDetailActivity.this, "Failed: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(EquipmentDetailActivity.this, "Failed to submit review.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                Toast.makeText(EquipmentDetailActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
