package com.safeguard.womensafety;

import java.io.File;

public class RecordingItem {
    public final File file;
    public final String displayName;
    public final String dateTime;

    public RecordingItem(File file, String displayName, String dateTime) {
        this.file = file;
        this.displayName = displayName;
        this.dateTime = dateTime;
    }
}
