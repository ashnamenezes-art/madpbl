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
import com.smartbus.app.models.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    public interface OnRouteClickListener {
        void onRouteClick(Route route);
        void onEditClick(Route route);
        void onDeleteClick(Route route);
    }

    private final Context context;
    private List<Route> routeList;
    private OnRouteClickListener listener;

    public RouteAdapter(Context context) {
        this.context = context;
        this.routeList = new ArrayList<>();
    }

    public RouteAdapter(Context context, List<Route> routeList, OnRouteClickListener listener) {
        this.context = context;
        this.routeList = routeList != null ? routeList : new ArrayList<>();
        this.listener = listener;
    }

    public void setRoutes(List<Route> routes) {
        this.routeList = routes != null ? routes : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnRouteClickListener(OnRouteClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_route, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        Route route = routeList.get(position);
        holder.bind(route);
    }

    @Override
    public int getItemCount() {
        return routeList != null ? routeList.size() : 0;
    }

    class RouteViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvRouteName;
        private final TextView tvFromTo;
        private final TextView tvBusCount;
        private final TextView tvDistance;
        private final TextView tvDuration;
        private final TextView tvFare;
        private final ImageView btnEdit;
        private final ImageView btnDelete;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRouteName = itemView.findViewById(R.id.tvRouteName);
            tvFromTo = itemView.findViewById(R.id.tvFromTo);
            tvBusCount = itemView.findViewById(R.id.tvBusCount);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvFare = itemView.findViewById(R.id.tvFare);
            btnEdit = itemView.findViewById(R.id.btn_edit_route);
            btnDelete = itemView.findViewById(R.id.btn_delete_route);
        }

        public void bind(final Route route) {
            tvRouteName.setText(route.getRouteName() != null ? route.getRouteName() : "Unnamed Route");

            String source = route.getSourceStop() != null ? route.getSourceStop() : "Source";
            String dest = route.getDestinationStop() != null ? route.getDestinationStop() : "Destination";
            if (tvFromTo != null) {
                tvFromTo.setText(source + " → " + dest);
            }

            if (tvDistance != null) {
                tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", route.getDistanceKm()));
            }

            if (tvDuration != null) {
                tvDuration.setText(String.format(Locale.getDefault(), "%d mins", route.getEstimatedDuration()));
            }

            if (tvFare != null) {
                tvFare.setText(String.format(Locale.getDefault(), "₹ %.2f", route.getFareAmount()));
            }

            if (tvBusCount != null) {
                tvBusCount.setText(route.getTotalStops() > 0 ? (route.getTotalStops() + " stops") : "Active");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRouteClick(route);
                }
            });

            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEditClick(route);
                    }
                });
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClick(route);
                    }
                });
            }
        }
    }
}
