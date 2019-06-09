package com.example.gnssanalyzerplus;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.gnssanalyzerplus.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements LocationListener {

    public static final String TAG = "MainActivity";

    private TabLayout tabLayout;
    private AppBarLayout appBarLayout;
    private ViewPager viewPager;

    private int[] tabIcons = {
      R.drawable.status_icon,
      R.drawable.map_icon,
      R.drawable.report_icon
    };

    private static MainActivity mActivity;

    private boolean mDebug = true;

    // GnssListeners for fragments
    private ArrayList<GnssListener> mGnssListeners = new ArrayList<>();

    private Location mLastLocation;

    private boolean mUserDeniedPermission = false;

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.WAKE_LOCK
    };

    public static final int REQUEST_CODE = 1;

    private LocationManager mLocationManager;

    private LocationProvider mProvider;

    boolean mStarted;

    final long MIN_TIME = 1000;

    final float MIN_DISTANCE = 0;

    private GnssStatus.Callback mGnssStatusListener;

    private GnssStatus mGnssStatus;

    private GpsStatus.Listener mLegacyStatusListener;

    private GpsStatus mLegacyStatus;

    private PowerManager.WakeLock mWakeLock;

    private PowerManager mPowerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActivity = this;

        // Initializing variables
        tabLayout = (TabLayout) findViewById(R.id.tab_layout_id);
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar_id);
        viewPager = (ViewPager) findViewById(R.id.view_pager_id);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        // Adding fragments
        adapter.addFragment(new StatusFragment());
        adapter.addFragment(new MapFragment());
        adapter.addFragment(new ReportFragment());

        // Adapter setup and configure tab layout icons
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
        configTabIcons();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!mUserDeniedPermission) {
            // Request permission and initialize app
            requestPermissionAndInit(this);

        } else {
            // Show permission request dialog
            showRequestPermissionDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        removeStatusListener();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(mWakeLock.isHeld()) {
            mWakeLock.release();
            Log.d(TAG, "wake lock released");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mUserDeniedPermission = false;
                init();
            } else {
                mUserDeniedPermission = true;
            }
        }
    }

    private void requestPermissionAndInit(final Activity activity) {
        if(PermissionUtils.hasGrantedPermissions(activity, REQUIRED_PERMISSIONS)) {
            init();
        } else {
            // Request permission from user
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, REQUEST_CODE);
        }
    }

    private void init() {
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakeLock");
        if(!mWakeLock.isHeld()) {
            mWakeLock.acquire();
            Log.d(TAG, "wake lock acquired");
        }

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mProvider = mLocationManager.getProvider(LocationManager.GPS_PROVIDER);
        if(mProvider == null) {
            Log.e(TAG, "Unable to get GPS_PROVIDER");
            Toast.makeText(this, getString(R.string.gps_not_supported),
                    Toast.LENGTH_SHORT).show();
        }

        gpsStart();
        addStatusListener();
    }

    private void addStatusListener() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            addGnssStatusListener();
        } else {
            addLegacyStatusListener();
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private void addGnssStatusListener() {
        mGnssStatusListener = new GnssStatus.Callback() {
            @Override
            public void onStarted() {
                for(GnssListener listener: mGnssListeners) {
                    listener.onGnssStarted();
                }
            }

            @Override
            public void onStopped() {
                for(GnssListener listener: mGnssListeners) {
                    listener.onGnssStopped();
                }
            }

            @Override
            public void onFirstFix(int ttffMillis) {
                for(GnssListener listener: mGnssListeners) {
                    listener.onGnssFirstFix(ttffMillis);
                }
            }

            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                mGnssStatus = status;

                for(GnssListener listener: mGnssListeners) {
                    listener.onSatelliteStatusChanged(mGnssStatus);
                }
            }
        };

        mLocationManager.registerGnssStatusCallback(mGnssStatusListener);
    }

    private void addLegacyStatusListener() {
        mLegacyStatusListener = new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(int event) {
                mLegacyStatus = mLocationManager.getGpsStatus(mLegacyStatus);

                switch(event) {

                    case GpsStatus.GPS_EVENT_STARTED:
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED:
                        break;
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        break;
                }

                for(GnssListener listener: mGnssListeners) {
                    listener.onGpsStatusChanged(event, mLegacyStatus);
                }
            }
        };

        mLocationManager.addGpsStatusListener(mLegacyStatusListener);
    }

    private void removeStatusListener() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            removeGnssStatusListener();
        } else {
            removeLegacyStatusListener();
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private void removeGnssStatusListener() {
        if(mLocationManager != null) {
            mLocationManager.unregisterGnssStatusCallback(mGnssStatusListener);
        }
    }

    private void removeLegacyStatusListener() {
        if(mLocationManager!= null && mLegacyStatusListener != null) {
            mLocationManager.removeGpsStatusListener(mLegacyStatusListener);
        }
    }

    private synchronized void gpsStart() {
        if(mLocationManager == null || mProvider == null) {
            return;
        }

        if(!mStarted) {
            mLocationManager.requestLocationUpdates(mProvider.getName(), MIN_TIME, MIN_DISTANCE, this);
            mStarted = true;
        }

        for(GnssListener listener: mGnssListeners) {
            listener.gpsStart();
        }
    }

    private synchronized void gpsStop() {
        if(mLocationManager == null) {
            return;
        }

        if(mStarted) {
            mLocationManager.removeUpdates(this);
            mStarted = false;
        }

        for(GnssListener listener: mGnssListeners) {
            listener.gpsStop();
        }
    }

    private void showRequestPermissionDialog() {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this)
                .setTitle(R.string.title_permission)
                .setMessage(R.string.text_permission)
                .setCancelable(false)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Request permissions from the user
                                ActivityCompat.requestPermissions(mActivity, REQUIRED_PERMISSIONS, REQUEST_CODE);
                            }
                        }
                )
                .setNegativeButton(R.string.exit,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Exit app
                                finish();
                            }
                        }
                );
        builder.create().show();
    }

    private void configTabIcons() {
        int i;
        for(i = 0; i < tabLayout.getTabCount(); i++) {
            Objects.requireNonNull(tabLayout.getTabAt(i)).setIcon(tabIcons[i]);
        }
    }

    public static MainActivity getInstance() {
        return mActivity;
    }

    public void addListener(GnssListener listener) {
        mGnssListeners.add(listener);
    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;

        for(GnssListener listener: mGnssListeners) {
            listener.onLocationChanged(location);
        }

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
