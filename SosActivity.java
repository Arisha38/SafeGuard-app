package com.safeguard.womensafety;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Looper;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class SosActivity extends AppCompatActivity {
    private static final int REQ_SMS = 900;
    private static final int REQ_VIDEO_PERMS = 901;
    private FusedLocationProviderClient fusedClient;
    private ContactsDbHelper dbHelper;
    private FirebaseHelper firebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        dbHelper = new ContactsDbHelper(this);
        firebase = new FirebaseHelper(this);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavMain);
        AppBottomNavigation.setup(this, bottomNav, R.id.nav_emergency);

        MaterialButton sos = findViewById(R.id.btnTriggerSos);
        MaterialButton startBgRecording = findViewById(R.id.btnStartBgRecording);
        MaterialButton stopBgRecording = findViewById(R.id.btnStopBgRecording);
        MaterialButton viewVideos = findViewById(R.id.btnViewVideos);
        sos.setOnClickListener(v -> triggerSos());
        startBgRecording.setOnClickListener(v -> startBackgroundRecordingWithPermission());
        stopBgRecording.setOnClickListener(v -> stopBackgroundRecording());
        viewVideos.setOnClickListener(v -> startActivity(new Intent(this, VideoGalleryActivity.class)));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppBottomNavigation.syncSelection(findViewById(R.id.bottomNavMain), R.id.nav_emergency);
    }

    private void triggerSos() {
        startBackgroundRecordingWithPermission();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQ_SMS);
            return;
        }
        fetchRealtimeLocationAndSendSos();
    }

    private void startBackgroundRecordingWithPermission() {
        String[] videoPerms = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        if (!cameraGranted || !audioGranted) {
            ActivityCompat.requestPermissions(this, videoPerms, REQ_VIDEO_PERMS);
            return;
        }

        Intent serviceIntent = new Intent(this, VideoRecordingService.class);
        serviceIntent.setAction(VideoRecordingService.ACTION_START);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopBackgroundRecording() {
        Intent serviceIntent = new Intent(this, VideoRecordingService.class);
        serviceIntent.setAction(VideoRecordingService.ACTION_STOP);
        startService(serviceIntent);
    }

    private void fetchRealtimeLocationAndSendSos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            sendAlert("Location unavailable");
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
                .setMinUpdateIntervalMillis(1000L)
                .setMaxUpdates(1)
                .build();

        LocationCallback callback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    sendAlert("https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude());
                } else {
                    sendAlert("Location unavailable");
                }
                fusedClient.removeLocationUpdates(this);
            }
        };

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper());
    }

    private void sendAlert(String locationText) {
        List<Contact> contacts = dbHelper.getAllContacts();
        if (contacts.isEmpty()) {
            Toast.makeText(this, "No emergency contacts saved", Toast.LENGTH_SHORT).show();
            return;
        }
        String message = getString(R.string.sos_alert, locationText);
        SmsManager smsManager = SmsManager.getDefault();
        for (Contact c : contacts) {
            smsManager.sendTextMessage(c.phone, null, message, null, null);
        }
        firebase.logSosAlert(
                new SosAlert(System.currentTimeMillis(), locationText, message, "SENT"),
                new FirebaseHelper.CompletionCallback() {
                    @Override public void onSuccess() {}
                    @Override public void onError(@NonNull String msg) {}
                }
        );
        // Loud local alarm tone for quick nearby attention.
        android.media.ToneGenerator tg = new android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100);
        tg.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 3000);
        Toast.makeText(this, "SOS SMS sent to trusted contacts", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fetchRealtimeLocationAndSendSos();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_VIDEO_PERMS) {
            if (grantResults.length >= 2
                    && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startBackgroundRecordingWithPermission();
            } else {
                Toast.makeText(this, "Camera/Mic permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
