package com.smartbus.app.activities.passenger;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.adapters.TicketAdapter;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Ticket;
import com.smartbus.app.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TicketActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView rvTickets;
    private Button btnPurchaseTicket;

    private TicketAdapter ticketAdapter;
    private List<Ticket> allTickets = new ArrayList<>();
    private List<Ticket> displayTickets = new ArrayList<>();
    private boolean showActive = true;

    private FirebaseHelper firebaseHelper;
    private ValueEventListener ticketsListener;
    private String passengerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        firebaseHelper = FirebaseHelper.getInstance();
        passengerId = SharedPrefManager.getUserId(this);
        if (passengerId == null) {
            passengerId = firebaseHelper.getCurrentUid();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tabLayout = findViewById(R.id.tabLayout);
        rvTickets = findViewById(R.id.rvTickets);
        btnPurchaseTicket = findViewById(R.id.btnPurchaseTicket);

        ticketAdapter = new TicketAdapter(displayTickets);
        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        rvTickets.setAdapter(ticketAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showActive = (tab.getPosition() == 0);
                filterTickets();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        if (btnPurchaseTicket != null) {
            btnPurchaseTicket.setOnClickListener(v -> showPurchaseTicketDialog());
        }

        loadTickets();
    }

    private void loadTickets() {
        if (passengerId == null) {
            passengerId = "demo_passenger";
        }

        ticketsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTickets.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Ticket ticket = child.getValue(Ticket.class);
                    if (ticket != null) {
                        if (passengerId.equals(ticket.getPassengerId()) || "demo_passenger".equals(passengerId)) {
                            allTickets.add(ticket);
                        }
                    }
                }
                filterTickets();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TicketActivity.this, "Failed to load tickets: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        firebaseHelper.getTicketsRef().addValueEventListener(ticketsListener);
    }

    private void filterTickets() {
        displayTickets.clear();
        for (Ticket ticket : allTickets) {
            boolean isActive = "active".equalsIgnoreCase(ticket.getStatus());
            if (showActive && isActive) {
                displayTickets.add(ticket);
            } else if (!showActive && !isActive) {
                displayTickets.add(ticket);
            }
        }
        ticketAdapter.notifyDataSetChanged();
    }

    private void showPurchaseTicketDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Ticket Purchase")
                .setMessage("Purchase Single Journey Ticket?\n\nFrom: City Center\nTo: Airport Terminal\nFare: ₹ 25.00")
                .setPositiveButton("Purchase", (dialog, which) -> {
                    String ticketId = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    Ticket ticket = new Ticket(
                            ticketId,
                            passengerId,
                            "101",
                            "route_1",
                            "City Center",
                            "Airport Terminal",
                            25.0,
                            System.currentTimeMillis(),
                            "active",
                            "QR_" + ticketId
                    );

                    firebaseHelper.getTicketsRef().child(ticketId).setValue(ticket)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Ticket purchased successfully!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ticketsListener != null) {
            firebaseHelper.getTicketsRef().removeEventListener(ticketsListener);
        }
    }
}
