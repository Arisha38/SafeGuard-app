package com.safeguard.womensafety;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int REQ_LOCATION = 501;
    private static final int REQ_SMS = 502;
    private FusedLocationProviderClient fusedClient;
    private GoogleMap map;
    private TextView tvCurrentLocation;
    private TextView tvLastUpdated;
    private String currentLink = "";
    private double currentLat;
    private double currentLng;
    private final ActivityResultLauncher<Intent> locationSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (isGpsEnabled()) fetchLocation();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        Button refresh = findViewById(R.id.btnRefreshLocation);
        Button viewOnMap = findViewById(R.id.btnViewOnMap);
        Button share = findViewById(R.id.btnShareLocation);

        refresh.setOnClickListener(v -> fetchLocation());
        viewOnMap.setOnClickListener(v -> openLocationInMaps());
        share.setOnClickListener(v -> shareLocationBySms());
    }

    private void fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION);
            return;
        }

        if (!isGpsEnabled()) {
            showEnableGpsDialog();
            return;
        }

        fusedClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        updateUiForLocation(location);
                    } else {
                        requestNewLocationUpdate();
                    }
                })
                .addOnFailureListener(e -> requestNewLocationUpdate());
    }

    private boolean isGpsEnabled() {
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void showEnableGpsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable GPS")
                .setMessage("GPS is turned off. Please enable GPS to fetch your location.")
                .setPositiveButton("Open Settings", (DialogInterface dialog, int which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    locationSettingsLauncher.launch(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestNewLocationUpdate() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMaxUpdates(1)
                .build();

        LocationCallback callback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateUiForLocation(location);
                } else {
                    tvCurrentLocation.setText("Location not found");
                    Toast.makeText(LocationActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                }
                fusedClient.removeLocationUpdates(this);
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper());
        }
    }

    private void updateUiForLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        currentLat = latLng.latitude;
        currentLng = latLng.longitude;
        currentLink = "https://maps.google.com/?q=" + latLng.latitude + "," + latLng.longitude;
        tvCurrentLocation.setText(resolveAddress(latLng.latitude, latLng.longitude));
        tvLastUpdated.setText("Last updated: " + DateFormat.format("hh:mm a", System.currentTimeMillis()));
        if (map != null) {
            map.clear();
            map.addMarker(new MarkerOptions().position(latLng).title("You are here"));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        }
    }

    private String resolveAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                String compact = joinParts(addr.getSubLocality(), addr.getLocality(), addr.getCountryName());
                if (!compact.isEmpty()) return compact;
                String line = addr.getAddressLine(0);
                if (!TextUtils.isEmpty(line)) return line;
            }
        } catch (IOException ignored) {
        }
        return "Location not found";
    }

    private String joinParts(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!TextUtils.isEmpty(p)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(p);
            }
        }
        return sb.toString();
    }

    private void openLocationInMaps() {
        if (currentLink.isEmpty()) {
            Toast.makeText(this, "Fetch location first", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentLink));
        mapIntent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(mapIntent);
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(currentLink)));
        }
    }

    private void shareLocationBySms() {
        if (currentLink.isEmpty()) {
            Toast.makeText(this, "Fetch location first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQ_SMS);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
        intent.putExtra("sms_body", "My live location: " + currentLink);
        startActivity(intent);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                tvCurrentLocation.setText("Location permission denied");
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                shareLocationBySms();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
