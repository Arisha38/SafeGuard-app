package com.safeguard.womensafety;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight Firebase Realtime Database helper.
 * Keeps existing offline-first behavior intact by making all cloud operations best-effort.
 */
public class FirebaseHelper {
    private static final String PREFS = "safeguard_firebase_prefs";
    private static final String KEY_DEVICE_ID = "device_id";

    public interface CompletionCallback {
        void onSuccess();
        void onError(@NonNull String message);
    }

    public interface ContactsCallback {
        void onSuccess(@NonNull List<Contact> contacts);
        void onError(@NonNull String message);
    }

    public interface SosAlertsCallback {
        void onSuccess(@NonNull List<SosAlert> alerts);
        void onError(@NonNull String message);
    }

    public interface ProfileCallback {
        void onSuccess(@NonNull UserProfile profile);
        void onError(@NonNull String message);
    }

    private final DatabaseReference root;

    public FirebaseHelper(@NonNull Context context) {
        String deviceId = getOrCreateDeviceId(context.getApplicationContext());
        root = FirebaseDatabase.getInstance()
                .getReference("safeguard")
                .child(deviceId);
    }

    private String getOrCreateDeviceId(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = sp.getString(KEY_DEVICE_ID, "");
        if (id != null && !id.isEmpty()) return id;

        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.trim().isEmpty()) androidId = UUID.randomUUID().toString();
        sp.edit().putString(KEY_DEVICE_ID, androidId).apply();
        return androidId;
    }

    private static String safeKeyFromPhone(String phone) {
        if (phone == null) return "unknown";
        return phone.replaceAll("[^0-9+]", "_");
    }

    // ---------------- Contacts ----------------

    public void upsertEmergencyContact(@NonNull Contact contact, @NonNull CompletionCallback cb) {
        String key = safeKeyFromPhone(contact.phone);
        Map<String, Object> data = new HashMap<>();
        data.put("id", contact.id);
        data.put("name", contact.name);
        data.put("phone", contact.phone);
        data.put("updatedAt", System.currentTimeMillis());

        root.child("contacts").child(key).setValue(data)
                .addOnSuccessListener(unused -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Failed to save contact" : e.getMessage()));
    }

    public void deleteEmergencyContact(@NonNull Contact contact, @NonNull CompletionCallback cb) {
        String key = safeKeyFromPhone(contact.phone);
        root.child("contacts").child(key).removeValue()
                .addOnSuccessListener(unused -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Failed to delete contact" : e.getMessage()));
    }

    public void fetchEmergencyContacts(@NonNull ContactsCallback cb) {
        root.child("contacts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Contact> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = String.valueOf(child.child("name").getValue());
                    String phone = String.valueOf(child.child("phone").getValue());
                    int id = 0;
                    try {
                        Object idObj = child.child("id").getValue();
                        if (idObj != null) id = Integer.parseInt(String.valueOf(idObj));
                    } catch (Exception ignored) {}
                    if (phone != null && !phone.equals("null")) {
                        list.add(new Contact(id, name == null || name.equals("null") ? "" : name, phone));
                    }
                }
                cb.onSuccess(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                cb.onError(error.getMessage());
            }
        });
    }

    // ---------------- SOS alerts ----------------

    public void logSosAlert(@NonNull SosAlert alert, @NonNull CompletionCallback cb) {
        String key = root.child("sosAlerts").push().getKey();
        if (key == null) {
            cb.onError("Failed to create SOS record");
            return;
        }
        root.child("sosAlerts").child(key).setValue(alert)
                .addOnSuccessListener(unused -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Failed to store SOS alert" : e.getMessage()));
    }

    public void fetchSosAlerts(@NonNull SosAlertsCallback cb) {
        root.child("sosAlerts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SosAlert> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    SosAlert alert = child.getValue(SosAlert.class);
                    if (alert != null) list.add(alert);
                }
                cb.onSuccess(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                cb.onError(error.getMessage());
            }
        });
    }

    // ---------------- Profile ----------------

    public void saveUserProfile(@NonNull UserProfile profile, @NonNull CompletionCallback cb) {
        root.child("profile").setValue(profile)
                .addOnSuccessListener(unused -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Failed to save profile" : e.getMessage()));
    }

    public void fetchUserProfile(@NonNull ProfileCallback cb) {
        root.child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserProfile profile = snapshot.getValue(UserProfile.class);
                if (profile == null) profile = new UserProfile();
                cb.onSuccess(profile);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                cb.onError(error.getMessage());
            }
        });
    }
}

