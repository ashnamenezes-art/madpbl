package com.smartbus.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartbus.app.R;
import com.smartbus.app.models.Bus;

import java.util.List;

public class BusAdapter extends RecyclerView.Adapter<BusAdapter.BusViewHolder> {

    private final List<Bus> busList;
    private final OnBusClickListener listener;

    public interface OnBusClickListener {
        void onBusClick(Bus bus);
    }

    public BusAdapter(List<Bus> busList, OnBusClickListener listener) {
        this.busList = busList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bus, parent, false);
        return new BusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BusViewHolder holder, int position) {
        Bus bus = busList.get(position);
        if (bus == null) return;

        if (holder.tvBusNumber != null) {
            holder.tvBusNumber.setText(bus.getBusNumber() != null ? "Bus #" + bus.getBusNumber() : "Bus");
        }

        if (holder.tvRouteName != null) {
            holder.tvRouteName.setText(bus.getBusName() != null ? bus.getBusName() : (bus.getRouteId() != null ? "Route: " + bus.getRouteId() : "Active Route"));
        }

        if (holder.tvFromTo != null) {
            holder.tvFromTo.setText(bus.getBusName() != null ? bus.getBusName() : "Daily Schedule");
        }

        if (holder.tvBusType != null) {
            holder.tvBusType.setText(bus.getBusType() != null ? bus.getBusType() : "Standard");
        }

        if (holder.tvStatus != null) {
            holder.tvStatus.setText(bus.isActive() ? "Active" : "Inactive");
            holder.tvStatus.setTextColor(bus.isActive() ? 0xFF388E3C : 0xFF757575);
        }

        if (holder.viewStatusDot != null) {
            holder.viewStatusDot.setBackgroundColor(bus.isActive() ? 0xFF388E3C : 0xFF9E9E9E);
        }

        if (holder.tvETA != null) {
            holder.tvETA.setText("5-10 min");
        }

        View.OnClickListener clickListener = v -> {
            if (listener != null) {
                listener.onBusClick(bus);
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        if (holder.btnTrack != null) {
            holder.btnTrack.setOnClickListener(clickListener);
        }
    }

    @Override
    public int getItemCount() {
        return busList != null ? busList.size() : 0;
    }

    public void updateList(List<Bus> newList) {
        busList.clear();
        if (newList != null) {
            busList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    static class BusViewHolder extends RecyclerView.ViewHolder {
        TextView tvBusNumber, tvRouteName, tvFromTo, tvBusType, tvStatus, tvETA;
        View viewStatusDot;
        Button btnTrack;

        BusViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBusNumber = itemView.findViewById(R.id.tvBusNumber);
            tvRouteName = itemView.findViewById(R.id.tvRouteName);
            tvFromTo = itemView.findViewById(R.id.tvFromTo);
            tvBusType = itemView.findViewById(R.id.tvBusType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvETA = itemView.findViewById(R.id.tvETA);
            viewStatusDot = itemView.findViewById(R.id.viewStatusDot);
            btnTrack = itemView.findViewById(R.id.btnTrack);
        }
    }
}
