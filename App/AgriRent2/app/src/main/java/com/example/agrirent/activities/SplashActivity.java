package com.example.agrirent.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.agrirent.R;
import com.example.agrirent.models.RefreshRequest;
import com.example.agrirent.models.RefreshResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.network.LoadingInterceptor;
import com.example.agrirent.utils.LocaleHelper;
import com.example.agrirent.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends BaseActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        session = new SessionManager(this);

        // 🌗 Apply Theme
        if (session.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // Register activity for loading dialog
        LoadingInterceptor.setCurrentActivity(this);

        // 🌍 Apply language FIRST
        LocaleHelper.applyLanguage(this, session.getLanguage());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.imgLogo);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        TextView txtProgress = findViewById(R.id.txtProgress);

        // 🔄 Animated Vector
        Drawable d = ContextCompat.getDrawable(this, R.drawable.avd_agrirent_animated);
        if (d instanceof AnimatedVectorDrawable) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) d;
            logo.setImageDrawable(avd);
            avd.start();
        }

        startLoading(progressBar, txtProgress);
    }

    private void startLoading(ProgressBar bar, TextView txt) {

        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(1200); // 1.2 sec
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(a -> {
            int p = (int) a.getAnimatedValue();
            bar.setProgress(p);
            txt.setText(p + "%");
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                decideNextScreen();
            }
        });

        animator.start();
    }

    private void decideNextScreen() {

        // 1️⃣ Language not selected
        if (!session.isLanguageSelected()) {
            startActivity(new Intent(this, LanguageActivity.class));
            finish();
            return;
        }

        // 2️⃣ Has refresh token (valid for 30 days) - Go directly to Home!
        if (session.hasRefreshToken()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // 3️⃣ No refresh token - Go to Login
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
