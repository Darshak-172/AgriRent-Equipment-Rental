package com.example.agrirent.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.agrirent.R;
import com.example.agrirent.activities.OrderDetailActivity;
import com.example.agrirent.models.OrderResponseDto;
import android.widget.ImageView;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private final Context context;
    private final List<OrderResponseDto> orderList;
    private final boolean isSellerView;

    public OrderAdapter(Context context, List<OrderResponseDto> orderList, boolean isSellerView) {
        this.context = context;
        this.orderList = orderList;
        this.isSellerView = isSellerView;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderResponseDto order = orderList.get(position);

        holder.tvProductName.setText(order.getProductName());
        holder.tvQuantity.setText(String.valueOf(order.getQuantity()));
        holder.tvTotalPrice.setText("₹" + String.format("%.0f", order.getTotalPrice()));
        holder.tvStatus.setText(order.getStatus());

        // Load Product Image
        Glide.with(context)
                .load(order.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(holder.ivProductPhoto);

        if (isSellerView) {
            holder.tvOtherPartyLabel.setText("Buyer: ");
            holder.tvOtherPartyName.setText(order.getFarmerName());
        } else {
            holder.tvOtherPartyLabel.setText("Seller: ");
            holder.tvOtherPartyName.setText(order.getSellerName());
        }

        switch (order.getStatus().toLowerCase()) {
            case "pending":
                holder.tvStatus.setTextColor(Color.parseColor("#F59E0B")); // amber
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                break;
            case "processing":
            case "approved":
            case "shipped":
                holder.tvStatus.setTextColor(Color.parseColor("#3B82F6")); // blue
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending); // Reuse or use specific bg
                break;
            case "delivered":
                holder.tvStatus.setTextColor(Color.parseColor("#10B981")); // green
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending); // Update with green bg if exists
                break;
            case "canceled":
            case "cancelled":
                holder.tvStatus.setTextColor(Color.parseColor("#EF4444")); // red
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                break;
            default:
                holder.tvStatus.setTextColor(Color.parseColor("#6B7280")); // gray
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailActivity.class);
            intent.putExtra("order", order);
            intent.putExtra("isSellerView", isSellerView);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvStatus, tvOtherPartyLabel, tvOtherPartyName;
        TextView tvQuantity, tvTotalPrice, tvViewDetails;
        ImageView ivProductPhoto;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductPhoto = itemView.findViewById(R.id.ivProductPhoto);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOtherPartyLabel = itemView.findViewById(R.id.tvOtherPartyLabel);
            tvOtherPartyName = itemView.findViewById(R.id.tvOtherPartyName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvViewDetails = itemView.findViewById(R.id.tvViewDetails);
        }
    }
}
