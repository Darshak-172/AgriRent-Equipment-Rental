package com.example.agrirent.adapters;

import android.content.Context;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.agrirent.R;
import com.example.agrirent.models.Equipment;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ManageEquipmentAdapter extends RecyclerView.Adapter<ManageEquipmentAdapter.ViewHolder> {

    private final Context context;
    private final List<Equipment> equipmentList;
    private final OnEquipmentActionClickListener listener;

    public interface OnEquipmentActionClickListener {
        void onEditClick(Equipment equipment);
        void onToggleStatusClick(Equipment equipment, int position, View itemView);
        void onDeleteClick(Equipment equipment, int position, View itemView);
    }

    public ManageEquipmentAdapter(Context context, List<Equipment> equipmentList, OnEquipmentActionClickListener listener) {
        this.context = context;
        this.equipmentList = equipmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_equipment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);

        holder.tvEquipmentName.setText(equipment.getEquipmentName());
        holder.tvCategory.setText(equipment.getSubCategoryName() != null ? equipment.getSubCategoryName() : "General");
        String hourly = "₹" + String.format(java.util.Locale.getDefault(), "%.0f", equipment.getHourlyPrice()) + "/hr";
        String daily = "₹" + String.format(java.util.Locale.getDefault(), "%.0f", equipment.getDailyPrice()) + "/day";
        holder.tvHourlyPrice.setText(hourly);
        holder.tvDailyPrice.setText(daily);
        
        // Modern Status Badge Logic
        String status = equipment.getStatus() != null ? equipment.getStatus() : "Pending";
        holder.tvStatus.setText("● " + status);
        
        if (status.equalsIgnoreCase("Active")) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tonal_green_text));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_perfect_active);
            
            holder.btnToggleStatus.setText("Block");
            holder.btnToggleStatus.setIconResource(R.drawable.ic_block);
            holder.btnToggleStatus.setTextColor(ContextCompat.getColor(context, R.color.tonal_orange_text));
            holder.btnToggleStatus.setIconTintResource(R.color.tonal_orange_text);
            holder.btnToggleStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.tonal_orange_bg));
        } else if (status.equalsIgnoreCase("Blocked") || status.equalsIgnoreCase("Inactive")) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tonal_orange_text));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_perfect_blocked);
            
            holder.btnToggleStatus.setText("Unblock");
            holder.btnToggleStatus.setIconResource(R.drawable.ic_check_circle);
            holder.btnToggleStatus.setTextColor(ContextCompat.getColor(context, R.color.tonal_green_text));
            holder.btnToggleStatus.setIconTintResource(R.color.tonal_green_text);
            holder.btnToggleStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.tonal_green_bg));
        } else {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.tonal_orange_text));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            holder.btnToggleStatus.setText("Block");
            holder.btnToggleStatus.setIconResource(R.drawable.ic_block);
            holder.btnToggleStatus.setTextColor(ContextCompat.getColor(context, R.color.tonal_orange_text));
            holder.btnToggleStatus.setIconTintResource(R.color.tonal_orange_text);
            holder.btnToggleStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.tonal_orange_bg));
        }

        // Load image mapping Glide
        if (equipment.getImageUrl() != null && !equipment.getImageUrl().isEmpty()) {
            Glide.with(context)
                 .load(equipment.getImageUrl())
                 .placeholder(R.drawable.ic_agrirent_logo)
                 .into(holder.ivEquipment);
        } else {
            holder.ivEquipment.setImageResource(R.drawable.ic_agrirent_logo);
        }

        holder.btnEdit.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (listener != null && adapterPos != RecyclerView.NO_POSITION && adapterPos < equipmentList.size()) {
                listener.onEditClick(equipmentList.get(adapterPos));
            }
        });

        holder.btnToggleStatus.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (listener != null && adapterPos != RecyclerView.NO_POSITION && adapterPos < equipmentList.size()) {
                listener.onToggleStatusClick(equipmentList.get(adapterPos), adapterPos, holder.itemView);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (listener != null && adapterPos != RecyclerView.NO_POSITION && adapterPos < equipmentList.size()) {
                listener.onDeleteClick(equipmentList.get(adapterPos), adapterPos, holder.itemView);
            }
        });
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEquipment;
        TextView tvEquipmentName, tvCategory, tvStatus, tvHourlyPrice, tvDailyPrice;
        MaterialButton btnEdit, btnToggleStatus;
        android.view.View btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEquipment = itemView.findViewById(R.id.ivEquipment);
            tvEquipmentName = itemView.findViewById(R.id.tvEquipmentName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvHourlyPrice = itemView.findViewById(R.id.tvHourlyPrice);
            tvDailyPrice = itemView.findViewById(R.id.tvDailyPrice);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnToggleStatus = itemView.findViewById(R.id.btnToggleStatus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
