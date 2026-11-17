package com.smartbus.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.smartbus.app.R;
import com.smartbus.app.models.Bus;

import java.util.ArrayList;
import java.util.List;

public class AdminBusAdapter extends RecyclerView.Adapter<AdminBusAdapter.AdminBusViewHolder> {

    public interface OnAdminBusActionListener {
        void onBusClick(Bus bus);
        void onEditClick(Bus bus);
        void onDeleteClick(Bus bus);
        void onStatusToggled(Bus bus, boolean isActive);
    }

    private final Context context;
    private List<Bus> busList;
    private OnAdminBusActionListener listener;

    public AdminBusAdapter(Context context) {
        this.context = context;
        this.busList = new ArrayList<>();
    }

    public AdminBusAdapter(Context context, List<Bus> busList, OnAdminBusActionListener listener) {
        this.context = context;
        this.busList = busList != null ? busList : new ArrayList<>();
        this.listener = listener;
    }

    public void setBuses(List<Bus> buses) {
        this.busList = buses != null ? buses : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnAdminBusActionListener(OnAdminBusActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminBusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_bus, parent, false);
        return new AdminBusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminBusViewHolder holder, int position) {
        Bus bus = busList.get(position);
        holder.bind(bus);
    }

    @Override
    public int getItemCount() {
        return busList != null ? busList.size() : 0;
    }

    class AdminBusViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvBusNumber;
        private final TextView tvBusNameType;
        private final TextView tvBusRoute;
        private final TextView tvBusDriver;
        private final SwitchMaterial switchBusStatus;
        private final TextView tvStatusLabel;
        private final ImageView btnEditBus;
        private final ImageView btnDeleteBus;

        public AdminBusViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBusNumber = itemView.findViewById(R.id.tv_bus_number);
            tvBusNameType = itemView.findViewById(R.id.tv_bus_name_type);
            tvBusRoute = itemView.findViewById(R.id.tv_bus_route);
            tvBusDriver = itemView.findViewById(R.id.tv_bus_driver);
            switchBusStatus = itemView.findViewById(R.id.switch_bus_status);
            tvStatusLabel = itemView.findViewById(R.id.tv_status_label);
            btnEditBus = itemView.findViewById(R.id.btn_edit_bus);
            btnDeleteBus = itemView.findViewById(R.id.btn_delete_bus);
        }

        public void bind(final Bus bus) {
            tvBusNumber.setText(bus.getBusNumber() != null ? bus.getBusNumber() : "No Number");

            String name = bus.getBusName() != null ? bus.getBusName() : "Bus";
            String type = bus.getBusType() != null ? bus.getBusType() : "Standard";
            int seats = bus.getTotalSeats();
            tvBusNameType.setText(name + " • " + type + " • " + seats + " Seats");

            if (bus.getRouteId() != null && !bus.getRouteId().isEmpty()) {
                tvBusRoute.setText("Route ID: " + bus.getRouteId());
            } else {
                tvBusRoute.setText("Route: Not Assigned");
            }

            if (bus.getDriverId() != null && !bus.getDriverId().isEmpty()) {
                tvBusDriver.setText("Driver ID: " + bus.getDriverId());
            } else {
                tvBusDriver.setText("Driver: Not Assigned");
            }

            // Unset listener before setting checked to avoid triggering during recycle
            switchBusStatus.setOnCheckedChangeListener(null);
            switchBusStatus.setChecked(bus.isActive());
            updateStatusUi(bus.isActive());

            switchBusStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                bus.setActive(isChecked);
                updateStatusUi(isChecked);
                if (listener != null) {
                    listener.onStatusToggled(bus, isChecked);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBusClick(bus);
                }
            });

            btnEditBus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(bus);
                }
            });

            btnDeleteBus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(bus);
                }
            });
        }

        private void updateStatusUi(boolean active) {
            if (active) {
                tvStatusLabel.setText("Active");
                tvStatusLabel.setTextColor(Color.parseColor("#2E7D32"));
            } else {
                tvStatusLabel.setText("Inactive");
                tvStatusLabel.setTextColor(Color.parseColor("#757575"));
            }
        }
    }
}
