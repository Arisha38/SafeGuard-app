package com.safeguard.womensafety;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class VideoRepository {
    private static final ArrayList<Uri> savedVideos = new ArrayList<>();

    public static void add(Uri uri) {
        if (uri != null) savedVideos.add(0, uri);
    }

    public static List<Uri> getAll() {
        return new ArrayList<>(savedVideos);
    }
}
