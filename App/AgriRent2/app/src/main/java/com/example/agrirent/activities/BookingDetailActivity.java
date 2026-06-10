package com.example.agrirent.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.agrirent.R;
import com.example.agrirent.models.BookingResponseDto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookingDetailActivity extends BaseActivity {

    private ImageView ivEquipmentImage;
    private TextView tvEquipmentName, tvStatus, tvLocation;
    private TextView tvPartyRoleLabel, tvPartyName, tvPartyMobile;
    private TextView tvStatusTimeline, tvDateRange, tvTotalPrice;

    private boolean isOwnerView = false;
    private BookingResponseDto booking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        if (getIntent() != null) {
            booking = (BookingResponseDto) getIntent().getSerializableExtra("booking");
            isOwnerView = getIntent().getBooleanExtra("isOwnerView", false);
        }

        initViews();
        if (booking != null) {
            populateData();
        }
    }

    private void initViews() {
        ivEquipmentImage = findViewById(R.id.ivEquipmentImage);
        tvEquipmentName = findViewById(R.id.tvEquipmentName);
        tvStatus = findViewById(R.id.tvStatus);
        tvLocation = findViewById(R.id.tvLocation);

        tvPartyRoleLabel = findViewById(R.id.tvPartyRoleLabel);
        tvPartyName = findViewById(R.id.tvPartyName);
        tvPartyMobile = findViewById(R.id.tvPartyMobile);

        tvStatusTimeline = findViewById(R.id.tvStatusTimeline);
        tvDateRange = findViewById(R.id.tvDateRange);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void populateData() {
        tvEquipmentName.setText(booking.getEquipmentName());
        tvStatus.setText(booking.getStatus());
        tvLocation.setText(booking.getEquipmentLocation());

        if (isOwnerView) {
            tvPartyRoleLabel.setText("Farmer Details");
            tvPartyName.setText(booking.getFarmerName());
            tvPartyMobile.setText(booking.getFarmerMobile());
        } else {
            tvPartyRoleLabel.setText("Owner Details");
            tvPartyName.setText(booking.getOwnerName());
            tvPartyMobile.setText(booking.getOwnerMobile());
        }

        tvTotalPrice.setText(String.format(Locale.getDefault(), "₹%.0f", booking.getTotalPrice()));

        if (booking.getEquipmentImageUrl() != null && !booking.getEquipmentImageUrl().isEmpty()) {
            Glide.with(this)
                .load(booking.getEquipmentImageUrl())
                .placeholder(R.drawable.ic_agrirent_logo)
                .error(R.drawable.ic_agrirent_logo)
                .centerCrop()
                .into(ivEquipmentImage);
        } else {
            ivEquipmentImage.setImageResource(R.drawable.ic_agrirent_logo);
        }

        try {
            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

            Date start = apiFormat.parse(booking.getStartDate());
            Date end = apiFormat.parse(booking.getEndDate());

            if (start != null && end != null) {
                tvDateRange.setText(displayFormat.format(start) + " - " + displayFormat.format(end));
            }
        } catch (ParseException e) {
            tvDateRange.setText(booking.getStartDate() + " to " + booking.getEndDate());
        }

        int bgRes, textCol;
        String status = booking.getStatus() != null ? booking.getStatus() : "Pending";
        if ("Accepted".equalsIgnoreCase(status)) {
            bgRes = R.drawable.bg_status_active;
            textCol = R.color.status_active_text;
        } else if ("Rejected".equalsIgnoreCase(status)) {
            bgRes = R.drawable.bg_status_inactive;
            textCol = R.color.status_inactive_text;
        } else {
            bgRes = R.drawable.bg_status_pending;
            textCol = R.color.status_pending_text;
        }
        
        tvStatus.setBackgroundResource(bgRes);
        tvStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, textCol));
        tvStatusTimeline.setText(buildBookingTimeline(status));

        findViewById(R.id.btnCallParty).setOnClickListener(v -> {
            String mobile = isOwnerView ? booking.getFarmerMobile() : booking.getOwnerMobile();
            if (mobile != null && !mobile.isEmpty()) {
                android.content.Intent callIntent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
                callIntent.setData(android.net.Uri.parse("tel:" + mobile));
                startActivity(callIntent);
            } else {
                android.widget.Toast.makeText(this, "Mobile number not available", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnWhatsApp).setOnClickListener(v -> {
            String mobile = isOwnerView ? booking.getFarmerMobile() : booking.getOwnerMobile();
            if (mobile != null && !mobile.isEmpty()) {
                String formattedMobile = mobile.replaceAll("[^0-9]", "");
                if (formattedMobile.length() == 10) {
                    formattedMobile = "91" + formattedMobile;
                }
                
                String defaultMessage = getString(R.string.whatsapp_booking_message, booking.getEquipmentName());
                String url = "https://api.whatsapp.com/send?phone=" + formattedMobile + "&text=" + android.net.Uri.encode(defaultMessage);
                
                android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                i.setData(android.net.Uri.parse(url));
                try {
                    startActivity(i);
                } catch (android.content.ActivityNotFoundException e) {
                    android.widget.Toast.makeText(this, "WhatsApp not installed", android.widget.Toast.LENGTH_SHORT).show();
                }
            } else {
                android.widget.Toast.makeText(this, "Mobile number not available", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String buildBookingTimeline(String status) {
        if ("Accepted".equalsIgnoreCase(status)) {
            return "Timeline: Requested → Accepted → Ready for pickup";
        }
        if ("Rejected".equalsIgnoreCase(status)) {
            return "Timeline: Requested → Rejected";
        }
        return "Timeline: Requested → Waiting for owner review";
    }
}
