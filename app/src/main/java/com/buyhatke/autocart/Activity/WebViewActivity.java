package com.buyhatke.autocart.Activity;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.buyhatke.autocart.AutoCart;
import com.buyhatke.autocart.R;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.List;

public class WebViewActivity extends AppCompatActivity {

    public static WebView webView;
    private ProgressBar pb;
    private static boolean readNode;
    private List<String> variansts = new ArrayList<>();
    private static int clickID = 0;
    private static final String newUA= "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.107 Safari/537.36";
    private boolean reviewDialogDone = false;
    private static final String MI_URL = "www.mi.com";
    private static final String AMAZON_URL = "www.amazon.in";
    private static final String FLIPKART_URL = "www.flipkart";
    private static String currUrl;
    private static final String AMAZON_TEST_URL = "https://www.amazon.in/gp/goldbox/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pb = (ProgressBar) findViewById(R.id.progress_bar);
        pb.setMax(100);

        webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new MyWebClient());
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.addJavascriptInterface(new MyInterface(),"MyTag");
        webView.getSettings().setUserAgentString(newUA);
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                pb.setProgress(newProgress);
            }
        });
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        String url = getIntent().getStringExtra("url");
        readNode = getIntent().getBooleanExtra("readNode",false);
        webView.loadUrl(url);
        //webView.loadUrl(AMAZON_TEST_URL);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_choose){
            variansts.clear();
            readVariants();
        } else if (item.getItemId() == R.id.action_refresh){
            webView.loadUrl("javascript:window.location.reload()");
        }
        return super.onOptionsItemSelected(item);
    }

    public static void loadAndRead(String url) {
        webView.loadUrl(url);
        readNode = true;
    }

    public class MyInterface{

        @JavascriptInterface
        public void addItem(String name){

            if (TextUtils.isEmpty(name)){
                showDialog();
            } else if (name.equals("Reload")){
                Toast.makeText(WebViewActivity.this, "Reloading page to try adding to cart again.", Toast.LENGTH_SHORT).show();
                AutoCart.sendUpdateToServer("Reloading", variansts.get(clickID));
            } else if (name.equals("Success")){
                if (!reviewDialogDone){
                    AutoCart.sendUpdateToServer("SUCCESS", variansts.get(clickID));
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.showReviewDialog(WebViewActivity.this);
                        }
                    },5 * 1000);
                    reviewDialogDone = true;
                }
            } else {
                variansts.add(name.trim());
            }
        }

    }

    private void showDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Choose Variant");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, variansts);
        alert.setSingleChoiceItems(adapter, clickID, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clickID = which;
                webView.post(new Runnable() {
                    @Override
                    public void run() {
                        applyClick();
                    }
                });
                Toast.makeText(WebViewActivity.this, variansts.get(which)+ " selected!", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
        alert.show();
    }

    private class MyWebClient extends WebViewClient{

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            currUrl = url;
            pb.setVisibility(View.VISIBLE);
            applyClick();
        }

        @Override
        public void onPageFinished(WebView view, final String url) {
            super.onPageFinished(view, url);
            pb.setVisibility(View.GONE);
            if (readNode) {
                readNode = false;
                variansts.clear();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!url.contains("signin"))
                            readVariants();
                    }
                },5*1000);
            } else {
                applyClick();
            }
        }
    }

    public static void applyClick(){
        if (currUrl.contains(AMAZON_URL)) applyAmazonClick();
        else if (currUrl.contains(FLIPKART_URL)) applyFlipkartClick();
        else if (currUrl.contains(MI_URL)) applyMiClick();
    }

    private void readVariants() {
        webView.loadUrl("javascript:var len = document.querySelectorAll('.dealTile').length;" +
                "for (var i = 0; i < len; i++){" +
                "    var name = document.querySelectorAll('.dealTile')[i].querySelectorAll('.singleCellTitle')[0].innerText;" +
                "    window.MyTag.addItem(name);" +
                "}" +
                "window.MyTag.addItem('')");
    }

    private static void applyFlipkartClick(){

    }

    private static void applyMiClick(){

    }

    private static void applyAmazonClick(){
        webView.loadUrl("javascript: var clickID = '"+clickID+"';" +
                "function addToCart(){" +
                "   someClick = setInterval(function(){" +
                "       if(document.querySelectorAll('.dealTile').length > clickID){" +
                "           if(document.querySelectorAll('.dealTile')[clickID].querySelectorAll('button').length > 0){" +
                "               document.querySelectorAll('.dealTile')[clickID].querySelectorAll('button')[0].click();" +
                "               clearInterval(someClick);" +
                "               checkAdded();" +
                "           }" +
                "       }" +
                "   }, 200)" +
                "}" +
                "function checkAdded(){" +
                "   setTimeout(function(){checkAdded()},1000);" +
                "   if(document.querySelectorAll('.a-alert-content').length>0){" +
                "       if(document.querySelectorAll('.a-alert-content')[0].innerText.split('in your Cart').length > 0){" +
                "           localStorage.saleTried = 2;" +
                "           window.MyTag.addItem('Success');" +
                "       }" +
                "   }" +
                "   else if(document.querySelectorAll('.dealStatusMessageHolder').length > 0){" +
                "           setTimeout(function(){" +
                "               if(document.querySelectorAll('.dealStatusMessageHolder')[0].innerText.split('Checking Deal').length > 0){" +
                "                   window.MyTag.addItem('Reload');" +
                "                   window.location.reload();" +
                "               }" +
                "           },10*1000);" +
                "   }" +
                "}" +
                "addToCart();");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack())
            webView.goBack();
        else
            finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_webactivity, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tracker tracker = AutoCart.getInstance().getDefaultTracker();
        tracker.setScreenName("WebViewActivity");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

}
