package com.smartbus.app.utils;

public final class Constants {

    private Constants() {}

    // ─── Firebase Node Names ───────────────────────────────────────────────────
    public static final String NODE_USERS          = "users";
    public static final String NODE_PASSENGERS     = "passengers";
    public static final String NODE_DRIVERS        = "drivers";
    public static final String NODE_BUSES          = "buses";
    public static final String NODE_ROUTES         = "routes";
    public static final String NODE_STOPS          = "stops";
    public static final String NODE_LIVE_LOCATIONS = "liveLocations";
    public static final String NODE_TRIPS          = "trips";
    public static final String NODE_FEEDBACK       = "feedback";
    public static final String NODE_COMPLAINTS     = "complaints";
    public static final String NODE_TICKETS        = "tickets";

    // ─── User Roles ────────────────────────────────────────────────────────────
    public static final String ROLE_PASSENGER = "passenger";
    public static final String ROLE_DRIVER    = "driver";
    public static final String ROLE_ADMIN     = "admin";

    // ─── Trip Status ───────────────────────────────────────────────────────────
    public static final String TRIP_ACTIVE    = "active";
    public static final String TRIP_COMPLETED = "completed";
    public static final String TRIP_CANCELLED = "cancelled";

    // ─── Complaint Status ──────────────────────────────────────────────────────
    public static final String COMPLAINT_PENDING   = "pending";
    public static final String COMPLAINT_IN_REVIEW = "in_review";
    public static final String COMPLAINT_RESOLVED  = "resolved";

    // ─── SharedPreferences Keys ────────────────────────────────────────────────
    public static final String PREF_NAME       = "SmartBusPrefs";
    public static final String KEY_USER_ID     = "userId";
    public static final String KEY_USER_ROLE   = "userRole";
    public static final String KEY_USER_NAME   = "userName";
    public static final String KEY_USER_EMAIL  = "userEmail";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    public static final String KEY_BUS_ID      = "busId";
    public static final String KEY_ROUTE_ID    = "routeId";

    // ─── Map Config ────────────────────────────────────────────────────────────
    public static final String MAP_STYLE_URL =
            "https://demotiles.maplibre.org/style.json";
    // Tile-based fallback:
    // "https://api.maptiler.com/maps/streets/style.json?key=YOUR_KEY"

    public static final double DEFAULT_LAT   = 20.5937; // India centre
    public static final double DEFAULT_LNG   = 78.9629;
    public static final double DEFAULT_ZOOM  = 5.0;
    public static final double TRACKING_ZOOM = 15.0;

    // ─── Location Service ──────────────────────────────────────────────────────
    public static final String ACTION_START_LOCATION = "ACTION_START_LOCATION";
    public static final String ACTION_STOP_LOCATION  = "ACTION_STOP_LOCATION";
    public static final int    LOCATION_UPDATE_INTERVAL    = 5000; // ms
    public static final int    NOTIFICATION_ID_LOCATION    = 1001;
    public static final String CHANNEL_ID_LOCATION   = "location_channel";
    public static final String CHANNEL_NAME_LOCATION  = "Live Location Sharing";

    // ─── Intent Extras ─────────────────────────────────────────────────────────
    public static final String EXTRA_BUS_ID    = "busId";
    public static final String EXTRA_ROUTE_ID  = "routeId";
    public static final String EXTRA_TRIP_ID   = "tripId";
    public static final String EXTRA_DRIVER_ID = "driverId";
    public static final String EXTRA_USER_ROLE = "userRole";
}
