package com.example.gnssanalyzerplus.utils;

public class GnssSatelliteStatus {

    private int mPrns;
    private int mConstellationType;
    private float mSnrCn0s;
    private float mSvAzimuths;
    private float mSvElevations;

    public GnssSatelliteStatus(int Prns, int ConstellationType, float SnrCn0s, float SvElevations, float SvAzimuths) {
        this.mPrns = Prns;
        this.mConstellationType = ConstellationType;
        this.mSnrCn0s = SnrCn0s;
        this.mSvAzimuths = SvAzimuths;
        this.mSvElevations = SvElevations;

    }

    public int getPrns() {
        return this.mPrns;
    }

    public int getConstellationType() {
        return this.mConstellationType;
    }

    public float getSnrCn0s() {
        return this.mSnrCn0s;
    }

    public float getSvElevations() {
        return this.mSvElevations;
    }

    public float getSvAzimuths() {
        return this.mSvAzimuths;
    }
}
