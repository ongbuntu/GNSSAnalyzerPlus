package com.example.gnssanalyzerplus.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {

    public static String formatDate(Date dateObject) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
        return simpleDateFormat.format(dateObject);
    }
    public static String formatTime(Date dateObject) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        return simpleDateFormat.format(dateObject);
    }

    /**
     * Returns the Global Navigation Satellite System (GNSS) for a satellite given the PRN.  For
     * Android 6.0.1 (API Level 23) and lower.  Android 7.0 and higher should use getGnssConstellationType()
     *
     * @param prn PRN value provided by the GpsSatellite.getPrn() method
     * @return GnssType for the given PRN
     */
    @Deprecated
    public static GnssType getGnssType(int prn) {
        if (prn >= 1 && prn <= 32) {
            return GnssType.NAVSTAR;
        } else if (prn == 33) {
            return GnssType.INMARSAT_3F2;
        } else if (prn == 39) {
            // See Issue #205
            return GnssType.INMARSAT_3F5;
        } else if (prn >= 40 && prn <= 41) {
            // See Issue #92
            return GnssType.GAGAN;
        } else if (prn == 46) {
            return GnssType.INMARSAT_4F3;
        } else if (prn == 48) {
            return GnssType.GALAXY_15;
        } else if (prn == 49) {
            return GnssType.SES_5;
        } else if (prn == 51) {
            return GnssType.ANIK;
        } else if (prn >= 65 && prn <= 96) {
            // See Issue #26 for details
            return GnssType.GLONASS;
        } else if (prn >= 193 && prn <= 200) {
            // See Issue #54 for details
            return GnssType.QZSS;
        } else if (prn >= 201 && prn <= 235) {
            // See Issue #54 for details
            return GnssType.BEIDOU;
        } else if (prn >= 301 && prn <= 330) {
            // See https://github.com/barbeau/gpstest/issues/58#issuecomment-252235124 for details
            return GnssType.GALILEO;
        } else {
            return GnssType.UNKNOWN;
        }
    }
}
