package com.example.smartpantry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** A single item in the user's pantry (one row of the "pantry" table). */
public class Ingredient {
    public long id;
    public String name;
    public double quantity;
    public String unit;
    public String expiry; // "yyyy-MM-dd" or empty

    public Ingredient(long id, String name, double quantity, String unit, String expiry) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.expiry = expiry;
    }

    /** Days until expiry (negative = already expired), or null if no/invalid expiry date. */
    public Integer daysUntilExpiry() {
        if (expiry == null || expiry.isEmpty()) return null;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setLenient(false);
            Date d = f.parse(expiry);
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            return (int) Math.round((d.getTime() - today.getTimeInMillis()) / 86400000.0);
        } catch (ParseException e) {
            return null;
        }
    }

    /** 2.0 -> "2", 2.5 -> "2.5" */
    public static String formatQty(double q) {
        return (q == (long) q) ? String.valueOf((long) q) : String.valueOf(q);
    }
}
