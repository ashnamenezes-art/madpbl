package com.smartbus.app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public final class PermissionUtils {

    private PermissionUtils() {}

    // ─── Fine + Coarse Location ───────────────────────────────────────────────

    public static boolean hasLocationPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                requestCode
        );
    }

    // ─── Background Location (Android 10+) ───────────────────────────────────

    public static boolean hasBackgroundLocationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // On Android 9 and below, ACCESS_FINE_LOCATION covers background use
            return hasLocationPermission(activity);
        }
        return ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestBackgroundLocationPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    requestCode
            );
        }
        // On older Android, background location is implicitly granted with fine location
    }

    // ─── Notification (Android 13+) ───────────────────────────────────────────

    public static boolean hasNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // POST_NOTIFICATIONS not required before Android 13
            return true;
        }
        return ContextCompat.checkSelfPermission(activity,
                Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestNotificationPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    requestCode
            );
        }
    }
}
