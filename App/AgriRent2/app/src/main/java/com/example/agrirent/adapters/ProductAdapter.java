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
import com.example.agrirent.models.Product;

import java.util.List;

/**
 * Legacy ProductAdapter — kept for backward compatibility.
 * New code should use ProductItemAdapter with ProductItem model.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private Context context;
    private List<Product> productList;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.tvProductName.setText(product.getName());
        holder.tvProductPrice.setText(product.getPrice());

        // Load image: prefer URL (API), fallback to drawable resource
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_products)
                    .error(R.drawable.ic_products)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.ivProductImage);
        } else if (product.getImageRes() != 0) {
            holder.ivProductImage.setImageResource(product.getImageRes());
        } else {
            holder.ivProductImage.setImageResource(R.drawable.ic_products);
        }

        holder.itemView.setOnClickListener(v ->
            Toast.makeText(context, product.getName(), Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public void updateData(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName;
        TextView tvProductPrice;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName  = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
        }
    }
}
