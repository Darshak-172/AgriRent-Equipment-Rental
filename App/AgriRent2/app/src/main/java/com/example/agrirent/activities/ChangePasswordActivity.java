package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.agrirent.R;
import com.example.agrirent.models.ChangePasswordRequest;
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

public class ChangePasswordActivity extends BaseActivity {

    private EditText etMobile, etOldPassword, etOtp, etNewPassword;
    private MaterialButton btnSendOtp, btnResendOtp, btnChangePassword;
    private LinearLayout layoutResetSection;

    private String otpSessionId;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Register activity for loading dialog
        LoadingInterceptor.setCurrentActivity(this);

        session = new SessionManager(this);

        // Bind Views
        etMobile = findViewById(R.id.etMobile);
        etOldPassword = findViewById(R.id.etOldPassword);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);

        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnResendOtp = findViewById(R.id.btnResendOtp); // The inner button
        btnChangePassword = findViewById(R.id.btnChangePassword);

        layoutResetSection = findViewById(R.id.layoutResetSection);

        // Setup Clicks
        btnSendOtp.setOnClickListener(v -> sendOtp(false));
        btnResendOtp.setOnClickListener(v -> sendOtp(true)); // Resend logic
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    // ================= SEND OTP =================
    private void sendOtp(boolean isResend) {
        String mobile = etMobile.getText().toString().trim();
        String oldPass = etOldPassword.getText().toString().trim();

        if (!mobile.matches("\\d{10}")) {
            etMobile.setError(getString(R.string.mobile_error));
            return;
        }

        if (oldPass.length() < 6) {
            etOldPassword.setError(getString(R.string.password_length_error));
            return;
        }

        if (!isResend) {
            btnSendOtp.setEnabled(false);
            btnSendOtp.setText("Sending...");
        } else {
            Toast.makeText(this, "Resending OTP...", Toast.LENGTH_SHORT).show();
        }

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.sendOtp(new SendOtpRequest("+91" + mobile))
                .enqueue(new Callback<SendOtpResponse>() {
                    @Override
                    public void onResponse(Call<SendOtpResponse> call, Response<SendOtpResponse> response) {

                        if (!isResend) {
                            btnSendOtp.setEnabled(true);
                            btnSendOtp.setText(getString(R.string.send_otp));
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            otpSessionId = response.body().getSessionId();

                            // Toggle Visibility
                            btnSendOtp.setVisibility(View.GONE);
                            layoutResetSection.setVisibility(View.VISIBLE);

                            Toast.makeText(ChangePasswordActivity.this, getString(R.string.otp_sent), Toast.LENGTH_SHORT).show();
                            etOtp.requestFocus();
                        } else {
                            Toast.makeText(ChangePasswordActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<SendOtpResponse> call, Throwable t) {
                        if (!isResend) {
                            btnSendOtp.setEnabled(true);
                            btnSendOtp.setText(getString(R.string.send_otp));
                        }
                        Toast.makeText(ChangePasswordActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ================= CHANGE PASSWORD =================
    private void changePassword() {
        String mobile = "+91" + etMobile.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String oldPass = etOldPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (otp.length() != 6) {
            etOtp.setError(getString(R.string.invalid_otp));
            return;
        }
        if (newPass.length() < 6) {
            etNewPassword.setError(getString(R.string.password_length_error));
            return;
        }
        if (otpSessionId == null) {
            Toast.makeText(this, "Session Expired. Resend OTP.", Toast.LENGTH_SHORT).show();
            return;
        }

        ChangePasswordRequest request = new ChangePasswordRequest(
                mobile,
                otpSessionId,
                otp,
                oldPass,
                newPass
        );

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.changePassword(request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ChangePasswordActivity.this, getString(R.string.password_changed), Toast.LENGTH_SHORT).show();

                    // 🔐 LOGOUT and Redirect
                    session.logout();
                    Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(ChangePasswordActivity.this, "Change password failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(ChangePasswordActivity.this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
            }
        });
    }
}