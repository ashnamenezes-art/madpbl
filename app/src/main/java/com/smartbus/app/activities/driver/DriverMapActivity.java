package com.smartbus.app.activities.driver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Bus;
import com.smartbus.app.models.Route;
import com.smartbus.app.models.Stop;
import com.smartbus.app.services.LocationUpdateService;
import com.smartbus.app.utils.Constants;
import com.smartbus.app.utils.GeoJsonUtils;
import com.smartbus.app.utils.PermissionUtils;
import com.smartbus.app.utils.SharedPrefManager;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.Property;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DriverMapActivity extends AppCompatActivity {

    private static final int RC_LOCATION = 1002;
    private static final String SOURCE_ID = "driver-route-source";
    private static final String LAYER_ID = "driver-route-layer";

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private Style currentStyle;

    private ImageView ivMapBack;
    private ImageView ivRecenter;
    private TextView tvMapRouteName;
    private TextView tvMapBusInfo;
    private TextView tvNextStop;
    private TextView tvSpeed;
    private View viewGpsDot;
    private TextView tvGpsStatus;
    private MaterialButton btnToggleSharing;

    private FirebaseHelper firebaseHelper;
    private SharedPrefManager prefManager;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker driverMarker;
    private LatLng lastDriverLatLng;

    private String routeId;
    private String busId;
    private String driverId;
    private List<Stop> routeStops = new ArrayList<>();
    private boolean isSharing = true;
    private boolean hasCenteredFirstTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // MapLibre must be initialized before inflating the view
        MapLibre.getInstance(this);

        setContentView(R.layout.activity_driver_map);

        firebaseHelper = FirebaseHelper.getInstance();
        prefManager = SharedPrefManager.getInstance(this);

        routeId = prefManager.getRouteId();
        busId = prefManager.getBusId();
        driverId = prefManager.getUserId();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupListeners();

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            mapLibreMap = map;
            map.setStyle(new Style.Builder().fromUri(Constants.MAP_STYLE_URL), style -> {
                currentStyle = style;
                loadRouteDetails();
                loadRouteStops();
                startDriverLocationUpdates();
            });
        });
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        ivMapBack = findViewById(R.id.ivMapBack);
        ivRecenter = findViewById(R.id.ivRecenter);
        tvMapRouteName = findViewById(R.id.tvMapRouteName);
        tvMapBusInfo = findViewById(R.id.tvMapBusInfo);
        tvNextStop = findViewById(R.id.tvNextStop);
        tvSpeed = findViewById(R.id.tvSpeed);
        viewGpsDot = findViewById(R.id.viewGpsDot);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);
        btnToggleSharing = findViewById(R.id.btnToggleSharing);
    }

    private void setupListeners() {
        ivMapBack.setOnClickListener(v -> finish());

        ivRecenter.setOnClickListener(v -> {
            if (lastDriverLatLng != null && mapLibreMap != null) {
                mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(lastDriverLatLng)
                                .zoom(16.0)
                                .build()
                ), 1000);
            } else {
                Toast.makeText(this, "Acquiring GPS location…", Toast.LENGTH_SHORT).show();
            }
        });

        btnToggleSharing.setOnClickListener(v -> toggleLocationSharingService());
    }

    private void loadRouteDetails() {
        if (busId != null && !busId.isEmpty()) {
            firebaseHelper.getBusesRef().child(busId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Bus bus = snapshot.getValue(Bus.class);
                    if (bus != null) {
                        tvMapBusInfo.setText("Bus #" + bus.getBusNumber() + " • Active Navigation");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        if (routeId != null && !routeId.isEmpty()) {
            firebaseHelper.getRoutesRef().child(routeId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Route route = snapshot.getValue(Route.class);
                    if (route != null && route.getRouteName() != null) {
                        tvMapRouteName.setText(route.getRouteName());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void loadRouteStops() {
        if (routeId == null || routeId.isEmpty()) {
            tvNextStop.setText("No Route Assigned");
            return;
        }

        Query query = firebaseHelper.getStopsRef().orderByChild("routeId").equalTo(routeId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                routeStops.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Stop stop = child.getValue(Stop.class);
                    if (stop != null) {
                        routeStops.add(stop);
                    }
                }

                Collections.sort(routeStops, (s1, s2) -> Integer.compare(s1.getStopOrder(), s2.getStopOrder()));

                if (!routeStops.isEmpty()) {
                    tvNextStop.setText(routeStops.get(0).getStopName());
                } else {
                    tvNextStop.setText("No stops on route");
                }

                renderStopsOnMap();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverMapActivity.this, "Failed to load stops", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderStopsOnMap() {
        if (mapLibreMap == null || currentStyle == null || routeStops.isEmpty()) return;

        // Add stop markers
        for (Stop stop : routeStops) {
            mapLibreMap.addMarker(new MarkerOptions()
                    .position(new LatLng(stop.getLatitude(), stop.getLongitude()))
                    .title(stop.getStopName())
                    .snippet("Stop #" + stop.getStopOrder()));
        }

        // Draw Route Line
        String geoJson = GeoJsonUtils.createRouteGeoJson(routeStops);
        if (currentStyle.getSource(SOURCE_ID) == null) {
            GeoJsonSource source = new GeoJsonSource(SOURCE_ID, geoJson);
            currentStyle.addSource(source);

            LineLayer lineLayer = new LineLayer(LAYER_ID, SOURCE_ID);
            lineLayer.setProperties(
                    PropertyFactory.lineColor(Color.parseColor("#2E7D32")),
                    PropertyFactory.lineWidth(5.5f),
                    PropertyFactory.lineOpacity(0.9f),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND)
            );
            currentStyle.addLayer(lineLayer);
        }

        // Center on the first stop if driver location is not yet known
        if (!hasCenteredFirstTime && !routeStops.isEmpty()) {
            Stop firstStop = routeStops.get(0);
            mapLibreMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder()
                            .target(new LatLng(firstStop.getLatitude(), firstStop.getLongitude()))
                            .zoom(13.5)
                            .build()
            ));
        }
    }

    private void startDriverLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    RC_LOCATION);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        updateDriverLocation(location);
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateDriverLocation(Location location) {
        lastDriverLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (mapLibreMap != null) {
            if (driverMarker == null) {
                driverMarker = mapLibreMap.addMarker(new MarkerOptions()
                        .position(lastDriverLatLng)
                        .title("My Bus")
                        .snippet("Live GPS Position"));
            } else {
                driverMarker.setPosition(lastDriverLatLng);
            }

            if (!hasCenteredFirstTime) {
                hasCenteredFirstTime = true;
                mapLibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(lastDriverLatLng)
                                .zoom(16.0)
                                .build()
                ), 1000);
            }
        }

        // Speed
        float speedKmh = location.hasSpeed() ? (location.getSpeed() * 3.6f) : 0f;
        tvSpeed.setText(String.format(Locale.US, "%.0f km/h", speedKmh));

        // Update closest next stop
        updateNextStop(location.getLatitude(), location.getLongitude());
    }

    private void updateNextStop(double currentLat, double currentLng) {
        if (routeStops == null || routeStops.isEmpty()) return;

        Stop closestStop = null;
        float minDistance = Float.MAX_VALUE;

        for (Stop stop : routeStops) {
            float[] results = new float[1];
            Location.distanceBetween(currentLat, currentLng, stop.getLatitude(), stop.getLongitude(), results);
            if (results[0] < minDistance) {
                minDistance = results[0];
                closestStop = stop;
            }
        }

        if (closestStop != null) {
            if (minDistance < 1000) {
                tvNextStop.setText(closestStop.getStopName() + String.format(Locale.US, " (%.0fm)", minDistance));
            } else {
                tvNextStop.setText(closestStop.getStopName() + String.format(Locale.US, " (%.1fkm)", minDistance / 1000f));
            }
        }
    }

    private void toggleLocationSharingService() {
        if (isSharing) {
            Intent serviceIntent = new Intent(this, LocationUpdateService.class);
            serviceIntent.setAction(Constants.ACTION_STOP_LOCATION);
            startService(serviceIntent);

            isSharing = false;
            btnToggleSharing.setText("Start Sharing");
            btnToggleSharing.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.driver_color));
            viewGpsDot.setBackgroundResource(R.drawable.bg_status_inactive);
            tvGpsStatus.setText("GPS Paused");
            tvGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            Toast.makeText(this, "Live location sharing paused", Toast.LENGTH_SHORT).show();
        } else {
            if (!PermissionUtils.hasLocationPermission(this)) {
                PermissionUtils.requestLocationPermission(this, RC_LOCATION);
                return;
            }

            Intent serviceIntent = new Intent(this, LocationUpdateService.class);
            serviceIntent.setAction(Constants.ACTION_START_LOCATION);
            serviceIntent.putExtra(Constants.EXTRA_BUS_ID, busId);
            serviceIntent.putExtra(Constants.EXTRA_DRIVER_ID, driverId);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent);
            } else {
                startService(serviceIntent);
            }

            isSharing = true;
            btnToggleSharing.setText("Stop Sharing");
            btnToggleSharing.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.sos_red));
            viewGpsDot.setBackgroundResource(R.drawable.bg_status_active);
            tvGpsStatus.setText("GPS Active");
            tvGpsStatus.setTextColor(ContextCompat.getColor(this, R.color.driver_color));
            Toast.makeText(this, "Live location sharing active", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_LOCATION) {
            if (PermissionUtils.hasLocationPermission(this)) {
                startDriverLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission required for navigation", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ─── MapLibre Lifecycle ───────────────────────────────────────────────────

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
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (mapView != null) mapView.onDestroy();
        super.onDestroy();
    }
}
