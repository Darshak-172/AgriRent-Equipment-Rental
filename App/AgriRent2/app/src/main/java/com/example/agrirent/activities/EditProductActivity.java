package com.example.agrirent.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.agrirent.R;
import com.example.agrirent.models.AddProductRequest;
import com.example.agrirent.models.ImageUploadResponse;
import com.example.agrirent.models.ProductItem;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProductActivity extends BaseActivity {

    private static final int MAX_PHOTOS = 5;
    private static final int LOCATION_PERMISSION_REQUEST = 101;
    private static final int CAMERA_PERMISSION_REQUEST = 102;

    private ImageView btnBack;
    private MaterialCardView cardAddPhoto;
    private HorizontalScrollView hsvPhotoContainer;
    private LinearLayout llPhotoContainer;
    private TextView tvPhotoCount;
    private EditText etName, etDescription, etPrice, etQuantity, etLocation;
    private AutoCompleteTextView actvUnit;
    private MaterialButton btnSubmit, btnDetect;
    private FrameLayout flLoadingOverlay;
    private TextView tvLoadingText;

    private ApiService apiService;
    private ProductItem product;
    private FusedLocationProviderClient fusedLocationClient;

    private final List<Uri> selectedNewImages = new ArrayList<>();
    private final List<String> existingImageUrls = new ArrayList<>();
    private Uri cameraImageUri;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            if ((selectedNewImages.size() + existingImageUrls.size()) < MAX_PHOTOS) {
                                addImageToList(imageUri);
                            }
                        }
                    } else if (data.getData() != null) {
                        if ((selectedNewImages.size() + existingImageUrls.size()) < MAX_PHOTOS) {
                            addImageToList(data.getData());
                        }
                    }
                    updatePhotoVisibility();
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                    addImageToList(cameraImageUri);
                    updatePhotoVisibility();
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) pickImageFromGallery();
                else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (getIntent().hasExtra("product")) {
            product = (ProductItem) getIntent().getSerializableExtra("product");
        } else {
            Toast.makeText(this, "Product tracking error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupClickListeners();
        setupUnitDropdown();
        prefillData();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        cardAddPhoto = findViewById(R.id.cardAddPhoto);
        hsvPhotoContainer = findViewById(R.id.hsvPhotoContainer);
        llPhotoContainer = findViewById(R.id.llPhotoContainer);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);

        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etPrice = findViewById(R.id.etPrice);
        etQuantity = findViewById(R.id.etQuantity);
        actvUnit = findViewById(R.id.actvUnit);
        btnDetect = findViewById(R.id.btnDetect);

        btnSubmit = findViewById(R.id.btnSubmit);
        flLoadingOverlay = findViewById(R.id.flLoadingOverlay);
        tvLoadingText = findViewById(R.id.tvLoadingText);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> validateAndUpdate());
        cardAddPhoto.setOnClickListener(v -> {
            if ((selectedNewImages.size() + existingImageUrls.size()) < MAX_PHOTOS) {
                showPhotoOptionsDialog();
            } else {
                Toast.makeText(this, "Maximum " + MAX_PHOTOS + " photos allowed.", Toast.LENGTH_SHORT).show();
            }
        });
        btnDetect.setOnClickListener(v -> detectLocation());
    }

    private void showPhotoOptionsDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Update Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkCameraPermissionAndLaunch();
                    else checkGalleryPermissionAndPick();
                })
                .show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void checkGalleryPermissionAndPick() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageFromGallery();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Pictures"));
    }

    private void setupUnitDropdown() {
        String[] units = {"Kilogram (kg)", "Gram (g)", "Unit (pcs)", "Packet", "Litre (L)", "Bora", "Quintal"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, units);
        actvUnit.setAdapter(adapter);
    }

    private void prefillData() {
        if (product != null) {
            etName.setText(product.getProductName());
            etDescription.setText(product.getDescription());
            etLocation.setText(product.getLocation());
            etPrice.setText(String.valueOf(product.getPrice()));
            etQuantity.setText(String.valueOf(product.getStock()));
            actvUnit.setText(product.getUnit(), false);

            // ⭐ Advanced: Collect ALL existing images (Primary + Gallery)
            boolean addedPrimary = false;
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty() && !product.getImageUrl().contains("via.placeholder")) {
                existingImageUrls.add(product.getImageUrl());
                addExistingPhotoThumb(product.getImageUrl());
                addedPrimary = true;
            }

            if (product.getImages() != null) {
                for (ProductItem.ProductImage img : product.getImages()) {
                    String url = img.getImageUrl();
                    // Avoid duplicating the primary image if it's already in the gallery
                    if (url != null && !url.isEmpty() && !url.contains("via.placeholder")) {
                         if (!addedPrimary || !url.equals(product.getImageUrl())) {
                             existingImageUrls.add(url);
                             addExistingPhotoThumb(url);
                         }
                    }
                }
            }
            updatePhotoVisibility();
        }
    }

    private void addImageToList(Uri uri) {
        selectedNewImages.add(uri);
        addNewPhotoThumb(uri);
    }

    private void addNewPhotoThumb(Uri uri) {
        View thumbView = LayoutInflater.from(this).inflate(R.layout.item_photo_thumb, llPhotoContainer, false);
        ImageView ivThumb = thumbView.findViewById(R.id.ivThumb);
        ImageView ivRemove = thumbView.findViewById(R.id.ivRemove);

        Glide.with(this).load(uri).centerCrop().into(ivThumb);
        ivRemove.setOnClickListener(v -> {
            selectedNewImages.remove(uri);
            llPhotoContainer.removeView(thumbView);
            updatePhotoVisibility();
        });
        llPhotoContainer.addView(thumbView);
    }

    private void addExistingPhotoThumb(String url) {
        View thumbView = LayoutInflater.from(this).inflate(R.layout.item_photo_thumb, llPhotoContainer, false);
        ImageView ivThumb = thumbView.findViewById(R.id.ivThumb);
        ImageView ivRemove = thumbView.findViewById(R.id.ivRemove);

        Glide.with(this).load(url).centerCrop().into(ivThumb);
        ivRemove.setOnClickListener(v -> {
            existingImageUrls.remove(url);
            llPhotoContainer.removeView(thumbView);
            updatePhotoVisibility();
        });
        llPhotoContainer.addView(thumbView);
    }

    private void updatePhotoVisibility() {
        int count = selectedNewImages.size() + existingImageUrls.size();
        tvPhotoCount.setText(count + "/" + MAX_PHOTOS + " Photos");
        tvPhotoCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        hsvPhotoContainer.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }

    private void detectLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        flLoadingOverlay.setVisibility(View.VISIBLE);
        tvLoadingText.setText("Detecting location...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                getAddressFromLocation(location);
            } else {
                flLoadingOverlay.setVisibility(View.GONE);
                Toast.makeText(this, "Could not detect location. Please type manually.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getLocality() + ", " + address.getAdminArea();
                etLocation.setText(fullAddress);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error getting address", Toast.LENGTH_SHORT).show();
        } finally {
            flLoadingOverlay.setVisibility(View.GONE);
        }
    }

    private void validateAndUpdate() {
        String name = etName.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String quantityStr = etQuantity.getText().toString().trim();
        String unit = actvUnit.getText().toString().trim();

        if (name.isEmpty() || desc.isEmpty() || location.isEmpty() || priceStr.isEmpty() || quantityStr.isEmpty() || unit.isEmpty()) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedNewImages.isEmpty() && existingImageUrls.isEmpty()) {
             Toast.makeText(this, "At least one photo is required.", Toast.LENGTH_SHORT).show();
             return;
        }

        double price;
        int quantity;
        try {
            price = Double.parseDouble(priceStr);
            quantity = (int) Double.parseDouble(quantityStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price or quantity.", Toast.LENGTH_SHORT).show();
            return;
        }

        tvLoadingText.setText("Saving...");
        flLoadingOverlay.setVisibility(View.VISIBLE);

        if (!selectedNewImages.isEmpty()) {
            uploadImagesSequentially(0, new ArrayList<>(existingImageUrls), name, desc, price, quantity, unit, location);
        } else {
            submitUpdate(name, desc, price, quantity, unit, location, existingImageUrls);
        }
    }

    private void uploadImagesSequentially(int index, List<String> finalUrls, String name, String desc, double price, int qty, String unit, String loc) {
        if (index >= selectedNewImages.size()) {
            submitUpdate(name, desc, price, qty, unit, loc, finalUrls);
            return;
        }

        tvLoadingText.setText("Uploading photo " + (index + 1) + " of " + selectedNewImages.size());
        Uri uri = selectedNewImages.get(index);
        
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            byte[] bytes = readBytes(is);
            RequestBody requestBody = RequestBody.create(bytes, MediaType.parse("image/*"));
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "new_product_image.jpg", requestBody);
            RequestBody prodIdBody = RequestBody.create(String.valueOf(product.getProductId()), MediaType.parse("text/plain"));

            apiService.uploadProductImage(prodIdBody, imagePart).enqueue(new Callback<ImageUploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<ImageUploadResponse> call, @NonNull Response<ImageUploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        finalUrls.add(response.body().getImageUrl());
                        uploadImagesSequentially(index + 1, finalUrls, name, desc, price, qty, unit, loc);
                    } else {
                        flLoadingOverlay.setVisibility(View.GONE);
                        Toast.makeText(EditProductActivity.this, "Failed to upload photo " + (index+1), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ImageUploadResponse> call, @NonNull Throwable t) {
                    flLoadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(EditProductActivity.this, "Upload error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            flLoadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Error reading image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitUpdate(String name, String desc, double price, int qty, String unit, String loc, List<String> imageUrls) {
        tvLoadingText.setText("Finalizing update...");
        
        AddProductRequest request = new AddProductRequest(name, desc, price, qty, unit, loc, product.getSubCategoryId());
        request.setImageUrls(imageUrls);
        request.setCategory(product.getCategory());
        
        if (imageUrls != null && !imageUrls.isEmpty()) {
            request.setImageUrl(imageUrls.get(0));
        } else {
            request.setImageUrl("https://via.placeholder.com/400");
        }

        apiService.updateProduct(product.getProductId(), request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                flLoadingOverlay.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(EditProductActivity.this, "✅ Product updated! (Pending Approval)", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EditProductActivity.this, "Update failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                flLoadingOverlay.setVisibility(View.GONE);
                Toast.makeText(EditProductActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            detectLocation();
        } else if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        }
    }
}
