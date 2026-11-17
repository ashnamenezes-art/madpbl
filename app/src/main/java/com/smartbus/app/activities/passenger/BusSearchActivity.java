package com.smartbus.app.activities.passenger;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.adapters.BusAdapter;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Bus;
import com.smartbus.app.models.Route;
import com.smartbus.app.models.Stop;
import com.smartbus.app.utils.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BusSearchActivity extends AppCompatActivity implements BusAdapter.OnBusClickListener {

    private AutoCompleteTextView actvFrom, actvTo;
    private ImageButton btnSwap;
    private TextView tvDate, tvTime, tvResultsLabel;
    private Button btnSearch;
    private RecyclerView rvSearchResults;
    private BusAdapter busAdapter;
    private List<Bus> searchResults = new ArrayList<>();
    private List<String> stopNames = new ArrayList<>();
    private ArrayAdapter<String> stopsAdapter;

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_search);

        firebaseHelper = FirebaseHelper.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        actvFrom = findViewById(R.id.actvFrom);
        actvTo = findViewById(R.id.actvTo);
        btnSwap = findViewById(R.id.btnSwap);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        btnSearch = findViewById(R.id.btnSearch);
        tvResultsLabel = findViewById(R.id.tvResultsLabel);
        rvSearchResults = findViewById(R.id.rvSearchResults);

        // RecyclerView
        busAdapter = new BusAdapter(searchResults, this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(busAdapter);

        // Setup AutoComplete adapters
        stopsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, stopNames);
        actvFrom.setAdapter(stopsAdapter);
        actvTo.setAdapter(stopsAdapter);

        loadStops();

        // Swap button
        btnSwap.setOnClickListener(v -> {
            String fromText = actvFrom.getText().toString();
            String toText = actvTo.getText().toString();
            actvFrom.setText(toText);
            actvTo.setText(fromText);
        });

        // Date Picker
        tvDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                tvDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Time Picker
        tvTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        // Search button
        btnSearch.setOnClickListener(v -> performSearch());
    }

    private void loadStops() {
        firebaseHelper.getStopsRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> uniqueStops = new HashSet<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Stop stop = child.getValue(Stop.class);
                    if (stop != null && stop.getStopName() != null && !stop.getStopName().isEmpty()) {
                        uniqueStops.add(stop.getStopName());
                    }
                }
                stopNames.clear();
                stopNames.addAll(uniqueStops);
                stopsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void performSearch() {
        String from = actvFrom.getText().toString().trim().toLowerCase();
        String to = actvTo.getText().toString().trim().toLowerCase();

        firebaseHelper.getBusesRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                searchResults.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Bus bus = child.getValue(Bus.class);
                    if (bus != null && bus.isActive()) {
                        String busName = bus.getBusName() != null ? bus.getBusName().toLowerCase() : "";
                        String busNumber = bus.getBusNumber() != null ? bus.getBusNumber().toLowerCase() : "";

                        if (from.isEmpty() && to.isEmpty()) {
                            searchResults.add(bus);
                        } else if (!from.isEmpty() && !to.isEmpty()) {
                            if (busName.contains(from) || busName.contains(to)) {
                                searchResults.add(bus);
                            }
                        } else if (!from.isEmpty()) {
                            if (busName.contains(from) || busNumber.contains(from)) {
                                searchResults.add(bus);
                            }
                        } else {
                            if (busName.contains(to) || busNumber.contains(to)) {
                                searchResults.add(bus);
                            }
                        }
                    }
                }

                if (tvResultsLabel != null) {
                    tvResultsLabel.setVisibility(View.VISIBLE);
                    tvResultsLabel.setText(searchResults.isEmpty() ? "No buses found for this search" : "Available Buses (" + searchResults.size() + ")");
                }
                busAdapter.notifyDataSetChanged();

                if (searchResults.isEmpty()) {
                    Toast.makeText(BusSearchActivity.this, "No matching active buses found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BusSearchActivity.this, "Search failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBusClick(Bus bus) {
        if (bus != null && bus.getBusId() != null) {
            Intent intent = new Intent(this, LiveTrackingActivity.class);
            intent.putExtra(Constants.EXTRA_BUS_ID, bus.getBusId());
            startActivity(intent);
        }
    }
}
