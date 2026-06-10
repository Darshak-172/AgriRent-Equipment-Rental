package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.ColorStateList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Handler;
import android.os.Looper;

import com.bumptech.glide.Glide;
import com.example.agrirent.R;
import com.example.agrirent.adapters.EquipmentAdapter;
import com.example.agrirent.adapters.ProductItemAdapter;
import com.example.agrirent.models.ApiCategory;
import com.example.agrirent.models.Equipment;
import com.example.agrirent.models.PaginatedResponse;
import com.example.agrirent.models.ProductItem;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.network.LoadingInterceptor;
import com.example.agrirent.utils.LocaleHelper;
import com.example.agrirent.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.example.agrirent.fragments.AddListingBottomSheet;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.firebase.messaging.FirebaseMessaging;

public class HomeActivity extends BaseActivity {

    // ── Session & API ─────────────────────────────────────────────────────────
    private SessionManager session;
    private ApiService api;

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView tvAvatar;
    private TextView tvGreeting;
    private TextView tvUserName;
    private EditText etSearch;
    private TextView tvSearchError;

    // Weather
    private TextView tvTemp, tvCondition, tvDate, tvLocationText, tvHumidity, tvWind, tvRain;

    // Equipment
    private RecyclerView rvEquipment;
    private LinearLayout llEquipmentLoading, llEquipmentEmpty;

    // Products
    private RecyclerView rvProducts;
    private LinearLayout llProductsLoading, llProductsEmpty;

    // Premium Banner
    private View cvPremiumBanner;

    private SwipeRefreshLayout swipeRefreshLayout;

    // Bottom Nav
    private BottomNavigationView bottomNavigation;

    // ── Adapters ──────────────────────────────────────────────────────────────
    private EquipmentAdapter equipmentAdapter;
    private ProductItemAdapter productItemAdapter;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<Equipment> equipmentList = new ArrayList<>();
    private final List<Equipment> allEquipmentList = new ArrayList<>(); // full list for filter reset

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Weather Caching (prevent re-fetch on theme change)
    private static double cachedLat = 0.0;
    private static double cachedLon = 0.0;
    private static String cachedLocText = null;
    private static String cachedTempStr = null;
    private static String cachedConditionStr = null;
    private static String cachedHumidityStr = null;
    private static String cachedWindStr = null;
    private static String cachedRainStr = null;
    private static long lastWeatherFetchTime = 0;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        session = new SessionManager(this);
        LoadingInterceptor.setCurrentActivity(this);
        LocaleHelper.applyLanguage(this, session.getLanguage());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupAdapters();
        setupBottomNavigation();
        setupSearch();
        loadHomeData();

        registerFcmToken();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────────

    private void initViews() {
        tvAvatar            = findViewById(R.id.tvAvatar);
        tvGreeting          = findViewById(R.id.tvGreeting);
        tvUserName          = findViewById(R.id.tvUserName);
        etSearch            = findViewById(R.id.etSearch);
        tvSearchError       = findViewById(R.id.tvSearchError);

        tvTemp              = findViewById(R.id.tvTemp);
        tvCondition         = findViewById(R.id.tvCondition);
        tvDate              = findViewById(R.id.tvDate);
        tvLocationText      = findViewById(R.id.tvLocationText);
        tvHumidity          = findViewById(R.id.tvHumidity);
        tvWind              = findViewById(R.id.tvWind);
        tvRain              = findViewById(R.id.tvRain);

        rvEquipment         = findViewById(R.id.rvEquipment);
        llEquipmentLoading  = findViewById(R.id.llEquipmentLoading);
        llEquipmentEmpty    = findViewById(R.id.llEquipmentEmpty);

        rvProducts          = findViewById(R.id.rvProducts);
        llProductsLoading   = findViewById(R.id.llProductsLoading);
        llProductsEmpty     = findViewById(R.id.llProductsEmpty);
        cvPremiumBanner     = findViewById(R.id.cvPremiumBanner);
        swipeRefreshLayout  = findViewById(R.id.swipeRefreshLayout);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadHomeData();
                new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }

        bottomNavigation    = findViewById(R.id.bottomNavigation);

        // Set user name and initials
        String name = session.getUserName();
        if (name == null || name.isEmpty()) {
            name = "Farmer";
        }
        tvUserName.setText(name);

