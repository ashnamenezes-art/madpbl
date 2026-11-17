package com.smartbus.app.activities.driver;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.activities.RoleSelectionActivity;
import com.smartbus.app.adapters.DriverStopAdapter;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Bus;
import com.smartbus.app.models.Driver;
import com.smartbus.app.models.Route;
import com.smartbus.app.models.Stop;
import com.smartbus.app.services.LocationUpdateService;
import com.smartbus.app.utils.Constants;
import com.smartbus.app.utils.PermissionUtils;
import com.smartbus.app.utils.SharedPrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class DriverHomeActivity extends AppCompatActivity {

    private static final int RC_LOCATION_PERMISSION = 1001;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private CircleImageView ivDriverProfile;
    private TextView tvDriverName, tvLicenseNumber;
    private Chip chipDutyStatus;
    private TextView tvBusNumber, tvRouteName, tvTotalStops, tvBusType;
    private TextView tvTripBadge, tvTripStartTime, tvLocationSharingStatus;
    private LinearLayout layoutTripDetails;
    private MaterialButton btnStartTrip, btnEndTrip, btnShareLocation, btnOpenMap;
    private RecyclerView rvStops;
    private TextView tvNoStops;

    private FirebaseHelper firebaseHelper;
    private SharedPrefManager prefManager;
    private DriverStopAdapter stopAdapter;

    private String driverId;
    private String busId;
    private String routeId;
    private String tripId;
    private boolean isTripActive = false;
    private boolean isLocationSharing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        firebaseHelper = FirebaseHelper.getInstance();
        prefManager = SharedPrefManager.getInstance(this);

        driverId = prefManager.getUserId();
        busId = prefManager.getBusId();
        routeId = prefManager.getRouteId();
        tripId = prefManager.getTripId();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();

        loadAllData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        ivDriverProfile = findViewById(R.id.ivDriverProfile);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvLicenseNumber = findViewById(R.id.tvLicenseNumber);
        chipDutyStatus = findViewById(R.id.chipDutyStatus);

        tvBusNumber = findViewById(R.id.tvBusNumber);
        tvRouteName = findViewById(R.id.tvRouteName);
        tvTotalStops = findViewById(R.id.tvTotalStops);
        tvBusType = findViewById(R.id.tvBusType);

        tvTripBadge = findViewById(R.id.tvTripBadge);
        layoutTripDetails = findViewById(R.id.layoutTripDetails);
        tvTripStartTime = findViewById(R.id.tvTripStartTime);
        tvLocationSharingStatus = findViewById(R.id.tvLocationSharingStatus);

        btnStartTrip = findViewById(R.id.btnStartTrip);
        btnEndTrip = findViewById(R.id.btnEndTrip);
        btnShareLocation = findViewById(R.id.btnShareLocation);
        btnOpenMap = findViewById(R.id.btnOpenMap);

        rvStops = findViewById(R.id.rvStops);
        tvNoStops = findViewById(R.id.tvNoStops);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(true);
            }
        }
    }

    private void setupRecyclerView() {
        stopAdapter = new DriverStopAdapter(this);
        rvStops.setLayoutManager(new LinearLayoutManager(this));
        rvStops.setAdapter(stopAdapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadAllData);

        btnStartTrip.setOnClickListener(v -> handleStartTripClick());
        btnEndTrip.setOnClickListener(v -> handleEndTripClick());
        btnShareLocation.setOnClickListener(v -> toggleLocationSharing());
        btnOpenMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(DriverHomeActivity.this, DriverMapActivity.class);
            startActivity(mapIntent);
        });
    }

    private void loadAllData() {
        swipeRefresh.setRefreshing(true);
        loadDriverData();
    }

    private void loadDriverData() {
        if (driverId == null) {
            driverId = firebaseHelper.getCurrentUserId();
        }

        if (driverId == null) {
            swipeRefresh.setRefreshing(false);
            logout();
            return;
        }

        firebaseHelper.getDriversRef().child(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Driver driver = snapshot.getValue(Driver.class);
                if (driver != null) {
                    tvDriverName.setText(driver.getName() != null ? driver.getName() : "Driver");
                    tvLicenseNumber.setText("Lic: " + (driver.getLicenseNumber() != null ? driver.getLicenseNumber() : "N/A"));

                    if (toolbar != null) {
                        toolbar.setSubtitle("Hello, " + (driver.getName() != null ? driver.getName() : "Driver"));
                    }

                    if (driver.getProfileImage() != null && !driver.getProfileImage().isEmpty()) {
                        Glide.with(DriverHomeActivity.this)
                                .load(driver.getProfileImage())
                                .placeholder(R.drawable.ic_driver)
                                .into(ivDriverProfile);
                    }

                    busId = driver.getAssignedBusId();
                    routeId = driver.getAssignedRouteId();

                    prefManager.setBusId(busId);
                    prefManager.setRouteId(routeId);

                    updateDutyUI(driver.isOnDuty());
                    loadBusData();
                } else {
                    swipeRefresh.setRefreshing(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefresh.setRefreshing(false);
                showSnackbar("Failed to load driver: " + error.getMessage());
            }
        });
    }

    private void loadBusData() {
        if (busId == null || busId.isEmpty()) {
            tvBusNumber.setText("Not Assigned");
            tvRouteName.setText("No Route");
            tvTotalStops.setText("0 stops");
            loadRouteStops(null);
            swipeRefresh.setRefreshing(false);
            return;
        }

        firebaseHelper.getBusesRef().child(busId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Bus bus = snapshot.getValue(Bus.class);
                if (bus != null) {
                    tvBusNumber.setText(bus.getBusNumber() != null ? bus.getBusNumber() : busId);
                    tvBusType.setText(bus.getBusType() != null ? bus.getBusType() : "Standard");
                    if (bus.getRouteId() != null) {
                        routeId = bus.getRouteId();
                        prefManager.setRouteId(routeId);
                    }
                }
                loadRouteData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void loadRouteData() {
        if (routeId == null || routeId.isEmpty()) {
            tvRouteName.setText("No Route Assigned");
            loadRouteStops(null);
            swipeRefresh.setRefreshing(false);
            return;
        }

        firebaseHelper.getRoutesRef().child(routeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Route route = snapshot.getValue(Route.class);
                if (route != null) {
                    tvRouteName.setText(route.getRouteName() != null ? route.getRouteName() : routeId);
                    tvTotalStops.setText(route.getTotalStops() + " stops");
                }
                loadRouteStops(routeId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void loadRouteStops(String rId) {
        if (rId == null || rId.isEmpty()) {
            stopAdapter.setStops(new ArrayList<>());
            tvNoStops.setVisibility(View.VISIBLE);
            swipeRefresh.setRefreshing(false);
            checkActiveTrip();
            return;
        }

        Query stopsQuery = firebaseHelper.getStopsRef().orderByChild("routeId").equalTo(rId);
        stopsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                swipeRefresh.setRefreshing(false);
                List<Stop> stops = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Stop s = child.getValue(Stop.class);
                    if (s != null) {
                        stops.add(s);
                    }
                }

                Collections.sort(stops, (a, b) -> Integer.compare(a.getStopOrder(), b.getStopOrder()));
                stopAdapter.setStops(stops);

                tvNoStops.setVisibility(stops.isEmpty() ? View.VISIBLE : View.GONE);
                checkActiveTrip();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefresh.setRefreshing(false);
                checkActiveTrip();
            }
        });
    }

    private void checkActiveTrip() {
        tripId = prefManager.getTripId();
        if (tripId != null && !tripId.isEmpty()) {
            firebaseHelper.getTripsRef().child(tripId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String status = snapshot.child("status").getValue(String.class);
                    Long startTime = snapshot.child("startTime").getValue(Long.class);
                    if (Constants.TRIP_ACTIVE.equalsIgnoreCase(status)) {
                        setTripActiveState(true, startTime != null ? startTime : System.currentTimeMillis());
                    } else {
                        setTripActiveState(false, 0);
                        prefManager.setTripId(null);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    setTripActiveState(false, 0);
                }
            });
        } else {
            setTripActiveState(false, 0);
        }
    }

    private void handleStartTripClick() {
        if (!PermissionUtils.hasLocationPermission(this)) {
            PermissionUtils.requestLocationPermission(this, RC_LOCATION_PERMISSION);
            return;
        }

        if (busId == null || busId.isEmpty()) {
            showSnackbar("No bus assigned. Please contact the administrator.");
            return;
        }

        if (isTripActive) {
            showSnackbar("Trip is already active.");
            return;
        }

        // Start Trip in Firebase
        tripId = firebaseHelper.startTrip(busId, driverId, routeId);
        prefManager.setTripId(tripId);

        // Update database statuses
        firebaseHelper.getBusesRef().child(busId).child("isActive").setValue(true);
        if (driverId != null) {
            firebaseHelper.getDriversRef().child(driverId).child("isOnDuty").setValue(true);
        }

        // Start Location service
        startLocationService();

        setTripActiveState(true, System.currentTimeMillis());
        showSnackbar("Trip started. Location sharing active.");
    }

    private void handleEndTripClick() {
        if (tripId != null && !tripId.isEmpty()) {
            firebaseHelper.endTrip(tripId, busId);
        }

        if (busId != null) {
            firebaseHelper.getBusesRef().child(busId).child("isActive").setValue(false);
        }

        if (driverId != null) {
            firebaseHelper.getDriversRef().child(driverId).child("isOnDuty").setValue(false);
        }

        stopLocationService();
        prefManager.setTripId(null);
        tripId = null;

        setTripActiveState(false, 0);
        showSnackbar("Trip ended.");
    }

    private void toggleLocationSharing() {
        if (!isTripActive) {
            Toast.makeText(this, "Start a trip first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLocationSharing) {
            stopLocationService();
            showSnackbar("Location sharing paused.");
        } else {
            if (!PermissionUtils.hasLocationPermission(this)) {
                PermissionUtils.requestLocationPermission(this, RC_LOCATION_PERMISSION);
                return;
            }
            startLocationService();
            showSnackbar("Location sharing resumed.");
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        serviceIntent.setAction(Constants.ACTION_START_LOCATION);
        serviceIntent.putExtra(Constants.EXTRA_BUS_ID, busId);
        serviceIntent.putExtra(Constants.EXTRA_DRIVER_ID, driverId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            startService(serviceIntent);
        }
        isLocationSharing = true;
        updateLocationSharingUI();
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        serviceIntent.setAction(Constants.ACTION_STOP_LOCATION);
        startService(serviceIntent);
        isLocationSharing = false;
        updateLocationSharingUI();
    }

    private void setTripActiveState(boolean active, long startTime) {
        isTripActive = active;
        btnStartTrip.setVisibility(active ? View.GONE : View.VISIBLE);
        btnEndTrip.setVisibility(active ? View.VISIBLE : View.GONE);
        layoutTripDetails.setVisibility(active ? View.VISIBLE : View.GONE);

        if (active) {
            tvTripBadge.setText("Active Trip");
            tvTripBadge.setTextColor(ContextCompat.getColor(this, R.color.driver_color));
            tvTripBadge.setBackgroundColor(ContextCompat.getColor(this, R.color.white));

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            tvTripStartTime.setText("Started: " + sdf.format(new Date(startTime)));
            isLocationSharing = true;
            updateDutyUI(true);
        } else {
            tvTripBadge.setText("No Active Trip");
            tvTripBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            updateDutyUI(false);
        }
        updateLocationSharingUI();
    }

    private void updateDutyUI(boolean onDuty) {
        if (onDuty) {
            chipDutyStatus.setText("On Duty");
            chipDutyStatus.setChipBackgroundColorResource(R.color.driver_color);
        } else {
            chipDutyStatus.setText("Off Duty");
            chipDutyStatus.setChipBackgroundColorResource(R.color.text_secondary);
        }
    }

    private void updateLocationSharingUI() {
        if (isLocationSharing) {
            btnShareLocation.setText("Stop Sharing Location");
            btnShareLocation.setTextColor(ContextCompat.getColor(this, R.color.sos_red));
            btnShareLocation.setStrokeColorResource(R.color.sos_red);
            btnShareLocation.setIconTintResource(R.color.sos_red);
            tvLocationSharingStatus.setText("GPS Live");
            tvLocationSharingStatus.setTextColor(ContextCompat.getColor(this, R.color.driver_color));
        } else {
            btnShareLocation.setText("Share Live Location");
            btnShareLocation.setTextColor(ContextCompat.getColor(this, R.color.primary));
            btnShareLocation.setStrokeColorResource(R.color.primary);
            btnShareLocation.setIconTintResource(R.color.primary);
            tvLocationSharingStatus.setText("GPS Paused");
            tvLocationSharingStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private void logout() {
        if (isTripActive) {
            handleEndTripClick();
        }
        firebaseHelper.signOut();
        prefManager.clearSession();
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_driver_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_driver_map) {
            startActivity(new Intent(this, DriverMapActivity.class));
            return true;
        } else if (id == R.id.action_driver_profile) {
            startActivity(new Intent(this, DriverProfileActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_LOCATION_PERMISSION) {
            if (PermissionUtils.hasLocationPermission(this)) {
                handleStartTripClick();
            } else {
                showSnackbar("Location permission is required to broadcast bus coordinates.");
            }
        }
    }
}
