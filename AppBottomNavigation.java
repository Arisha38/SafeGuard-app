package com.safeguard.womensafety;

import android.content.Intent;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.safeguard.womensafety.ui.profile.ProfileActivity;

/**
 * Central bottom navigation for main app tabs. Activities use {@code android:launchMode="singleTask"}
 * so switching tabs reuses existing instances instead of stacking duplicates.
 */
public final class AppBottomNavigation {

    private AppBottomNavigation() {
    }

    public static void setup(
            @NonNull AppCompatActivity activity,
            @NonNull BottomNavigationView nav,
            @IdRes int selectedItemId
    ) {
        nav.setSelectedItemId(selectedItemId);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) {
                return true;
            }
            Class<?> dest = resolveDestination(id);
            if (dest == null) {
                return false;
            }
            if (dest == activity.getClass()) {
                return true;
            }
            Intent intent = new Intent(activity, dest);
            activity.startActivity(intent);
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return true;
        });
    }

    public static void syncSelection(@NonNull BottomNavigationView nav, @IdRes int selectedItemId) {
        if (nav.getSelectedItemId() != selectedItemId) {
            nav.setSelectedItemId(selectedItemId);
        }
    }

    private static Class<?> resolveDestination(int menuItemId) {
        if (menuItemId == R.id.nav_home) {
            return MainActivity.class;
        }
        if (menuItemId == R.id.nav_services) {
            return EmergencyServicesActivity.class;
        }
        if (menuItemId == R.id.nav_emergency) {
            return SosActivity.class;
        }
        if (menuItemId == R.id.nav_profile) {
            return ProfileActivity.class;
        }
        return null;
    }
}
