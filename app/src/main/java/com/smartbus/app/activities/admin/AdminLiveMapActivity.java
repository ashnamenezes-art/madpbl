package com.smartbus.app.activities.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.LiveLocation;
import com.smartbus.app.utils.Constants;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminLiveMapActivity extends AppCompatActivity {

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private ImageView ivBack, ivRecenter;
    private TextView tvActiveBusesCount, tvBottomActive, tvBottomLastUpdated;

    private FirebaseHelper firebaseHelper;
    private ValueEventListener liveLocationsListener;

    private final Map<String, Marker> busMarkerMap = new HashMap<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // MapLibre must be initialized before inflating the MapView
        MapLibre.getInstance(this);

        setContentView(R.layout.activity_admin_live_map);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews();
        setupListeners();

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            mapLibreMap = map;
            map.setStyle(new Style.Builder().fromUri(Constants.MAP_STYLE_URL), style -> {
                // Initialize camera at India center
                LatLng centerIndia = new LatLng(Constants.DEFAULT_LAT, Constants.DEFAULT_LNG);
                mapLibreMap.setCameraPosition(new CameraPosition.Builder()
                        .target(centerIndia)
                        .zoom(Constants.DEFAULT_ZOOM)
                        .build());

                // Setup marker click callback to show info window
                mapLibreMap.setOnMarkerClickListener(marker -> {
                    marker.showInfoWindow(mapLibreMap, mapView);
                    return true;
                });

                startTrackingAllBuses();
            });
        });
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        ivBack = findViewById(R.id.iv_back);
        ivRecenter = findViewById(R.id.iv_recenter);
        tvActiveBusesCount = findViewById(R.id.tv_active_buses_count);
        tvBottomActive = findViewById(R.id.tv_bottom_active);
        tvBottomLastUpdated = findViewById(R.id.tv_bottom_last_updated);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        ivRecenter.setOnClickListener(v -> {
            if (mapLibreMap != null) {
                LatLng centerIndia = new LatLng(Constants.DEFAULT_LAT, Constants.DEFAULT_LNG);
                mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(centerIndia)
                                .zoom(Constants.DEFAULT_ZOOM)
                                .build()
                ), 1000);
            }
        });
    }

    private void startTrackingAllBuses() {
        liveLocationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int activeCount = 0;

                for (DataSnapshot child : snapshot.getChildren()) {
                    String busId = child.getKey();
                    LiveLocation loc = child.getValue(LiveLocation.class);

                    if (busId != null && loc != null) {
                        double lat = loc.getLatitude();
                        double lng = loc.getLongitude();

                        // Check if lat/lng are non-zero
                        if (lat != 0.0 && lng != 0.0) {
                            activeCount++;
                            LatLng latLng = new LatLng(lat, lng);

                            String driverInfo = (loc.getDriverId() != null && !loc.getDriverId().isEmpty())
                                    ? ("Driver: " + loc.getDriverId()) : "Driver: Unassigned";
                            String timeStr = loc.getTimestamp() > 0 ? dateFormat.format(new Date(loc.getTimestamp())) : "Just now";
                            String snippet = driverInfo + "\nUpdated: " + timeStr + "\nStatus: " + (loc.getTripStatus() != null ? loc.getTripStatus() : "Active");

                            if (busMarkerMap.containsKey(busId)) {
                                Marker existingMarker = busMarkerMap.get(busId);
                                if (existingMarker != null) {
                                    existingMarker.setPosition(latLng);
                                    existingMarker.setSnippet(snippet);
                                }
                            } else {
                                MarkerOptions options = new MarkerOptions()
                                        .position(latLng)
                                        .title("Bus ID: " + busId)
                                        .snippet(snippet);

                                if (mapLibreMap != null) {
                                    Marker newMarker = mapLibreMap.addMarker(options);
                                    busMarkerMap.put(busId, newMarker);
                                }
                            }
                        }
                    }
                }

                // Update overlay stats
                tvActiveBusesCount.setText(activeCount + " buses currently active");
                tvBottomActive.setText("Total Active Buses: " + activeCount);
                tvBottomLastUpdated.setText("Last updated: " + timeFormat.format(new Date()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminLiveMapActivity.this, "Live tracking error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        firebaseHelper.getLiveLocationsRef().addValueEventListener(liveLocationsListener);
    }

    // ─── MapLibre Lifecycle ──────────────────────────────────────────────────────────

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
        if (liveLocationsListener != null) {
            firebaseHelper.getLiveLocationsRef().removeEventListener(liveLocationsListener);
        }
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroy();
    }
}
