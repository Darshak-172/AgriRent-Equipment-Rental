package com.example.agrirent.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.agrirent.R;
import com.example.agrirent.models.AddEquipmentRequest;
import com.example.agrirent.models.Equipment;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.models.ImageUploadResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditEquipmentActivity extends BaseActivity {

    private ImageView btnBack;
    private EditText etName, etDescription, etLocation, etHourly, etDaily;
    private MaterialButton btnSubmit, btnDetect;
    private LinearLayout llPhotoContainer;
    private TextView tvPhotoCount, tvLoadingText;
    private FrameLayout flLoadingOverlay;
    private View cardAddPhoto;

    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final int CAPTURE_IMAGE_REQUEST = 1002;
    private static final int LOCATION_PERMISSION_REQUEST = 1003;

    private List<Uri> selectedNewImages = new ArrayList<>();
    private List<String> existingImageUrls = new ArrayList<>();
    private Equipment equipment;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_equipment);

        equipment = (Equipment) getIntent().getSerializableExtra("equipment");
        if (equipment == null) {
            Toast.makeText(this, "Equipment data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        prefillData();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etHourly = findViewById(R.id.etHourly);
        etDaily = findViewById(R.id.etDaily);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnDetect = findViewById(R.id.btnDetect);
        llPhotoContainer = findViewById(R.id.llPhotoContainer);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);
        flLoadingOverlay = findViewById(R.id.flLoadingOverlay);
        tvLoadingText = findViewById(R.id.tvLoadingText);
        cardAddPhoto = findViewById(R.id.cardAddPhoto);
    }

    private void prefillData() {
        etName.setText(equipment.getEquipmentName());
        etDescription.setText(equipment.getDescription());
        etLocation.setText(equipment.getLocation());
        etHourly.setText(String.valueOf(equipment.getHourlyPrice()));
        etDaily.setText(String.valueOf(equipment.getDailyPrice()));

        // Handle existing images
        existingImageUrls.clear();
        if (equipment.getImages() != null && !equipment.getImages().isEmpty()) {
            for (Equipment.EquipmentImage img : equipment.getImages()) {
                if (img.getImageUrl() != null) existingImageUrls.add(img.getImageUrl());
            }
        } else if (equipment.getImageUrl() != null && !equipment.getImageUrl().isEmpty()) {
            existingImageUrls.add(equipment.getImageUrl());
        }

        updatePhotoUI();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        cardAddPhoto.setOnClickListener(v -> showImagePickerDialog());
        btnDetect.setOnClickListener(v -> checkLocationPermission());
        btnSubmit.setOnClickListener(v -> validateAndUpdate());
    }

    private void showImagePickerDialog() {
        if (selectedNewImages.size() + existingImageUrls.size() >= 5) {
            Toast.makeText(this, "Maximum 5 photos allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Add Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) captureImage();
                    else pickImage();
                }).show();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAPTURE_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    selectedNewImages.add(uri);
                    updatePhotoUI();
                }
            } else if (requestCode == CAPTURE_IMAGE_REQUEST && data != null) {
                // Handle captured image
                Uri uri = data.getData();
                 if (uri != null) {
                    selectedNewImages.add(uri);
                    updatePhotoUI();
                }
            }
        }
    }

    private void updatePhotoUI() {
        llPhotoContainer.removeAllViews();
        findViewById(R.id.hsvPhotoContainer).setVisibility(
                (selectedNewImages.isEmpty() && existingImageUrls.isEmpty()) ? View.GONE : View.VISIBLE
        );
        tvPhotoCount.setVisibility(View.VISIBLE);
        tvPhotoCount.setText((selectedNewImages.size() + existingImageUrls.size()) + "/5 Photos");

        // Existing Photos
        for (String url : existingImageUrls) {
            addExistingPhotoThumb(url);
        }

        // New Photos
        for (Uri uri : selectedNewImages) {
            addNewPhotoThumb(uri);
        }
    }

    private void addExistingPhotoThumb(String url) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_photo_thumb, llPhotoContainer, false);
        ImageView ivThumb = view.findViewById(R.id.ivThumb);
        ImageView ivRemove = view.findViewById(R.id.ivRemove);

        Glide.with(this).load(url).centerCrop().into(ivThumb);
        ivRemove.setOnClickListener(v -> {
            existingImageUrls.remove(url);
            updatePhotoUI();
        });
        llPhotoContainer.addView(view);
    }

    private void addNewPhotoThumb(Uri uri) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_photo_thumb, llPhotoContainer, false);
        ImageView ivThumb = view.findViewById(R.id.ivThumb);
        ImageView ivRemove = view.findViewById(R.id.ivRemove);

        Glide.with(this).load(uri).centerCrop().into(ivThumb);
        ivRemove.setOnClickListener(v -> {
            selectedNewImages.remove(uri);
            updatePhotoUI();
        });
        llPhotoContainer.addView(view);
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            detectLocation();
        }
    }

    private void detectLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    updateAddressFromLocation(location);
                } else {
                    Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                String fullAddress = "";
                if (addr.getLocality() != null) fullAddress += addr.getLocality() + ", ";
                if (addr.getSubAdminArea() != null) fullAddress += addr.getSubAdminArea() + ", ";
                if (addr.getAdminArea() != null) fullAddress += addr.getAdminArea();
                etLocation.setText(fullAddress);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Geocoder failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateAndUpdate() {
        String name = etName.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String loc = etLocation.getText().toString().trim();
        String hourlyStr = etHourly.getText().toString().trim();
        String dailyStr = etDaily.getText().toString().trim();

        if (name.isEmpty() || desc.isEmpty() || loc.isEmpty() || hourlyStr.isEmpty() || dailyStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedNewImages.isEmpty() && existingImageUrls.isEmpty()) {
             Toast.makeText(this, "At least one photo is required.", Toast.LENGTH_SHORT).show();
             return;
        }

        double hourly, daily;
        try {
            hourly = Double.parseDouble(hourlyStr);
            daily = Double.parseDouble(dailyStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show();
            return;
        }

        tvLoadingText.setText("Saving...");
        flLoadingOverlay.setVisibility(View.VISIBLE);

        if (!selectedNewImages.isEmpty()) {
            uploadImagesSequentially(0, new ArrayList<>(existingImageUrls), name, desc, loc, hourly, daily);
        } else {
            submitUpdate(name, desc, loc, hourly, daily, existingImageUrls);
        }
    }

    private void uploadImagesSequentially(int index, List<String> finalUrls, String name, String desc, String loc, double hourly, double daily) {
        if (index >= selectedNewImages.size()) {
            submitUpdate(name, desc, loc, hourly, daily, finalUrls);
            return;
        }

        tvLoadingText.setText("Uploading photo " + (index + 1) + "/" + selectedNewImages.size() + "...");
        Uri uri = selectedNewImages.get(index);
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

        try {
            InputStream is = getContentResolver().openInputStream(uri);
            byte[] bytes = readBytes(is);
            RequestBody requestBody = RequestBody.create(bytes, MediaType.parse("image/*"));
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "eq_img_" + index + ".jpg", requestBody);
            RequestBody idBody = RequestBody.create(String.valueOf(equipment.getEquipmentId()), MediaType.parse("text/plain"));

            apiService.uploadEquipmentImage(idBody, imagePart).enqueue(new Callback<ImageUploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<ImageUploadResponse> call, @NonNull Response<ImageUploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        finalUrls.add(response.body().getImageUrl());
                        uploadImagesSequentially(index + 1, finalUrls, name, desc, loc, hourly, daily);
                    } else {
                        flLoadingOverlay.setVisibility(View.GONE);
                        Toast.makeText(EditEquipmentActivity.this, "Failed to upload photo " + (index+1), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ImageUploadResponse> call, @NonNull Throwable t) {
                    flLoadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(EditEquipmentActivity.this, "Upload error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (IOException e) {
            flLoadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Error reading image", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void submitUpdate(String name, String desc, String loc, double hourly, double daily, List<String> imageUrls) {
        tvLoadingText.setText("Finalizing update...");
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

        AddEquipmentRequest request = new AddEquipmentRequest(name, desc, loc, hourly, daily, equipment.getSubCategoryId());
        request.setImageUrls(imageUrls);

        apiService.updateEquipment(equipment.getEquipmentId(), request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                flLoadingOverlay.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(EditEquipmentActivity.this, "Equipment updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EditEquipmentActivity.this, "Update failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                flLoadingOverlay.setVisibility(View.GONE);
                Toast.makeText(EditEquipmentActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            detectLocation();
        }
    }
}
