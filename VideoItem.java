package com.safeguard.womensafety;

import android.net.Uri;

public class VideoItem {
    public final long id;
    public final Uri uri;
    public final String name;
    public final long dateMs;

    public VideoItem(long id, Uri uri, String name, long dateMs) {
        this.id = id;
        this.uri = uri;
        this.name = name;
        this.dateMs = dateMs;
    }
}
