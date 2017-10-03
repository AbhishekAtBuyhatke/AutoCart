package com.buyhatke.autocart;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.android.volley.toolbox.Authenticator;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private RequestQueue queue;
    private static final String SALE_URL = "https://compare.buyhatke.com/options/flashSaleNew.php";
    private RecyclerView rvAmazon, rvFlipkart, rvMi;
    private TextView tvMiSaleInfo, tvFlipkartInfo, tv_amazon_info, tv_amazon, tvFlipkart, tvInstructionText;
    private ProgressDialog pd;
    private ImageView ivInstruction;
    private LinearLayout llInstructionContainer;
    private boolean instrOpen;
    private static final String REQUEST_TAG = "downloadSaleData";

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
        super.onPause();
        queue.cancelAll(REQUEST_TAG);
        if (pd.isShowing())
            pd.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tracker tracker = AutoCart.getInstance().getDefaultTracker();
        tracker.setScreenName("MainActivity");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}
