package com.example.agrirent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrirent.R;
import com.example.agrirent.models.ComplaintResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ComplaintAdapter extends RecyclerView.Adapter<ComplaintAdapter.ViewHolder> {

    private List<ComplaintResponse> complaintList;

    public ComplaintAdapter(List<ComplaintResponse> complaintList) {
        this.complaintList = complaintList;
    }

    public void setComplaints(List<ComplaintResponse> complaints) {
        this.complaintList = complaints;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_complaint, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComplaintResponse complaint = complaintList.get(position);

        holder.tvComplaintId.setText("#CMP" + complaint.getId());
        holder.tvSubject.setText(complaint.getSubject() != null ? complaint.getSubject() : "No Subject");
        holder.tvDescription.setText(complaint.getDescription() != null ? complaint.getDescription() : "No description provided");

        String status = complaint.getStatus() != null ? complaint.getStatus().toUpperCase() : "PENDING";
        holder.tvStatus.setText(status);

        if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
            holder.tvStatus.setTextColor(0xFF2E7D32); // Green
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE8F5E9));
            }
        } else if ("IN PROGRESS".equals(status)) {
            holder.tvStatus.setTextColor(0xFF1976D2); // Blue
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
            }
        } else {
            holder.tvStatus.setTextColor(0xFFF57F17); // Orange
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFF8E1));
            }
        }

        // Format date
        String dateStr = complaint.getCreatedAt();
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(dateStr);
                if (date != null) {
                    SimpleDateFormat outFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    holder.tvDate.setText(outFormat.format(date));
                } else {
                    holder.tvDate.setText(dateStr.substring(0, Math.min(10, dateStr.length())));
                }
            } catch (Exception e) {
                holder.tvDate.setText(dateStr.substring(0, Math.min(10, dateStr.length())));
            }
        } else {
            holder.tvDate.setText("Unknown Date");
        }

        String tType = complaint.getTransactionType();
        String tId = complaint.getTransactionId();
        String tName = complaint.getTargetName();

        if (tName != null && !tName.isEmpty()) {
            holder.tvTransactionInfo.setText("Ref: " + tName);
        } else if (tType != null && tId != null) {
            holder.tvTransactionInfo.setText("Ref: " + tType + " #" + tId);
        } else {
            holder.tvTransactionInfo.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return complaintList == null ? 0 : complaintList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvComplaintId, tvStatus, tvSubject, tvDescription, tvDate, tvTransactionInfo;

        ViewHolder(View itemView) {
            super(itemView);
            tvComplaintId = itemView.findViewById(R.id.tvComplaintId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTransactionInfo = itemView.findViewById(R.id.tvTransactionInfo);
        }
    }
}
