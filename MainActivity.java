package com.safeguard.womensafety;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_ALL_PERMISSIONS = 100;
    private final Handler badgeHandler = new Handler(Looper.getMainLooper());
    private Runnable badgeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestAppPermissions();
        setSafetyStatus();
        setupButtons();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavMain);
        AppBottomNavigation.setup(this, bottomNav, R.id.nav_home);
        startScreenEntryAnimation();
        startAiPulseAnimation();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setSafetyStatus();
        BottomNavigationView nav = findViewById(R.id.bottomNavMain);
        if (nav != null) {
            AppBottomNavigation.syncSelection(nav, R.id.nav_home);
        }
    }

    private void requestAppPermissions() {
        String[] permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE
        };
        ActivityCompat.requestPermissions(this, permissions, REQ_ALL_PERMISSIONS);
    }

    private void setSafetyStatus() {
        TextView tvStatus = findViewById(R.id.tvStatus);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String status = hour >= 19 || hour <= 5 ? "Be Careful (Night Time)" : "Safe (Day Time)";
        tvStatus.setText("Safety Status: " + status);
    }

    private void setupButtons() {
        setClick(R.id.cardSos, SosActivity.class);
        setClick(R.id.cardLocation, LocationActivity.class);
        setClick(R.id.cardFakeCall, FakeCallActivity.class);
        setClick(R.id.cardContacts, ContactsActivity.class);
        setClick(R.id.cardRecord, RecordingActivity.class);
        setClick(R.id.cardRide, RideTrackingActivity.class);
        setClick(R.id.cardCommunity, CommunityAlertActivity.class);
        setClick(R.id.cardSettings, SettingsActivity.class);
        setClick(R.id.cardAiAssistant, AiSafetyAssistantActivity.class);
        setClick(R.id.cardEmergencyServices, EmergencyServicesActivity.class);
    }

    private void setClick(int id, Class<?> activityClass) {
        View v = findViewById(id);
        v.setOnClickListener(view -> {
            view.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .alpha(0.9f)
                    .setDuration(90)
                    .withEndAction(() -> {
                        view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(90).start();
                        startActivity(new Intent(this, activityClass));
                        if (activityClass == AiSafetyAssistantActivity.class
                                || activityClass == EmergencyServicesActivity.class) {
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        }
                    })
                    .start();
        });
    }

    private void startScreenEntryAnimation() {
        View root = findViewById(R.id.rootDashboard);
        root.setAlpha(0f);
        root.setTranslationY(40f);
        root.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void startAiPulseAnimation() {
        View aiCard = findViewById(R.id.cardAiAssistant);
        View aiGlow = findViewById(R.id.aiGlow);
        TextView tvAiBadge = findViewById(R.id.tvAiBadge);

        ObjectAnimator pulseX = ObjectAnimator.ofFloat(aiCard, View.SCALE_X, 1f, 1.02f, 1f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(aiCard, View.SCALE_Y, 1f, 1.02f, 1f);
        ObjectAnimator glow = ObjectAnimator.ofFloat(aiGlow, View.ALPHA, 0.35f, 0.8f, 0.35f);
        pulseX.setDuration(2200);
        pulseY.setDuration(2200);
        glow.setDuration(2800);
        pulseX.setRepeatCount(ObjectAnimator.INFINITE);
        pulseY.setRepeatCount(ObjectAnimator.INFINITE);
        glow.setRepeatCount(ObjectAnimator.INFINITE);
        pulseX.start();
        pulseY.start();
        glow.start();

        badgeRunnable = new Runnable() {
            private boolean showNew = true;

            @Override
            public void run() {
                tvAiBadge.setText(showNew ? "NEW" : "AI");
                showNew = !showNew;
                badgeHandler.postDelayed(this, 1800);
            }
        };
        badgeHandler.postDelayed(badgeRunnable, 1800);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (badgeRunnable != null) badgeHandler.removeCallbacks(badgeRunnable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
