package com.smartbus.app.activities.passenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.adapters.BusAdapter;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Bus;
import com.smartbus.app.utils.Constants;
import com.smartbus.app.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;

public class PassengerHomeActivity extends AppCompatActivity implements BusAdapter.OnBusClickListener {

    private TextView tvGreeting;
    private RecyclerView rvBuses;
    private BusAdapter busAdapter;
    private List<Bus> busList = new ArrayList<>();
    private FirebaseHelper firebaseHelper;
    private ValueEventListener busListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_home);

        firebaseHelper = FirebaseHelper.getInstance();

        // Greeting
        tvGreeting = findViewById(R.id.tvGreeting);
        String name = SharedPrefManager.getUserName(this);
        if (tvGreeting != null && name != null && !name.isEmpty()) {
            tvGreeting.setText("Hello, " + name + "!");
        }

        // Notification Bell
        ImageButton btnNotification = findViewById(R.id.btnNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v ->
                    Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show());
        }

        // Search Bar Card
        View cardSearch = findViewById(R.id.cardSearch);
        if (cardSearch != null) {
            cardSearch.setOnClickListener(v ->
                    startActivity(new Intent(this, BusSearchActivity.class)));
        }

        // Quick Action Cards
        View actionTrackBus = findViewById(R.id.actionTrackBus);
        if (actionTrackBus != null) {
            actionTrackBus.setOnClickListener(v ->
                    startActivity(new Intent(this, BusSearchActivity.class)));
        }

        View actionMyTickets = findViewById(R.id.actionMyTickets);
        if (actionMyTickets != null) {
            actionMyTickets.setOnClickListener(v ->
                    startActivity(new Intent(this, TicketActivity.class)));
        }

        View actionSos = findViewById(R.id.actionSos);
        if (actionSos != null) {
            actionSos.setOnClickListener(v ->
                    startActivity(new Intent(this, SosActivity.class)));
        }

        View actionFeedback = findViewById(R.id.actionFeedback);
        if (actionFeedback != null) {
            actionFeedback.setOnClickListener(v ->
                    startActivity(new Intent(this, FeedbackActivity.class)));
        }

        // See All Buses
        TextView tvSeeAll = findViewById(R.id.tvSeeAll);
        if (tvSeeAll != null) {
            tvSeeAll.setOnClickListener(v ->
                    startActivity(new Intent(this, BusListActivity.class)));
        }

        // RecyclerView setup
        rvBuses = findViewById(R.id.rvBuses);
        if (rvBuses != null) {
            busAdapter = new BusAdapter(busList, this);
            rvBuses.setLayoutManager(new LinearLayoutManager(this));
            rvBuses.setAdapter(busAdapter);
        }

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    return true;
                } else if (itemId == R.id.nav_search) {
                    startActivity(new Intent(this, BusSearchActivity.class));
                    return true;
                } else if (itemId == R.id.nav_track) {
                    startActivity(new Intent(this, BusListActivity.class));
                    return true;
                } else if (itemId == R.id.nav_ticket) {
                    startActivity(new Intent(this, TicketActivity.class));
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }

        loadActiveBuses();
    }

    private void loadActiveBuses() {
        busListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                busList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Bus bus = child.getValue(Bus.class);
                    if (bus != null && bus.isActive()) {
                        busList.add(bus);
                    }
                }
                if (busAdapter != null) {
                    busAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PassengerHomeActivity.this, "Failed to load buses: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        firebaseHelper.getBusesRef().addValueEventListener(busListener);
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
        if (busListener != null) {
            firebaseHelper.getBusesRef().removeEventListener(busListener);
        }
    }
}
