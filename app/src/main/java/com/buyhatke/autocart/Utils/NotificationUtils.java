package com.buyhatke.autocart.Utils;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.Patterns;

import com.buyhatke.autocart.Constants;
import com.buyhatke.autocart.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Abhishek on 03-Oct-17.
 */

public class NotificationUtils {

    private static final String TAG = "NotificationUtils";
    private Context context;

    public NotificationUtils(Context context){
        this.context = context;
    }

    public void showNotificationMessage(String title, String message, String timeStamp, Intent intent){
        showNotificationMessage(title, message, timeStamp, intent, null);
    }

    public void showNotificationMessage(String title, String message, String timeStamp, Intent intent, String imageUrl) {
        if (TextUtils.isEmpty(message))
            return;
        final int icon = R.mipmap.ic_launcher;
        final PendingIntent pendingIntent = PendingIntent.getActivity(context,0,intent, PendingIntent.FLAG_CANCEL_CURRENT);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        if (!TextUtils.isEmpty(imageUrl)){
            if (imageUrl.length() > 4 && Patterns.WEB_URL.matcher(imageUrl).matches()){
                Bitmap bitmap = getBitmapFromURL(imageUrl);
                if (bitmap != null)
                    showBigNotification(bitmap, builder, icon, title, message, timeStamp, pendingIntent);
                else
                    showSmallNotification(builder, icon, title, message, timeStamp, pendingIntent);
            }
        } else
            showSmallNotification(builder, icon, title, message, timeStamp, pendingIntent);
    }

    private void showSmallNotification(NotificationCompat.Builder builder, int icon, String title, String message, String timeStamp, PendingIntent pendingIntent) {
        Notification notification;
        notification = builder.setSmallIcon(icon).setTicker(title).setWhen(0)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setTicker(title)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), icon))
                .setContentText(message)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(Constants.NOTIFICATION_ID, notification);
    }

    private void showBigNotification(Bitmap bitmap, NotificationCompat.Builder builder, int icon, String title, String message, String timeStamp, PendingIntent pendingIntent) {

        Notification notification = builder.setSmallIcon(icon).setTicker(title).setWhen(0)
                .setAutoCancel(true)
                .setTicker(title)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), icon))
                .setContentText(message)
                .build();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(Constants.NOTIFICATION_ID, notification);
    }

    public static boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }

    public static void clearNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    private Bitmap getBitmapFromURL(String imageUrl) {
        try{
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

}
