package com.smartbus.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartbus.app.R;
import com.smartbus.app.models.Stop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StopAdapter extends RecyclerView.Adapter<StopAdapter.StopViewHolder> {

    public interface OnStopClickListener {
        void onStopClick(Stop stop);
        void onEditClick(Stop stop);
        void onDeleteClick(Stop stop);
    }

    private final Context context;
    private List<Stop> stopList;
    private OnStopClickListener listener;

    public StopAdapter(Context context) {
        this.context = context;
        this.stopList = new ArrayList<>();
    }

    public StopAdapter(Context context, List<Stop> stopList, OnStopClickListener listener) {
        this.context = context;
        this.stopList = stopList != null ? stopList : new ArrayList<>();
        this.listener = listener;
    }

    public void setStops(List<Stop> stops) {
        this.stopList = stops != null ? stops : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnStopClickListener(OnStopClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public StopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_stop, parent, false);
        return new StopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StopViewHolder holder, int position) {
        Stop stop = stopList.get(position);
        holder.bind(stop);
    }

    @Override
    public int getItemCount() {
        return stopList != null ? stopList.size() : 0;
    }

    class StopViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvStopOrder;
        private final TextView tvStopName;
        private final TextView tvLatLng;
        private final TextView tvAddress;
        private final ImageView btnEdit;
        private final ImageView btnDelete;

        public StopViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStopOrder = itemView.findViewById(R.id.tvStopOrder);
            tvStopName = itemView.findViewById(R.id.tvStopName);
            tvLatLng = itemView.findViewById(R.id.tvLatLng);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            btnEdit = itemView.findViewById(R.id.btn_edit_stop);
            btnDelete = itemView.findViewById(R.id.btn_delete_stop);
        }

        public void bind(final Stop stop) {
            if (tvStopOrder != null) {
                tvStopOrder.setText(String.valueOf(stop.getStopOrder() > 0 ? stop.getStopOrder() : (getAdapterPosition() + 1)));
            }

            if (tvStopName != null) {
                tvStopName.setText(stop.getStopName() != null ? stop.getStopName() : "Unnamed Stop");
            }

            if (tvLatLng != null) {
                tvLatLng.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", stop.getLatitude(), stop.getLongitude()));
            }

            if (tvAddress != null) {
                if (stop.getAddress() != null && !stop.getAddress().trim().isEmpty()) {
                    tvAddress.setText(stop.getAddress());
                    tvAddress.setVisibility(View.VISIBLE);
                } else {
                    tvAddress.setVisibility(View.GONE);
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStopClick(stop);
                }
            });

            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEditClick(stop);
                    }
                });
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClick(stop);
                    }
                });
            }
        }
    }
}
