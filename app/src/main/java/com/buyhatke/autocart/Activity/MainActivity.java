package com.buyhatke.autocart.Activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.buyhatke.autocart.AutoCart;
import com.buyhatke.autocart.Constants;
import com.buyhatke.autocart.R;
import com.buyhatke.autocart.SaleAdapter;
import com.buyhatke.autocart.SaleItem;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;

import static com.buyhatke.autocart.Constants.SALE_URL;
import static com.buyhatke.autocart.Constants.SHARED_PREF;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private RequestQueue queue;
    private RecyclerView rvAmazon, rvFlipkart, rvMi;
    private TextView tvMiSaleInfo, tvFlipkartInfo, tv_amazon_info, tv_amazon, tvFlipkart, tvInstructionText;
    private ProgressDialog pd;
    private ImageView ivInstruction;
    private LinearLayout llInstructionContainer;
    private boolean instrOpen;
    private static final String REQUEST_TAG = "downloadSaleData";
    private BroadcastReceiver broadcastReceiver;
    private SharedPreferences sharedPref;
    private static final String IS_REGISTERED = "isRegistered";
    private static final String REGISTER_TAG = "Registration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queue = Volley.newRequestQueue(this);

        pd = new ProgressDialog(this);
        pd.setMessage("Fetching sales from server...");
        pd.setTitle("Please Wait");
        pd.show();

        instrOpen = false;

        tvMiSaleInfo = (TextView) findViewById(R.id.tv_mi_sale_info);
        tvFlipkartInfo = (TextView) findViewById(R.id.tv_flipkart_sale_info);
        tv_amazon_info = (TextView) findViewById(R.id.tv_amazon_sale_info);
        tv_amazon = (TextView) findViewById(R.id.tv_amazon_sale);
        tvFlipkart = (TextView) findViewById(R.id.tv_flipkart_sale);
        tvInstructionText = (TextView) findViewById(R.id.instrucion_text);
        ivInstruction = (ImageView) findViewById(R.id.iv_instr_arrow);
        llInstructionContainer = (LinearLayout) findViewById(R.id.ll_instruction_container);

        tvMiSaleInfo.setOnClickListener(this);
        tvFlipkartInfo.setOnClickListener(this);
        tv_amazon_info.setOnClickListener(this);
        tv_amazon.setOnClickListener(this);
        tvFlipkart.setOnClickListener(this);
        tvInstructionText.setOnClickListener(this);

        rvAmazon = (RecyclerView) findViewById(R.id.rv_amazon);
        rvFlipkart = (RecyclerView) findViewById(R.id.rv_flipkart);
        rvMi = (RecyclerView) findViewById(R.id.rv_mi);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // checking for type intent filter
                if (intent.getAction().equals(Constants.REGISTRATION_COMPLETE)) {
                    // gcm successfully registered
                    // now subscribe to `global` topic to receive app wide notifications
                    FirebaseMessaging.getInstance().subscribeToTopic(Constants.TOPIC_GLOBAL);

                } else if (intent.getAction().equals(Constants.PUSH_NOTIFICATION)) {
                    // new push notification is received
                    String message = intent.getStringExtra("message");
                    Toast.makeText(getApplicationContext(), "Push notification: " + message, Toast.LENGTH_LONG).show();
                }
            }
        };

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        rvAmazon.setLayoutManager(new LinearLayoutManager(this));
        rvFlipkart.setLayoutManager(new LinearLayoutManager(this));
        rvMi.setLayoutManager(new LinearLayoutManager(this));

        rvAmazon.setHasFixedSize(true);
        rvMi.setHasFixedSize(true);
        rvFlipkart.setHasFixedSize(true);

        prepareItems();
    }

    private void prepareItems() {
        StringRequest request = new StringRequest(SALE_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    Gson gson = new Gson();
                    SaleItem[] saleItems = gson.fromJson(jsonArray.getString(0),SaleItem[].class);
                    rvMi.setAdapter(new SaleAdapter(saleItems, MainActivity.this, false));
                    saleItems = gson.fromJson(jsonArray.getString(1), SaleItem[].class);
                    rvFlipkart.setAdapter(new SaleAdapter(saleItems, MainActivity.this, false));
                    saleItems = gson.fromJson(jsonArray.getString(2), SaleItem[].class);
                    rvAmazon.setAdapter(new SaleAdapter(saleItems, MainActivity.this, true));
                    sharedPref = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
                    if (!sharedPref.getBoolean(IS_REGISTERED, false)) {
                        registerApp();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Something went wrong! Make sure you have an active Internet connection.", Toast.LENGTH_SHORT).show();
                }
                pd.dismiss();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                pd.dismiss();
                Toast.makeText(MainActivity.this, "Something went wrong! Make sure you have an active Internet connection.", Toast.LENGTH_SHORT).show();
                prepareItems();
            }
        });
        request.setShouldCache(false);
        request.setTag(REQUEST_TAG);
        queue.add(request);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_help) {
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_help);
            dialog.findViewById(R.id.dialog_help).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, WebViewActivity.class);
        if (v.getId() == R.id.tv_mi_sale_info ||
                v.getId() == R.id.tv_flipkart_sale_info ||
                v.getId() == R.id.tv_amazon_sale_info) {
            intent.putExtra("url", "https://compare.buyhatke.com/xiaomi-flash-sale/redmi-note-mi/?utm_source=ext");
            startActivity(intent);
        }
        else if (v.getId() == R.id.tv_amazon_sale){
            intent.putExtra("url", "http://www.amazon.in");
            startActivity(intent);
        } else if (v == tvInstructionText){
            AutoCart.sendUpdateToServer("InstructionsClicked","");
            if (instrOpen){
                instrOpen = false;
                llInstructionContainer.setVisibility(View.GONE);
                ivInstruction.setImageResource(R.drawable.ic_keyboard_arrow_down_black_24px);
            } else {
                instrOpen = true;
                llInstructionContainer.setVisibility(View.VISIBLE);
                ivInstruction.setImageResource(R.drawable.ic_keyboard_arrow_up_black_24px);
            }
        }
    }

    @Override
    protected void onPause() {
        queue.cancelAll(REQUEST_TAG);
        if (pd.isShowing())
            pd.dismiss();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tracker tracker = AutoCart.getInstance().getDefaultTracker();
        tracker.setScreenName("MainActivity");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
        // register GCM registration complete receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.REGISTRATION_COMPLETE));

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.PUSH_NOTIFICATION));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(REGISTER_TAG, "Permission granted");
                registerApp();
            }
        }
    }

    private String getDeviceId() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_PHONE_STATE},101);
            return null;
        }
        else if (telephonyManager.getDeviceId() != null)
            return telephonyManager.getDeviceId();
        else
            return Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ANDROID_ID);
    }

    private void sendRegisterIdToBackend(final String appId, final String appAuth) {
        final String setTokenUrl = Constants.SET_TOKEN_URL
                + "&app_id=" + appId
                + "&app_auth=" + appAuth;
        StringRequest request = new StringRequest(setTokenUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(REGISTER_TAG, response);
                if (response.equals("1")) {
                    Log.d(REGISTER_TAG, "Successfully registered app");
                    sharedPref.edit().putBoolean(IS_REGISTERED, true).apply();
                }
                else {
                    Log.d(REGISTER_TAG, "Invalid Response Recieved. Trying again");
                    sendRegisterIdToBackend(appId, appAuth);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                sendRegisterIdToBackend(appId, appAuth);
                Log.d(REGISTER_TAG, "Token set failed. Trying again");
            }
        });
        request.setTag(REGISTER_TAG);
        queue.add(request);
    }

    private void registerApp() {
        String idPhone = getDeviceId();
        if (!TextUtils.isEmpty(idPhone)){
            String url = Uri.parse(Constants.REGISTER_URL).buildUpon()
                    .appendQueryParameter("source", "autocart_app")
                    .appendQueryParameter("imei", idPhone).toString();

            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    try {
                        String appId = response.getJSONObject(0).getString("app_id");
                        String appAuth = response.getJSONObject(0).getString("app_auth");
                        Log.d(REGISTER_TAG, appId + " " + appAuth);
                        sendRegisterIdToBackend(appId, appAuth);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d(REGISTER_TAG, "App registration failed. Trying again.");
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    registerApp();
                }
            });

            jsonArrayRequest.setTag(REGISTER_TAG);
            queue.add(jsonArrayRequest);
        }
    }
}
