package com.example.agrirent.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrirent.R;
import com.example.agrirent.adapters.ForecastAdapter;
import com.example.agrirent.network.LoadingInterceptor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherActivity extends BaseActivity {

    private TextView tvLocationTitle, tvMainTemp, tvMainCondition;
    private TextView tvWind, tvHumidity, tvRain;
    private TextView tvDewPoint, tvUV, tvSurfaceTemp;
    private RecyclerView rvForecast;
    private ProgressBar pbForecast;
    private WebView wvWeatherMap;
    private com.google.android.material.button.MaterialButton btnMapRadar, btnMapWind, btnMapRain, btnMapTemp, btnMapClouds;

    private double currentLat = 0.0;
    private double currentLon = 0.0;
    private String currentOverlay = "radar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Safe context for loader
        LoadingInterceptor.setCurrentActivity(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        Toolbar toolbar = findViewById(R.id.toolbarWeather);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvLocationTitle = findViewById(R.id.tvWeatherLocationTitle);
        tvMainTemp = findViewById(R.id.tvWeatherMainTemp);
        tvMainCondition = findViewById(R.id.tvWeatherMainCondition);
        tvWind = findViewById(R.id.tvDetailWind);
        tvHumidity = findViewById(R.id.tvDetailHumidity);
        tvRain = findViewById(R.id.tvDetailRain);
        tvDewPoint = findViewById(R.id.tvDetailDewPoint);
        tvUV = findViewById(R.id.tvDetailUV);
        tvSurfaceTemp = findViewById(R.id.tvDetailSurfaceTemp);
        rvForecast = findViewById(R.id.rvForecast);
        pbForecast = findViewById(R.id.pbForecast);
        wvWeatherMap = findViewById(R.id.wvWeatherMap);
        
        btnMapRadar = findViewById(R.id.btnMapRadar);
        btnMapWind = findViewById(R.id.btnMapWind);
        btnMapRain = findViewById(R.id.btnMapRain);
        btnMapTemp = findViewById(R.id.btnMapTemp);
        btnMapClouds = findViewById(R.id.btnMapClouds);

        // Setup WebView for Windy Interactive Map
        wvWeatherMap.getSettings().setJavaScriptEnabled(true);
        wvWeatherMap.getSettings().setDomStorageEnabled(true);
        wvWeatherMap.getSettings().setSupportZoom(true);
        wvWeatherMap.getSettings().setBuiltInZoomControls(true);
        wvWeatherMap.getSettings().setDisplayZoomControls(false);
        
        // Add Javascript Interface to intercept clicks
        wvWeatherMap.addJavascriptInterface(new WebAppInterface(), "Android");
        wvWeatherMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Inject click listener into the windy iframe to grab lat/lon from the URL hash or center
                // Since Windy is an iframe and cross-origin blocks perfect click mapping, 
                // we tell the user they can just drag the map and wait, but a better approach 
                // for the widget is to read the URL hash change.
                view.loadUrl("javascript:(function() {" +
                        "  window.addEventListener('message', function(event) {" +
                        "    if(event.data && event.data.lat && event.data.lon) {" +
                        "       Android.receiveCoordinates(event.data.lat, event.data.lon);" +
                        "    }" +
                        "  });" +
                        "  setTimeout(function(){" +
                        "     document.body.addEventListener('click', function(e) {" +
                        "         Android.notifyMapClicked();" +
                        "     });" +
                        "  }, 2000);" +
                        "})()");
            }
        });

        // Allow 2-finger zoom and pan by disabling parent scroll interception
        wvWeatherMap.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        // Map Control Listeners
        btnMapRadar.setOnClickListener(v -> loadWindyMap("radar"));
        btnMapWind.setOnClickListener(v -> loadWindyMap("wind"));
        btnMapRain.setOnClickListener(v -> loadWindyMap("rain"));
        btnMapTemp.setOnClickListener(v -> loadWindyMap("temp"));
        btnMapClouds.setOnClickListener(v -> loadWindyMap("clouds"));

        rvForecast.setLayoutManager(new LinearLayoutManager(this));

        // Get Data from Intent
        Intent intent = getIntent();
        double lat = intent.getDoubleExtra("LAT", 0.0);
        double lon = intent.getDoubleExtra("LON", 0.0);
        String cityName = intent.getStringExtra("CITY_NAME");

        if (cityName != null && !cityName.isEmpty()) {
            tvLocationTitle.setText(cityName);
        } else {
            tvLocationTitle.setText("AgriRent Forecast");
        }

        if (lat != 0.0 && lon != 0.0) {
            currentLat = lat;
            currentLon = lon;
            fetch7DayForecast(lat, lon);
            
            // Load interactive map defaulting to radar
            loadWindyMap("radar");
        } else {
            Toast.makeText(this, "Valid Location coordinates missing.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetch7DayForecast(double lat, double lon) {
        pbForecast.setVisibility(View.VISIBLE);
        rvForecast.setVisibility(View.GONE);

        // Fetch current + daily 7 days + hourly (for dew/uv/surface)
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                "&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m" +
                "&hourly=dew_point_2m,surface_temperature,uv_index" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    pbForecast.setVisibility(View.GONE);
                    Toast.makeText(WeatherActivity.this, "Failed to fetch weather data.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        JSONObject root = new JSONObject(jsonString);

                        // 1. Parse Current Weather (Top Header & Grid)
                        JSONObject current = root.getJSONObject("current");
                        double curTemp = current.getDouble("temperature_2m");
                        int curHumidity = current.getInt("relative_humidity_2m");
                        double curWind = current.getDouble("wind_speed_10m");
                        double curRain = current.getDouble("precipitation");
                        int curWeatherCode = current.getInt("weather_code");
                        String curCondition = getWeatherDescription(curWeatherCode);

                        // 1.5 Parse Hourly Metrics (take first index [0] as current approximations)
                        JSONObject hourly = root.getJSONObject("hourly");
                        double dewPoint = hourly.getJSONArray("dew_point_2m").getDouble(0);
                        double rawUV = hourly.getJSONArray("uv_index").getDouble(0);
                        double surfaceTemp = hourly.getJSONArray("surface_temperature").getDouble(0);
                        
                        String uvDesc = (rawUV > 7) ? "High" : ((rawUV > 3) ? "Med" : "Low");
                        String uvString = String.format(Locale.getDefault(), "%.1f (%s)", rawUV, uvDesc);

                        // 2. Parse 7-Day Daily Forecast (List)
                        JSONObject daily = root.getJSONObject("daily");
                        JSONArray timeArray = daily.getJSONArray("time");
                        JSONArray codeArray = daily.getJSONArray("weather_code");
                        JSONArray maxArray = daily.getJSONArray("temperature_2m_max");
                        JSONArray minArray = daily.getJSONArray("temperature_2m_min");

                        List<ForecastAdapter.ForecastItem> list = new ArrayList<>();
                        SimpleDateFormat dateInputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        SimpleDateFormat dateOutputFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

                        for (int i = 0; i < timeArray.length(); i++) {
                            ForecastAdapter.ForecastItem item = new ForecastAdapter.ForecastItem();
                            
                            // Format date to "Monday", "Tuesday" etc.
                            try {
                                Date date = dateInputFormat.parse(timeArray.getString(i));
                                if (i == 0) {
                                    item.dayName = "Today";
                                } else if (i == 1) {
                                    item.dayName = "Tomorrow";
                                } else {
                                    item.dayName = dateOutputFormat.format(date);
                                }
                            } catch (Exception e) {
                                item.dayName = timeArray.getString(i);
                            }

                            item.conditionIcon = getWeatherEmoji(codeArray.getInt(i));
                            item.maxTemp = String.format(Locale.getDefault(), "%.0f°", maxArray.getDouble(i));
                            item.minTemp = String.format(Locale.getDefault(), "%.0f°", minArray.getDouble(i));
                            list.add(item);
                        }

                        // 3. Update UI
                        runOnUiThread(() -> {
                            tvMainTemp.setText(String.format(Locale.getDefault(), "%.0f°", curTemp));
                            tvMainCondition.setText(curCondition);
                            tvWind.setText(String.format(Locale.getDefault(), "%.1f km/h", curWind));
                            tvHumidity.setText(curHumidity + "%");
                            tvRain.setText(String.format(Locale.getDefault(), "%.1f mm", curRain));
                            tvDewPoint.setText(String.format(Locale.getDefault(), "%.0f°", dewPoint));
                            tvUV.setText(uvString);
                            tvSurfaceTemp.setText(String.format(Locale.getDefault(), "%.0f°", surfaceTemp));

                            ForecastAdapter adapter = new ForecastAdapter(list);
                            rvForecast.setAdapter(adapter);
                            pbForecast.setVisibility(View.GONE);
                            rvForecast.setVisibility(View.VISIBLE);
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> pbForecast.setVisibility(View.GONE));
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

    private String getWeatherEmoji(int code) {
        if (code == 0) return "☀️";
        if (code == 1 || code == 2 || code == 3) return "⛅";
        if (code == 45 || code == 48) return "🌫️";
        if (code >= 51 && code <= 55) return "🌧️";
        if (code >= 61 && code <= 65) return "🌧️";
        if (code >= 71 && code <= 77) return "❄️";
        if (code >= 80 && code <= 82) return "🌦️";
        if (code >= 95 && code <= 99) return "⛈️";
        return "❓";
    }

    private void loadWindyMap(String overlay) {
        currentOverlay = overlay;
        if (currentLat == 0.0 || currentLon == 0.0) return;
        
        // Removed detailLat/detailLon to prevent the bottom forecast panel from opening automatically.
        // Keeping lat, lon, and marker=true to center the map and drop the pin. 
        // Added message=true to allow window messages.
        String windyUrl = "https://embed.windy.com/embed.html?type=map&location=coordinates&metricRain=mm&metricTemp=°C&metricWind=km/h&zoom=7&overlay=" + currentOverlay + "&product=" + currentOverlay + "&level=surface" +
                "&lat=" + currentLat + "&lon=" + currentLon + 
                "&message=true" +
                "&marker=true";
                
        wvWeatherMap.loadUrl(windyUrl);
    }

    // Javascript Interface to handle Map Clicks
    public class WebAppInterface {
        @JavascriptInterface
        public void receiveCoordinates(double lat, double lon) {
            runOnUiThread(() -> {
                currentLat = lat;
                currentLon = lon;
                Toast.makeText(WeatherActivity.this, "Adjusting location to pin...", Toast.LENGTH_SHORT).show();
                fetch7DayForecast(lat, lon);
                loadWindyMap(currentOverlay);
            });
        }
        
        @JavascriptInterface
        public void notifyMapClicked() {
            // Silently handle map click to avoid extraneous toasts
        }
    }
}
