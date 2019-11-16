package com.example.gnssanalyzerplus;

import android.app.Application;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.List;


public class App extends Application {

    public static final String TAG = "GnssAnalyzerPlus";

    @Override
    public void onCreate() {
        super.onCreate();

        final String uniqueId  = Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);

        Parse.enableLocalDatastore(this);

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("bdd2ff3e78544bf11e977e1d74f80071caa3a070")
                .clientKey("90a9f48e9e36e6783d2cc41485dd9d3d1e855e42")
                .server("http://ec2-52-77-219-242.ap-southeast-1.compute.amazonaws.com/parse")
                .build()
        );

        final ParseQuery<ParseObject> query = ParseQuery.getQuery("Sensors");
        final ParseObject object = new ParseObject("Sensors");

        query.whereEqualTo("sensorId", uniqueId);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null) {
                    if(objects.size() != 0) {
                        Toast.makeText(getApplicationContext(),
                                "Sensor registered. ID: "+ objects.get(0).getString("sensorId"),
                                Toast.LENGTH_LONG).show();
                    } else {
                        object.put("sensorId", uniqueId);
                        object.put("latitude", 0.00);
                        object.put("longitude", 0.00);
                        object.put("gpsSnr", 0.0);
                        object.put("glonassSnr", 0.0);
                        object.put("beidouSnr", 0.0);
                        object.put("galileoSnr", 0.0);
                        object.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if(e == null) {
                                    Toast.makeText(getApplicationContext(),
                                            "Sensor registered. ID: "+ uniqueId,
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            "Failed to register sensor. Please check DB connection",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                }

            }
        });


//        Parse.initialize(new Parse.Configuration.Builder(this)
//                .applicationId("myAppId")
//                .clientKey("myMasterKey")
//                .server("http://192.168.100.60:1337/parse/")
//                .build()
//        );

    }

}
