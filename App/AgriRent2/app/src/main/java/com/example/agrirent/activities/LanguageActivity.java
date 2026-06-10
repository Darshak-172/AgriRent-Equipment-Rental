package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.agrirent.R;
import com.example.agrirent.models.FcmTokenRequest;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.LocaleHelper;
import com.example.agrirent.utils.SessionManager;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LanguageActivity extends BaseActivity {

    private static final String TAG = "LanguageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        SessionManager session = new SessionManager(this);

        findViewById(R.id.btnEnglish).setOnClickListener(v ->
                selectLanguage(session, "en")
        );

        findViewById(R.id.btnHindi).setOnClickListener(v ->
                selectLanguage(session, "hi")
        );

        findViewById(R.id.btnGujarati).setOnClickListener(v ->
                selectLanguage(session, "gu")
        );
    }

    private void selectLanguage(SessionManager session, String lang) {

        session.setLanguage(lang);
        session.setLanguageSelected(true);

        // If user is already logged in, update language on server and go to Home
        if (session.hasRefreshToken()) {
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
                if (token != null) {
                    String deviceId = android.provider.Settings.Secure.getString(
                            getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    ApiService api = ApiClient.getClient(this).create(ApiService.class);
                    api.updateFcmToken(new FcmTokenRequest(token, lang, deviceId)).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            Log.d(TAG, "Language updated on server: " + lang);
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.e(TAG, "Failed to update language: " + t.getMessage());
                        }
                    });
                }
            });
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        finish();
    }
}