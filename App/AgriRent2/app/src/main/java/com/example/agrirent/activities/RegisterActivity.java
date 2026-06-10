package com.example.agrirent.activities;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.agrirent.R;
import com.example.agrirent.models.LoginResponse;
import com.example.agrirent.models.RegisterRequest;
import com.example.agrirent.models.SendOtpRequest;
import com.example.agrirent.models.SendOtpResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.network.LoadingInterceptor;
import com.example.agrirent.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends BaseActivity {

    private EditText etFullName, etMobile, etOtp, etPassword, etConfirmPassword;
    private MaterialButton btnSendOtp, btnRegister;
    private TextView tvSwitchAuth, tvResendLabel;
    private CheckBox cbTerms;

    private String otpSessionId;
    private SessionManager session;

    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Register activity for loading dialog
        LoadingInterceptor.setCurrentActivity(this);

        session = new SessionManager(this);

        // Bind Views
        etFullName = findViewById(R.id.etFullName);
        etMobile   = findViewById(R.id.etMobile);
        etOtp      = findViewById(R.id.etOtp);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        cbTerms = findViewById(R.id.cbTerms);
        tvResendLabel = findViewById(R.id.tvResendLabel);

        // "Verify" / "Send OTP" Button inside the text box
        btnSendOtp  = findViewById(R.id.btnSendOtp);
        btnRegister = findViewById(R.id.btnRegister);
        tvSwitchAuth = findViewById(R.id.tvSwitchAuth);

        // Set initial text using Resource
        btnSendOtp.setText(R.string.send_otp);

        // Setup Clicks
        setupLoginRedirectText();

        btnSendOtp.setOnClickListener(v -> sendOtp());

        // Handle Resend Label Click
        tvResendLabel.setOnClickListener(v -> sendOtp());

        btnRegister.setOnClickListener(v -> registerUser());

        // 1. Initialize ScrollView
        scrollView = findViewById(R.id.scrollView);

        // 2. Add Keyboard Listener
        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();

                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    View focusedView = getCurrentFocus();
                    if (focusedView != null && scrollView != null) {
                        scrollView.post(() -> {
                            scrollView.smoothScrollTo(0, focusedView.getBottom() + 100);
                        });
                    }
                }
            }
        });
    }

    // ================= SEND OTP =================

    private void sendOtp() {
        String mobile = etMobile.getText().toString().trim();

        if (!mobile.matches("\\d{10}")) {
            etMobile.setError(getString(R.string.mobile_error));
            return;
        }

        // Disable button while sending
        btnSendOtp.setEnabled(false);
        btnSendOtp.setText("Sending...");

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.sendOtp(new SendOtpRequest("+91" + mobile))
                .enqueue(new Callback<SendOtpResponse>() {
                    @Override
                    public void onResponse(Call<SendOtpResponse> call, Response<SendOtpResponse> response) {
                        btnSendOtp.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            otpSessionId = response.body().getSessionId();

                            // Change button text to "Resend OTP"
                            btnSendOtp.setText(R.string.resend_otp);

                            Toast.makeText(RegisterActivity.this, getString(R.string.otp_sent), Toast.LENGTH_SHORT).show();
                            etOtp.requestFocus();
                        } else {
                            btnSendOtp.setText(R.string.send_otp); // Reset on failure
                            Toast.makeText(RegisterActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<SendOtpResponse> call, Throwable t) {
                        btnSendOtp.setEnabled(true);
                        btnSendOtp.setText(R.string.send_otp);
                        Toast.makeText(RegisterActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ================= REGISTER =================

    private void registerUser() {
        String name = etFullName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty()) {
            etFullName.setError("Full name required");
            return;
        }

        if (otpSessionId == null) {
            Toast.makeText(this, "Please verify mobile number first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (otp.length() != 6) {
            etOtp.setError(getString(R.string.invalid_otp));
            return;
        }

        if (password.length() < 6) {
            etPassword.setError(getString(R.string.password_length_error));
            return;
        }

        if (!password.equals(confirmPass)) {
            etConfirmPassword.setError(getString(R.string.password_mismatch));
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, getString(R.string.terms_error), Toast.LENGTH_SHORT).show();
            return;
        }

        RegisterRequest request = new RegisterRequest(name, "+91" + mobile, password, otpSessionId, otp);
        ApiService api = ApiClient.getClient(this).create(ApiService.class);

        api.otpRegister(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                // 1. SUCCESS
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.reg_success), Toast.LENGTH_LONG).show();
                    goToLogin();
                }
                // 2. FAILURE
                else {
                    try {
                        String errorBody = "";
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }

                        // Check for ALREADY REGISTERED
                        if (response.code() == 400 && errorBody.contains("Already registered")) {
                            Toast.makeText(RegisterActivity.this, getString(R.string.already_registered_error), Toast.LENGTH_LONG).show();
                            goToLogin();
                        } else {
                            // Generic Failure
                            Toast.makeText(RegisterActivity.this, getString(R.string.registration_failed), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(RegisterActivity.this, getString(R.string.registration_failed), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ================= LOGIN REDIRECT =================

    private void setupLoginRedirectText() {
        // Get the full sentence (e.g., "Already have an account? Login")
        String fullText = getString(R.string.already_have_account);

        // Get the word to make clickable (e.g., "Login" or "लॉगिन करें")
        // NOTE: In your string files, the 'already_have_account' string ends with the text from 'login'.
        // We will try to find "Login" or just use the last word.
        String actionWord = getString(R.string.login);

        SpannableString spannable = new SpannableString(fullText);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(getResources().getColor(R.color.agri_green_dark));
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        };

        // Find where the word "Login" starts in the sentence
        int start = fullText.lastIndexOf(actionWord);

        // Fallback: If "Login" word isn't strictly found, just highlight the last 5 chars
        if (start == -1) start = fullText.length() - 5;
        if (start < 0) start = 0;

        spannable.setSpan(clickableSpan, start, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvSwitchAuth.setText(spannable);
        tvSwitchAuth.setMovementMethod(LinkMovementMethod.getInstance());
    }
}