package com.example.agrirent.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.agrirent.R;
import com.example.agrirent.activities.BookingsActivity;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "agrirent_notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "AgriRent";
        String body = "";

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle() != null
                    ? remoteMessage.getNotification().getTitle() : "AgriRent";
            body = remoteMessage.getNotification().getBody() != null
                    ? remoteMessage.getNotification().getBody() : "";
        }

        sendNotification(title, body);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM Token: " + token);
        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {
        // Get saved auth token from SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("agrirent_prefs", MODE_PRIVATE);
        String authToken = prefs.getString("auth_token", null);

        if (authToken == null) {
            Log.w(TAG, "Not logged in. FCM token will be sent on next login.");
            // Save token to send later when user logs in
            prefs.edit().putString("pending_fcm_token", token).apply();
            return;
        }

        // Get language from session
        android.content.SharedPreferences sessionPrefs = getSharedPreferences("AgriRentSession", MODE_PRIVATE);
        String language = sessionPrefs.getString("LANGUAGE", "en");
        String deviceId = android.provider.Settings.Secure.getString(
                getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        Call<Void> call = apiService.updateFcmToken(new com.example.agrirent.models.FcmTokenRequest(token, language, deviceId));
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                Log.d(TAG, "Token sent to server. Code: " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Token upload failed: " + t.getMessage());
            }
        });
    }

    private void sendNotification(String title, String messageBody) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "AgriRent Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Booking, equipment, and subscription notifications");
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, BookingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(android.graphics.BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        int notificationId = (int) System.currentTimeMillis();
        manager.notify(notificationId, builder.build());
    }
}
