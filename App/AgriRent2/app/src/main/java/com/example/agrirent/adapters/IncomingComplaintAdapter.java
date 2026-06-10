package com.example.agrirent.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrirent.R;
import com.example.agrirent.models.ComplaintResponse;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class IncomingComplaintAdapter extends RecyclerView.Adapter<IncomingComplaintAdapter.ViewHolder> {

    private final Context context;
    private final List<ComplaintResponse> list;
    private final OnResolveListener listener;

    public interface OnResolveListener {
        void onResolve(ComplaintResponse complaint);
    }

    public IncomingComplaintAdapter(Context context, List<ComplaintResponse> list, OnResolveListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_incoming_complaint, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComplaintResponse c = list.get(position);
        holder.tvDescription.setText(c.getDescription());
        holder.tvStatus.setText(c.getStatus());
        holder.tvDate.setText(c.getCreatedAt());
        holder.tvFiledBy.setText("Filed by: " + (c.getFiledBy() != null ? c.getFiledBy() : "Unknown"));

        // Status coloring
        if ("Pending".equalsIgnoreCase(c.getStatus())) {
            holder.tvStatus.setTextColor(0xFFF57F17); // Orange
            holder.btnResolve.setVisibility(View.VISIBLE);
        } else if ("Resolved".equalsIgnoreCase(c.getStatus())) {
            holder.tvStatus.setTextColor(0xFF2E7D32); // Green
            holder.btnResolve.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setTextColor(0xFFD32F2F); // Red
            holder.btnResolve.setVisibility(View.GONE);
        }

        holder.btnResolve.setOnClickListener(v -> {
            if (listener != null) listener.onResolve(c);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvStatus, tvDate, tvFiledBy;
        MaterialButton btnResolve;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvFiledBy = itemView.findViewById(R.id.tvFiledBy);
            btnResolve = itemView.findViewById(R.id.btnResolve);
        }
    }
}
