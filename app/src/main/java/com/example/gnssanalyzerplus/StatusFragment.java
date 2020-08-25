package com.example.gnssanalyzerplus;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.gnssanalyzerplus.utils.ConstellationTypeComparator;
import com.example.gnssanalyzerplus.utils.GnssSatelliteStatus;
import com.example.gnssanalyzerplus.utils.GnssType;
import com.example.gnssanalyzerplus.utils.PrnComparator;
import com.example.gnssanalyzerplus.utils.Utils;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class StatusFragment extends Fragment implements GnssListener {

    public static final String TAG = "StatusFragment";

    private static final float NO_DATA = 0.0f;

    private static final int MAX_COUNT = 10;

    View view;

    TextView mLatitude, mLongitude, mDate, mTime,
            mTTFF, mError, mSpeed, mAltitude,
            mBearing, mSatCount, mAverageGpsCno,
            mAverageGlonassCno, mAverageBeidouCno, mAverageGalileoCno;

    private Resources mRes;

    private GnssStatusAdapter mAdapter;

    private int mSvCount, mUsedInFixCount;

    private ArrayList<GnssSatelliteStatus> mGnssSatelliteStatusList;

    private Drawable mFlagUsa, mFlagRussia, mFlagJapan, mFlagChina, mFlagEU, mFlagIndia,
            mFlagCanada, mFlagUnitedKingdom;

    private float mCummulativeGpsSnr, mCummulativeGlonassSnr,
                mCummulativeBeidouSnr, mCummulativeGalileoSnr;

    private int mCummulativeGpsCount, mCummulativeGlonassCount,
                mCummulativeBeidouCount, mCummulativeGalileoCount;

    private int mCount;

    private Location mLastLocation;

    private ParseQuery<ParseObject> mQuery;

    private String mUniqueId;

    public StatusFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.status_fragment, container, false);

        mRes = getResources();

        // Initialize variables and views
        mLatitude = view.findViewById(R.id.tv_latitude);
        mLongitude = view.findViewById(R.id.tv_longitude);
        mDate = view.findViewById(R.id.tv_date);
        mTime = view.findViewById(R.id.tv_time);
        mTTFF = view.findViewById(R.id.tv_ttff);
        mError = view.findViewById(R.id.tv_error);
        mSpeed = view.findViewById(R.id.tv_speed);
        mAltitude = view.findViewById(R.id.tv_altitude);
        mBearing = view.findViewById(R.id.tv_bearing);
        mSatCount = view.findViewById(R.id.tv_sat_count);
        mAverageGpsCno = view.findViewById(R.id.tv_average_gps_cno);
        mAverageGlonassCno = view.findViewById(R.id.tv_average_glonass_cno);
        mAverageBeidouCno = view.findViewById(R.id.tv_average_beidou_cno);
        mAverageGalileoCno = view.findViewById(R.id.tv_average_galileo_cno);

        mFlagCanada = getResources().getDrawable(R.drawable.ic_canada);
        mFlagChina = getResources().getDrawable(R.drawable.ic_china);
        mFlagEU = getResources().getDrawable(R.drawable.ic_eu);
        mFlagIndia = getResources().getDrawable(R.drawable.ic_india);
        mFlagJapan = getResources().getDrawable(R.drawable.ic_japan);
        mFlagRussia = getResources().getDrawable(R.drawable.ic_russia);
        mFlagUnitedKingdom = getResources().getDrawable(R.drawable.ic_uk);
        mFlagUsa = getResources().getDrawable(R.drawable.ic_america);

        mGnssSatelliteStatusList = new ArrayList<>();

        mCummulativeGpsSnr = 0.0f;
        mCummulativeGlonassSnr = 0.0f;
        mCummulativeBeidouSnr = 0.0f;
        mCummulativeGalileoSnr = 0.0f;
        mCummulativeGpsCount = 0;
        mCummulativeGlonassCount = 0;
        mCummulativeBeidouCount = 0;
        mCummulativeGalileoCount = 0;
        mCount = 0;

        // Create parse query object
        mUniqueId = Settings.Secure.getString(getActivity().getContentResolver(),Settings.Secure.ANDROID_ID);
        mQuery = ParseQuery.getQuery("Sensors");
        mQuery.whereEqualTo("sensorId", mUniqueId);

        RecyclerView mStatusList = view.findViewById(R.id.status_list);
        mAdapter = new GnssStatusAdapter();
        mStatusList.setAdapter(mAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mStatusList.setLayoutManager(llm);

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
    public void onGpsStatusChanged(int event, GpsStatus s) {

        Iterator<GpsSatellite> status = s.getSatellites().iterator();

        float time = s.getTimeToFirstFix()/1000.0f;
        mTTFF.setText(String.format(Locale.ENGLISH, "%.3f", time));

        float gpsSnrSum = 0;
        int gpsCount = 0;
        float glonassSnrSum = 0;
        int glonassCount = 0;
        float beidouSnrSum = 0;
        int beidouCount = 0;
        float galileoSnrSum = 0;
        int galileoCount = 0;
        mSvCount = 0;
        mUsedInFixCount = 0;
        mGnssSatelliteStatusList.clear();
        while (status.hasNext()) {

            GpsSatellite satellite = status.next();

            if(satellite.getSnr() != 0.0f) {
                GnssType type = Utils.getGnssType(satellite.getPrn());

                int constellationType = 0;

                switch (type) {

                    case NAVSTAR:
                        gpsSnrSum += satellite.getSnr();
                        gpsCount++;
                        constellationType = GnssStatus.CONSTELLATION_GPS;
                        break;
                    case GLONASS:
                        glonassSnrSum += satellite.getSnr();
                        glonassCount++;
                        constellationType = GnssStatus.CONSTELLATION_GLONASS;
                        break;
                    case BEIDOU:
                        beidouSnrSum += satellite.getSnr();
                        beidouCount++;
                        constellationType = GnssStatus.CONSTELLATION_BEIDOU;
                        break;
                    case GALILEO:
                        galileoSnrSum += satellite.getSnr();
                        galileoCount++;
                        constellationType = GnssStatus.CONSTELLATION_GALILEO;
                        break;
                    case GAGAN:
                        constellationType = GnssStatus.CONSTELLATION_SBAS;
                        break;
                    case QZSS:
                        constellationType = GnssStatus.CONSTELLATION_QZSS;
                        break;
                }

                mGnssSatelliteStatusList.add(new GnssSatelliteStatus(satellite.getPrn(),
                        constellationType,
                        satellite.getSnr(),
                        satellite.getAzimuth(),
                        satellite.getElevation()));

                if (satellite.usedInFix()) {
                    mUsedInFixCount++;
                }

                mSvCount++;
            }
        }

        if(mCount < MAX_COUNT) {

            if(gpsCount != 0) {
                mCummulativeGpsSnr += gpsSnrSum / gpsCount;
                mCummulativeGpsCount++;
            }

            if(glonassCount != 0) {
                mCummulativeGlonassSnr += glonassSnrSum / glonassCount;
                mCummulativeGlonassCount++;
            }

            if(beidouCount != 0) {
                mCummulativeBeidouSnr += beidouSnrSum / beidouCount;
                mCummulativeBeidouCount++;
            }

            if(galileoCount != 0) {
                mCummulativeGalileoSnr += galileoSnrSum / galileoCount;
                mCummulativeGalileoCount++;
            }

            mCount++;

            if (mCount == MAX_COUNT) {

                float averageGpsSnr = 0.0f;
                float averageGlonassSnr = 0.0f;
                float averageBeidouSnr = 0.0f;
                float averageGalileoSnr = 0.0f;

                if(mCummulativeGpsCount != 0) {
                    averageGpsSnr = mCummulativeGpsSnr / mCummulativeGpsCount;
                    mCummulativeGpsCount = 0;
                }

                if(mCummulativeGlonassCount != 0) {
                    averageGlonassSnr = mCummulativeGlonassSnr / mCummulativeGlonassCount;
                    mCummulativeGlonassCount = 0;
                }

                if(mCummulativeBeidouSnr != 0) {
                    averageBeidouSnr = mCummulativeBeidouSnr / mCummulativeBeidouCount;
                    mCummulativeBeidouCount = 0;
                }

                if(mCummulativeGalileoSnr != 0) {
                    averageGalileoSnr = mCummulativeGalileoSnr / mCummulativeGalileoCount;
                    mCummulativeGalileoCount = 0;
                }

                mAverageGpsCno.setText(mRes.getString(R.string.average_cno_value, averageGpsSnr, gpsCount));
                mAverageGlonassCno.setText(mRes.getString(R.string.average_cno_value, averageGlonassSnr, glonassCount));
                mAverageBeidouCno.setText(mRes.getString(R.string.average_cno_value, averageBeidouSnr, beidouCount));
                mAverageGalileoCno.setText(mRes.getString(R.string.average_cno_value, averageGalileoSnr, galileoCount));

                writeSnrToParseServer(averageGpsSnr, averageGlonassSnr, averageBeidouSnr, averageGalileoSnr,
                        gpsCount, glonassCount, beidouCount, galileoCount);


                mCummulativeGpsSnr = 0;
                mCummulativeGlonassSnr = 0;
                mCummulativeBeidouSnr = 0;
                mCummulativeGalileoSnr = 0;
                mCount = 0;
            }
        }

        Collections.sort(mGnssSatelliteStatusList, new PrnComparator());
        Collections.sort(mGnssSatelliteStatusList, new ConstellationTypeComparator());

        mSatCount.setText(mRes.getString(R.string.gps_num_sats_value, mUsedInFixCount, mSvCount));

        mAdapter.notifyDataSetChanged();

    }

    @Override
    @RequiresApi(Build.VERSION_CODES.N)
    public void onGnssFirstFix(int ttffMillis) {
        double time = ttffMillis/1000.0f;
        mTTFF.setText(String.format(Locale.ENGLISH, "%.3f", time));
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.N)
    public void onSatelliteStatusChanged(GnssStatus status) {

        final int length = status.getSatelliteCount();
        float gpsSnrSum = 0;
        int gpsCount = 0;
        float glonassSnrSum = 0;
        int glonassCount = 0;
        float beidouSnrSum = 0;
        int beidouCount = 0;
        float galileoSnrSum = 0;
        int galileoCount = 0;
        mSvCount = 0;
        mUsedInFixCount = 0;
        mGnssSatelliteStatusList.clear();
        while (mSvCount < length) {

            if(status.getConstellationType(mSvCount) == GnssStatus.CONSTELLATION_GPS) {
                gpsSnrSum += status.getCn0DbHz(mSvCount);
                gpsCount++;
            }

            if(status.getConstellationType(mSvCount) == GnssStatus.CONSTELLATION_GLONASS) {
                glonassSnrSum += status.getCn0DbHz(mSvCount);
                glonassCount++;
            }

            if(status.getConstellationType(mSvCount) == GnssStatus.CONSTELLATION_BEIDOU) {
                beidouSnrSum += status.getCn0DbHz(mSvCount);
                beidouCount++;
            }

            if(status.getConstellationType(mSvCount) == GnssStatus.CONSTELLATION_GALILEO) {
                galileoSnrSum += status.getCn0DbHz(mSvCount);
                galileoCount++;
            }

            mGnssSatelliteStatusList.add(new GnssSatelliteStatus(status.getSvid(mSvCount),
                    status.getConstellationType(mSvCount),
                    status.getCn0DbHz(mSvCount),
                    status.getAzimuthDegrees(mSvCount),
                    status.getElevationDegrees(mSvCount)));

            if (status.usedInFix(mSvCount)) {
                mUsedInFixCount++;
            }

            mSvCount++;
        }

        if(mCount < MAX_COUNT) {
            if(gpsCount != 0) {
                mCummulativeGpsSnr += gpsSnrSum / gpsCount;
                mCummulativeGpsCount++;
            }

            if(glonassCount != 0) {
                mCummulativeGlonassSnr += glonassSnrSum / glonassCount;
                mCummulativeGlonassCount++;
            }

            if(beidouCount != 0) {
                mCummulativeBeidouSnr += beidouSnrSum / beidouCount;
                mCummulativeBeidouCount++;
            }

            if(galileoCount != 0) {
                mCummulativeGalileoSnr += galileoSnrSum / galileoCount;
                mCummulativeGalileoCount++;
            }

            mCount++;

            if (mCount == MAX_COUNT) {

                float averageGpsSnr = 0.0f;
                float averageGlonassSnr = 0.0f;
                float averageBeidouSnr = 0.0f;
                float averageGalileoSnr = 0.0f;

                if(mCummulativeGpsCount != 0) {
                    averageGpsSnr = mCummulativeGpsSnr / mCummulativeGpsCount;
                    mCummulativeGpsCount = 0;
                }

                if(mCummulativeGlonassCount != 0) {
                    averageGlonassSnr = mCummulativeGlonassSnr / mCummulativeGlonassCount;
                    mCummulativeGlonassCount = 0;
                }

                if(mCummulativeBeidouSnr != 0) {
                    averageBeidouSnr = mCummulativeBeidouSnr / mCummulativeBeidouCount;
                    mCummulativeBeidouCount = 0;
                }

                if(mCummulativeGalileoSnr != 0) {
                    averageGalileoSnr = mCummulativeGalileoSnr / mCummulativeGalileoCount;
                    mCummulativeGalileoCount = 0;
                }

                mAverageGpsCno.setText(mRes.getString(R.string.average_cno_value, averageGpsSnr, gpsCount));
                mAverageGlonassCno.setText(mRes.getString(R.string.average_cno_value, averageGlonassSnr, glonassCount));
                mAverageBeidouCno.setText(mRes.getString(R.string.average_cno_value, averageBeidouSnr, beidouCount));
                mAverageGalileoCno.setText(mRes.getString(R.string.average_cno_value, averageGalileoSnr, galileoCount));

                writeSnrToParseServer(averageGpsSnr, averageGlonassSnr, averageBeidouSnr, averageGalileoSnr,
                        gpsCount, glonassCount, beidouCount, galileoCount);


                mCummulativeGpsSnr = 0;
                mCummulativeGlonassSnr = 0;
                mCummulativeBeidouSnr = 0;
                mCummulativeGalileoSnr = 0;
                mCount = 0;
            }
        }

        Collections.sort(mGnssSatelliteStatusList, new PrnComparator());
        Collections.sort(mGnssSatelliteStatusList, new ConstellationTypeComparator());

        mSatCount.setText(mRes.getString(R.string.gps_num_sats_value, mUsedInFixCount, mSvCount));

        mAdapter.notifyDataSetChanged();

    }

    private void writeSnrToParseServer(final float gps, final float glonass, final float beidou, final float galileo,
                                       final int gpsCount, final int glonassCount, final int beidouCount, final int galileoCount) {

        mQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null) {
                    if(objects.size() >= 1) {
                        if(!Double.isNaN(gps)) {
                            objects.get(0).put("gpsSnr", gps);
                            objects.get(0).put("gpsCount", gpsCount);
                        }
                        if(!Double.isNaN(glonass)) {
                            objects.get(0).put("glonassSnr", glonass);
                            objects.get(0).put("glonassCount", glonassCount);
                        }
                        if(!Double.isNaN(beidou)) {
                            objects.get(0).put("beidouSnr", beidou);
                            objects.get(0).put("beidouCount", beidouCount);
                        }
                        if(!Double.isNaN(galileo)) {
                            objects.get(0).put("galileoSnr", galileo);
                            objects.get(0).put("galileoCount", galileoCount);
                        }
                        objects.get(0).saveInBackground();
                    }
                }
            }
        });
    }

    @Override
    public void onGnssStarted() {

    }

    @Override
    public void onGnssStopped() {

    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent events) {

    }

    @Override
    public void onOrientationChanged(double orientation, double tilt) {

    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;

        // Extract latitude and longitude information and display in views
        mLatitude.setText(String.format(Locale.ENGLISH,"%.6f",location.getLatitude()));
        mLongitude.setText(String.format(Locale.ENGLISH,"%.6f",location.getLongitude()));
        mError.setText(String.format(Locale.ENGLISH,"%.3f", location.getAccuracy()));
        mSpeed.setText(String.format(Locale.ENGLISH,"%.3f", location.getSpeed()));
        mAltitude.setText(String.format(Locale.ENGLISH, "%.3f", location.getAltitude()));
        mBearing.setText(String.format(Locale.ENGLISH,"%.3f", location.getBearing()));

        // Extract datetime information and display in views
        Date dateObject = new Date(location.getTime());
        String date = Utils.formatDate(dateObject);
        String time = Utils.formatTime(dateObject);
        mDate.setText(date);
        mTime.setText(time);

        writeLocationToParseServer(location);

    }

    private void writeLocationToParseServer(final Location location) {
        mQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null) {
                    if(objects.size() >= 1) {
                        objects.get(0).put("latitude", location.getLatitude());
                        objects.get(0).put("longitude", location.getLongitude());
                        objects.get(0).saveInBackground();
                    }
                }
            }
        });
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

    private class GnssStatusAdapter extends RecyclerView.Adapter<GnssStatusAdapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView svId;
            private final ImageView flag;
            private final TextView azimuth;
            private final TextView elevation;
            private final TextView cno;
            private final ProgressBar cnoBar;

            public TextView getSvId() {
                return svId;
            }

            public ImageView getFlag() {
                return flag;
            }

            public TextView getAzimuth() {
                return azimuth;
            }

            public TextView getElevation() {
                return elevation;
            }

            public TextView getCno() {
                return cno;
            }

            public ProgressBar getCnoBar() {
                return cnoBar;
            }

            public ViewHolder(@NonNull View v) {
                super(v);
                svId = v.findViewById(R.id.tv_svid);
                flag = v.findViewById(R.id.iv_flag);
                azimuth = v.findViewById(R.id.tv_azimuth);
                elevation = v.findViewById(R.id.tv_elevation);
                cno = v.findViewById(R.id.tv_cno);
                cnoBar = v.findViewById(R.id.progressBar);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.status_row_item, viewGroup,false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder v, int position) {

            GnssSatelliteStatus g = mGnssSatelliteStatusList.get(position);

            v.getSvId().setText(String.format(Locale.ENGLISH, "%d", g.getPrns()));
            v.getFlag().setScaleType(ImageView.ScaleType.FIT_START);

            int type = g.getConstellationType();
            switch (type) {
                case GnssStatus.CONSTELLATION_GPS:
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        v.getFlag().setImageDrawable(mFlagUsa);
                    else
                        v.getFlag().setImageResource(R.drawable.ic_america);
                    break;

                case GnssStatus.CONSTELLATION_GLONASS:
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        v.getFlag().setImageDrawable(mFlagRussia);
                    else
                        v.getFlag().setImageResource(R.drawable.ic_russia);
                    break;

                case GnssStatus.CONSTELLATION_BEIDOU:
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        v.getFlag().setImageDrawable(mFlagChina);
                    else
                        v.getFlag().setImageResource(R.drawable.ic_china);
                    break;

                case GnssStatus.CONSTELLATION_GALILEO:
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        v.getFlag().setImageDrawable(mFlagEU);
                    else
                        v.getFlag().setImageResource(R.drawable.ic_eu);
                    break;

                case GnssStatus.CONSTELLATION_QZSS:
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        v.getFlag().setImageDrawable(mFlagJapan);
                    else
                        v.getFlag().setImageResource(R.drawable.ic_japan);
                    break;
                case GnssStatus.CONSTELLATION_SBAS:
                    if(g.getPrns() == 40 || g.getPrns() == 41) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            v.getFlag().setImageDrawable(mFlagIndia);
                        else
                            v.getFlag().setImageResource(R.drawable.ic_india);
                    }
                    break;
            }

            float snr = g.getSnrCn0s();
            if (snr != NO_DATA) {
                v.getCno().setText(String.format(Locale.ENGLISH, "%.0f", snr));
                v.getCnoBar().setProgress((int) snr);
                if (snr > 20) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        v.getCnoBar().setProgressTintList(ColorStateList.valueOf(mRes.getColor(R.color.green)));
                    else {
                        v.getCnoBar().getProgressDrawable().setColorFilter(mRes.getColor(R.color.green), PorterDuff.Mode.SRC_IN);
                    }

                } else if (snr < 15) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        v.getCnoBar().setProgressTintList(ColorStateList.valueOf(mRes.getColor(R.color.red)));
                    else {
                        v.getCnoBar().getProgressDrawable().setColorFilter(mRes.getColor(R.color.red), PorterDuff.Mode.SRC_IN);
                    }
                } else {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        v.getCnoBar().setProgressTintList(ColorStateList.valueOf(mRes.getColor(R.color.amber)));
                    else {
                        v.getCnoBar().getProgressDrawable().setColorFilter(mRes.getColor(R.color.amber), PorterDuff.Mode.SRC_IN);
                    }
                }
            } else {
                v.getCno().setText("");
            }

            if (g.getSvAzimuths() != NO_DATA) {
                v.getAzimuth().setText(mRes.getString(R.string.gps_azimuth_column_value,
                        g.getSvAzimuths()));
            } else {
                v.getAzimuth().setText("");
            }

            if (g.getSvElevations() != NO_DATA) {
                v.getElevation().setText(mRes.getString(R.string.gps_elevation_column_value,
                        g.getSvElevations()));
            } else {
                v.getElevation().setText("");
            }

        }


        @Override
        public int getItemCount() {
            return mSvCount ;
        }



    }
}
