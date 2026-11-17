package com.smartbus.app.activities.passenger;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.smartbus.app.R;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.utils.PermissionUtils;
import com.smartbus.app.utils.SharedPrefManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SosActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2001;

    private View viewRingOuter, viewRingMiddle;
    private Button btnSOS, btnShareLocation, btnCallEmergency;
    private ImageButton btnBack;

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseHelper firebaseHelper;
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        firebaseHelper = FirebaseHelper.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        viewRingOuter = findViewById(R.id.viewRingOuter);
        viewRingMiddle = findViewById(R.id.viewRingMiddle);
        btnSOS = findViewById(R.id.btnSOS);
        btnShareLocation = findViewById(R.id.btnShareLocation);
        btnCallEmergency = findViewById(R.id.btnCallEmergency);

        startPulseAnimation();
        checkAndFetchLocation();

        if (btnSOS != null) {
            btnSOS.setOnClickListener(v -> confirmAndSendSOS());
        }

        if (btnShareLocation != null) {
            btnShareLocation.setOnClickListener(v -> shareLocationViaIntent());
        }

        if (btnCallEmergency != null) {
            btnCallEmergency.setOnClickListener(v -> {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:112"));
                startActivity(dialIntent);
            });
        }
    }

    private void startPulseAnimation() {
        if (viewRingOuter != null) {
            ObjectAnimator pulseOuter = ObjectAnimator.ofPropertyValuesHolder(
                    viewRingOuter,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.15f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.15f),
                    PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0.1f)
            );
            pulseOuter.setDuration(1200);
            pulseOuter.setRepeatCount(ValueAnimator.INFINITE);
            pulseOuter.setRepeatMode(ValueAnimator.REVERSE);
            pulseOuter.start();
        }

        if (viewRingMiddle != null) {
            ObjectAnimator pulseMiddle = ObjectAnimator.ofPropertyValuesHolder(
                    viewRingMiddle,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.1f),
                    PropertyValuesHolder.ofFloat(View.ALPHA, 0.7f, 0.2f)
            );
            pulseMiddle.setDuration(1200);
            pulseMiddle.setStartDelay(200);
            pulseMiddle.setRepeatCount(ValueAnimator.INFINITE);
            pulseMiddle.setRepeatMode(ValueAnimator.REVERSE);
            pulseMiddle.start();
        }
    }

    private void checkAndFetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLastLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchLastLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLastLocation();
            } else {
                Toast.makeText(this, "Location permission is needed for accurate SOS emergency dispatch", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void confirmAndSendSOS() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Emergency SOS")
                .setMessage("Are you sure you want to broadcast an emergency alert to transport authorities and emergency services?")
                .setPositiveButton("SEND SOS NOW", (dialog, which) -> triggerEmergencyAlert())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void triggerEmergencyAlert() {
        String passengerId = SharedPrefManager.getUserId(this);
        if (passengerId == null) passengerId = firebaseHelper.getCurrentUid();
        if (passengerId == null) passengerId = "anonymous_passenger";

        String passengerName = SharedPrefManager.getUserName(this);
        String alertId = "SOS_" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> sosData = new HashMap<>();
        sosData.put("alertId", alertId);
        sosData.put("type", "SOS");
        sosData.put("passengerId", passengerId);
        sosData.put("passengerName", passengerName);
        sosData.put("latitude", currentLat);
        sosData.put("longitude", currentLng);
        sosData.put("timestamp", System.currentTimeMillis());
        sosData.put("status", "ACTIVE_EMERGENCY");
        sosData.put("message", "Immediate assistance requested by passenger " + passengerName);

        firebaseHelper.getFeedbackRef().child(alertId).setValue(sosData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SosActivity.this, "SOS Emergency Alert Broadcasted!", Toast.LENGTH_LONG).show();
                    new AlertDialog.Builder(SosActivity.this)
                            .setTitle("Emergency Alert Sent")
                            .setMessage("Your live coordinates (" + currentLat + ", " + currentLng + ") and alert have been transmitted to dispatchers. Remain calm and stay where you are.")
                            .setPositiveButton("Call Police (100)", (d, w) -> {
                                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:100"));
                                startActivity(dial);
                            })
                            .setNegativeButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(SosActivity.this, "Failed to send SOS: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void shareLocationViaIntent() {
        String shareMsg = "EMERGENCY: I need help! My current location is: https://www.google.com/maps/search/?api=1&query=" + currentLat + "," + currentLng;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "EMERGENCY SOS ALERT");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMsg);
        startActivity(Intent.createChooser(shareIntent, "Share Emergency Location"));
    }
}
