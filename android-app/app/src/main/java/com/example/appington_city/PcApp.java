package com.example.appington_city;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/** Applies the saved dark-mode choice as soon as the app starts. */
public class PcApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Prefs.isDarkMode(this)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
