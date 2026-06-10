package com.example.agrirent.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
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
import com.example.agrirent.models.ProductItem;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ManageProductAdapter extends RecyclerView.Adapter<ManageProductAdapter.ViewHolder> {

    private final Context context;
    private final List<ProductItem> productList;
    private final OnProductActionClickListener listener;

    public interface OnProductActionClickListener {
        void onEditClick(ProductItem product);
        void onDeleteClick(ProductItem product, int position, View itemView);
        void onToggleStatusClick(ProductItem product, int position, View itemView);
    }

    public ManageProductAdapter(Context context, List<ProductItem> productList, OnProductActionClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductItem product = productList.get(position);

        holder.tvProductName.setText(product.getProductName());
        holder.tvCategory.setText(product.getCategory() != null ? product.getCategory() : "General");
        
        String priceText = "₹" + String.format(java.util.Locale.getDefault(), "%.0f", product.getPrice()) + "/" + (product.getUnit() != null && !product.getUnit().isEmpty() ? product.getUnit() : "unit");
        holder.tvPrice.setText(priceText);
        
        holder.tvStock.setText("Stock: " + product.getStock());
        
        // Modern Status Badge Logic
        String status = product.getStatus() != null ? product.getStatus() : "Pending";
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
        if (product.getFirstImageUrl() != null && !product.getFirstImageUrl().isEmpty()) {
            Glide.with(context)
                 .load(product.getFirstImageUrl())
                 .placeholder(R.drawable.ic_agrirent_logo)
                 .into(holder.ivProduct);
        } else {
            holder.ivProduct.setImageResource(R.drawable.ic_agrirent_logo);
        }

        holder.btnEdit.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (listener != null && adapterPos != RecyclerView.NO_POSITION && adapterPos < productList.size()) {
                listener.onEditClick(productList.get(adapterPos));
            }
        });

        holder.btnToggleStatus.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (listener != null && adapterPos != RecyclerView.NO_POSITION && adapterPos < productList.size()) {
                listener.onToggleStatusClick(productList.get(adapterPos), adapterPos, holder.itemView);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (listener != null && adapterPos != RecyclerView.NO_POSITION && adapterPos < productList.size()) {
                listener.onDeleteClick(productList.get(adapterPos), adapterPos, holder.itemView);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvProductName, tvCategory, tvStatus, tvPrice, tvStock;
        MaterialButton btnEdit, btnToggleStatus;
        android.view.View btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStock = itemView.findViewById(R.id.tvStock);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnToggleStatus = itemView.findViewById(R.id.btnToggleStatus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
