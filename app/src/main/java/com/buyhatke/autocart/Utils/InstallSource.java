package com.buyhatke.autocart.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Abhishek on 01-Mar-18.
 */

public class InstallSource extends BroadcastReceiver {

    private static final String REFERRER= "referrer";
    private static final String TAG = "InstallSource";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceived Called");
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey(REFERRER)){
            String referrer = Uri.decode(bundle.getString(REFERRER));
            Toast.makeText(context, referrer, Toast.LENGTH_SHORT).show();
            Log.d(TAG, referrer);
        }
    }

}
