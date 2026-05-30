package com.safeguard.womensafety;

public class UserProfile {
    public String name = "";
    public String phone = "";
    public String city = "";
    /** Detailed medical / emergency notes for responders */
    public String medicalInfo = "";
    /** Home and current address */
    public String addressInfo = "";
    /** Occupation and workplace */
    public String professionInfo = "";
    public long updatedAt = 0L;

    public UserProfile() {}

    public UserProfile(String name, String phone, String city) {
        this.name = name;
        this.phone = phone;
        this.city = city;
        this.updatedAt = System.currentTimeMillis();
    }
}

