package com.buyhatke.autocart;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.json.JSONArray;
import org.json.JSONException;

import static com.buyhatke.autocart.Constants.SHARED_PREF;

/**
 * Created by Abhishek on 20-Sep-17.
 */

public class AutoCart extends Application {

    private Tracker tracker;
    private static AutoCart instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static AutoCart getInstance(){
        return instance;
    }

    synchronized public Tracker getDefaultTracker(){
        if (tracker == null){
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            tracker = analytics.newTracker(R.xml.tracker);
            tracker.enableExceptionReporting(true);
        }
        return tracker;
    }

    public static void sendUpdateToServer(String category, String event){
        Tracker tracker = AutoCart.getInstance().getDefaultTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(event)
                .build());
    }

}
