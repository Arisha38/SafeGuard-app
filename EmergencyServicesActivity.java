package com.safeguard.womensafety;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EmergencyServicesActivity extends AppCompatActivity {

    private static final String NUM_POLICE = "15";
    private static final String NUM_FIRE = "16";
    private static final String NUM_MEDICAL = "1122";
    private static final String NUM_MOTORWAY = "130";
    private static final String NUM_PATROL = "124";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_services);

        MaterialToolbar toolbar = findViewById(R.id.toolbarEmergency);
        toolbar.setNavigationOnClickListener(v -> finish());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavMain);
        AppBottomNavigation.setup(this, bottomNav, R.id.nav_services);

        MaterialCardView cardCall15 = findViewById(R.id.cardCall15);
        ImageButton btnCall15Dial = findViewById(R.id.btnCall15Dial);
        MaterialCardView cardLiveChat = findViewById(R.id.cardLiveChat);
        MaterialCardView cardVideoCall = findViewById(R.id.cardVideoCall);

        MaterialButton btnFire = findViewById(R.id.btnAgencyFireCall);
        MaterialButton btnMedical = findViewById(R.id.btnAgencyMedicalCall);
        MaterialButton btnMotorway = findViewById(R.id.btnAgencyMotorwayCall);
        MaterialButton btnPatrol = findViewById(R.id.btnAgencyPatrolCall);

        cardCall15.setOnClickListener(v -> animateCard(v, () -> confirmDial(NUM_POLICE)));
        btnCall15Dial.setOnClickListener(v -> animateCard(v, () -> confirmDial(NUM_POLICE)));

        cardLiveChat.setOnClickListener(v -> animateCard(v, this::showLiveChatPlaceholder));
        cardVideoCall.setOnClickListener(v -> animateCard(v, this::showVideoCallPlaceholder));

        btnFire.setOnClickListener(v -> animateCard(v, () -> confirmDial(NUM_FIRE)));
        btnMedical.setOnClickListener(v -> animateCard(v, () -> confirmDial(NUM_MEDICAL)));
        btnMotorway.setOnClickListener(v -> animateCard(v, () -> confirmDial(NUM_MOTORWAY)));
        btnPatrol.setOnClickListener(v -> animateCard(v, () -> confirmDial(NUM_PATROL)));

        runEntryAnimations();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppBottomNavigation.syncSelection(findViewById(R.id.bottomNavMain), R.id.nav_services);
    }

    private void confirmDial(@NonNull String number) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.emergency_call_confirm_title)
                .setMessage(R.string.emergency_call_this_service)
                .setPositiveButton(R.string.emergency_call_confirm_positive, (d, w) -> openDialer(number))
                .setNegativeButton(R.string.emergency_call_confirm_negative, (d, w) -> d.dismiss())
                .show();
    }

    private void openDialer(@NonNull String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + Uri.encode(number)));
        startActivity(intent);
    }

    private void showLiveChatPlaceholder() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.live_chat_coming_soon_title)
                .setMessage(R.string.live_chat_coming_soon_message)
                .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                .show();
    }

    private void showVideoCallPlaceholder() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.video_call_placeholder_title)
                .setMessage(R.string.video_call_placeholder_message)
                .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                .show();
    }

    private void animateCard(@NonNull View view, @NonNull Runnable action) {
        view.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(70)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(110)
                        .withEndAction(action)
                        .start())
                .start();
    }

    private void runEntryAnimations() {
        View root = findViewById(R.id.rootEmergencyServices);
        root.setAlpha(0f);
        root.animate()
                .alpha(1f)
                .setDuration(360)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        View rowTop = findViewById(R.id.rowTopEmergencyCards);
        rowTop.setAlpha(0f);
        rowTop.setTranslationY(28f);
        rowTop.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(60)
                .setDuration(440)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
