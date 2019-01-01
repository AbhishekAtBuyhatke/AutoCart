package com.buyhatke.autocart.Adapter;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.buyhatke.autocart.Activity.WebViewActivity;
import com.buyhatke.autocart.AutoCart;
import com.buyhatke.autocart.Models.SaleItem;
import com.buyhatke.autocart.Models.Variants;
import com.buyhatke.autocart.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Abhishek on 26-Aug-17.
 */

public class SaleAdapter extends RecyclerView.Adapter<SaleAdapter.ViewHolder> {

    private SaleItem[] saleItems;
    private String[] allVariants;
    private Context context;
    private SQLiteDatabase db;
    private static final String SCRIPT_ENABLED = "ScriptEnabled";
    private static final String ALERT_SET = "AlertSet";
    private static final String ALERT_UNSET = "AlertUnSet";
    private String SITE;
    public static final String SITE_AMAZON = "amazon";
    public static final String SITE_FLIPKART = "flipkart";
    public static final String SITE_MI = "mi";


    public SaleAdapter(SaleItem[] saleItems, String[] allVariants, Context context, String SITE) {
        this.saleItems = saleItems;
        this.allVariants = allVariants;
        this.context = context;
        this.SITE = SITE;
        db = context.openOrCreateDatabase("SaleItem.db", Context.MODE_PRIVATE, null);
        db.execSQL("create table if not exists itemlist(" +
                "id varchar(10) PRIMARY KEY," +
                "name varchar(30))");
    }

