package com.example.agrirent.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
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
import android.widget.ProgressBar;
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
import com.example.agrirent.models.AddEquipmentRequest;
import com.example.agrirent.models.AddEquipmentResponse;
import com.example.agrirent.models.ApiCategory;
import com.example.agrirent.models.ImageUploadResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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

public class AddEquipmentActivity extends BaseActivity {

    private static final int MAX_PHOTOS = 5;
    private static final int LOCATION_PERMISSION_REQUEST = 101;
    private static final int CAMERA_PERMISSION_REQUEST = 102;

    // Views
    private ImageView btnBack;
    private MaterialCardView cardAddPhoto;
    private HorizontalScrollView hsvPhotoContainer;
    private LinearLayout llPhotoContainer;
    private TextView tvPhotoCount;
    private EditText etName, etDescription, etHourlyPrice, etDailyPrice;
    private EditText etLocation;
    private MaterialButton btnDetect;
    private AutoCompleteTextView actvCategory, actvSubCategory;
    private TextInputLayout tilSubCategory;
    private ProgressBar pbCategory;
    private MaterialButton btnSubmit;
    private FrameLayout flLoadingOverlay;
    private TextView tvLoadingText;

    // Data
    private final List<Uri> selectedImages = new ArrayList<>();
    private ApiService apiService;
    private FusedLocationProviderClient fusedLocationClient;
    private List<ApiCategory> categories = new ArrayList<>();
    private ApiCategory.ApiSubCategory selectedSubCategory;
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
                            if (selectedImages.size() < MAX_PHOTOS) {
                                addImageToList(imageUri);
                            } else {
                                Toast.makeText(this, "Maximum " + MAX_PHOTOS + " photos allowed.", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }
                    } else if (data.getData() != null) {
                        Uri imageUri = data.getData();
                        if (selectedImages.size() < MAX_PHOTOS) {
                            addImageToList(imageUri);
                        } else {
                            Toast.makeText(this, "Maximum " + MAX_PHOTOS + " photos allowed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                    addImageToList(cameraImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_equipment);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        bindViews();
        setupClickListeners();
        loadCategories();
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
        etHourlyPrice = findViewById(R.id.etHourlyPrice);
        etDailyPrice = findViewById(R.id.etDailyPrice);
        btnDetect = findViewById(R.id.btnDetect);

        actvCategory = findViewById(R.id.actvCategory);
        actvSubCategory = findViewById(R.id.actvSubCategory);
        tilSubCategory = findViewById(R.id.tilSubCategory);
        pbCategory = findViewById(R.id.pbCategory);

        btnSubmit = findViewById(R.id.btnSubmit);
        flLoadingOverlay = findViewById(R.id.flLoadingOverlay);
        tvLoadingText = findViewById(R.id.tvLoadingText);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        cardAddPhoto.setOnClickListener(v -> {
            if (selectedImages.size() < MAX_PHOTOS) {
                showPhotoOptionsDialog();
            } else {
                Toast.makeText(this, "Maximum " + MAX_PHOTOS + " photos allowed.", Toast.LENGTH_SHORT).show();
            }
        });
        btnDetect.setOnClickListener(v -> detectLocation());
        btnSubmit.setOnClickListener(v -> submitEquipment());
    }

    private void showPhotoOptionsDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Add Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndLaunch();
                    } else {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Pictures"));
                    }
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

    private void addImageToList(Uri uri) {
        if (selectedImages.size() < MAX_PHOTOS) {
            selectedImages.add(uri);
            addPhotoThumb(uri);
            tvPhotoCount.setText(selectedImages.size() + "/" + MAX_PHOTOS + " Photos");
            tvPhotoCount.setVisibility(View.VISIBLE);
            hsvPhotoContainer.setVisibility(View.VISIBLE);
        }
    }

    private void addPhotoThumb(Uri uri) {
        View thumb = LayoutInflater.from(this).inflate(R.layout.item_photo_thumb, llPhotoContainer, false);
        ImageView ivThumb = thumb.findViewById(R.id.ivThumb);
        ImageView ivRemove = thumb.findViewById(R.id.ivRemove);

        Glide.with(this).load(uri).centerCrop().into(ivThumb);

        ivRemove.setOnClickListener(v -> {
            selectedImages.remove(uri);
            llPhotoContainer.removeView(thumb);
            tvPhotoCount.setText(selectedImages.size() + "/" + MAX_PHOTOS + " Photos");
            if (selectedImages.isEmpty()) {
                hsvPhotoContainer.setVisibility(View.GONE);
                tvPhotoCount.setVisibility(View.GONE);
            }
        });

        // Add to the container
        llPhotoContainer.addView(thumb);
    }

    private void detectLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        btnDetect.setText("Detecting...");
        btnDetect.setEnabled(false);

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        String city = addr.getLocality();
                        String state = addr.getAdminArea();
                        if (city != null && state != null) {
                            etLocation.setText(city + ", " + state);
                        } else if (state != null) {
                            etLocation.setText(state);
                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(this, "Could not determine location name.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show();
            }
            btnDetect.setText("📍 DETECT");
            btnDetect.setEnabled(true);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnDetect.setText("📍 DETECT");
            btnDetect.setEnabled(true);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            detectLocation();
        } else if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadCategories() {
        pbCategory.setVisibility(View.VISIBLE);
        apiService.getCategories("Equipment").enqueue(new Callback<List<ApiCategory>>() {
            @Override
            public void onResponse(@NonNull Call<List<ApiCategory>> call, @NonNull Response<List<ApiCategory>> response) {
                pbCategory.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    categories.clear();
                    categories.addAll(response.body());
                    setupCategoryDropdown();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<ApiCategory>> call, @NonNull Throwable t) {
                pbCategory.setVisibility(View.GONE);
                Toast.makeText(AddEquipmentActivity.this, "Could not load categories.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCategoryDropdown() {
        List<String> categoryNames = new ArrayList<>();
        for (ApiCategory c : categories) categoryNames.add(c.getName());

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categoryNames);
        actvCategory.setAdapter(catAdapter);

        actvCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedSubCategory = null;
            actvSubCategory.setText("", false);
            actvSubCategory.setEnabled(true);
            tilSubCategory.setAlpha(1.0f);
            populateSubCategory(categories.get(position));
        });
    }

    private void populateSubCategory(ApiCategory category) {
        List<String> subNames = new ArrayList<>();
        List<ApiCategory.ApiSubCategory> subs = category.getSubCategories();
        if (subs != null) {
            for (ApiCategory.ApiSubCategory s : subs) subNames.add(s.getSubCategoryName());
        }

        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, subNames);
        actvSubCategory.setAdapter(subAdapter);

        actvSubCategory.setOnItemClickListener((parent, view, position, id) -> {
            if (subs != null && position < subs.size()) {
                selectedSubCategory = subs.get(position);
            }
        });
    }

    private void submitEquipment() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String desc = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String location = etLocation.getText().toString().trim();
        String hourlyPriceStr = etHourlyPrice.getText() != null ? etHourlyPrice.getText().toString().trim() : "";
        String dailyPriceStr = etDailyPrice.getText() != null ? etDailyPrice.getText().toString().trim() : "";

        if (name.isEmpty() || desc.isEmpty() || location.isEmpty() || hourlyPriceStr.isEmpty() || dailyPriceStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedSubCategory == null) {
            Toast.makeText(this, "Please select a Category and Sub-Category.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Please add at least one photo.", Toast.LENGTH_SHORT).show();
            return;
        }

        double hourlyPrice, dailyPrice;
        try {
            hourlyPrice = Double.parseDouble(hourlyPriceStr);
            dailyPrice = Double.parseDouble(dailyPriceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price amount.", Toast.LENGTH_SHORT).show();
            return;
        }

        tvLoadingText.setText("Creating Equipment...");
        flLoadingOverlay.setVisibility(View.VISIBLE);

        AddEquipmentRequest request = new AddEquipmentRequest(name, desc, location, hourlyPrice, dailyPrice,
                selectedSubCategory.getSubCategoryId());
        apiService.addEquipment(request).enqueue(new Callback<AddEquipmentResponse>() {
            @Override
            public void onResponse(@NonNull Call<AddEquipmentResponse> call, @NonNull Response<AddEquipmentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    uploadImagesSequentially(response.body().getEquipmentId(), 0);
                } else {
                    flLoadingOverlay.setVisibility(View.GONE);
                    String msg = "Failed to create equipment.";
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            if (errorStr.trim().startsWith("{")) {
                                org.json.JSONObject jObjError = new org.json.JSONObject(errorStr);
                                if (jObjError.has("message")) {
                                    msg = jObjError.getString("message");
                                } else if (jObjError.has("errors")) {
                                    org.json.JSONObject errors = jObjError.getJSONObject("errors");
                                    java.util.Iterator<String> keys = errors.keys();
                                    StringBuilder sb = new StringBuilder();
                                    while (keys.hasNext()) {
                                        String key = keys.next();
                                        org.json.JSONArray fieldErrors = errors.getJSONArray(key);
                                        for (int i = 0; i < fieldErrors.length(); i++) {
                                            sb.append(fieldErrors.getString(i)).append("\n");
                                        }
                                    }
                                    msg = sb.toString().trim();
                                } else if (jObjError.has("title")) {
                                    msg = jObjError.getString("title");
                                }
                            } else if (!errorStr.isEmpty()) {
                                msg = errorStr.replace("\"", "");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (response.code() == 400) msg = "Validation failed. Check your data or active subscription limit.";
                    }
                    Toast.makeText(AddEquipmentActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<AddEquipmentResponse> call, @NonNull Throwable t) {
                flLoadingOverlay.setVisibility(View.GONE);
                Toast.makeText(AddEquipmentActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImagesSequentially(int equipmentId, int index) {
        if (index >= selectedImages.size()) {
            flLoadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "✅ Equipment deployed successfully!", Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
            return;
        }

        tvLoadingText.setText("Uploading photo " + (index + 1) + "/" + selectedImages.size() + "...");

        try {
            Uri uri = selectedImages.get(index);
            InputStream is = getContentResolver().openInputStream(uri);
            byte[] bytes = readBytes(is);
            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "image/jpeg";

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), bytes);
            MultipartBody.Part imageBody = MultipartBody.Part.createFormData("image", "img_" + index + ".jpg", requestFile);
            RequestBody idBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(equipmentId));

            apiService.uploadEquipmentImage(idBody, imageBody).enqueue(new Callback<ImageUploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<ImageUploadResponse> call, @NonNull Response<ImageUploadResponse> response) {
                    uploadImagesSequentially(equipmentId, index + 1);
                }
                @Override
                public void onFailure(@NonNull Call<ImageUploadResponse> call, @NonNull Throwable t) {
                    // Continue even if one fails
                    uploadImagesSequentially(equipmentId, index + 1);
                }
            });
        } catch (Exception e) {
            uploadImagesSequentially(equipmentId, index + 1);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cameraImageUri != null) {
            outState.putString("cameraImageUri", cameraImageUri.toString());
        }
        if (!selectedImages.isEmpty()) {
            ArrayList<String> uriStrings = new ArrayList<>();
            for (Uri uri : selectedImages) uriStrings.add(uri.toString());
            outState.putStringArrayList("selectedImages", uriStrings);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String savedCameraStr = savedInstanceState.getString("cameraImageUri");
        if (savedCameraStr != null) {
            cameraImageUri = Uri.parse(savedCameraStr);
        }
        ArrayList<String> restoredStrings = savedInstanceState.getStringArrayList("selectedImages");
        if (restoredStrings != null) {
            for (String uriStr : restoredStrings) {
                Uri uri = Uri.parse(uriStr);
                if (!selectedImages.contains(uri)) {
                    addImageToList(uri);
                }
            }
        }
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = inputStream.read(buffer)) != -1) byteBuffer.write(buffer, 0, len);
        return byteBuffer.toByteArray();
    }
}
