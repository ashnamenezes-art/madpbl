package com.smartbus.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.smartbus.app.R;
import com.smartbus.app.models.Driver;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * RecyclerView adapter for admin-facing and general driver list.
 * Binds Driver model to item_driver.xml.
 */
public class DriverAdapter extends RecyclerView.Adapter<DriverAdapter.DriverViewHolder> {

    public interface OnDriverClickListener {
        void onDriverClick(Driver driver);
    }

    private final Context context;
    private List<Driver> driverList = new ArrayList<>();
    private OnDriverClickListener listener;

    public DriverAdapter(Context context) {
        this.context = context;
    }

    public DriverAdapter(Context context, List<Driver> driverList, OnDriverClickListener listener) {
        this.context = context;
        this.driverList = (driverList != null) ? driverList : new ArrayList<>();
        this.listener = listener;
    }

    public void setDrivers(List<Driver> drivers) {
        this.driverList = (drivers != null) ? drivers : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnDriverClickListener(OnDriverClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_driver, parent, false);
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        Driver driver = driverList.get(position);
        holder.bind(driver, listener);
    }

    @Override
    public int getItemCount() {
        return driverList.size();
    }

    static class DriverViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivAvatar;
        private final TextView tvName;
        private final TextView tvPhone;
        private final TextView tvBus;
        private final View viewDutyDot;
        private final TextView tvDutyStatusLabel;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivDriverItemAvatar);
            tvName = itemView.findViewById(R.id.tvDriverItemName);
            tvPhone = itemView.findViewById(R.id.tvDriverItemPhone);
            tvBus = itemView.findViewById(R.id.tvDriverItemBus);
            viewDutyDot = itemView.findViewById(R.id.viewDutyDot);
            tvDutyStatusLabel = itemView.findViewById(R.id.tvDutyStatusLabel);
        }

        public void bind(final Driver driver, final OnDriverClickListener listener) {
            tvName.setText(driver.getName() != null && !driver.getName().isEmpty()
                    ? driver.getName() : "Unnamed Driver");

            tvPhone.setText(driver.getPhone() != null && !driver.getPhone().isEmpty()
                    ? driver.getPhone() : "No phone provided");

            if (driver.getAssignedBusId() != null && !driver.getAssignedBusId().isEmpty()) {
                tvBus.setText("Bus ID: " + driver.getAssignedBusId());
            } else {
                tvBus.setText("No Bus Assigned");
            }

            if (driver.isOnDuty()) {
                viewDutyDot.setBackgroundResource(R.drawable.bg_status_active);
                tvDutyStatusLabel.setText("On Duty");
                tvDutyStatusLabel.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.driver_color));
            } else {
                viewDutyDot.setBackgroundResource(R.drawable.bg_status_inactive);
                tvDutyStatusLabel.setText("Off Duty");
                tvDutyStatusLabel.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            }

            if (driver.getProfileImage() != null && !driver.getProfileImage().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(driver.getProfileImage())
                        .placeholder(R.drawable.ic_driver)
                        .error(R.drawable.ic_driver)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_driver);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDriverClick(driver);
                }
            });
        }
    }
}
