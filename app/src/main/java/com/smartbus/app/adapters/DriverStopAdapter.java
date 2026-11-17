package com.smartbus.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartbus.app.R;
import com.smartbus.app.models.Stop;

import java.util.ArrayList;
import java.util.List;

public class DriverStopAdapter extends RecyclerView.Adapter<DriverStopAdapter.StopViewHolder> {

    private final Context context;
    private List<Stop> stopList = new ArrayList<>();

    public DriverStopAdapter(Context context) {
        this.context = context;
    }

    public void setStops(List<Stop> stops) {
        this.stopList = (stops != null) ? stops : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_driver_stop, parent, false);
        return new StopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StopViewHolder holder, int position) {
        Stop stop = stopList.get(position);
        holder.bind(stop);
    }

    @Override
    public int getItemCount() {
        return stopList.size();
    }

    static class StopViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvStopOrder;
        private final TextView tvStopName;
        private final TextView tvStopAddress;

        public StopViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStopOrder = itemView.findViewById(R.id.tvStopOrder);
            tvStopName = itemView.findViewById(R.id.tvStopName);
            tvStopAddress = itemView.findViewById(R.id.tvStopAddress);
        }

        public void bind(Stop stop) {
            tvStopOrder.setText(String.valueOf(stop.getStopOrder()));
            tvStopName.setText(stop.getStopName() != null ? stop.getStopName() : "Unnamed Stop");
            tvStopAddress.setText(stop.getAddress() != null && !stop.getAddress().isEmpty()
                    ? stop.getAddress()
                    : String.format("Lat: %.4f, Lng: %.4f", stop.getLatitude(), stop.getLongitude()));
        }
    }
}
