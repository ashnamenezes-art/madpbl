package com.smartbus.app.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.adapters.AdminBusAdapter;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Bus;
import com.smartbus.app.models.Driver;
import com.smartbus.app.models.Route;
import com.smartbus.app.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class ManageBusesActivity extends AppCompatActivity {

    private EditText etSearchBus;
    private RecyclerView rvBuses;
    private ProgressBar progressBar;
    private TextView tvNoBuses;
    private FloatingActionButton fabAddBus;

    private AdminBusAdapter busAdapter;
    private final List<Bus> allBuses = new ArrayList<>();
    private final List<Bus> filteredBuses = new ArrayList<>();

    // Cached for dialog spinners
    private final List<Route> availableRoutes = new ArrayList<>();
    private final List<Driver> availableDrivers = new ArrayList<>();

    private FirebaseHelper firebaseHelper;
    private ValueEventListener busesListener;
    private ValueEventListener routesListener;
    private ValueEventListener driversListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_buses);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        loadData();
    }

    private void initViews() {
        etSearchBus = findViewById(R.id.et_search_bus);
        rvBuses = findViewById(R.id.rv_buses);
        progressBar = findViewById(R.id.progress_bar);
        tvNoBuses = findViewById(R.id.tv_no_buses);
        fabAddBus = findViewById(R.id.fab_add_bus);

        fabAddBus.setOnClickListener(v -> showAddOrEditBusDialog(null));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Buses");
        }
    }

    private void setupRecyclerView() {
        busAdapter = new AdminBusAdapter(this, filteredBuses, new AdminBusAdapter.OnAdminBusActionListener() {
            @Override
            public void onBusClick(Bus bus) {
                showAddOrEditBusDialog(bus);
            }

            @Override
            public void onEditClick(Bus bus) {
                showAddOrEditBusDialog(bus);
            }

            @Override
            public void onDeleteClick(Bus bus) {
                confirmDeleteBus(bus);
            }

            @Override
            public void onStatusToggled(Bus bus, boolean isActive) {
                if (bus.getBusId() != null) {
                    firebaseHelper.getBusesRef().child(bus.getBusId()).child("isActive").setValue(isActive);
                    Toast.makeText(ManageBusesActivity.this, "Bus status updated", Toast.LENGTH_SHORT).show();
                }
            }
        });

        rvBuses.setLayoutManager(new LinearLayoutManager(this));
        rvBuses.setAdapter(busAdapter);
    }

    private void setupSearch() {
        etSearchBus.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBuses(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterBuses(String query) {
        filteredBuses.clear();
        String lowerQuery = query.toLowerCase().trim();

        if (TextUtils.isEmpty(lowerQuery)) {
            filteredBuses.addAll(allBuses);
        } else {
            for (Bus bus : allBuses) {
                boolean matchNumber = bus.getBusNumber() != null && bus.getBusNumber().toLowerCase().contains(lowerQuery);
                boolean matchName = bus.getBusName() != null && bus.getBusName().toLowerCase().contains(lowerQuery);
                boolean matchRoute = bus.getRouteId() != null && bus.getRouteId().toLowerCase().contains(lowerQuery);
                if (matchNumber || matchName || matchRoute) {
                    filteredBuses.add(bus);
                }
            }
        }

        busAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredBuses.isEmpty()) {
            tvNoBuses.setVisibility(View.VISIBLE);
            rvBuses.setVisibility(View.GONE);
        } else {
            tvNoBuses.setVisibility(View.GONE);
            rvBuses.setVisibility(View.VISIBLE);
        }
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);

        // Load Routes for spinner
        routesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                availableRoutes.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Route route = child.getValue(Route.class);
                    if (route != null) {
                        if (route.getRouteId() == null) route.setRouteId(child.getKey());
                        availableRoutes.add(route);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        firebaseHelper.getRoutesRef().addValueEventListener(routesListener);

        // Load Drivers for spinner
        driversListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                availableDrivers.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Driver driver = child.getValue(Driver.class);
                    if (driver != null) {
                        if (driver.getUid() == null) driver.setUid(child.getKey());
                        availableDrivers.add(driver);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        firebaseHelper.getDriversRef().addValueEventListener(driversListener);

        // Load Buses
        busesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                allBuses.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Bus bus = child.getValue(Bus.class);
                    if (bus != null) {
                        if (bus.getBusId() == null) bus.setBusId(child.getKey());
                        allBuses.add(bus);
                    }
                }
                filterBuses(etSearchBus.getText() != null ? etSearchBus.getText().toString() : "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageBusesActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        firebaseHelper.getBusesRef().addValueEventListener(busesListener);
    }

    private void showAddOrEditBusDialog(final Bus existingBus) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_bus, null);

        EditText etNumber = dialogView.findViewById(R.id.et_dialog_bus_number);
        EditText etName = dialogView.findViewById(R.id.et_dialog_bus_name);
        EditText etSeats = dialogView.findViewById(R.id.et_dialog_total_seats);
        Spinner spType = dialogView.findViewById(R.id.sp_dialog_bus_type);
        Spinner spRoute = dialogView.findViewById(R.id.sp_dialog_route);
        Spinner spDriver = dialogView.findViewById(R.id.sp_dialog_driver);
        SwitchMaterial switchActive = dialogView.findViewById(R.id.switch_dialog_active);

        // Bus types
        String[] busTypes = {"Standard", "AC Express", "Electric", "Deluxe", "Mini Bus", "Double Decker"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, busTypes);
        spType.setAdapter(typeAdapter);

        // Routes list
        List<String> routeDisplayList = new ArrayList<>();
        routeDisplayList.add("-- None --");
        int selectedRoutePos = 0;
        for (int i = 0; i < availableRoutes.size(); i++) {
            Route r = availableRoutes.get(i);
            String label = (r.getRouteName() != null ? r.getRouteName() : "Route") + " (" + r.getRouteId() + ")";
            routeDisplayList.add(label);
            if (existingBus != null && r.getRouteId() != null && r.getRouteId().equals(existingBus.getRouteId())) {
                selectedRoutePos = i + 1;
            }
        }
        ArrayAdapter<String> routeSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, routeDisplayList);
        spRoute.setAdapter(routeSpinnerAdapter);
        spRoute.setSelection(selectedRoutePos);

        // Drivers list
        List<String> driverDisplayList = new ArrayList<>();
        driverDisplayList.add("-- None --");
        int selectedDriverPos = 0;
        for (int i = 0; i < availableDrivers.size(); i++) {
            Driver d = availableDrivers.get(i);
            String label = (d.getName() != null ? d.getName() : "Driver") + " (" + d.getUid() + ")";
            driverDisplayList.add(label);
            if (existingBus != null && d.getUid() != null && d.getUid().equals(existingBus.getDriverId())) {
                selectedDriverPos = i + 1;
            }
        }
        ArrayAdapter<String> driverSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, driverDisplayList);
        spDriver.setAdapter(driverSpinnerAdapter);
        spDriver.setSelection(selectedDriverPos);

        // Pre-fill if editing
        if (existingBus != null) {
            etNumber.setText(existingBus.getBusNumber());
            etName.setText(existingBus.getBusName());
            etSeats.setText(String.valueOf(existingBus.getTotalSeats()));
            switchActive.setChecked(existingBus.isActive());

            if (existingBus.getBusType() != null) {
                for (int i = 0; i < busTypes.length; i++) {
                    if (busTypes[i].equalsIgnoreCase(existingBus.getBusType())) {
                        spType.setSelection(i);
                        break;
                    }
                }
            }
        }

        String dialogTitle = existingBus == null ? "Add New Bus" : "Edit Bus";

        new MaterialAlertDialogBuilder(this)
                .setTitle(dialogTitle)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String busNumber = etNumber.getText() != null ? etNumber.getText().toString().trim() : "";
                    String busName = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String seatsStr = etSeats.getText() != null ? etSeats.getText().toString().trim() : "40";
                    String busType = spType.getSelectedItem() != null ? spType.getSelectedItem().toString() : "Standard";
                    boolean isActive = switchActive.isChecked();

                    if (TextUtils.isEmpty(busNumber)) {
                        Toast.makeText(this, "Bus number is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int totalSeats = 40;
                    try {
                        totalSeats = Integer.parseInt(seatsStr);
                    } catch (NumberFormatException ignored) {}

                    String selectedRouteId = "";
                    int routePos = spRoute.getSelectedItemPosition();
                    if (routePos > 0 && (routePos - 1) < availableRoutes.size()) {
                        selectedRouteId = availableRoutes.get(routePos - 1).getRouteId();
                    }

                    String selectedDriverId = "";
                    int driverPos = spDriver.getSelectedItemPosition();
                    if (driverPos > 0 && (driverPos - 1) < availableDrivers.size()) {
                        selectedDriverId = availableDrivers.get(driverPos - 1).getUid();
                    }

                    DatabaseReference busesRef = firebaseHelper.getBusesRef();
                    String busId = existingBus != null && existingBus.getBusId() != null
                            ? existingBus.getBusId()
                            : busesRef.push().getKey();

                    double lat = existingBus != null ? existingBus.getCurrentLatitude() : Constants.DEFAULT_LAT;
                    double lng = existingBus != null ? existingBus.getCurrentLongitude() : Constants.DEFAULT_LNG;

                    Bus bus = new Bus(
                            busId,
                            busNumber,
                            busName,
                            selectedRouteId,
                            selectedDriverId,
                            totalSeats,
                            busType,
                            isActive,
                            lat,
                            lng,
                            System.currentTimeMillis()
                    );

                    if (busId != null) {
                        busesRef.child(busId).setValue(bus);
                        Toast.makeText(this, existingBus == null ? "Bus added successfully" : "Bus updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteBus(Bus bus) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Bus")
                .setMessage("Are you sure you want to delete bus " + (bus.getBusNumber() != null ? bus.getBusNumber() : "") + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (bus.getBusId() != null) {
                        firebaseHelper.getBusesRef().child(bus.getBusId()).removeValue();
                        Toast.makeText(this, "Bus deleted", Toast.LENGTH_SHORT).show();
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
        if (busesListener != null) firebaseHelper.getBusesRef().removeEventListener(busesListener);
        if (routesListener != null) firebaseHelper.getRoutesRef().removeEventListener(routesListener);
        if (driversListener != null) firebaseHelper.getDriversRef().removeEventListener(driversListener);
    }
}
