package com.buyhatke.autocart;

/**
 * Created by Abhishek on 03-Oct-17.
 */

public class Constants {
    // global topic to receive app wide push notifications
    public static final String TOPIC_GLOBAL = "global";

    // broadcast receiver intent filters
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    public static final String PUSH_NOTIFICATION = "pushNotification";

    // id to handle the notification in the notification tray
    public static final int NOTIFICATION_ID = 100;
    public static final int NOTIFICATION_ID_BIG_IMAGE = 101;

    public static final String SHARED_PREF = "autobuy_sharedPref";

    final public static String REGISTER_URL = "https://compare.buyhatke.com/application/registerApp.php?platform=android";
    final public static String SET_TOKEN_URL = "http://buyhatke.com/application/setToken.php?platform=android";
    final public static String SALE_URL = "https://compare.buyhatke.com/options/flashSaleNew.php";
}
