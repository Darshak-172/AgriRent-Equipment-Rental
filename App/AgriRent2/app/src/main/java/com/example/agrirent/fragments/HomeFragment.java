package com.example.agrirent.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Handler;
import android.os.Looper;

import com.example.agrirent.R;
import com.example.agrirent.activities.AllEquipmentActivity;
import com.example.agrirent.activities.AllProductsActivity;
import com.example.agrirent.activities.MainActivity;
import com.example.agrirent.activities.SearchResultsActivity;
import com.example.agrirent.activities.NotificationsActivity;
import com.example.agrirent.activities.SubscriptionActivity;
import com.example.agrirent.activities.WeatherActivity;
import com.example.agrirent.activities.CartActivity;
import com.example.agrirent.adapters.EquipmentAdapter;
import com.example.agrirent.adapters.ProductItemAdapter;
import com.example.agrirent.models.Equipment;
import com.example.agrirent.models.PaginatedResponse;
import com.example.agrirent.models.ProductItem;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.network.LoadingInterceptor;
import com.example.agrirent.utils.SessionManager;
import com.example.agrirent.utils.CartManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private SessionManager session;
    private ApiService api;

    @Override
    public void onResume() {
        super.onResume();
        updateNotificationBadge();
        updateCartBadge();
    }

    private void updateCartBadge() {
        if (getView() == null || getContext() == null) return;
        TextView tvCartBadge = getView().findViewById(R.id.tvHomeCartBadge);
        if (tvCartBadge != null) {
            CartManager cartManager = new CartManager(getContext());
            int count = cartManager.getCartCount();
            if (count > 0) {
                tvCartBadge.setVisibility(View.VISIBLE);
                tvCartBadge.setText(String.valueOf(count));
            } else {
                tvCartBadge.setVisibility(View.GONE);
            }
        }
    }

    private void updateNotificationBadge() {
        if (getView() == null) return;
        TextView tvNotificationBadge = getView().findViewById(R.id.tvNotificationBadge);
        if (tvNotificationBadge != null) {
            int count = com.example.agrirent.models.NotificationData.getUnreadCount();
            if (count > 0) {
                tvNotificationBadge.setVisibility(View.VISIBLE);
                tvNotificationBadge.setText(String.valueOf(count));
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        }
    }

    private TextView tvAvatar, tvGreeting, tvUserName, tvSearchError;
    private EditText etSearch;

    private TextView tvTemp, tvCondition, tvDate, tvLocationText, tvHumidity, tvWind, tvRain;

    private RecyclerView rvEquipment, rvProducts;
    private LinearLayout llEquipmentLoading, llEquipmentEmpty;
    private LinearLayout llProductsLoading, llProductsEmpty;
    private View cvPremiumBanner;
    private SwipeRefreshLayout swipeRefreshLayout;

    private EquipmentAdapter equipmentAdapter;
    private ProductItemAdapter productItemAdapter;

    private final List<Equipment> equipmentList = new ArrayList<>();
    private final List<Equipment> allEquipmentList = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private static double cachedLat = 0.0;
    private static double cachedLon = 0.0;
    private static String cachedLocText = null;
    private static String cachedTempStr = null;
    private static String cachedConditionStr = null;
    private static String cachedHumidityStr = null;
    private static String cachedWindStr = null;
    private static String cachedRainStr = null;
    private static long lastWeatherFetchTime = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        session = new SessionManager(requireContext());
        LoadingInterceptor.setCurrentActivity(requireActivity());

        initViews(view);
        setupAdapters();
        setupSearch(view);
        loadHomeData();
        registerFcmToken();

        return view;
    }

    private void initViews(View view) {
        tvAvatar            = view.findViewById(R.id.tvAvatar);
        tvGreeting          = view.findViewById(R.id.tvGreeting);
        tvUserName          = view.findViewById(R.id.tvUserName);
        etSearch            = view.findViewById(R.id.etSearch);
        tvSearchError       = view.findViewById(R.id.tvSearchError);

        tvTemp              = view.findViewById(R.id.tvTemp);
        tvCondition         = view.findViewById(R.id.tvCondition);
        tvDate              = view.findViewById(R.id.tvDate);
        tvLocationText      = view.findViewById(R.id.tvLocationText);
        tvHumidity          = view.findViewById(R.id.tvHumidity);
        tvWind              = view.findViewById(R.id.tvWind);
        tvRain              = view.findViewById(R.id.tvRain);

        rvEquipment         = view.findViewById(R.id.rvEquipment);
        llEquipmentLoading  = view.findViewById(R.id.llEquipmentLoading);
        llEquipmentEmpty    = view.findViewById(R.id.llEquipmentEmpty);

        rvProducts          = view.findViewById(R.id.rvProducts);
        llProductsLoading   = view.findViewById(R.id.llProductsLoading);
        llProductsEmpty     = view.findViewById(R.id.llProductsEmpty);
        cvPremiumBanner     = view.findViewById(R.id.cvPremiumBanner);
        swipeRefreshLayout  = view.findViewById(R.id.swipeRefreshLayout);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadHomeData();
                new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }

        String name = session.getUserName();
        if (name == null || name.isEmpty()) {
            name = "Farmer";
        }
        if (tvUserName != null) tvUserName.setText(name);

        if (tvAvatar != null) {
            String[] parts = name.trim().split("\\s+");
            String initials = "";
            if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].substring(0, 1).toUpperCase();
            if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].substring(0, 1).toUpperCase();
            tvAvatar.setText(initials.isEmpty() ? "U" : initials);
            tvAvatar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.weather_card_bg)));
        }

        if (tvGreeting != null) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            int timeOfDay = c.get(java.util.Calendar.HOUR_OF_DAY);
            if (timeOfDay >= 4 && timeOfDay < 12) tvGreeting.setText("GOOD MORNING");
            else if (timeOfDay >= 12 && timeOfDay < 16) tvGreeting.setText("GOOD AFTERNOON");
            else if (timeOfDay >= 16 && timeOfDay < 20) tvGreeting.setText("GOOD EVENING");
            else tvGreeting.setText("GOOD NIGHT");
        }

        if (tvDate != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
            tvDate.setText(sdf.format(new java.util.Date()));
        }
    }

    private void setupAdapters() {
        rvEquipment.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        equipmentAdapter = new EquipmentAdapter(requireContext(), equipmentList);
        rvEquipment.setAdapter(equipmentAdapter);

        rvProducts.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        productItemAdapter = new ProductItemAdapter(requireContext(), new ArrayList<>());
        rvProducts.setAdapter(productItemAdapter);
    }

    private void setupSearch(View view) {
        etSearch.setFocusable(false);
        etSearch.setOnClickListener(v -> startActivity(new Intent(requireActivity(), SearchResultsActivity.class)));

        if (tvAvatar != null) {
            tvAvatar.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToTab(R.id.nav_profile);
                }
            });
        }

        TextView tvSeeAll = view.findViewById(R.id.tvSeeAllEquipment);
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(v -> {
                startActivity(new Intent(requireActivity(), AllEquipmentActivity.class));
            });
        }

        TextView tvSeeAllProducts = view.findViewById(R.id.tvSeeAllProducts);
        if (tvSeeAllProducts != null) {
            tvSeeAllProducts.setOnClickListener(v -> {
                startActivity(new Intent(requireActivity(), AllProductsActivity.class));
            });
        }

        View cvNotification = view.findViewById(R.id.cvNotification);
        if (cvNotification != null) {
            cvNotification.setOnClickListener(v -> startActivity(new Intent(requireActivity(), NotificationsActivity.class)));
        }

        View cvCart = view.findViewById(R.id.cvCart);
        if (cvCart != null) {
            cvCart.setOnClickListener(v -> startActivity(new Intent(requireActivity(), CartActivity.class)));
        }

        View btnUpgradePlan = view.findViewById(R.id.btnUpgradePlan);
        if (btnUpgradePlan != null) {
            btnUpgradePlan.setOnClickListener(v -> startActivity(new Intent(requireActivity(), SubscriptionActivity.class)));
        }

        View cvWeather = view.findViewById(R.id.cvWeather);
        if (cvWeather != null) {
            cvWeather.setOnClickListener(v -> {
                if (cachedLat != 0.0 && cachedLon != 0.0) {
                    Intent intent = new Intent(requireActivity(), WeatherActivity.class);
                    intent.putExtra("LAT", cachedLat);
                    intent.putExtra("LON", cachedLon);
                    if (cachedLocText != null) intent.putExtra("CITY_NAME", cachedLocText);
                    startActivity(intent);
                } else {
                    Toast.makeText(requireContext(), "Fetching Location, please wait...", Toast.LENGTH_SHORT).show();
                    checkLocationPermissionAndFetchWeather();
                }
            });
        }
    }

    private void loadHomeData() {
        api = ApiClient.getClient(requireContext()).create(ApiService.class);
        loadEquipment();
        loadProducts();
        checkActiveSubscriptions();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (System.currentTimeMillis() - lastWeatherFetchTime < 15 * 60 * 1000 && cachedLocText != null) {
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

    private void loadEquipment() {
        api.getAvailableEquipment(1, 10).enqueue(new Callback<PaginatedResponse<Equipment>>() {
            @Override
            public void onResponse(retrofit2.Call<PaginatedResponse<Equipment>> call, Response<PaginatedResponse<Equipment>> response) {
                if (!isAdded()) return;
                llEquipmentLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && !response.body().getData().isEmpty()) {
                    List<Equipment> list = response.body().getData();
                    allEquipmentList.clear(); allEquipmentList.addAll(list);
                    equipmentList.clear(); equipmentList.addAll(list);

                    rvEquipment.setVisibility(View.VISIBLE);
                    llEquipmentEmpty.setVisibility(View.GONE);
                    equipmentAdapter.updateData(equipmentList);
                } else {
                    llEquipmentEmpty.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onFailure(retrofit2.Call<PaginatedResponse<Equipment>> call, Throwable t) {
                if (!isAdded()) return;
                llEquipmentLoading.setVisibility(View.GONE);
                llEquipmentEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadProducts() {
        api.getProducts(1, 10).enqueue(new Callback<PaginatedResponse<ProductItem>>() {
            @Override
            public void onResponse(retrofit2.Call<PaginatedResponse<ProductItem>> call, Response<PaginatedResponse<ProductItem>> response) {
                if (!isAdded()) return;
                llProductsLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && !response.body().getData().isEmpty()) {
                    List<ProductItem> list = response.body().getData();
                    rvProducts.setVisibility(View.VISIBLE);
                    llProductsEmpty.setVisibility(View.GONE);
                    productItemAdapter.updateData(list);
                } else {
                    llProductsEmpty.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onFailure(retrofit2.Call<PaginatedResponse<ProductItem>> call, Throwable t) {
                if (!isAdded()) return;
                llProductsLoading.setVisibility(View.GONE);
                llProductsEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void checkActiveSubscriptions() {
        api.getMySubscriptions().enqueue(new Callback<com.example.agrirent.models.MySubscriptionResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.agrirent.models.MySubscriptionResponse> call, Response<com.example.agrirent.models.MySubscriptionResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<com.example.agrirent.models.MySubscription> activeSubs = response.body().getData();
                    if (activeSubs != null && !activeSubs.isEmpty()) {
                        if (cvPremiumBanner != null) cvPremiumBanner.setVisibility(View.GONE);
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<com.example.agrirent.models.MySubscriptionResponse> call, Throwable t) { }
        });
    }

    private void registerFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!isAdded()) return;
            if (task.isSuccessful() && task.getResult() != null) {
                String fcmToken = task.getResult();
                String authToken = session.getToken();
                if (authToken == null || authToken.isEmpty()) return;

                ApiService apiService = ApiClient.getClient(requireContext()).create(ApiService.class);
                String language = session.getLanguage();
                String deviceId = android.provider.Settings.Secure.getString(requireContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

                apiService.updateFcmToken(new com.example.agrirent.models.FcmTokenRequest(fcmToken, language, deviceId)).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<Void> call, @NonNull Response<Void> response) { }
                    @Override
                    public void onFailure(@NonNull retrofit2.Call<Void> call, @NonNull Throwable t) { }
                });
            }
        });
    }

    private void checkLocationPermissionAndFetchWeather() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            permissionsNeeded.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(permissionsNeeded.toArray(new String[0]), LOCATION_PERMISSION_REQUEST_CODE);
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
                if ((permissions[i].equals(android.Manifest.permission.ACCESS_FINE_LOCATION) || permissions[i].equals(android.Manifest.permission.ACCESS_COARSE_LOCATION))
                        && grantResults[i] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    locationGranted = true;
                }
            }
            if (locationGranted) fetchLocationAndWeather();
            else Toast.makeText(requireContext(), "Location permission denied. Cannot fetch live weather.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchLocationAndWeather() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    processLocationAndFetchWeather(location.getLatitude(), location.getLongitude());
                } else {
                    fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                        .addOnSuccessListener(requireActivity(), currentLoc -> {
                            if (currentLoc != null) processLocationAndFetchWeather(currentLoc.getLatitude(), currentLoc.getLongitude());
                            else {
                                if (tvLocationText != null) tvLocationText.setText("Unknown");
                                if (tvCondition != null) tvCondition.setText("Unknown");
                            }
                        }).addOnFailureListener(requireActivity(), e -> {
                            if (tvLocationText != null) tvLocationText.setText("Unknown");
                        });
                }
            }).addOnFailureListener(requireActivity(), e -> {
                if (tvLocationText != null) tvLocationText.setText("Unknown");
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void processLocationAndFetchWeather(double lat, double lon) {
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(requireContext(), Locale.getDefault());
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
        cachedLat = lat; cachedLon = lon;
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
                androidx.fragment.app.FragmentActivity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (tvLocationText != null && tvLocationText.getText().toString().equals("Fetching...")) tvLocationText.setText("Offline");
                    });
                }
            }
            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        JSONObject current = new JSONObject(jsonString).getJSONObject("current");
                        double temp = current.getDouble("temperature_2m");
                        int humidity = current.getInt("relative_humidity_2m");
                        double windSpeed = current.getDouble("wind_speed_10m");
                        double rain = current.getDouble("precipitation");
                        int weatherCode = current.getInt("weather_code");

                        cachedTempStr = String.format(Locale.getDefault(), "%.0f°", temp);
                        cachedConditionStr = getWeatherDescription(weatherCode);
                        cachedHumidityStr = humidity + "%";
                        cachedWindStr = String.format(Locale.getDefault(), "%.1fkm/h", windSpeed);
                        cachedRainStr = "💧 " + String.format(Locale.getDefault(), "%.1f", rain) + "mm Rain";
                        if (tvLocationText != null) cachedLocText = tvLocationText.getText().toString();
                        lastWeatherFetchTime = System.currentTimeMillis();

                        androidx.fragment.app.FragmentActivity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                if (tvTemp != null) tvTemp.setText(cachedTempStr);
                                if (tvCondition != null) tvCondition.setText(cachedConditionStr);
                                if (tvHumidity != null) tvHumidity.setText(cachedHumidityStr);
                                if (tvWind != null) tvWind.setText(cachedWindStr);
                                if (tvRain != null) tvRain.setText(cachedRainStr);
                            });
                        }
                    } catch (Exception e) { e.printStackTrace(); }
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
}