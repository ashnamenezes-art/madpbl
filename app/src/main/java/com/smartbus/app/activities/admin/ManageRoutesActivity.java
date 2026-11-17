package com.smartbus.app.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.adapters.RouteAdapter;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Route;
import com.smartbus.app.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class ManageRoutesActivity extends AppCompatActivity {

    private RecyclerView rvRoutes;
    private ProgressBar progressBar;
    private TextView tvNoRoutes;
    private FloatingActionButton fabAddRoute;

    private RouteAdapter routeAdapter;
    private final List<Route> routeList = new ArrayList<>();

    private FirebaseHelper firebaseHelper;
    private ValueEventListener routesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_routes);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadRoutes();
    }

    private void initViews() {
        rvRoutes = findViewById(R.id.rv_routes);
        progressBar = findViewById(R.id.progress_bar);
        tvNoRoutes = findViewById(R.id.tv_no_routes);
        fabAddRoute = findViewById(R.id.fab_add_route);

        fabAddRoute.setOnClickListener(v -> showAddOrEditRouteDialog(null));
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Routes");
        }
    }

    private void setupRecyclerView() {
        routeAdapter = new RouteAdapter(this, routeList, new RouteAdapter.OnRouteClickListener() {
            @Override
            public void onRouteClick(Route route) {
                Intent intent = new Intent(ManageRoutesActivity.this, ManageStopsActivity.class);
                intent.putExtra(Constants.EXTRA_ROUTE_ID, route.getRouteId());
                startActivity(intent);
            }

            @Override
            public void onEditClick(Route route) {
                showAddOrEditRouteDialog(route);
            }

            @Override
            public void onDeleteClick(Route route) {
                confirmDeleteRoute(route);
            }
        });

        rvRoutes.setLayoutManager(new LinearLayoutManager(this));
        rvRoutes.setAdapter(routeAdapter);
    }

    private void loadRoutes() {
        progressBar.setVisibility(View.VISIBLE);

        routesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                routeList.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Route route = child.getValue(Route.class);
                    if (route != null) {
                        if (route.getRouteId() == null) {
                            route.setRouteId(child.getKey());
                        }
                        routeList.add(route);
                    }
                }

                routeAdapter.notifyDataSetChanged();

                if (routeList.isEmpty()) {
                    tvNoRoutes.setVisibility(View.VISIBLE);
                    rvRoutes.setVisibility(View.GONE);
                } else {
                    tvNoRoutes.setVisibility(View.GONE);
                    rvRoutes.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageRoutesActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        firebaseHelper.getRoutesRef().addValueEventListener(routesListener);
    }

    private void showAddOrEditRouteDialog(final Route existingRoute) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_route, null);

        EditText etName = dialogView.findViewById(R.id.et_dialog_route_name);
        EditText etSource = dialogView.findViewById(R.id.et_dialog_source_stop);
        EditText etDest = dialogView.findViewById(R.id.et_dialog_destination_stop);
        EditText etDistance = dialogView.findViewById(R.id.et_dialog_distance);
        EditText etDuration = dialogView.findViewById(R.id.et_dialog_duration);
        EditText etFare = dialogView.findViewById(R.id.et_dialog_fare);

        if (existingRoute != null) {
            etName.setText(existingRoute.getRouteName());
            etSource.setText(existingRoute.getSourceStop());
            etDest.setText(existingRoute.getDestinationStop());
            etDistance.setText(String.valueOf(existingRoute.getDistanceKm()));
            etDuration.setText(String.valueOf(existingRoute.getEstimatedDuration()));
            etFare.setText(String.valueOf(existingRoute.getFareAmount()));
        }

        String title = existingRoute == null ? "Add New Route" : "Edit Route";

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String source = etSource.getText() != null ? etSource.getText().toString().trim() : "";
                    String dest = etDest.getText() != null ? etDest.getText().toString().trim() : "";
                    String distStr = etDistance.getText() != null ? etDistance.getText().toString().trim() : "0";
                    String durStr = etDuration.getText() != null ? etDuration.getText().toString().trim() : "0";
                    String fareStr = etFare.getText() != null ? etFare.getText().toString().trim() : "0";

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "Route name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double distance = 0.0;
                    try {
                        distance = Double.parseDouble(distStr);
                    } catch (NumberFormatException ignored) {}

                    int duration = 0;
                    try {
                        duration = Integer.parseInt(durStr);
                    } catch (NumberFormatException ignored) {}

                    double fare = 0.0;
                    try {
                        fare = Double.parseDouble(fareStr);
                    } catch (NumberFormatException ignored) {}

                    DatabaseReference routesRef = firebaseHelper.getRoutesRef();
                    String routeId = existingRoute != null && existingRoute.getRouteId() != null
                            ? existingRoute.getRouteId()
                            : routesRef.push().getKey();

                    int stopsCount = existingRoute != null ? existingRoute.getTotalStops() : 0;
                    boolean isActive = existingRoute == null || existingRoute.isActive();

                    Route route = new Route(
                            routeId,
                            name,
                            source,
                            dest,
                            stopsCount,
                            distance,
                            duration,
                            fare,
                            isActive
                    );

                    if (routeId != null) {
                        routesRef.child(routeId).setValue(route);
                        Toast.makeText(this, existingRoute == null ? "Route created" : "Route updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteRoute(final Route route) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Route")
                .setMessage("Are you sure you want to delete " + (route.getRouteName() != null ? route.getRouteName() : "this route") + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (route.getRouteId() != null) {
                        firebaseHelper.getRoutesRef().child(route.getRouteId()).removeValue();
                        Toast.makeText(this, "Route deleted", Toast.LENGTH_SHORT).show();
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
        if (routesListener != null) {
            firebaseHelper.getRoutesRef().removeEventListener(routesListener);
        }
    }
}
