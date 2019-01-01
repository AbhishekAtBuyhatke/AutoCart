package com.buyhatke.autocart.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.buyhatke.autocart.Adapter.FetchBannerAdapter;
import com.buyhatke.autocart.Adapter.SaleAdapter;
import com.buyhatke.autocart.AutoCart;
import com.buyhatke.autocart.Constants;
import com.buyhatke.autocart.Models.Variants;
import com.buyhatke.autocart.R;
import com.buyhatke.autocart.Models.SaleItem;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.lang.reflect.Field;

import me.relex.circleindicator.CircleIndicator;

import static com.buyhatke.autocart.Constants.FEEDBACK_URL;
import static com.buyhatke.autocart.Constants.REFERRER;
import static com.buyhatke.autocart.Constants.SALE_URL_NEW;
import static com.buyhatke.autocart.Constants.SHARED_PREF;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

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
    private static final String IS_REGISTERED = "callRegisterAPI";
    private static final String REGISTER_TAG = "Registration";
    private static final String APP_OPEN_COUNT = "appOpenCount";
    private static final String APP_OPEN_COUNT_SDK = "appOpenCountSDK";
    private static final String REVIEW_DONE = "reviewDone";
    private static final String SDK_NOTIFICATION = "SDKNotification";
    private long BANNER_TIME_DELAY = 7 * 1000;
    private int BANNER_TRANSITION_DELAY = 1 * 1200;
    private ViewPager pager;
    private Handler handler;
    private Runnable runnable;
    private CircleIndicator circleIndicator;
    private String currentVersion;
    private static final String urlOfApp = "https://play.google.com/store/apps/details?id=com.buyhatke.autocart&hl=en";
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queue = Volley.newRequestQueue(this);
        sharedPref = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

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
        circleIndicator = (CircleIndicator) findViewById(R.id.circular_indicator_home_pager);
        pager = (ViewPager) findViewById(R.id.pager);

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

        compareAppVersion();
        setupRecyclerView();
        fetchBanner();
        if (!sharedPref.getBoolean(REVIEW_DONE, false))
            checkAppOpenCount();
