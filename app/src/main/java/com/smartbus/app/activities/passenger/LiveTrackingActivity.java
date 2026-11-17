package com.smartbus.app.activities.passenger;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.maps.MapManager;
import com.smartbus.app.models.Bus;
import com.smartbus.app.models.Driver;
import com.smartbus.app.models.LiveLocation;
import com.smartbus.app.models.Stop;
import com.smartbus.app.utils.Constants;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;

import java.util.ArrayList;
import java.util.List;

public class LiveTrackingActivity extends AppCompatActivity {

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private MapManager mapManager;
    private FirebaseHelper firebaseHelper;

    private String busId;
    private ValueEventListener locationListener;
    private ValueEventListener busDetailsListener;
    private Marker busMarker;

    // Bottom info views
    private TextView tvBusNumber, tvRouteName, tvDriverName, tvETA, tvStatus;
    private FloatingActionButton fabBack, fabShareLocation;

    private double currentLat = 0.0;
    private double currentLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_live_tracking);

        busId = getIntent().getStringExtra(Constants.EXTRA_BUS_ID);
        firebaseHelper = FirebaseHelper.getInstance();

        // Map
        mapView = findViewById(R.id.mapView);
        if (mapView != null) mapView.onCreate(savedInstanceState);

        // Views
        tvBusNumber = findViewById(R.id.tvBusNumber);
        tvRouteName = findViewById(R.id.tvRouteName);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvETA = findViewById(R.id.tvETA);
        tvStatus = findViewById(R.id.tvStatus);

        fabBack = findViewById(R.id.fabBack);
        if (fabBack != null) {
            fabBack.setOnClickListener(v -> finish());
        }

        fabShareLocation = findViewById(R.id.fabShareLocation);
        if (fabShareLocation != null) {
            fabShareLocation.setOnClickListener(v -> shareBusLocation());
        }

        if (mapView != null) {
            mapView.getMapAsync(map -> {
                this.mapLibreMap = map;
                mapManager = new MapManager(this, map);
                map.setStyle(Constants.MAP_STYLE_URL, style -> {
                    // Set initial camera to India centre
                    map.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(Constants.DEFAULT_LAT, Constants.DEFAULT_LNG))
                                    .zoom(Constants.DEFAULT_ZOOM)
                                    .build()
                    ));
                    loadBusInfo();
                    startLocationTracking();
                });
            });
        }
    }

    private void loadBusInfo() {
        if (busId == null || busId.isEmpty()) return;

        busDetailsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Bus bus = snapshot.getValue(Bus.class);
                if (bus != null) {
                    if (tvBusNumber != null) {
                        tvBusNumber.setText(bus.getBusNumber() != null ? "Bus #" + bus.getBusNumber() : "Bus");
                    }
                    if (tvRouteName != null) {
                        tvRouteName.setText(bus.getBusName() != null ? bus.getBusName() : "Route: " + bus.getRouteId());
                    }
                    if (tvStatus != null) {
                        tvStatus.setText(bus.isActive() ? "● Active" : "● Inactive");
                    }

                    // Load driver details if available
                    if (bus.getDriverId() != null && !bus.getDriverId().isEmpty()) {
                        loadDriverInfo(bus.getDriverId());
                    }

                    // Load route stops & draw path
                    if (bus.getRouteId() != null && !bus.getRouteId().isEmpty()) {
                        loadRouteStops(bus.getRouteId());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        firebaseHelper.getBusesRef().child(busId).addValueEventListener(busDetailsListener);
    }

    private void loadDriverInfo(String driverId) {
        firebaseHelper.getRef(Constants.NODE_DRIVERS).child(driverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Driver driver = snapshot.getValue(Driver.class);
                        if (driver != null && tvDriverName != null) {
                            tvDriverName.setText(driver.getName() != null ? driver.getName() : "Assigned Driver");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadRouteStops(String routeId) {
        firebaseHelper.getStopsRef().orderByChild("routeId").equalTo(routeId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Stop> stops = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Stop stop = child.getValue(Stop.class);
                            if (stop != null) stops.add(stop);
                        }
                        if (mapLibreMap != null && mapLibreMap.getStyle() != null && mapManager != null) {
                            mapManager.drawRoute(stops, mapLibreMap.getStyle());
                            mapManager.addStopMarkers(stops);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @SuppressLint("SetTextI18n")
    private void startLocationTracking() {
        if (busId == null || busId.isEmpty()) return;

        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                LiveLocation loc = snapshot.getValue(LiveLocation.class);
                if (loc != null && mapLibreMap != null) {
                    currentLat = loc.getLatitude();
                    currentLng = loc.getLongitude();

                    LatLng position = new LatLng(currentLat, currentLng);
                    if (busMarker == null) {
                        busMarker = mapLibreMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title("Bus #" + busId)
                                .snippet("Live Location"));
                    } else {
                        busMarker.setPosition(position);
                    }

                    mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(position)
                                    .zoom(Constants.TRACKING_ZOOM)
                                    .build()), 1000);

                    if (tvETA != null) {
                        tvETA.setText("Live tracking active");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LiveTrackingActivity.this, "Failed to track location: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        firebaseHelper.getLiveLocationsRef().child(busId).addValueEventListener(locationListener);
    }

    private void shareBusLocation() {
        String shareText;
        if (currentLat != 0.0 && currentLng != 0.0) {
            shareText = "Track Bus " + (busId != null ? busId : "") + " live at: https://www.google.com/maps/search/?api=1&query=" + currentLat + "," + currentLng;
        } else {
            shareText = "Tracking Bus " + (busId != null ? busId : "") + " on Smart Bus Assistant!";
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Live Bus Tracking");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Bus Location"));
    }

    // ─── MapView Lifecycle ─────────────────────────────────────────────────────

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationListener != null && busId != null) {
            firebaseHelper.getLiveLocationsRef().child(busId).removeEventListener(locationListener);
        }
        if (busDetailsListener != null && busId != null) {
            firebaseHelper.getBusesRef().child(busId).removeEventListener(busDetailsListener);
        }
        if (mapView != null) mapView.onDestroy();
    }
}
