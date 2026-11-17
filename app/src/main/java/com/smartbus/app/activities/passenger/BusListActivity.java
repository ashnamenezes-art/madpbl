package com.smartbus.app.activities.passenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.adapters.BusAdapter;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Bus;
import com.smartbus.app.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class BusListActivity extends AppCompatActivity implements BusAdapter.OnBusClickListener {

    private RecyclerView rvBusList;
    private LinearLayout layoutEmpty;
    private BusAdapter busAdapter;
    private List<Bus> allBuses = new ArrayList<>();
    private List<Bus> filteredBuses = new ArrayList<>();
    private String selectedFilter = "All";
    private String routeFilterId = null;

    private Chip chipAll, chipAC, chipNonAC, chipExpress;
    private FirebaseHelper firebaseHelper;
    private ValueEventListener busesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_list);

        firebaseHelper = FirebaseHelper.getInstance();
        routeFilterId = getIntent().getStringExtra(Constants.EXTRA_ROUTE_ID);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        chipAll = findViewById(R.id.chipAll);
        chipAC = findViewById(R.id.chipAC);
        chipNonAC = findViewById(R.id.chipNonAC);
        chipExpress = findViewById(R.id.chipExpress);
        rvBusList = findViewById(R.id.rvBusList);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        busAdapter = new BusAdapter(filteredBuses, this);
        rvBusList.setLayoutManager(new LinearLayoutManager(this));
        rvBusList.setAdapter(busAdapter);

        setupChips();
        loadBuses();
    }

    private void setupChips() {
        View.OnClickListener chipClickListener = v -> {
            int id = v.getId();
            resetChipStyles();
            if (id == R.id.chipAll) {
                selectedFilter = "All";
                highlightChip(chipAll);
            } else if (id == R.id.chipAC) {
                selectedFilter = "AC";
                highlightChip(chipAC);
            } else if (id == R.id.chipNonAC) {
                selectedFilter = "Non-AC";
                highlightChip(chipNonAC);
            } else if (id == R.id.chipExpress) {
                selectedFilter = "Express";
                highlightChip(chipExpress);
            }
            applyFilter();
        };

        if (chipAll != null) chipAll.setOnClickListener(chipClickListener);
        if (chipAC != null) chipAC.setOnClickListener(chipClickListener);
        if (chipNonAC != null) chipNonAC.setOnClickListener(chipClickListener);
        if (chipExpress != null) chipExpress.setOnClickListener(chipClickListener);
    }

    private void resetChipStyles() {
        int white = getResources().getColor(R.color.white);
        int primary = getResources().getColor(R.color.primary);

        Chip[] chips = {chipAll, chipAC, chipNonAC, chipExpress};
        for (Chip c : chips) {
            if (c != null) {
                c.setChipBackgroundColorResource(R.color.white);
                c.setTextColor(primary);
            }
        }
    }

    private void highlightChip(Chip chip) {
        if (chip != null) {
            chip.setChipBackgroundColorResource(R.color.primary);
            chip.setTextColor(getResources().getColor(R.color.white));
        }
    }

    private void loadBuses() {
        busesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allBuses.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Bus bus = child.getValue(Bus.class);
                    if (bus != null && bus.isActive()) {
                        if (routeFilterId == null || routeFilterId.isEmpty() || routeFilterId.equals(bus.getRouteId())) {
                            allBuses.add(bus);
                        }
                    }
                }
                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BusListActivity.this, "Failed to load buses: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        firebaseHelper.getBusesRef().addValueEventListener(busesListener);
    }

    private void applyFilter() {
        filteredBuses.clear();
        for (Bus bus : allBuses) {
            if ("All".equalsIgnoreCase(selectedFilter)) {
                filteredBuses.add(bus);
            } else if ("AC".equalsIgnoreCase(selectedFilter)) {
                if (bus.getBusType() != null && bus.getBusType().equalsIgnoreCase("AC")) {
                    filteredBuses.add(bus);
                }
            } else if ("Non-AC".equalsIgnoreCase(selectedFilter)) {
                if (bus.getBusType() != null && (bus.getBusType().equalsIgnoreCase("Non-AC") || bus.getBusType().equalsIgnoreCase("Regular"))) {
                    filteredBuses.add(bus);
                }
            } else if ("Express".equalsIgnoreCase(selectedFilter)) {
                if (bus.getBusType() != null && bus.getBusType().equalsIgnoreCase("Express")) {
                    filteredBuses.add(bus);
                }
            }
        }

        busAdapter.notifyDataSetChanged();

        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(filteredBuses.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onBusClick(Bus bus) {
        if (bus != null && bus.getBusId() != null) {
            Intent intent = new Intent(this, LiveTrackingActivity.class);
            intent.putExtra(Constants.EXTRA_BUS_ID, bus.getBusId());
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (busesListener != null) {
            firebaseHelper.getBusesRef().removeEventListener(busesListener);
        }
    }
}
