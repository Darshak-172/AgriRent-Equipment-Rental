package com.example.agrirent.utils;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper {

    /**
     * Wraps a Context with the given locale.
     * Call this from attachBaseContext() in every Activity.
     */
    public static Context wrap(Context context, String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            languageCode = "en";
        }
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = context.getResources().getConfiguration();
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

    /** Legacy helper — kept for call sites that use it directly. */
    public static void applyLanguage(Context context, String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            languageCode = "en";
        }
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        context.getResources().updateConfiguration(
                config,
                context.getResources().getDisplayMetrics()
        );
    }
}
