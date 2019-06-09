package com.example.gnssanalyzerplus;

import android.app.Activity;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.gnssanalyzerplus.utils.Utils;

import java.util.Date;
import java.util.Locale;

public class StatusFragment extends Fragment implements GnssListener {

    public static final String TAG = "StatusFragment";

    View view;

    TextView mLatitude, mLongitude, mDate, mTime,
            mTTFF, mError, mSpeed, mAltitude;

    public StatusFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.status_fragment, container, false);

        // Initialize variables and views
        mLatitude = view.findViewById(R.id.tv_latitude);
        mLongitude = view.findViewById(R.id.tv_longitude);
        mDate = view.findViewById(R.id.tv_date);
        mTime = view.findViewById(R.id.tv_time);
        mTTFF = view.findViewById(R.id.tv_ttff);
        mError = view.findViewById(R.id.tv_error);
        mSpeed = view.findViewById(R.id.tv_speed);
        mAltitude = view.findViewById(R.id.tv_altitude);

        // Add fragment to GnssListener
        MainActivity.getInstance().addListener(this);

        return view;
    }

    @Override
    public void gpsStart() {

    }

    @Override
    public void gpsStop() {

    }

    @Override
    public void onGpsStatusChanged(int event, GpsStatus status) {

    }

    @Override
    public void onGnssFirstFix(int ttffMillis) {
        double time = ttffMillis/1000.0f;
        mTTFF.setText(String.format(Locale.ENGLISH, "%.3f", time));
    }

    @Override
    public void onSatelliteStatusChanged(GnssStatus status) {
        int i;
        String snr_string = "";
        for(i = 0; i < status.getSatelliteCount(); i++) {
            snr_string += " " + Float.toString(status.getCn0DbHz(i));
        }
        Log.d(TAG, snr_string);
    }

    @Override
    public void onGnssStarted() {

    }

    @Override
    public void onGnssStopped() {

    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {

    }

    @Override
    public void onOrientationChanged(double orientation, double tilt) {

    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {

    }

    @Override
    public void onLocationChanged(Location location) {

        // Extract latitude and longitude information and display in views
        mLatitude.setText(String.format(Locale.ENGLISH,"%.6f",location.getLatitude()));
        mLongitude.setText(String.format(Locale.ENGLISH,"%.6f",location.getLongitude()));
        mError.setText(String.format(Locale.ENGLISH,"%.3f", location.getAccuracy()));
        mSpeed.setText(String.format(Locale.ENGLISH,"%.3f", location.getSpeed()));
        mAltitude.setText(String.format(Locale.ENGLISH, "%.3f", location.getAltitude()));

        // Extract datetime information and display in views
        Date dateObject = new Date(location.getTime());
        String date = Utils.formatDate(dateObject);
        String time = Utils.formatTime(dateObject);
        mDate.setText(date);
        mTime.setText(time);


    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
