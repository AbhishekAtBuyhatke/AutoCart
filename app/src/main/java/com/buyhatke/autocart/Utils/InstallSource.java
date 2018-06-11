package com.buyhatke.autocart.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import static android.content.Context.MODE_PRIVATE;
import static com.buyhatke.autocart.Constants.REFERRER;
import static com.buyhatke.autocart.Constants.SHARED_PREF;

/**
 * Created by Abhishek on 01-Mar-18.
 */

public class InstallSource extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        if (bundle != null && bundle.containsKey(REFERRER)){
            String referrer = Uri.decode(bundle.getString(REFERRER));
            sharedPref.edit().putString(REFERRER, referrer).apply();
        }
    }

}
