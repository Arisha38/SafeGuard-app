package com.safeguard.womensafety;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.safeguard.womensafety.databinding.ActivityRideTrackingBinding;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class RideTrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityRideTrackingBinding binding;
    private GoogleMap map;
    private FusedLocationProviderClient fusedClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRideTrackingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        binding.toolbarRide.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.rideMapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupServiceTypeDropdown();

        binding.btnRideCancel.setOnClickListener(v -> finish());
        binding.btnRideShare.setOnClickListener(v -> shareRideDetails());
    }

    private void setupServiceTypeDropdown() {
        String[] types = getResources().getStringArray(R.array.ride_service_types);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.item_profile_dropdown,
                android.R.id.text1,
                types);
        MaterialAutoCompleteTextView field = binding.autoServiceType;
        field.setAdapter(adapter);
        field.setKeyListener(null);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        if (PermissionUtils.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            try {
                map.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {
            }
            fusedClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null && map != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(loc.getLatitude(), loc.getLongitude()),
                            14f));
                }
            });
        }
    }

    private void shareRideDetails() {
        if (!validate()) {
            return;
        }
        String service = String.valueOf(binding.autoServiceType.getText()).trim();
        String driver = String.valueOf(binding.editDriverName.getText()).trim();
        String vehicle = String.valueOf(binding.editVehicleNumber.getText()).trim();
        String destination = String.valueOf(binding.editDestination.getText()).trim();
        String when = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.MEDIUM,
                Locale.getDefault()).format(new Date());
        String message = getString(
                R.string.smart_ride_share_message,
                service,
                driver,
                vehicle,
                destination,
                when);

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(send, getString(R.string.smart_ride_share_chooser)));
    }

    private boolean validate() {
        clearErrors();
        boolean ok = true;

        if (String.valueOf(binding.autoServiceType.getText()).trim().isEmpty()) {
            binding.tilServiceType.setError(getString(R.string.smart_ride_error_required));
            ok = false;
        }
        if (String.valueOf(binding.editDriverName.getText()).trim().isEmpty()) {
            binding.tilDriverName.setError(getString(R.string.smart_ride_error_required));
            ok = false;
        }
        if (String.valueOf(binding.editVehicleNumber.getText()).trim().isEmpty()) {
            binding.tilVehicleNumber.setError(getString(R.string.smart_ride_error_required));
            ok = false;
        }
        if (String.valueOf(binding.editDestination.getText()).trim().isEmpty()) {
            binding.tilDestination.setError(getString(R.string.smart_ride_error_required));
            ok = false;
        }

        return ok;
    }

    private void clearErrors() {
        binding.tilServiceType.setError(null);
        binding.tilDriverName.setError(null);
        binding.tilVehicleNumber.setError(null);
        binding.tilDestination.setError(null);
    }
}
