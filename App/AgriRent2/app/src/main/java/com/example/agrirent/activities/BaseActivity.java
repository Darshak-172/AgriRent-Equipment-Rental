package com.example.agrirent.activities;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.example.agrirent.utils.LocaleHelper;
import com.example.agrirent.utils.SessionManager;

/**
 * All Activities extend this so the user's chosen language is applied
 * every time Android creates or recreates an Activity context.
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        String lang = new SessionManager(newBase).getLanguage();
        super.attachBaseContext(LocaleHelper.wrap(newBase, lang));
    }
}
