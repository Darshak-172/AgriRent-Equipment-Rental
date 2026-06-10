package com.example.agrirent.network;

import android.content.Context;

import com.example.agrirent.utils.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final SessionManager session;

    public AuthInterceptor(Context context) {
        session = new SessionManager(context);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request originalRequest = chain.request();

        // Ensure language is always present
        String language = session.getLanguage();
        if (language == null || language.isEmpty()) {
            language = "en";
        }

        Request.Builder requestBuilder = originalRequest.newBuilder()
                .addHeader("Accept-Language", language)
                .addHeader("Content-Type", "application/json");

        // Add Authorization only if token exists
        String token = session.getToken();
        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader(
                    "Authorization",
                    "Bearer " + token
            );
        }
        System.out.println("LANGUAGE SENT = " + language);

        return chain.proceed(requestBuilder.build());
    }
}
