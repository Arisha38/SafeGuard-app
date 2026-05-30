package com.safeguard.womensafety;

public class SosAlert {
    public long timestamp;
    public String locationLink;
    public String message;
    public String status;

    public SosAlert() {}

    public SosAlert(long timestamp, String locationLink, String message, String status) {
        this.timestamp = timestamp;
        this.locationLink = locationLink;
        this.message = message;
        this.status = status;
    }
}

