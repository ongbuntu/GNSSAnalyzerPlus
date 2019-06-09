package com.example.gnssanalyzerplus.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {

    public static String formatDate(Date dateObject) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-YYYY", Locale.US);
        return simpleDateFormat.format(dateObject);
    }
    public static String formatTime(Date dateObject) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
        return simpleDateFormat.format(dateObject);
    }
}
