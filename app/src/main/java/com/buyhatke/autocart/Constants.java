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
    public static final String REFERRER= "referrer";

    final public static String REGISTER_URL = "https://compare.buyhatke.com/application/registerApp.php?platform=autocart";
    final public static String SET_TOKEN_URL = "https://compare.buyhatke.com/application/setToken.php?platform=autocart";
    final public static String SALE_URL = "https://compare.buyhatke.com/options/flashSaleNew.php";
    final public static String FEEDBACK_URL = "https://goo.gl/forms/J22S1m65wDMq8qir1";
}