//        if (!sharedPref.getBoolean(SDK_NOTIFICATION, false))
//            checkAppOpenCountSDK();
        Log.d("AppToken", "" + FirebaseInstanceId.getInstance().getToken());
    }

    private void compareAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(this.getPackageName(), 0);
            currentVersion = pInfo.versionName;
            new GetCurrentVersion().execute();
        } catch (PackageManager.NameNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void fetchBanner() {
        String appId = sharedPref.getString("appId", "");
        String appAuth = sharedPref.getString("appAuth", "");
        String uri = Uri.parse("http://buyhatke.com/application/sendOfferNew2.php")
                .buildUpon().appendQueryParameter("app_id", appId)
                .appendQueryParameter("app_auth", appAuth).build().toString();

        JsonArrayRequest jsArrayRequest = new JsonArrayRequest(uri,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            FetchBannerAdapter adapter = new FetchBannerAdapter(MainActivity.this, response);
                            pager.setAdapter(adapter);
                            pager.setVisibility(View.VISIBLE);

                            circleIndicator.setViewPager(pager);
                            adapter.registerDataSetObserver(circleIndicator.getDataSetObserver());

                            try {
                                Field mScroller = ViewPager.class.getDeclaredField("mScroller");
                                mScroller.setAccessible(true);
                                mScroller.set(pager, new CustomScroller(pager.getContext(), BANNER_TRANSITION_DELAY));
                            } catch (Exception e) {
                            }

                            handler = new Handler(Looper.getMainLooper());
                            final int num = response.length();
                            runnable = new Runnable() {
                                public void run() {
                                    int current = pager.getCurrentItem();
                                    if (current >= num - 1)
                                        current = -1;
                                    pager.setCurrentItem(++current, true);
                                }
                            };

                            //h2 = new Handler();
                            handler.postDelayed(runnable, BANNER_TIME_DELAY);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                fetchBanner();
                Log.e("Banner", "Error response:" +
                        error.getMessage());

            }
        });

        jsArrayRequest.setRetryPolicy(new DefaultRetryPolicy(5000,
                10,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        jsArrayRequest.setShouldCache(true);
        jsArrayRequest.setTag(this);
        queue.add(jsArrayRequest);
    }

    private void checkAppOpenCount() {
        int count = sharedPref.getInt(APP_OPEN_COUNT, 0);
        if (count == 5) {
            showReviewDialog(this);
        } else if (count < 5) {
            count++;
            sharedPref.edit().putInt(APP_OPEN_COUNT, count).apply();
        }
    }

    private void checkAppOpenCountSDK() {
        int count = sharedPref.getInt(APP_OPEN_COUNT_SDK, 0);
        if (count == 13) {
            //showSDKDialog();
        } else if (count < 13) {
            count++;
            sharedPref.edit().putInt(APP_OPEN_COUNT_SDK, count).apply();
        }
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

    public static void showReviewDialog(final Context context) {
        final Dialog dialog = new Dialog(context, R.style.DialogSlideAnim);
        dialog.setContentView(R.layout.review_popup);
        dialog.setCancelable(false);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        final SharedPreferences shared = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        dialog.findViewById(R.id.ll_review_liked_it).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                context.startActivity(goToMarket);
                shared.edit().putBoolean(REVIEW_DONE, true).apply();
            }
        });
        dialog.findViewById(R.id.ll_review_hated_it).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(FEEDBACK_URL)));
                shared.edit().putBoolean(REVIEW_DONE, true).apply();
            }
        });
        dialog.findViewById(R.id.ib_review_dismiss).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                shared.edit().putInt(APP_OPEN_COUNT, 0).apply();
            }
        });
    }

    private void prepareItems() {
        StringRequest request = new StringRequest(SALE_URL_NEW, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    Gson gson = new Gson();
                    SaleItem[] saleItems = gson.fromJson(jsonArray.getString(0), SaleItem[].class);
                    String[] allVariants = new String[saleItems.length];
                    //allVariants[i] = gson.fromJson(String.valueOf(new JSONArray(saleItems[i].getAll_variants())), Variants[].class);
                    for (int i = 0; i < saleItems.length; i++)
                        allVariants[i] = saleItems[i].getAll_variants();
                    if (saleItems.length != 0)
                        rvMi.setAdapter(new SaleAdapter(saleItems, allVariants, MainActivity.this, SaleAdapter.SITE_MI));
                    else
                        findViewById(R.id.cvMi).setVisibility(View.GONE);
                    saleItems = gson.fromJson(jsonArray.getString(1), SaleItem[].class);
                    allVariants = new String[saleItems.length];
                    for (int i = 0; i < saleItems.length; i++)
                        allVariants[i] = saleItems[i].getAll_variants();
                    if (saleItems.length != 0)
                        rvFlipkart.setAdapter(new SaleAdapter(saleItems, allVariants, MainActivity.this, SaleAdapter.SITE_FLIPKART));
                    else findViewById(R.id.cvFlipkart).setVisibility(View.GONE);
                    saleItems = gson.fromJson(jsonArray.getString(2), SaleItem[].class);
                    allVariants = new String[saleItems.length];
                    for (int i = 0; i < saleItems.length; i++)
                        allVariants[i] = saleItems[i].getAll_variants();
                    if (saleItems.length != 0)
                        rvAmazon.setAdapter(new SaleAdapter(saleItems, allVariants, MainActivity.this, SaleAdapter.SITE_AMAZON));
                    else findViewById(R.id.cvAmazon).setVisibility(View.GONE);
                    if (!sharedPref.getBoolean(IS_REGISTERED, false)) {
                        checkEMEIPermission();
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
                //prepareItems();
            }
        }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                Response<String> resp = super.parseNetworkResponse(response);
                if (!resp.isSuccess()) return resp;
                long now = System.currentTimeMillis();
                Cache.Entry entry = resp.cacheEntry;
                if (entry == null) {
                    entry = new Cache.Entry();
                    entry.data = response.data;
                    entry.responseHeaders = response.headers;
                }
                entry.ttl = now + 6 * 60 * 60 * 1000; //6 hours
                return Response.success(resp.result, entry);
            }
        };
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
        } else if (v.getId() == R.id.tv_amazon_sale) {
            intent.putExtra("url", "http://www.amazon.in");
            startActivity(intent);
        } else if (v == tvInstructionText) {
            AutoCart.sendUpdateToServer("InstructionsClicked", "");
            if (instrOpen) {
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
                registerApp(getDeviceId());
            } else {
                registerApp("");
            }
            //showSDKDialog();
        }
    }

    private void checkEMEIPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_PHONE_STATE}, 101);
        } else registerApp(getDeviceId());
    }

    private String getDeviceId() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        if (telephonyManager.getDeviceId() != null)
            return telephonyManager.getDeviceId();
        else
            return Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ANDROID_ID);
    }

    private void sendRegisterIdToBackend(final String appId, final String appAuth, final String appToken) {
        final String setTokenUrl = Constants.SET_TOKEN_URL
                + "&token=" + appToken
                + "&app_id=" + appId
                + "&app_auth=" + appAuth;
        Log.d(REGISTER_TAG, setTokenUrl);
        StringRequest request = new StringRequest(setTokenUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(REGISTER_TAG, response);
                if (response.equals("1")) {
                    Log.d(REGISTER_TAG, "Successfully sent token");
                    sharedPref.edit().putBoolean(IS_REGISTERED, true).apply();
                } else {
                    Log.d(REGISTER_TAG, "Invalid Response Recieved. Trying again");
                    sendRegisterIdToBackend(appId, appAuth, appToken);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                sendRegisterIdToBackend(appId, appAuth, appToken);
                Log.d(REGISTER_TAG, "Token set failed. Trying again");
            }
        });
        request.setTag(REGISTER_TAG);
        queue.add(request);
    }

    private void registerApp(final String idPhone) {
        final String appToken = FirebaseInstanceId.getInstance().getToken();
        String url = Uri.parse(Constants.REGISTER_URL).buildUpon()
                .appendQueryParameter("source", sharedPref.getString(REFERRER, "organic"))
                .appendQueryParameter("imei", idPhone).toString();
        Log.d(REGISTER_TAG, "Register API called. Emei: " + idPhone);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    String appId = response.getJSONObject(0).getString("app_id");
                    String appAuth = response.getJSONObject(0).getString("app_auth");
                    sendRegisterIdToBackend(appId, appAuth, appToken);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("appId", appId);
                    editor.putString("appAuth", appAuth);
                    editor.putString("appToken", appToken);
                    editor.apply();
                    Log.d(REGISTER_TAG, "Registered Successfully AppId: " + appId + " AppAuth: " + appAuth + " Token: " + appToken);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(REGISTER_TAG, "App registration failed. Trying again.");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(REGISTER_TAG, error.getMessage());
            }
        });

        jsonArrayRequest.setTag(REGISTER_TAG);
        queue.add(jsonArrayRequest);
    }

    private class CustomScroller extends Scroller {

        private int mDuration;

        public CustomScroller(Context context, int mDuration) {
            super(context);
            this.mDuration = mDuration;
        }

        public CustomScroller(Context context, Interpolator interpolator, int mDuration) {
            super(context, interpolator);
            this.mDuration = mDuration;
        }

        public CustomScroller(Context context, Interpolator interpolator, boolean flywheel, int mDuration) {
            super(context, interpolator, flywheel);
            this.mDuration = mDuration;
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, mDuration);
        }
    }

