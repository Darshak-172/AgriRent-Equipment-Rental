package com.example.agrirent.utils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.WindowManager;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.example.agrirent.R;

/**
 * Global Loading Dialog Manager
 * Shows a fullscreen loading dialog with transparent blur background and animated AgriRent logo
 */
public class LoadingDialog {

    private static Dialog loadingDialog;
    private static int requestCount = 0; // Counter for nested/parallel API calls
    private static Activity lastActivity;
    private static AnimatedVectorDrawable animatedLogo;
    private static Handler animationHandler = new Handler(Looper.getMainLooper());
    private static Runnable dotAnimationRunnable;
    private static TextView tvLoadingDots;
    private static View outerRotatingRing1;
    private static View outerRotatingRing2;
    private static int dotCounter = 0;

    /**
     * Show loading dialog
     */
    public static synchronized void show(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        requestCount++;
        lastActivity = activity;

        // Only create and show dialog on first request
        if (loadingDialog == null || !loadingDialog.isShowing()) {
            try {
                // Create fullscreen dialog
                // Use a standard material theme instead of legacy translucent
                loadingDialog = new Dialog(activity, R.style.Theme_Agrirent_FullScreen);

                // Inflate custom layout
                LayoutInflater inflater = LayoutInflater.from(activity);
                View dialogView = inflater.inflate(R.layout.dialog_loading, null);

                // Get ImageView and set animated drawable
                ImageView imgLoadingLogo = dialogView.findViewById(R.id.imgLoadingLogo);
                Drawable d = ContextCompat.getDrawable(activity, R.drawable.avd_agrirent_animated);

                if (d instanceof AnimatedVectorDrawable) {
                    animatedLogo = (AnimatedVectorDrawable) d;
                    imgLoadingLogo.setImageDrawable(animatedLogo);
                }

                // Get the rotating rings and apply rotation animations
                outerRotatingRing1 = dialogView.findViewById(R.id.outerRotatingRing1);
                outerRotatingRing2 = dialogView.findViewById(R.id.outerRotatingRing2);
                
                if (outerRotatingRing1 != null) {
                    startRotatingAnimation(outerRotatingRing1, 2000, true); // 2s, clockwise
                }
                
                if (outerRotatingRing2 != null) {
                    startRotatingAnimation(outerRotatingRing2, 3000, false); // 3s, counter-clockwise
                }

                // Get the dots TextView for animation
                tvLoadingDots = dialogView.findViewById(R.id.tvLoadingDots);

                loadingDialog.setContentView(dialogView);
                loadingDialog.setCancelable(false);

                Window window = loadingDialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawableResource(android.R.color.transparent);
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(activity.getWindow().getStatusBarColor());
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        window.getDecorView().setSystemUiVisibility(activity.getWindow().getDecorView().getSystemUiVisibility());
                    }
                }

                // Ensure dialog is shown on the UI thread
                activity.runOnUiThread(() -> {
                    try {
                        if (loadingDialog != null && !loadingDialog.isShowing()) {
                            loadingDialog.show();
                            // Start animations after dialog is shown
                            if (animatedLogo != null) {
                                animatedLogo.start();
                            }
                            // Start dot animation
                            startDotAnimation();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Hide loading dialog
     */
    public static synchronized void hide() {
        requestCount--;

        // Only hide dialog when all requests are complete
        if (requestCount <= 0) {
            requestCount = 0;

            if (loadingDialog != null && loadingDialog.isShowing()) {
                try {
                    // Stop animations
                    stopDotAnimation();
                    if (animatedLogo != null) {
                        animatedLogo.stop();
                        animatedLogo = null;
                    }

                    // Ensure dialog is dismissed on the UI thread
                    if (lastActivity != null && !lastActivity.isFinishing()) {
                        lastActivity.runOnUiThread(() -> {
                            try {
                                if (loadingDialog != null && loadingDialog.isShowing()) {
                                    loadingDialog.dismiss();
                                    loadingDialog = null;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } else {
                        loadingDialog.dismiss();
                        loadingDialog = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Force close all loading dialogs
     */
    public static synchronized void hideAll() {
        requestCount = 0;
        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                stopDotAnimation();
                if (animatedLogo != null) {
                    animatedLogo.stop();
                    animatedLogo = null;
                }
                loadingDialog.dismiss();
                loadingDialog = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check if loading dialog is visible
     */
    public static boolean isShowing() {
        return loadingDialog != null && loadingDialog.isShowing();
    }

    /**
     * Animate the dots - cycles between ".", "..", "..."
     */
    private static void startDotAnimation() {
        dotCounter = 0;
        dotAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                if (tvLoadingDots != null) {
                    switch (dotCounter % 4) {
                        case 0:
                            tvLoadingDots.setText(".");
                            break;
                        case 1:
                            tvLoadingDots.setText("..");
                            break;
                        case 2:
                            tvLoadingDots.setText("...");
                            break;
                        case 3:
                            tvLoadingDots.setText("");
                            break;
                    }
                    dotCounter++;
                    animationHandler.postDelayed(this, 500); // Update every 500ms
                }
            }
        };
        animationHandler.post(dotAnimationRunnable);
    }

    /**
     * Stop the dot animation
     */
    private static void stopDotAnimation() {
        if (dotAnimationRunnable != null) {
            animationHandler.removeCallbacks(dotAnimationRunnable);
            dotAnimationRunnable = null;
        }
        tvLoadingDots = null;
        dotCounter = 0;
    }

    /**
     * Start rotating animation for the rings
     * @param view The view to animate
     * @param duration Duration in milliseconds
     * @param clockwise True for clockwise, false for counter-clockwise
     */
    private static void startRotatingAnimation(View view, int duration, boolean clockwise) {
        // "Wobble" effect by rotating slightly off-center
        // Center is 0.5f, 0.5f. We use 0.52f to make it wobble eccentric
        float pivotX = 0.50f;
        float pivotY = clockwise ? 0.52f : 0.48f; 
        
        RotateAnimation rotateAnimation = new RotateAnimation(
                0, 
                clockwise ? 360 : -360,
                Animation.RELATIVE_TO_SELF, pivotX,
                Animation.RELATIVE_TO_SELF, pivotY);
        rotateAnimation.setDuration(duration);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setRepeatMode(Animation.RESTART);
        // Use LinearInterpolator for smooth rotation
        rotateAnimation.setInterpolator(new android.view.animation.LinearInterpolator());
        view.startAnimation(rotateAnimation);
    }
}
