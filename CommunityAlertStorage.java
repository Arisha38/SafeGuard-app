package com.safeguard.womensafety;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists community alerts in SharedPreferences (newest first in the returned list).
 */
public final class CommunityAlertStorage {

    private static final String PREFS_NAME = "safeguard_community_alerts_v1";
    private static final String KEY_ITEMS = "alert_items_json";

    private CommunityAlertStorage() {
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static List<CommunityAlert> load(@NonNull Context context) {
        List<CommunityAlert> out = new ArrayList<>();
        String raw = prefs(context).getString(KEY_ITEMS, null);
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new CommunityAlert(o.getLong("t"), o.getString("m")));
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    public static void saveAll(@NonNull Context context, @NonNull List<CommunityAlert> items) {
        JSONArray arr = new JSONArray();
        try {
            for (CommunityAlert a : items) {
                JSONObject o = new JSONObject();
                o.put("t", a.sentAtMillis);
                o.put("m", a.message);
                arr.put(o);
            }
        } catch (JSONException ignored) {
            return;
        }
        prefs(context).edit().putString(KEY_ITEMS, arr.toString()).apply();
    }

    public static void prepend(@NonNull Context context, @NonNull CommunityAlert item) {
        List<CommunityAlert> list = load(context);
        list.add(0, item);
        saveAll(context, list);
    }
}
