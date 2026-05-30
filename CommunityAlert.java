package com.safeguard.womensafety;

import androidx.annotation.NonNull;

/** One locally stored community alert. */
public final class CommunityAlert {

    public final long sentAtMillis;
    @NonNull
    public final String message;

    public CommunityAlert(long sentAtMillis, @NonNull String message) {
        this.sentAtMillis = sentAtMillis;
        this.message = message;
    }
}
