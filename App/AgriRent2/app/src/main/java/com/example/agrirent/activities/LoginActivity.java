package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.agrirent.R;
import com.example.agrirent.adapters.LoginSliderAdapter;
import com.example.agrirent.models.LoginRequest;
import com.example.agrirent.models.LoginResponse;
import com.example.agrirent.models.OtpLoginRequest;
import com.example.agrirent.models.SendOtpRequest;
import com.example.agrirent.models.SendOtpResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.network.LoadingInterceptor;
import com.example.agrirent.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {

    // Inputs
    private TextInputEditText etMobile, etPassword, etOtp;
    private TextInputLayout tilPassword, tilOtp;
    private CheckBox cbRememberMe;

    // Buttons & Texts
    private MaterialButton btnLogin, btnVerifyOtp, btnSwitchMode;
    private TextView tvForgot, tvSwitchAuth, tvResendOtp;

    private SessionManager session;
    private String otpSessionId;

    // State flags
    private boolean isOtpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Register activity for loading dialog
        LoadingInterceptor.setCurrentActivity(this);

        // ================= IMAGE SLIDER =================
        ViewPager2 viewPager = findViewById(R.id.viewPagerSlider);
        LoginSliderAdapter sliderAdapter = new LoginSliderAdapter();
        viewPager.setAdapter(sliderAdapter);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            int index = 0;
            @Override
            public void run() {
                index = (index + 1) % sliderAdapter.getItemCount();
                viewPager.setCurrentItem(index, true);
                handler.postDelayed(this, 3000);
            }
        }, 3000);

        // ================= INIT =================
        session = new SessionManager(this);

        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        etOtp = findViewById(R.id.etOtp);

        tilPassword = findViewById(R.id.tilPassword);
        tilOtp = findViewById(R.id.tilOtp);

        btnLogin = findViewById(R.id.btnLogin);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnSwitchMode = findViewById(R.id.btnSwitchMode);

        tvForgot = findViewById(R.id.tvForgot);
        tvSwitchAuth = findViewById(R.id.tvSwitchAuth);
        tvResendOtp = findViewById(R.id.tvResendOtp);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        // Load saved credentials if Remember Me was checked
        android.content.SharedPreferences prefs = getSharedPreferences("agrirent_prefs", MODE_PRIVATE);
        boolean remembered = prefs.getBoolean("remember_me", false);
        if (remembered) {
            String savedMobile = prefs.getString("saved_mobile", "");
            String savedPassword = prefs.getString("saved_password", "");
            etMobile.setText(savedMobile);
            etPassword.setText(savedPassword);
            cbRememberMe.setChecked(true);
        }

        // ================= CLICKS =================

        // 1. Register Logic
        tvSwitchAuth.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // 2. Login Logic
        btnLogin.setOnClickListener(v -> doPasswordLogin());

        // 3. Verify OTP
        btnVerifyOtp.setOnClickListener(v -> verifyOtp());

        // 4. Resend OTP
        tvResendOtp.setOnClickListener(v -> sendOtp());

        // 5. Toggle Mode
        btnSwitchMode.setOnClickListener(v -> toggleLoginMode());

        // 6. Forgot Password
        tvForgot.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class))
        );
    }

    // ================= MODE SWITCHING LOGIC =================

    private void toggleLoginMode() {
        if (!isOtpMode) {
            String rawMobile = etMobile.getText().toString().trim();
            if (!rawMobile.matches("\\d{10}")) {
                etMobile.setError(getString(R.string.mobile_error));
                return;
            }

            // --- SWITCH TO OTP MODE ---
            isOtpMode = true;

            tilPassword.setVisibility(View.GONE);
            btnLogin.setVisibility(View.GONE);

            tilOtp.setVisibility(View.VISIBLE);
            tvResendOtp.setVisibility(View.VISIBLE);
            btnVerifyOtp.setVisibility(View.VISIBLE);

            // Change button text using String Resource
            btnSwitchMode.setText(R.string.login_via_password);

            sendOtp();

        } else {
            // --- SWITCH BACK TO PASSWORD MODE ---
            isOtpMode = false;

            tilOtp.setVisibility(View.GONE);
            tvResendOtp.setVisibility(View.GONE);
            btnVerifyOtp.setVisibility(View.GONE);

            tilPassword.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.VISIBLE);

            // Change button text using String Resource
            btnSwitchMode.setText(R.string.login_via_otp);
        }
    }

    // ================= NETWORK CALLS =================

    private void doPasswordLogin() {
        String rawMobile = etMobile.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!rawMobile.matches("\\d{10}")) {
            etMobile.setError(getString(R.string.mobile_error));
            return;
        }
        if (password.length() < 6) {
            etPassword.setError(getString(R.string.password_length_error));
            return;
        }

        String mobile = "+91" + rawMobile;

        // Handle Remember Me
        android.content.SharedPreferences prefs = getSharedPreferences("agrirent_prefs", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        if (cbRememberMe.isChecked()) {
            editor.putBoolean("remember_me", true);
            editor.putString("saved_mobile", rawMobile);
            editor.putString("saved_password", password);
        } else {
            editor.putBoolean("remember_me", false);
            editor.remove("saved_mobile");
            editor.remove("saved_password");
        }
        editor.apply();

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.login(new LoginRequest(mobile, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    onLoginSuccess(response.body().getData());
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendOtp() {
        String rawMobile = etMobile.getText().toString().trim();
        if (!rawMobile.matches("\\d{10}")) {
            etMobile.setError(getString(R.string.mobile_error));
            return;
        }

        String mobile = "+91" + rawMobile;
        Toast.makeText(this, "Sending OTP...", Toast.LENGTH_SHORT).show();

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.sendOtp(new SendOtpRequest(mobile)).enqueue(new Callback<SendOtpResponse>() {
            @Override
            public void onResponse(Call<SendOtpResponse> call, Response<SendOtpResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    otpSessionId = response.body().getSessionId();
                    etOtp.requestFocus();
                    Toast.makeText(LoginActivity.this, getString(R.string.otp_sent), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SendOtpResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();
        String rawMobile = etMobile.getText().toString().trim();

        if (otp.length() != 6) {
            etOtp.setError(getString(R.string.invalid_otp));
            return;
        }
        if (otpSessionId == null) {
            Toast.makeText(this, "Please wait for OTP to send", Toast.LENGTH_SHORT).show();
            return;
        }

        String mobile = "+91" + rawMobile;
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.otpLogin(new OtpLoginRequest(mobile, otpSessionId, otp)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    onLoginSuccess(response.body().getData());
                } else {
                    Toast.makeText(LoginActivity.this, "OTP verification failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onLoginSuccess(LoginResponse.Data data) {

        // 1️⃣ Save session
        session.saveAuth(data.getToken(), data.getRefreshToken());
        session.saveUserName(data.getUsername());
        session.saveUserMobile(data.getMobile());
        session.saveUserId(data.getUserId());
        session.saveUserRoles(data.getRoles());

        String authToken = data.getToken();

        // 🔔 Register FCM Device Token, then navigate to Home
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String fcmToken = task.getResult();
                String language = session.getLanguage();
                String deviceId = android.provider.Settings.Secure.getString(
                        getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                ApiService api = ApiClient.getClient(LoginActivity.this).create(ApiService.class);
                api.updateFcmToken(new com.example.agrirent.models.FcmTokenRequest(fcmToken, language, deviceId)).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Log.d("FCM", "Token registered. Code: " + response.code());
                        handleSuccessfulLogin(data);
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("FCM", "Token upload failed: " + t.getMessage());
                        handleSuccessfulLogin(data); // Still navigate even if token upload fails
                    }
                });
            } else {
                Log.w("FCM", "Could not get FCM token", task.getException());
                handleSuccessfulLogin(data); // Still navigate
            }
        });
    }

    private void handleSuccessfulLogin(LoginResponse.Data loginResponse) {
        // Navigate to Home screen
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}