        if (tvAvatar != null) {
            String[] parts = name.trim().split("\\s+");
            String initials = "";
            if (parts.length > 0 && !parts[0].isEmpty()) {
                initials += parts[0].substring(0, 1).toUpperCase();
            }
            if (parts.length > 1 && !parts[1].isEmpty()) {
                initials += parts[1].substring(0, 1).toUpperCase();
            }
            tvAvatar.setText(initials.isEmpty() ? "U" : initials);
            // Fix Color Tint on Theme Change
            tvAvatar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.weather_card_bg)));
        }

        // Set dynamic greeting based on time
        if (tvGreeting != null) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            int timeOfDay = c.get(java.util.Calendar.HOUR_OF_DAY);

            if (timeOfDay >= 4 && timeOfDay < 12) {
                tvGreeting.setText("GOOD MORNING");
            } else if (timeOfDay >= 12 && timeOfDay < 16) {
                tvGreeting.setText("GOOD AFTERNOON");
            } else if (timeOfDay >= 16 && timeOfDay < 20) {
                tvGreeting.setText("GOOD EVENING");
            } else {
                tvGreeting.setText("GOOD NIGHT");
            }
        }

        // Set initial date
        if (tvDate != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
            tvDate.setText(sdf.format(new java.util.Date()));
        }
    }

    private void setupAdapters() {
        // Equipment — horizontal card carousel
        rvEquipment.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        equipmentAdapter = new EquipmentAdapter(this, equipmentList);
        rvEquipment.setAdapter(equipmentAdapter);

        // Products — 2-column staggered grid
        rvProducts.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        productItemAdapter = new ProductItemAdapter(this, new ArrayList<>());
        rvProducts.setAdapter(productItemAdapter);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEARCH VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    private void setupSearch() {
        // Navigate to SearchResultsActivity when the search bar is focused or clicked
        etSearch.setFocusable(false);
        etSearch.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, SearchResultsActivity.class));
        });

        // Profile icon → open Profile tab in MainActivity
        if (tvAvatar != null) {
            tvAvatar.setOnClickListener(v -> openMainWithTab(R.id.nav_profile));
        }

        // See All Equipment → open All Equipment screen
        TextView tvSeeAll = findViewById(R.id.tvSeeAllEquipment);
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, AllEquipmentActivity.class)));
        }

        // See All Products → open All Products screen
        TextView tvSeeAllProducts = findViewById(R.id.tvSeeAllProducts);
        if (tvSeeAllProducts != null) {
            tvSeeAllProducts.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, AllProductsActivity.class)));
        }

        // Settings shortcut from the home header
        View cvNotification = findViewById(R.id.cvNotification);
        if (cvNotification != null) {
            cvNotification.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class)));
        }

        // Upgrade Plan navigation
        View btnUpgradePlan = findViewById(R.id.btnUpgradePlan);
        if (btnUpgradePlan != null) {
            btnUpgradePlan.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, SubscriptionActivity.class)));
        }

        // Weather Card navigation
        View cvWeather = findViewById(R.id.cvWeather);
        if (cvWeather != null) {
            cvWeather.setOnClickListener(v -> {
                if (cachedLat != 0.0 && cachedLon != 0.0) {
                    Intent intent = new Intent(HomeActivity.this, WeatherActivity.class);
                    intent.putExtra("LAT", cachedLat);
                    intent.putExtra("LON", cachedLon);
                    if (cachedLocText != null) {
                        intent.putExtra("CITY_NAME", cachedLocText);
                    }
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Fetching Location, please wait...", Toast.LENGTH_SHORT).show();
                    // trigger manual fetch if user really wants to
                    checkLocationPermissionAndFetchWeather();
                }
            });
        }
    }

    private void performSearch() {
        // This method is largely unused now since clicks redirect to SearchResultsActivity,
        // but left here for structural integrity if needed.
        startActivity(new Intent(HomeActivity.this, SearchResultsActivity.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BOTTOM NAVIGATION
    // ─────────────────────────────────────────────────────────────────────────

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_explore) {
                openMainWithTab(R.id.nav_explore);
                return true;
            } else if (id == R.id.nav_add) {
                if (!getSupportFragmentManager().isStateSaved()) {
                    AddListingBottomSheet.newInstance().show(getSupportFragmentManager(), "addListing");
                }
                return false;
            } else if (id == R.id.nav_orders) {
                openMainWithTab(R.id.nav_orders);
                return true;
            } else if (id == R.id.nav_profile) {
                openMainWithTab(R.id.nav_profile);
                return true;
            }
            return false;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    private void openMainWithTab(int tabId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_TARGET_TAB, tabId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATA LOADING
    // ─────────────────────────────────────────────────────────────────────────

    private void loadHomeData() {
        api = ApiClient.getClient(this).create(ApiService.class);
        loadEquipment();
        loadProducts();
        checkActiveSubscriptions();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (System.currentTimeMillis() - lastWeatherFetchTime < 15 * 60 * 1000 && cachedLocText != null) {
            // Use cached data
            if (tvLocationText != null) tvLocationText.setText(cachedLocText);
            if (tvTemp != null) tvTemp.setText(cachedTempStr);
            if (tvCondition != null) tvCondition.setText(cachedConditionStr);
            if (tvHumidity != null) tvHumidity.setText(cachedHumidityStr);
            if (tvWind != null) tvWind.setText(cachedWindStr);
            if (tvRain != null) tvRain.setText(cachedRainStr);
        } else {
            checkLocationPermissionAndFetchWeather();
        }
    }

    /** GET /api/public/equipment/available — no auth needed */
    private void loadEquipment() {
        api.getAvailableEquipment(1, 10).enqueue(new Callback<PaginatedResponse<Equipment>>() {
            @Override
            public void onResponse(retrofit2.Call<PaginatedResponse<Equipment>> call, Response<PaginatedResponse<Equipment>> response) {
                llEquipmentLoading.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && !response.body().getData().isEmpty()) {
                    List<Equipment> list = response.body().getData();
                    allEquipmentList.clear();
                    allEquipmentList.addAll(list);
                    equipmentList.clear();
                    equipmentList.addAll(list);

                    rvEquipment.setVisibility(View.VISIBLE);
                    llEquipmentEmpty.setVisibility(View.GONE);
                    equipmentAdapter.updateData(equipmentList);
                } else {
                    llEquipmentEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<PaginatedResponse<Equipment>> call, Throwable t) {
                llEquipmentLoading.setVisibility(View.GONE);
                llEquipmentEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    /** GET /api/products/list — requires JWT auth token */
    private void loadProducts() {
        api.getProducts(1, 10).enqueue(new Callback<PaginatedResponse<ProductItem>>() {
            @Override
            public void onResponse(retrofit2.Call<PaginatedResponse<ProductItem>> call, Response<PaginatedResponse<ProductItem>> response) {
                llProductsLoading.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && !response.body().getData().isEmpty()) {
                    List<ProductItem> list = response.body().getData();

                    rvProducts.setVisibility(View.VISIBLE);
                    llProductsEmpty.setVisibility(View.GONE);
                    productItemAdapter.updateData(list);
                } else {
                    // 401 or empty — show empty state gracefully
                    llProductsEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<PaginatedResponse<ProductItem>> call, Throwable t) {
                llProductsLoading.setVisibility(View.GONE);
                llProductsEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    /** GET /api/subscription/my-subscription — Hide Premium Banner if active */
    private void checkActiveSubscriptions() {
        api.getMySubscriptions().enqueue(new Callback<com.example.agrirent.models.MySubscriptionResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.agrirent.models.MySubscriptionResponse> call, Response<com.example.agrirent.models.MySubscriptionResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<com.example.agrirent.models.MySubscription> activeSubs = response.body().getData();
                    if (activeSubs != null && !activeSubs.isEmpty()) {
                        // Find first Active subscription and cache it for offline gate checks
                        com.example.agrirent.models.MySubscription firstActive = null;
                        for (com.example.agrirent.models.MySubscription s : activeSubs) {
                            if ("Active".equalsIgnoreCase(s.getStatus())) {
                                firstActive = s;
                                break;
                            }
                        }
                        if (firstActive != null) {
                            session.saveSubscription(firstActive.getPlanName(), firstActive.getStatus());
                        } else {
                            session.saveSubscription("", "");
                        }
                        if (cvPremiumBanner != null) {
                            cvPremiumBanner.setVisibility(View.GONE);
                        }
                    } else {
                        // No subscriptions — clear cache
                        session.saveSubscription("", "");
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.agrirent.models.MySubscriptionResponse> call, Throwable t) {
                // Silently ignore or show error
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void registerFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String fcmToken = task.getResult();
                String authToken = session.getToken();
                if (authToken == null || authToken.isEmpty()) return;

                ApiService apiService = ApiClient.getClient(HomeActivity.this).create(ApiService.class);
                String language = session.getLanguage();
                String deviceId = android.provider.Settings.Secure.getString(
                        getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                apiService.updateFcmToken(new com.example.agrirent.models.FcmTokenRequest(fcmToken, language, deviceId)).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<Void> call, @NonNull Response<Void> response) {
                        android.util.Log.d("FCM", "Token registered from Home. Code: " + response.code());
                    }
                    @Override
                    public void onFailure(@NonNull retrofit2.Call<Void> call, @NonNull Throwable t) {
                        android.util.Log.e("FCM", "Token upload failed: " + t.getMessage());
                    }
                });
            }
        });
    }

    private void checkLocationPermissionAndFetchWeather() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            permissionsNeeded.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocationAndWeather();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean locationGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                if ((permissions[i].equals(android.Manifest.permission.ACCESS_FINE_LOCATION) || 
                     permissions[i].equals(android.Manifest.permission.ACCESS_COARSE_LOCATION))
                        && grantResults[i] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    locationGranted = true;
                }
            }
            if (locationGranted) {
                fetchLocationAndWeather();
            } else {
                Toast.makeText(this, "Location permission denied. Cannot fetch live weather.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchLocationAndWeather() {
        try {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        processLocationAndFetchWeather(location.getLatitude(), location.getLongitude());
                    } else {
                        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                            .addOnSuccessListener(this, currentLoc -> {
                                if (currentLoc != null) {
                                    processLocationAndFetchWeather(currentLoc.getLatitude(), currentLoc.getLongitude());
                                } else {
                                    if (tvLocationText != null) tvLocationText.setText("Unknown");
                                    if (tvCondition != null) tvCondition.setText("Unknown");
                                }
                            })
                            .addOnFailureListener(this, e -> {
                                if (tvLocationText != null) tvLocationText.setText("Unknown");
                            });
                    }
                })
                .addOnFailureListener(this, e -> {
                    if (tvLocationText != null) tvLocationText.setText("Unknown");
                });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void processLocationAndFetchWeather(double lat, double lon) {
        // 1. Get City Name
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(HomeActivity.this, Locale.getDefault());
            List<android.location.Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address address = addresses.get(0);
                String city = address.getLocality();
                String state = address.getAdminArea();
                if (city != null && tvLocationText != null) {
                    String locText = state != null ? city + ", " + state : city;
                    tvLocationText.setText(locText);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (tvLocationText != null) tvLocationText.setText("Lat: " + String.format(Locale.getDefault(), "%.2f", lat));
        }

        // 2. Fetch Weather Data from Open-Meteo
        cachedLat = lat;
        cachedLon = lon;
        fetchWeatherData(lat, lon);
    }

    private void fetchWeatherData(double lat, double lon) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (tvLocationText != null && tvLocationText.getText().toString().equals("Fetching...")) {
                        tvLocationText.setText("Offline");
                    }
                });
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonString);
                        JSONObject current = jsonObject.getJSONObject("current");

                        double temp = current.getDouble("temperature_2m");
                        int humidity = current.getInt("relative_humidity_2m");
                        double windSpeed = current.getDouble("wind_speed_10m");
                        double rain = current.getDouble("precipitation");
                        int weatherCode = current.getInt("weather_code");

                        String weatherDesc = getWeatherDescription(weatherCode);

                        cachedTempStr = String.format(Locale.getDefault(), "%.0f°", temp);
                        cachedConditionStr = weatherDesc;
                        cachedHumidityStr = humidity + "%";
                        cachedWindStr = String.format(Locale.getDefault(), "%.1fkm/h", windSpeed);
                        cachedRainStr = "💧 " + String.format(Locale.getDefault(), "%.1f", rain) + "mm Rain";
                        if (tvLocationText != null) {
                            cachedLocText = tvLocationText.getText().toString();
                        }
                        lastWeatherFetchTime = System.currentTimeMillis();

                        runOnUiThread(() -> {
                            if (tvTemp != null) tvTemp.setText(cachedTempStr);
                            if (tvCondition != null) tvCondition.setText(cachedConditionStr);
                            if (tvHumidity != null) tvHumidity.setText(cachedHumidityStr);
                            if (tvWind != null) tvWind.setText(cachedWindStr);
                            if (tvRain != null) tvRain.setText(cachedRainStr);
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private String getWeatherDescription(int code) {
        if (code == 0) return "Clear Sky";
        if (code == 1 || code == 2 || code == 3) return "Partly Cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 55) return "Drizzle";
        if (code >= 61 && code <= 65) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Rain Showers";
        if (code >= 95 && code <= 99) return "Thunderstorm";
        return "Unknown";
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}