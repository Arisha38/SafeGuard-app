package com.safeguard.womensafety;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

public class SafeGuardApp extends Application {
    private int startedActivities = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                startedActivities++;
                if (!(activity instanceof PinLockActivity)
                        && AppLockManager.hasPin(activity)
                        && AppLockManager.isLocked(activity)) {
                    Intent i = new Intent(activity, PinLockActivity.class);
                    i.putExtra("from_background_lock", true);
                    activity.startActivity(i);
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivities--;
                if (startedActivities <= 0) {
                    startedActivities = 0;
                    AppLockManager.setLocked(activity, true);
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }
}
