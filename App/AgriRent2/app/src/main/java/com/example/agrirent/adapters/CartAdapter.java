package com.example.agrirent.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.agrirent.R;
import com.example.agrirent.models.CartItem;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private Context context;
    private List<CartItem> cartItems;
    private CartUpdateListener listener;
    private java.util.Set<Integer> errorPositions = new java.util.HashSet<>();

    public interface CartUpdateListener {
        void onQuantityChanged(int productId, int quantity);
        void onItemDeleted(int productId);
        void onValidationChanged(boolean isValid);
    }

    public CartAdapter(Context context, List<CartItem> cartItems, CartUpdateListener listener) {
        this.context = context;
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        
        // Seller Header Logic
        String currentSeller = item.getProduct().getSeller();
        if (position == 0) {
            holder.llSellerHeader.setVisibility(View.VISIBLE);
            holder.tvSellerNameHeader.setText("Seller: " + currentSeller);
        } else {
            String prevSeller = cartItems.get(position - 1).getProduct().getSeller();
            if (currentSeller != null && !currentSeller.equals(prevSeller)) {
                holder.llSellerHeader.setVisibility(View.VISIBLE);
                holder.tvSellerNameHeader.setText("Seller: " + currentSeller);
            } else {
                holder.llSellerHeader.setVisibility(View.GONE);
            }
        }
        
        holder.tvProductName.setText(item.getProduct().getProductName());
        holder.tvProductUnit.setText("per " + item.getProduct().getUnit());
        holder.tvProductPrice.setText("₹" + String.format("%.0f", item.getProduct().getPrice()));
        
        // Remove existing text watcher to avoid recycling issues
        if (holder.quantityTextWatcher != null) {
            holder.etQuantity.removeTextChangedListener(holder.quantityTextWatcher);
        }
        
        holder.etQuantity.setText(String.valueOf(item.getQuantity()));
        holder.etQuantity.setError(null); // Clear any old error

        holder.quantityTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String val = s.toString();
                if (val.isEmpty()) {
                    holder.etQuantity.setError("Enter valid qty");
                    return;
                }

                try {
                    int newQty = Integer.parseInt(val);
                    int stock = item.getProduct().getStock();

                    if (newQty <= 0) {
                        holder.etQuantity.setError("Min qty is 1");
                        errorPositions.add(holder.getAdapterPosition());
                        listener.onValidationChanged(false);
                    } else if (newQty > stock) {
                        holder.etQuantity.setError("Max available: " + stock);
                        errorPositions.add(holder.getAdapterPosition());
                        listener.onValidationChanged(false);
                    } else {
                        holder.etQuantity.setError(null);
                        boolean wasInError = errorPositions.remove(holder.getAdapterPosition());
                        if (wasInError || errorPositions.isEmpty()) {
                            listener.onValidationChanged(errorPositions.isEmpty());
                        }
                        if (newQty != item.getQuantity()) {
                            item.setQuantity(newQty);
                            listener.onQuantityChanged(item.getProduct().getProductId(), newQty);
                        }
                    }
                } catch (NumberFormatException e) {
                    holder.etQuantity.setError("Invalid number");
                }
            }
        };
        holder.etQuantity.addTextChangedListener(holder.quantityTextWatcher);

        holder.etQuantity.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus();
                return true;
            }
            return false;
        });

        Glide.with(context)
                .load(item.getProduct().getFirstImageUrl())
                .placeholder(R.drawable.ic_agrirent_logo)
                .centerCrop()
                .into(holder.ivProductImage);

        holder.btnPlus.setOnClickListener(v -> {
            int newQty = item.getQuantity() + 1;
            int stock = item.getProduct().getStock();
            if (newQty <= stock) {
                item.setQuantity(newQty);
                holder.etQuantity.setText(String.valueOf(newQty));
                holder.etQuantity.setError(null);
                listener.onQuantityChanged(item.getProduct().getProductId(), newQty);
            } else {
                holder.etQuantity.setError("Max available: " + stock);
            }
        });

        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                int newQty = item.getQuantity() - 1;
                item.setQuantity(newQty);
                holder.etQuantity.setText(String.valueOf(newQty));
                holder.etQuantity.setError(null);
                listener.onQuantityChanged(item.getProduct().getProductId(), newQty);
            } else {
                holder.etQuantity.setError("Min qty is 1");
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            listener.onItemDeleted(item.getProduct().getProductId());
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage, btnMinus, btnPlus, btnDelete;
        TextView tvProductName, tvProductUnit, tvProductPrice, tvSellerNameHeader;
        View llSellerHeader;
        EditText etQuantity;
        TextWatcher quantityTextWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductUnit = itemView.findViewById(R.id.tvProductUnit);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvSellerNameHeader = itemView.findViewById(R.id.tvSellerNameHeader);
            llSellerHeader = itemView.findViewById(R.id.llSellerHeader);
            etQuantity = itemView.findViewById(R.id.etQuantity);
        }
    }
}
