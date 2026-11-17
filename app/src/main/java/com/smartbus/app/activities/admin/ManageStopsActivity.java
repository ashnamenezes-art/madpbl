package com.smartbus.app.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.adapters.StopAdapter;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Route;
import com.smartbus.app.models.Stop;
import com.smartbus.app.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManageStopsActivity extends AppCompatActivity {

    private Spinner spinnerRoutes;
    private MaterialButton btnMapPreview;
    private RecyclerView rvStops;
    private ProgressBar progressBar;
    private TextView tvNoStops;
    private FloatingActionButton fabAddStop;

    private StopAdapter stopAdapter;
    private final List<Stop> currentRouteStops = new ArrayList<>();
    private final List<Route> routeList = new ArrayList<>();

    private String selectedRouteId = "";
    private FirebaseHelper firebaseHelper;
    private ValueEventListener routesListener;
    private ValueEventListener stopsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_stops);

        firebaseHelper = FirebaseHelper.getInstance();

        if (getIntent() != null && getIntent().hasExtra(Constants.EXTRA_ROUTE_ID)) {
            selectedRouteId = getIntent().getStringExtra(Constants.EXTRA_ROUTE_ID);
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadRoutes();
    }

    private void initViews() {
        spinnerRoutes = findViewById(R.id.spinner_routes);
        btnMapPreview = findViewById(R.id.btn_map_preview);
        rvStops = findViewById(R.id.rv_stops);
        progressBar = findViewById(R.id.progress_bar);
        tvNoStops = findViewById(R.id.tv_no_stops);
        fabAddStop = findViewById(R.id.fab_add_stop);

        fabAddStop.setOnClickListener(v -> showAddOrEditStopDialog(null));

        btnMapPreview.setOnClickListener(v -> {
            if (TextUtils.isEmpty(selectedRouteId)) {
                Toast.makeText(this, "Please select a route first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(ManageStopsActivity.this, AdminLiveMapActivity.class);
            intent.putExtra(Constants.EXTRA_ROUTE_ID, selectedRouteId);
            startActivity(intent);
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Stops");
        }
    }

    private void setupRecyclerView() {
        stopAdapter = new StopAdapter(this, currentRouteStops, new StopAdapter.OnStopClickListener() {
            @Override
            public void onStopClick(Stop stop) {
                showAddOrEditStopDialog(stop);
            }

            @Override
            public void onEditClick(Stop stop) {
                showAddOrEditStopDialog(stop);
            }

            @Override
            public void onDeleteClick(Stop stop) {
                confirmDeleteStop(stop);
            }
        });

        rvStops.setLayoutManager(new LinearLayoutManager(this));
        rvStops.setAdapter(stopAdapter);
    }

    private void loadRoutes() {
        progressBar.setVisibility(View.VISIBLE);

        routesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                routeList.clear();
                List<String> routeLabels = new ArrayList<>();
                int initialSelection = 0;

                for (DataSnapshot child : snapshot.getChildren()) {
                    Route route = child.getValue(Route.class);
                    if (route != null) {
                        if (route.getRouteId() == null) route.setRouteId(child.getKey());
                        routeList.add(route);
                        String label = (route.getRouteName() != null ? route.getRouteName() : "Route") + " (" + route.getSourceStop() + " → " + route.getDestinationStop() + ")";
                        routeLabels.add(label);

                        if (selectedRouteId != null && selectedRouteId.equals(route.getRouteId())) {
                            initialSelection = routeList.size() - 1;
                        }
                    }
                }

                if (routeList.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    tvNoStops.setText("No routes found.\nPlease create a route first.");
                    tvNoStops.setVisibility(View.VISIBLE);
                    rvStops.setVisibility(View.GONE);
                    return;
                }

                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        ManageStopsActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        routeLabels
                );
                spinnerRoutes.setAdapter(spinnerAdapter);

                if (initialSelection < routeList.size()) {
                    spinnerRoutes.setSelection(initialSelection);
                    selectedRouteId = routeList.get(initialSelection).getRouteId();
                } else if (!routeList.isEmpty()) {
                    selectedRouteId = routeList.get(0).getRouteId();
                }

                spinnerRoutes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position >= 0 && position < routeList.size()) {
                            selectedRouteId = routeList.get(position).getRouteId();
                            loadStopsForRoute(selectedRouteId);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

                if (!TextUtils.isEmpty(selectedRouteId)) {
                    loadStopsForRoute(selectedRouteId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageStopsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        firebaseHelper.getRoutesRef().addValueEventListener(routesListener);
    }

    private void loadStopsForRoute(final String routeId) {
        if (stopsListener != null) {
            firebaseHelper.getStopsRef().removeEventListener(stopsListener);
        }

        progressBar.setVisibility(View.VISIBLE);

        stopsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                currentRouteStops.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Stop stop = child.getValue(Stop.class);
                    if (stop != null) {
                        if (stop.getStopId() == null) stop.setStopId(child.getKey());
                        if (routeId.equals(stop.getRouteId())) {
                            currentRouteStops.add(stop);
                        }
                    }
                }

                // Sort by stopOrder
                Collections.sort(currentRouteStops, (s1, s2) -> Integer.compare(s1.getStopOrder(), s2.getStopOrder()));

                stopAdapter.notifyDataSetChanged();

                if (currentRouteStops.isEmpty()) {
                    tvNoStops.setText("No stops found for this route.\nTap + to add a stop.");
                    tvNoStops.setVisibility(View.VISIBLE);
                    rvStops.setVisibility(View.GONE);
                } else {
                    tvNoStops.setVisibility(View.GONE);
                    rvStops.setVisibility(View.VISIBLE);
                }

                // Update totalStops count on route in Firebase
                firebaseHelper.getRoutesRef().child(routeId).child("totalStops").setValue(currentRouteStops.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageStopsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        firebaseHelper.getStopsRef().addValueEventListener(stopsListener);
    }

    private void showAddOrEditStopDialog(final Stop existingStop) {
        if (TextUtils.isEmpty(selectedRouteId)) {
            Toast.makeText(this, "Please select a route first", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_stop, null);

        EditText etName = dialogView.findViewById(R.id.et_dialog_stop_name);
        EditText etOrder = dialogView.findViewById(R.id.et_dialog_stop_order);
        EditText etLat = dialogView.findViewById(R.id.et_dialog_latitude);
        EditText etLng = dialogView.findViewById(R.id.et_dialog_longitude);
        EditText etAddress = dialogView.findViewById(R.id.et_dialog_address);

        if (existingStop != null) {
            etName.setText(existingStop.getStopName());
            etOrder.setText(String.valueOf(existingStop.getStopOrder()));
            etLat.setText(String.valueOf(existingStop.getLatitude()));
            etLng.setText(String.valueOf(existingStop.getLongitude()));
            etAddress.setText(existingStop.getAddress());
        } else {
            // Suggest next order number
            etOrder.setText(String.valueOf(currentRouteStops.size() + 1));
            etLat.setText(String.valueOf(Constants.DEFAULT_LAT));
            etLng.setText(String.valueOf(Constants.DEFAULT_LNG));
        }

        String title = existingStop == null ? "Add Stop to Route" : "Edit Stop";

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String orderStr = etOrder.getText() != null ? etOrder.getText().toString().trim() : "1";
                    String latStr = etLat.getText() != null ? etLat.getText().toString().trim() : "0";
                    String lngStr = etLng.getText() != null ? etLng.getText().toString().trim() : "0";
                    String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "Stop name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int order = currentRouteStops.size() + 1;
                    try {
                        order = Integer.parseInt(orderStr);
                    } catch (NumberFormatException ignored) {}

                    double latitude = Constants.DEFAULT_LAT;
                    try {
                        latitude = Double.parseDouble(latStr);
                    } catch (NumberFormatException ignored) {}

                    double longitude = Constants.DEFAULT_LNG;
                    try {
                        longitude = Double.parseDouble(lngStr);
                    } catch (NumberFormatException ignored) {}

                    DatabaseReference stopsRef = firebaseHelper.getStopsRef();
                    String stopId = existingStop != null && existingStop.getStopId() != null
                            ? existingStop.getStopId()
                            : stopsRef.push().getKey();

                    Stop stop = new Stop(
                            stopId,
                            name,
                            selectedRouteId,
                            order,
                            latitude,
                            longitude,
                            address
                    );

                    if (stopId != null) {
                        stopsRef.child(stopId).setValue(stop);
                        Toast.makeText(this, existingStop == null ? "Stop added" : "Stop updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteStop(final Stop stop) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Stop")
                .setMessage("Are you sure you want to delete " + (stop.getStopName() != null ? stop.getStopName() : "this stop") + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (stop.getStopId() != null) {
                        firebaseHelper.getStopsRef().child(stop.getStopId()).removeValue();
                        Toast.makeText(this, "Stop deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (routesListener != null) firebaseHelper.getRoutesRef().removeEventListener(routesListener);
        if (stopsListener != null) firebaseHelper.getStopsRef().removeEventListener(stopsListener);
    }
}
