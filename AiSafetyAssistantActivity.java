package com.safeguard.womensafety;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class AiSafetyAssistantActivity extends AppCompatActivity {
    private static final String TEL_AMBULANCE = "1122";
    private static final String TEL_POLICE = "15";
    private static final String TEL_HIGHWAY = "124";
    private static final String TEL_RESCUE = "1122";

    private final ArrayList<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private RecyclerView recyclerView;
    private FusedLocationProviderClient fusedLocationClient;

    private View layoutEmergencySection;
    private MaterialCardView cardAmbulance;
    private MaterialCardView cardPolice;
    private MaterialCardView cardHighway;
    private MaterialCardView cardRescue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_safety_assistant);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        recyclerView = findViewById(R.id.recyclerChat);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(messages);
        recyclerView.setAdapter(adapter);

        layoutEmergencySection = findViewById(R.id.layoutEmergencySection);
        cardAmbulance = findViewById(R.id.cardEmergencyAmbulance);
        cardPolice = findViewById(R.id.cardEmergencyPolice);
        cardHighway = findViewById(R.id.cardEmergencyHighway);
        cardRescue = findViewById(R.id.cardEmergencyRescue);

        EditText input = findViewById(R.id.etChatInput);
        MaterialButton send = findViewById(R.id.btnSendChat);
        MaterialButton callPolice = findViewById(R.id.btnCallPolice);
        MaterialButton sendSos = findViewById(R.id.btnSendSosQuick);
        MaterialButton shareLocation = findViewById(R.id.btnShareLocationQuick);

        addAiMessage("Hi, I'm your Safety Assistant. Tell me what is happening and I will guide you.");

        fadeInEmergencySection();
        attachEmergencyCardClicks();

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessageFromInput(input);
                return true;
            }
            return false;
        });

        send.setOnClickListener(v -> animatePress(v, () -> sendMessageFromInput(input)));

        callPolice.setOnClickListener(v -> animatePress(v, () -> confirmDial(TEL_POLICE)));

        sendSos.setOnClickListener(v -> animatePress(v, () ->
                startActivity(new Intent(this, SosActivity.class))));

        shareLocation.setOnClickListener(v -> animatePress(v, this::shareCurrentLocation));
    }

    private void sendMessageFromInput(EditText input) {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        addUserMessage(text);
        input.setText("");
        applyEmergencySmartUi(text);
        addAiMessage(generateSafetyReply(text));
    }

    private void fadeInEmergencySection() {
        layoutEmergencySection.setAlpha(0f);
        layoutEmergencySection.setTranslationY(dp(12));
        layoutEmergencySection.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void attachEmergencyCardClicks() {
        cardAmbulance.setOnClickListener(v -> animatePress(v, () -> confirmDial(TEL_AMBULANCE)));
        cardPolice.setOnClickListener(v -> animatePress(v, () -> confirmDial(TEL_POLICE)));
        cardHighway.setOnClickListener(v -> animatePress(v, () -> confirmDial(TEL_HIGHWAY)));
        cardRescue.setOnClickListener(v -> animatePress(v, () -> confirmDial(TEL_RESCUE)));
    }

    private void confirmDial(@NonNull String number) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.emergency_call_confirm_title)
                .setMessage(R.string.emergency_call_confirm_message)
                .setPositiveButton(R.string.emergency_call_confirm_positive, (d, w) -> openDialer(number))
                .setNegativeButton(R.string.emergency_call_confirm_negative, (d, w) -> d.dismiss())
                .show();
    }

    private void openDialer(@NonNull String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + Uri.encode(number)));
        startActivity(intent);
    }

    private void animatePress(View view, Runnable action) {
        view.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(70)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .withEndAction(action)
                        .start())
                .start();
    }

    private void clearEmergencyHighlights() {
        int idle = ContextCompat.getColor(this, R.color.emergency_stroke_idle);
        for (MaterialCardView card : new MaterialCardView[]{cardAmbulance, cardPolice, cardHighway, cardRescue}) {
            card.setStrokeWidth(0);
            card.setStrokeColor(idle);
            card.setCardElevation(dp(10));
        }
    }

    private void highlightCard(@NonNull MaterialCardView card) {
        int glow = ContextCompat.getColor(this, R.color.emergency_highlight_stroke);
        card.setStrokeColor(glow);
        card.setStrokeWidth((int) dp(4));
        card.setCardElevation(dp(18));

        PropertyValuesHolder sx = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.05f, 1f);
        PropertyValuesHolder sy = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.05f, 1f);
        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(card, sx, sy);
        pulse.setDuration(520);
        pulse.start();
    }

    private void applyEmergencySmartUi(@NonNull String raw) {
        String q = raw.toLowerCase(Locale.getDefault());
        boolean help = q.contains("help");
        boolean ambulance = q.contains("ambulance")
                || q.contains("medical")
                || q.contains("hospital")
                || q.contains("injury")
                || q.contains("ems")
                || q.contains("call ambulance");
        boolean highway = q.contains("highway")
                || q.contains("motorway")
                || q.contains("motor way");
        boolean rescue = q.contains("rescue")
                || q.contains("fire")
                || q.contains("trapped")
                || q.contains("accident");
        boolean police = q.contains("police")
                || q.contains("cop")
                || q.contains("robber")
                || q.contains("thief")
                || q.contains("law enforcement");

        clearEmergencyHighlights();

        MaterialCardView primary = null;
        if (ambulance) {
            primary = cardAmbulance;
        } else if (highway) {
            primary = cardHighway;
        } else if (rescue) {
            primary = cardRescue;
        } else if (police) {
            primary = cardPolice;
        }

        if (primary != null) {
            highlightCard(primary);
        }
        if (help) {
            pulseEmergencyQuickActions();
        }
    }

    private void pulseEmergencyQuickActions() {
        layoutEmergencySection.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(0)
                .withEndAction(() -> layoutEmergencySection.animate()
                        .scaleX(1.02f)
                        .scaleY(1.02f)
                        .setDuration(160)
                        .withEndAction(() -> layoutEmergencySection.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start())
                        .start())
                .start();

        MaterialCardView[] cards = {cardAmbulance, cardPolice, cardHighway, cardRescue};
        long delay = 0L;
        for (MaterialCardView card : cards) {
            card.postDelayed(() -> {
                PropertyValuesHolder sx = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.04f, 1f);
                PropertyValuesHolder sy = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.04f, 1f);
                ObjectAnimator.ofPropertyValuesHolder(card, sx, sy).setDuration(360).start();
            }, delay);
            delay += 90L;
        }
    }

    private void shareCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            addAiMessage("Please enable location permission to share your current location.");
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                addAiMessage("I couldn't fetch your location. Please try again.");
                return;
            }
            String link = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "My current location: " + link);
            startActivity(Intent.createChooser(intent, "Share location"));
        });
    }

    private String generateSafetyReply(String userInput) {
        String q = userInput.toLowerCase(Locale.getDefault());
        if (q.contains("unsafe") || q.contains("scared") || q.contains("afraid")) {
            return "Stay in a public, well-lit area. Call someone you trust now, keep your phone in hand, and be ready to trigger SOS.";
        }
        if (q.contains("following") || q.contains("follow")) {
            return "Do not go home directly. Move to a crowded place or nearby shop, call police or a trusted contact, and share your live location.";
        }
        if (q.contains("ambulance") || q.contains("call ambulance") || q.contains("medical") || q.contains("hospital")) {
            return "If it is a medical emergency, tap Ambulance (1122) below to open the dialer, or ask someone nearby to help while you stay on the line with rescue.";
        }
        if (q.contains("police") || q.contains("cop")) {
            return "If you need law enforcement, use the Police quick card (15) below or the Call Police shortcut. Move to a safe, visible spot if you can.";
        }
        if (q.contains("highway") || q.contains("motorway")) {
            return "On highways, contact Highway Police (124) using the quick card. Pull over safely if you are driving and keep hazard lights on.";
        }
        if (q.contains("help")) {
            return "You are not alone. Below are official emergency quick-call options—Ambulance/Rescue (1122), Police (15), and Highway Police (124). Tap one if you need immediate help, or use SOS.";
        }
        if (q.contains("night") || q.contains("alone")) {
            return "Prefer main roads, avoid isolated shortcuts, and keep emergency contacts ready. Consider using fake call if needed.";
        }
        return "I recommend staying alert, moving to a safer public place, and informing a trusted person. If risk increases, use Emergency Quick Help or SOS immediately.";
    }

    private void addUserMessage(String text) {
        messages.add(new ChatMessage(text, true));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);
    }

    private void addAiMessage(String text) {
        messages.add(new ChatMessage(text, false));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
