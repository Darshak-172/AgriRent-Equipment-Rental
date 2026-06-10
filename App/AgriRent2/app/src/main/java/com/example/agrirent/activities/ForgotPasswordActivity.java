package com.example.agrirent.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.agrirent.R;
import com.example.agrirent.models.ForgotPasswordRequest;
import com.example.agrirent.models.SendOtpRequest;
import com.example.agrirent.models.SendOtpResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.network.LoadingInterceptor;
import com.google.android.material.button.MaterialButton;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends BaseActivity {

    private EditText etMobile, etOtp, etNewPassword;
    private MaterialButton btnSendOtp, btnResendOtp, btnChangePassword;
    private LinearLayout layoutResetSection;

    private String sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Register activity for loading dialog
        LoadingInterceptor.setCurrentActivity(this);

        // Bind Views
        etMobile = findViewById(R.id.etMobile);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);

        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnResendOtp = findViewById(R.id.btnResendOtp);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        layoutResetSection = findViewById(R.id.layoutResetSection);

        // --- Clicks ---

        // 1. Send OTP (The big button)
        btnSendOtp.setOnClickListener(v -> sendOtp(false));

        // 2. Resend OTP (The small button inside OTP box)
        btnResendOtp.setOnClickListener(v -> sendOtp(true));

        // 3. Change Password
        btnChangePassword.setOnClickListener(v -> resetPassword());
    }

    private void sendOtp(boolean isResend) {
        String mobile = etMobile.getText().toString().trim();

        if (!mobile.matches("\\d{10}")) {
            etMobile.setError(getString(R.string.mobile_error));
            return;
        }

        if(!isResend) {
            btnSendOtp.setText("Sending...");
            btnSendOtp.setEnabled(false);
        } else {
            Toast.makeText(this, "Resending OTP...", Toast.LENGTH_SHORT).show();
        }

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.sendOtp(new SendOtpRequest("+91" + mobile))
                .enqueue(new Callback<SendOtpResponse>() {
                    @Override
                    public void onResponse(Call<SendOtpResponse> call, Response<SendOtpResponse> response) {
                        if(!isResend) {
                            btnSendOtp.setEnabled(true);
                            btnSendOtp.setText(getString(R.string.send_otp));
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            sessionId = response.body().getSessionId();

                            // 1. Hide the Big Send OTP Button
                            btnSendOtp.setVisibility(View.GONE);

                            // 2. Show the Reset Section (OTP field, New Pass, Change Button)
                            layoutResetSection.setVisibility(View.VISIBLE);

                            Toast.makeText(ForgotPasswordActivity.this, getString(R.string.otp_sent), Toast.LENGTH_SHORT).show();
                            etOtp.requestFocus();
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<SendOtpResponse> call, Throwable t) {
                        if(!isResend) {
                            btnSendOtp.setEnabled(true);
                            btnSendOtp.setText(getString(R.string.send_otp));
                        }
                        Toast.makeText(ForgotPasswordActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resetPassword() {
        String otp = etOtp.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();

        if (otp.length() != 6) {
            etOtp.setError(getString(R.string.invalid_otp));
            return;
        }
        if (newPass.length() < 6) {
            etNewPassword.setError(getString(R.string.password_length_error));
            return;
        }
        if (sessionId == null) {
            Toast.makeText(this, "Session expired, please resend OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        ForgotPasswordRequest request = new ForgotPasswordRequest(
                "+91" + mobile,
                sessionId,
                otp,
                newPass
        );

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.forgotPassword(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, getString(R.string.password_changed), Toast.LENGTH_LONG).show();
                    finish(); // Go back to login
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Failed to reset password", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ForgotPasswordActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        });
    }
}