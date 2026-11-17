package com.smartbus.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.smartbus.app.models.User;

public class SharedPrefManager {

    private static final String PREF_NAME = Constants.PREF_NAME;
    private static final String KEY_IS_LOGGED_IN = Constants.KEY_IS_LOGGED_IN;
    private static final String KEY_USER_ID = Constants.KEY_USER_ID;
    private static final String KEY_USER_NAME = Constants.KEY_USER_NAME;
    private static final String KEY_USER_EMAIL = Constants.KEY_USER_EMAIL;
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_USER_ROLE = Constants.KEY_USER_ROLE;
    private static final String KEY_BUS_ID = Constants.KEY_BUS_ID;
    private static final String KEY_ROUTE_ID = Constants.KEY_ROUTE_ID;
    private static final String KEY_TRIP_ID = Constants.EXTRA_TRIP_ID;

    private static SharedPrefManager instance;
    private static Context appContext;
    private final SharedPreferences prefs;

    private SharedPrefManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    public static synchronized SharedPrefManager getInstance() {
        if (instance == null) {
            if (appContext != null) {
                instance = new SharedPrefManager(appContext);
            } else {
                throw new IllegalStateException("SharedPrefManager not initialized with Context yet. Call getInstance(Context) first.");
            }
        }
        return instance;
    }

    // ─── Instance Methods ─────────────────────────────────────────────────────

    public void saveUserSession(String uid, String name, String email, String phone, String role) {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ID, uid)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_PHONE, phone)
                .putString(KEY_USER_ROLE, role)
                .apply();
    }

    public void saveLoginSession(User user) {
        if (user == null) return;
        saveUserSession(user.getUid(), user.getName(), user.getEmail(), user.getPhone(), user.getRole());
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "User");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserPhone() {
        return prefs.getString(KEY_USER_PHONE, "");
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "passenger");
    }

    public void setBusId(String busId) {
        prefs.edit().putString(KEY_BUS_ID, busId).apply();
    }

    public String getBusId() {
        return prefs.getString(KEY_BUS_ID, null);
    }

    public void setRouteId(String routeId) {
        prefs.edit().putString(KEY_ROUTE_ID, routeId).apply();
    }

    public String getRouteId() {
        return prefs.getString(KEY_ROUTE_ID, null);
    }

    public void setTripId(String tripId) {
        prefs.edit().putString(KEY_TRIP_ID, tripId).apply();
    }

    public String getTripId() {
        return prefs.getString(KEY_TRIP_ID, null);
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    // ─── Static Convenience Methods ──────────────────────────────────────────

    public static boolean isLoggedIn(Context context) {
        return getInstance(context).isLoggedIn();
    }

    public static void saveLoginSession(Context context, User user) {
        getInstance(context).saveLoginSession(user);
    }

    public static String getUserId(Context context) {
        return getInstance(context).getUserId();
    }

    public static String getUserName(Context context) {
        return getInstance(context).getUserName();
    }

    public static String getUserEmail(Context context) {
        return getInstance(context).getUserEmail();
    }

    public static String getUserRole(Context context) {
        return getInstance(context).getUserRole();
    }

    public static String getBusId(Context context) {
        return getInstance(context).getBusId();
    }

    public static void setBusId(Context context, String busId) {
        getInstance(context).setBusId(busId);
    }

    public static String getRouteId(Context context) {
        return getInstance(context).getRouteId();
    }

    public static void setRouteId(Context context, String routeId) {
        getInstance(context).setRouteId(routeId);
    }

    public static String getTripId(Context context) {
        return getInstance(context).getTripId();
    }

    public static void setTripId(Context context, String tripId) {
        getInstance(context).setTripId(tripId);
    }

    public static void clearSession(Context context) {
        getInstance(context).clearSession();
    }
}
