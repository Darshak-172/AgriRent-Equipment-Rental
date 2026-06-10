package com.example.agrirent.network;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.example.agrirent.utils.LoadingDialog;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * OkHttp Interceptor to show/hide loading dialog for all API calls
 * Ensures dialog is shown/hidden on the main thread
 */
public class LoadingInterceptor implements Interceptor {

    private static WeakReference<Activity> currentActivity = null;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Set the current activity (call this from each Activity's onCreate)
     */
    public static void setCurrentActivity(Activity activity) {
        currentActivity = new WeakReference<>(activity);
    }

    /**
     * Clear the current activity (call this from Activity's onDestroy if needed)
     */
    public static void clearCurrentActivity() {
        currentActivity = null;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        okhttp3.Request request = chain.request();
        boolean skipLoading = request.header("No-Loading") != null;

        if (!skipLoading) {
            // Show loading dialog before request (on main thread)
            showLoading();
        }

        try {
            okhttp3.Request newRequest = request;
            if (skipLoading) {
                newRequest = request.newBuilder().removeHeader("No-Loading").build();
            }
            return chain.proceed(newRequest);
        } finally {
            if (!skipLoading) {
                // Hide loading dialog after response (on main thread)
                hideLoading();
            }
        }
    }

    private void showLoading() {
        // Post to main thread to show dialog
        mainHandler.post(() -> {
            try {
                if (currentActivity != null && currentActivity.get() != null) {
                    Activity activity = currentActivity.get();
                    if (!activity.isFinishing()) {
                        try {
                            // Check if a SwipeRefreshLayout is currently refreshing
                            int resId = activity.getResources().getIdentifier("swipeRefreshLayout", "id", activity.getPackageName());
                            if (resId != 0) {
                                android.view.View swipe = activity.findViewById(resId);
                                if (swipe instanceof SwipeRefreshLayout && ((SwipeRefreshLayout) swipe).isRefreshing()) {
                                    return; // Skip global loading dialog since local pull-to-refresh spinner is active
                                }
                            }
                        } catch (Exception ignored) {}

                        LoadingDialog.show(activity);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void hideLoading() {
        // Post to main thread to hide dialog
        mainHandler.post(LoadingDialog::hide);
    }
}