    @Override
    public SaleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sale_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SaleAdapter.ViewHolder holder, int position) {
        final SaleItem item = saleItems[position];
        final String variants = allVariants[position];
        holder.tv_item_name.setText(item.getTitle());
        Cursor cursor = db.rawQuery("select name from itemlist where id = '" + item.getCode() + "'", null);

        if (cursor.getCount() > 0)
            holder.switch_item.setChecked(true);
        cursor.close();

        long telTime = item.getTime() * 1000L;
        long currTime = System.currentTimeMillis();

        if (telTime < currTime) {
            holder.switch_item.setChecked(false);
            holder.switch_item.setEnabled(false);
            db.execSQL("delete from itemlist where id = '" + item.getCode() + "'");
        }

        Date date = new Date(telTime);
        SimpleDateFormat sdf = new SimpleDateFormat("EE, hh:mm a");
        String detail = sdf.format(date);
        holder.tv_item_time_details.setText(detail);

        holder.switch_item.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final int exact_time = item.getTime();
                if (isChecked) {
                    if ((exact_time * 1000L) < System.currentTimeMillis()) {
                        Toast.makeText(context, "Sale time not updated on server! Please check back later.", Toast.LENGTH_SHORT).show();
                        holder.switch_item.setChecked(false);
                    } else {
                        AutoCart.sendUpdateToServer(ALERT_SET, item.getTitle());
                        ContentValues cv = new ContentValues();
                        cv.put("id", item.getCode());
                        cv.put("name", item.getTitle());
                        db.replace("itemlist", null, cv);
                        String msg = "10 minutes remaining for flash sale. Make sure you have an active account for the required website and you are successfully logged in.";
                        setNotification(context, msg, exact_time - (10 * 60), item.getUrl());
                        checkAndOpenUrl(context, exact_time - (3 * 60), item.getUrl());
                        clickOnBookBtn(context, exact_time - 30, item.getCode());
                        Toast.makeText(context, "Successfully added in wishlist!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    AutoCart.sendUpdateToServer(ALERT_UNSET, item.getTitle());
                    db.execSQL("delete from itemlist where id = '" + item.getCode() + "'");
                    cancelAlarm(exact_time - (10 * 60));
                    cancelAlarm(exact_time - (3 * 60));
                    cancelAlarm(exact_time - 30);
                    Toast.makeText(context, "Successfully removed from wishlist!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        holder.tv_item_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, WebViewActivity.class);
                intent.putExtra("url", item.getUrl());
                intent.putExtra("variants", variants);
                context.startActivity(intent);
            }
        });
    }

    private void cancelAlarm(int epoch) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MyReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, epoch, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
    }

    public static void checkAndOpenUrl(Context context, int epoch, String url) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        long epochInMili = epoch * 1000L;
        calendar.setTimeInMillis(epochInMili);
        Intent intent = new Intent(context, MyReceiver.class);
        intent.putExtra("url", url);
        intent.putExtra("msg", "3 minutes remaining. Click here to land on booking page.");
        intent.putExtra("reopen", true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, epoch, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    public static void clickOnBookBtn(Context context, int epoch, String code) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        long epochInMili = epoch * 1000L;
        calendar.setTimeInMillis(epochInMili);
        Intent intent = new Intent(context, MyReceiver.class);
        intent.putExtra("itemId", code);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, epoch, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    @Override
    public int getItemCount() {
        return saleItems.length;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_item_name, tv_item_time_details;
        Switch switch_item;

        ViewHolder(final View itemView) {
            super(itemView);
            tv_item_name = (TextView) itemView.findViewById(R.id.tv_item_name);
            tv_item_time_details = (TextView) itemView.findViewById(R.id.tv_item_time_details);
            switch_item = (Switch) itemView.findViewById(R.id.switch_item_active);
        }
    }

    public static void setNotification(Context context, String msg, int epoch, String url) {
        Log.d("Timestamp", "" + epoch);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        long epochInMili = epoch * 1000L;
        calendar.setTimeInMillis(epochInMili);
        Intent intent = new Intent(context, MyReceiver.class);
        intent.putExtra("url", url);
        intent.putExtra("msg", msg);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, epoch, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    public static class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String url = intent.getStringExtra("url");
            if (TextUtils.isEmpty(url)) { //30 second to sale
                String code = intent.getStringExtra("itemId");
                SQLiteDatabase db = context.openOrCreateDatabase("SaleItem.db", Context.MODE_PRIVATE, null);
                Cursor cursor = db.rawQuery("select name from itemlist where id = '" + code + "'", null);
                cursor.moveToLast();
                String itemName = cursor.getString(0);
                if (!TextUtils.isEmpty(code)) {
                    db.execSQL("delete from itemlist where id = '" + code + "'");
                }
                if (WebViewActivity.webView != null) { //applyClick if webview open
                    WebViewActivity.applyClick(context);
                    Toast.makeText(context, "Auto-Click script enabled!", Toast.LENGTH_LONG).show();
                    AutoCart.sendUpdateToServer(SCRIPT_ENABLED, itemName);
                } else {
                    AutoCart.sendUpdateToServer(SCRIPT_ENABLED, "App Not Open");
                }
            } else { //send notification
                String msg = intent.getStringExtra("msg");
                if (intent.getBooleanExtra("reopen", false)) { //3-minute to sale
                    if (WebViewActivity.webView == null) {
                        sendNotification(msg, context, url, true);
                    } else {
                        Toast.makeText(context, "3 minutes remaining. Opening product page.", Toast.LENGTH_SHORT).show();
                        WebViewActivity.loadAndRead(url);
                    }
                } else { //10-minute to sale
                    sendNotification(msg, context, url, false);
                }
            }
        }

        private void sendNotification(String msg, Context context, String url, boolean readNode) {
            Intent intent = new Intent(context, WebViewActivity.class);
            intent.putExtra("url", url);
            intent.putExtra("readNode", readNode);
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context);
            mBuilder
                    .setContentTitle("Time for flash sale!")
                    .setContentText(msg)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                    .setAutoCancel(true)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setColor(context.getResources().getColor(R.color.colorPrimary))
                    .setContentIntent(pendingIntent)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setOngoing(true)
                    .setSound(soundUri)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(101, mBuilder.build());
        }
    }
}