//    private void showSDKDialog(){
//        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Enable Shopping Assistant?");
//        builder.setMessage("Let Buyhatke Shopping Assistant give suggestions and help save more whenever you surf on your favourite shopping app?");
//        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                if (!Buyhatke.isAccessibilityServiceRunning(MainActivity.this))
//                    Buyhatke.setAccessibilityNotification(MainActivity.this);
//                builder.show().dismiss();
//                Toast.makeText(MainActivity.this, "Click on the notification for further steps!", Toast.LENGTH_LONG).show();
//                sharedPref.edit().putBoolean(SDK_NOTIFICATION, true).apply();
//                AutoCart.sendUpdateToServer("SDK", "Yes");
//            }
//        });
//        builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                builder.show().dismiss();
//                sharedPref.edit().putInt(APP_OPEN_COUNT_SDK, 0).apply();
//                AutoCart.sendUpdateToServer("SDK","Later");
//            }
//        });
//        builder.setNeutralButton("Never", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                builder.show().dismiss();
//                AutoCart.sendUpdateToServer("SDK", "Never");
//            }
//        });
//        builder.show();
//    }

    private class GetCurrentVersion extends AsyncTask<Void, Void, Void> {

        private String latestVersion;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Document doc = Jsoup.connect(urlOfApp).get();
                latestVersion = doc.getElementsByAttributeValue
                        ("itemprop", "softwareVersion").first().text();

            } catch (Exception e) {

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (!TextUtils.isEmpty(currentVersion) && !TextUtils.isEmpty(latestVersion)) {
                Log.d("AppVersion", "Current : " + currentVersion + " Latest : " + latestVersion);
                if (currentVersion.compareTo(latestVersion) < 0) {
                    if (!isFinishing()) {
                        showUpdateDialog();
                    }
                }
            }
            super.onPostExecute(aVoid);
        }
    }

    private void showUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New update");
        builder.setMessage("We have changed since we last met. Let's get the updates.");
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
                        ("market://details?id=com.buyhatke.autocart")));
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

}
