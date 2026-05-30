package com.safeguard.womensafety;

import android.content.Context;
import android.content.SharedPreferences;

public class AppLockManager {
    private static final String PREFS = "safeguard_lock_prefs";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_LOCKED = "locked_state";

    public static boolean hasPin(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return !sp.getString(KEY_PIN_HASH, "").isEmpty();
    }

    public static void savePin(Context context, String pin) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_PIN_HASH, PinSecurityUtils.hashPin(pin)).apply();
    }

    public static boolean verifyPin(Context context, String pin) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String savedHash = sp.getString(KEY_PIN_HASH, "");
        return savedHash.equals(PinSecurityUtils.hashPin(pin));
    }

    public static void clearPin(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_PIN_HASH).apply();
    }

    public static void setLocked(Context context, boolean locked) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_LOCKED, locked).apply();
    }

    public static boolean isLocked(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_LOCKED, true);
    }
}
