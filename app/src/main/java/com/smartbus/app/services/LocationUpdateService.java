package com.smartbus.app.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.smartbus.app.R;
import com.smartbus.app.activities.driver.DriverHomeActivity;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.utils.Constants;
import com.smartbus.app.utils.SharedPrefManager;

public class LocationUpdateService extends Service {

    private static final String TAG = "LocationUpdateService";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseHelper firebaseHelper;
    private String busId;
    private String driverId;
    private String tripStatus = Constants.TRIP_ACTIVE;
    private boolean isUpdating = false;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firebaseHelper = FirebaseHelper.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && Constants.ACTION_STOP_LOCATION.equals(intent.getAction())) {
            stopLocationUpdates();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null && Constants.ACTION_START_LOCATION.equals(intent.getAction())) {
            SharedPrefManager sp = SharedPrefManager.getInstance(this);
            busId = sp.getBusId();
            driverId = sp.getUserId();

            if (intent.hasExtra(Constants.EXTRA_BUS_ID)) {
                busId = intent.getStringExtra(Constants.EXTRA_BUS_ID);
            }
            if (intent.hasExtra(Constants.EXTRA_DRIVER_ID)) {
                driverId = intent.getStringExtra(Constants.EXTRA_DRIVER_ID);
            }

            createNotificationChannel();
            Notification notification = createNotification();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(Constants.NOTIFICATION_ID_LOCATION, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(Constants.NOTIFICATION_ID_LOCATION, notification);
            }

            startLocationUpdates();
            return START_STICKY;
        }

        return START_STICKY;
    }

    private void startLocationUpdates() {
        if (isUpdating) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted in service");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, Constants.LOCATION_UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(3000)
                .setWaitForAccurateLocation(false)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        float speed = location.hasSpeed() ? location.getSpeed() * 3.6f : 0f; // convert m/s to km/h
                        float heading = location.hasBearing() ? location.getBearing() : 0f;

                        if (busId != null && !busId.isEmpty()) {
                            firebaseHelper.updateLiveLocation(
                                    busId,
                                    driverId != null ? driverId : "",
                                    lat,
                                    lng,
                                    tripStatus,
                                    speed,
                                    heading
                            );

                            // Also update bus lastKnown coordinates
                            firebaseHelper.getBusesRef().child(busId).child("currentLatitude").setValue(lat);
                            firebaseHelper.getBusesRef().child(busId).child("currentLongitude").setValue(lng);
                            firebaseHelper.getBusesRef().child(busId).child("lastUpdated").setValue(System.currentTimeMillis());
                        }
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        isUpdating = true;
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null && isUpdating) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isUpdating = false;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.CHANNEL_ID_LOCATION,
                    Constants.CHANNEL_NAME_LOCATION,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows notification when live bus location is being broadcasted.");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, DriverHomeActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, Constants.CHANNEL_ID_LOCATION)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Sharing live location for route tracking...")
                .setSmallIcon(R.drawable.ic_location)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
