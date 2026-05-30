package com.safeguard.womensafety;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS = "safeguard_ui_prefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupCards();
        setupThemeSwitch();
        runEntryAnimations();
    }

    private void applySavedTheme() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean darkEnabled = sp.getBoolean(KEY_DARK_MODE, true);
        AppCompatDelegate.setDefaultNightMode(
                darkEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void setupCards() {
        findViewById(R.id.cardMapsApi).setOnClickListener(v ->
                Toast.makeText(this, "Set API key in AndroidManifest meta-data.", Toast.LENGTH_LONG).show());

        findViewById(R.id.cardContacts).setOnClickListener(v -> openWithTransition(ContactsActivity.class));

        findViewById(R.id.cardPermissions).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(i);
        });

        findViewById(R.id.cardAlerts).setOnClickListener(v ->
                Toast.makeText(this, "Alert and sound controls can be configured here.", Toast.LENGTH_SHORT).show());

        findViewById(R.id.cardAbout).setOnClickListener(v ->
                Toast.makeText(this, "SafeGuard v1.0 - Women Safety App", Toast.LENGTH_SHORT).show());
    }

    private void setupThemeSwitch() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        MaterialSwitch sw = findViewById(R.id.switchDarkMode);
        boolean darkEnabled = sp.getBoolean(KEY_DARK_MODE, true);
        sw.setChecked(darkEnabled);

        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sp.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            int mode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            if (AppCompatDelegate.getDefaultNightMode() != mode) {
                AppCompatDelegate.setDefaultNightMode(mode);
                getWindow().getDecorView().post(this::recreate);
            }
        });
    }

    private void openWithTransition(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void runEntryAnimations() {
        TextView title = findViewById(R.id.tvSettingsTitle);
        title.setAlpha(0f);
        title.animate().alpha(1f).setDuration(350).start();

        int[] cardIds = new int[]{
                R.id.cardMapsApi, R.id.cardContacts, R.id.cardPermissions, R.id.cardAlerts, R.id.cardTheme, R.id.cardAbout
        };

        long delay = 50;
        for (int id : cardIds) {
            View card = findViewById(id);
            card.setAlpha(0f);
            card.setTranslationY(20f);
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(delay)
                    .setDuration(260)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            delay += 45;
        }
    }
}
