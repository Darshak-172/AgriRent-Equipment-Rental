package com.example.agrirent.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.agrirent.R;
import com.example.agrirent.models.Equipment;

import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.EquipmentViewHolder> {

    private Context context;
    private List<Equipment> equipmentList;

    public EquipmentAdapter(Context context, List<Equipment> equipmentList) {
        this.context = context;
        this.equipmentList = equipmentList;
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_equipment_carousel, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);

        // Name
        holder.tvEquipmentName.setText(equipment.getEquipmentName());

        String hourly = "₹" + String.format(java.util.Locale.getDefault(), "%.0f", equipment.getHourlyPrice()) + "/hr";
        String daily = "₹" + String.format(java.util.Locale.getDefault(), "%.0f", equipment.getDailyPrice()) + "/day";
        holder.tvHourlyPrice.setText(hourly);
        holder.tvDailyPrice.setText(daily);

        // Category badge
        if (holder.tvEquipmentCategory != null) {
            String cat = equipment.getCategoryName();
            holder.tvEquipmentCategory.setText(cat != null ? cat : "");
        }

        // Location
        if (holder.tvEquipmentLocation != null && equipment.getLocation() != null) {
            holder.tvEquipmentLocation.setText("📍 " + equipment.getLocation());
        }

        // Rating
        if (holder.tvAverageRatingCard != null) {
            holder.tvAverageRatingCard.setText(String.format(java.util.Locale.getDefault(), "%.1f", equipment.getAverageRating()));
        }

        // Image via Glide — uses images[] array first, then thumbnailUrl
        String imageUrl = equipment.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_agrirent_logo)
                    .error(R.drawable.ic_agrirent_logo)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.ivEquipmentImage);
        } else {
            holder.ivEquipmentImage.setImageResource(R.drawable.ic_agrirent_logo);
        }

        // Click
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, com.example.agrirent.activities.EquipmentDetailActivity.class);
            intent.putExtra("equipment", equipment);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return equipmentList != null ? equipmentList.size() : 0;
    }

    public void updateData(List<Equipment> newList) {
        this.equipmentList = newList;
        notifyDataSetChanged();
    }

    static class EquipmentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEquipmentImage;
        TextView tvEquipmentName;
        TextView tvHourlyPrice;
        TextView tvDailyPrice;
        TextView tvEquipmentCategory;
        TextView tvEquipmentLocation;
        TextView tvAverageRatingCard;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEquipmentImage    = itemView.findViewById(R.id.ivEquipmentImage);
            tvEquipmentName     = itemView.findViewById(R.id.tvEquipmentName);
            tvHourlyPrice       = itemView.findViewById(R.id.tvHourlyPrice);
            tvDailyPrice        = itemView.findViewById(R.id.tvDailyPrice);
            tvEquipmentCategory = itemView.findViewById(R.id.tvEquipmentCategory);
            tvEquipmentLocation = itemView.findViewById(R.id.tvEquipmentLocation);
            tvAverageRatingCard = itemView.findViewById(R.id.tvAverageRatingCard);
        }
    }
}
