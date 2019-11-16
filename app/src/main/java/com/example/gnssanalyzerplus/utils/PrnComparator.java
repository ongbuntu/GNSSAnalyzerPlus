package com.example.gnssanalyzerplus.utils;

import java.util.Comparator;

public class PrnComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {

        GnssSatelliteStatus g1 = (GnssSatelliteStatus) o1;
        GnssSatelliteStatus g2 = (GnssSatelliteStatus) o2;

        if(g1.getPrns() == g2.getPrns()) {
            return 0;
        } else if(g1.getPrns() > g2.getPrns()) {
            return 1;
        } else {
            return -1;
        }
    }
}
