package com.smartbus.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartbus.app.R;
import com.smartbus.app.models.Ticket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private final List<Ticket> ticketList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());

    public TicketAdapter(List<Ticket> ticketList) {
        this.ticketList = ticketList;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = ticketList.get(position);
        if (holder.tvTicketBusNumber != null) {
            holder.tvTicketBusNumber.setText("Bus: " + (ticket.getBusId() != null ? ticket.getBusId() : "Scheduled"));
        }
        if (holder.tvTicketRoute != null) {
            holder.tvTicketRoute.setText("Route: " + (ticket.getRouteId() != null ? ticket.getRouteId() : "Direct"));
        }
        if (holder.tvTicketFrom != null) {
            holder.tvTicketFrom.setText(ticket.getFromStop() != null ? ticket.getFromStop() : "Origin");
        }
        if (holder.tvTicketTo != null) {
            holder.tvTicketTo.setText(ticket.getToStop() != null ? ticket.getToStop() : "Destination");
        }
        if (holder.tvTicketFare != null) {
            holder.tvTicketFare.setText(String.format(Locale.getDefault(), "₹ %.2f", ticket.getFareAmount()));
        }
        if (holder.tvTicketStatus != null) {
            holder.tvTicketStatus.setText(ticket.getStatus() != null ? ticket.getStatus().toUpperCase() : "ACTIVE");
        }
        if (holder.tvTicketDate != null) {
            long time = ticket.getPurchaseTime() > 0 ? ticket.getPurchaseTime() : System.currentTimeMillis();
            holder.tvTicketDate.setText(dateFormat.format(new Date(time)));
        }
        if (holder.tvTicketId != null) {
            holder.tvTicketId.setText("ID: " + (ticket.getTicketId() != null ? ticket.getTicketId() : "TKT-" + position));
        }
    }

    @Override
    public int getItemCount() {
        return ticketList != null ? ticketList.size() : 0;
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicketBusNumber, tvTicketRoute, tvTicketStatus, tvTicketFrom, tvTicketTo, tvTicketDate, tvTicketFare, tvTicketId;

        TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicketBusNumber = itemView.findViewById(R.id.tvTicketBusNumber);
            tvTicketRoute = itemView.findViewById(R.id.tvTicketRoute);
            tvTicketStatus = itemView.findViewById(R.id.tvTicketStatus);
            tvTicketFrom = itemView.findViewById(R.id.tvTicketFrom);
            tvTicketTo = itemView.findViewById(R.id.tvTicketTo);
            tvTicketDate = itemView.findViewById(R.id.tvTicketDate);
            tvTicketFare = itemView.findViewById(R.id.tvTicketFare);
            tvTicketId = itemView.findViewById(R.id.tvTicketId);
        }
    }
}
