package com.smartbus.app.database;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.smartbus.app.utils.Constants;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private static FirebaseHelper instance;
    private final FirebaseAuth auth;
    private final FirebaseDatabase database;

    private FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    // ─── Auth ────────────────────────────────────────────────────────────────

    public FirebaseAuth getAuth() {
        return auth;
    }

    public void signOut() {
        auth.signOut();
    }

    /** @return current user UID, or null if not signed in */
    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    /** Alias kept for backward-compat */
    public String getCurrentUid() {
        return getCurrentUserId();
    }

    // ─── Generic ref ─────────────────────────────────────────────────────────

    public DatabaseReference getRef(String path) {
        return database.getReference(path);
    }

    // ─── Node References ─────────────────────────────────────────────────────

    public DatabaseReference getUsersRef() {
        return database.getReference(Constants.NODE_USERS);
    }

    public DatabaseReference getPassengersRef() {
        return database.getReference(Constants.NODE_PASSENGERS);
    }

    public DatabaseReference getDriversRef() {
        return database.getReference(Constants.NODE_DRIVERS);
    }

    public DatabaseReference getBusesRef() {
        return database.getReference(Constants.NODE_BUSES);
    }

    public DatabaseReference getRoutesRef() {
        return database.getReference(Constants.NODE_ROUTES);
    }

    public DatabaseReference getStopsRef() {
        return database.getReference(Constants.NODE_STOPS);
    }

    public DatabaseReference getLiveLocationsRef() {
        return database.getReference(Constants.NODE_LIVE_LOCATIONS);
    }

    public DatabaseReference getTripsRef() {
        return database.getReference(Constants.NODE_TRIPS);
    }

    public DatabaseReference getFeedbackRef() {
        return database.getReference(Constants.NODE_FEEDBACK);
    }

    public DatabaseReference getComplaintsRef() {
        return database.getReference(Constants.NODE_COMPLAINTS);
    }

    public DatabaseReference getTicketsRef() {
        return database.getReference(Constants.NODE_TICKETS);
    }

    // ─── Trip Helpers ─────────────────────────────────────────────────────────

    /**
     * Creates a new trip record in Firebase and returns the generated trip ID.
     */
    public String startTrip(String busId, String driverId, String routeId) {
        DatabaseReference tripRef = getTripsRef().push();
        String tripId = tripRef.getKey();

        Map<String, Object> tripData = new HashMap<>();
        tripData.put("tripId",   tripId);
        tripData.put("busId",    busId);
        tripData.put("driverId", driverId);
        tripData.put("routeId",  routeId != null ? routeId : "");
        tripData.put("status",   Constants.TRIP_ACTIVE);
        tripData.put("startTime", System.currentTimeMillis());
        tripData.put("endTime",  0L);

        tripRef.setValue(tripData);
        return tripId;
    }

    /**
     * Marks a trip as completed and sets bus back to inactive.
     */
    public void endTrip(String tripId, String busId) {
        if (tripId != null && !tripId.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status",  Constants.TRIP_COMPLETED);
            updates.put("endTime", System.currentTimeMillis());
            getTripsRef().child(tripId).updateChildren(updates);
        }
        if (busId != null && !busId.isEmpty()) {
            getBusesRef().child(busId).child("isActive").setValue(false);
        }
    }

    /**
     * Marks a trip as cancelled.
     */
    public void cancelTrip(String tripId) {
        if (tripId != null && !tripId.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status",  Constants.TRIP_CANCELLED);
            updates.put("endTime", System.currentTimeMillis());
            getTripsRef().child(tripId).updateChildren(updates);
        }
    }

    // ─── Live Location Helpers ────────────────────────────────────────────────

    /**
     * Full 7-argument overload used by LocationUpdateService.
     */
    public void updateLiveLocation(String busId, String driverId,
                                   double lat, double lng,
                                   String tripStatus, float speed, float heading) {
        if (busId == null || busId.isEmpty()) return;

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("busId",      busId);
        locationData.put("driverId",   driverId != null ? driverId : "");
        locationData.put("latitude",   lat);
        locationData.put("longitude",  lng);
        locationData.put("timestamp",  System.currentTimeMillis());
        locationData.put("tripStatus", tripStatus != null ? tripStatus : Constants.TRIP_ACTIVE);
        locationData.put("speed",      speed);
        locationData.put("heading",    heading);

        getLiveLocationsRef().child(busId).setValue(locationData);
    }

    /**
     * Simple 4-argument overload (lat/lng only).
     */
    public void updateLiveLocation(String busId, String driverId, double lat, double lng) {
        updateLiveLocation(busId, driverId, lat, lng, Constants.TRIP_ACTIVE, 0f, 0f);
    }
}
