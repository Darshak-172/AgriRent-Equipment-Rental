package com.example.agrirent.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.agrirent.R;
import com.example.agrirent.activities.AddEquipmentActivity;
import com.example.agrirent.activities.AddProductActivity;
import com.example.agrirent.activities.SubscriptionActivity;
import com.example.agrirent.utils.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;

public class AddListingBottomSheet extends BottomSheetDialogFragment {

    public static AddListingBottomSheet newInstance() {
        return new AddListingBottomSheet();
    }

    private SessionManager session;

    private MaterialCardView cardAddEquipment, cardAddProduct, cvUpgradeBanner;
    private LinearLayout stripEquipment, stripProduct;
    private MaterialCardView iconBgEquipment, iconBgProduct;
    private ImageView ivEquipmentChevron, ivProductChevron, ivSubStar;
    private MaterialCardView layoutSubInfo;
    private TextView tvSubInfo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_listing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        session = new SessionManager(requireContext());

        bindViews(view);
        applySubscriptionGates();
    }

    private void bindViews(View view) {
        cardAddEquipment   = view.findViewById(R.id.cardAddEquipment);
        cardAddProduct     = view.findViewById(R.id.cardAddProduct);
        stripEquipment     = view.findViewById(R.id.stripEquipment);
        stripProduct       = view.findViewById(R.id.stripProduct);
        iconBgEquipment    = view.findViewById(R.id.iconBgEquipment);
        iconBgProduct      = view.findViewById(R.id.iconBgProduct);
        ivEquipmentChevron = view.findViewById(R.id.ivEquipmentChevron);
        ivProductChevron   = view.findViewById(R.id.ivProductChevron);
        cvUpgradeBanner    = view.findViewById(R.id.cvUpgradeBanner);
        layoutSubInfo      = view.findViewById(R.id.layoutSubInfo);
        tvSubInfo          = view.findViewById(R.id.tvSubInfo);
        ivSubStar          = view.findViewById(R.id.ivSubStar);
    }

    private void applySubscriptionGates() {
        boolean hasEquipment = session.hasEquipmentSubscription();
        boolean hasProduct   = session.hasProductSubscription();
        boolean hasAny       = session.hasActiveSubscription();
        String  planName     = session.getSubscriptionPlanName();

        // Subtitle / Plan Badge
        if (!hasAny) {
            tvSubInfo.setText("Subscribe to unlock listing");
            if (ivSubStar != null) ivSubStar.setVisibility(View.GONE);
            if (layoutSubInfo != null) {
                layoutSubInfo.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
                layoutSubInfo.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            }
            tvSubInfo.setTextColor(Color.parseColor("#78909C"));
        } else {
            tvSubInfo.setText("Active Plan: " + (planName.isEmpty() ? "Verified" : planName));
            if (ivSubStar != null) ivSubStar.setVisibility(View.VISIBLE);
            if (layoutSubInfo != null) {
                layoutSubInfo.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                layoutSubInfo.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#C8E6C9")));
            }
            tvSubInfo.setTextColor(Color.parseColor("#2E7D32"));
        }

        // Equipment card
        if (hasEquipment) {
            applyUnlocked(cardAddEquipment, stripEquipment, iconBgEquipment, ivEquipmentChevron);
            cardAddEquipment.setOnClickListener(v -> {
                dismiss();
                startActivity(new Intent(requireContext(), AddEquipmentActivity.class));
            });
        } else {
            applyLocked(cardAddEquipment, stripEquipment, iconBgEquipment, ivEquipmentChevron);
            cardAddEquipment.setOnClickListener(v -> {
                dismiss();
                goToSubscription();
            });
        }

        // Product card
        if (hasProduct) {
            applyUnlocked(cardAddProduct, stripProduct, iconBgProduct, ivProductChevron);
            cardAddProduct.setOnClickListener(v -> {
                dismiss();
                startActivity(new Intent(requireContext(), AddProductActivity.class));
            });
        } else {
            applyLocked(cardAddProduct, stripProduct, iconBgProduct, ivProductChevron);
            cardAddProduct.setOnClickListener(v -> {
                dismiss();
                goToSubscription();
            });
        }

        // Upgrade banner
        if (!hasEquipment || !hasProduct) {
            cvUpgradeBanner.setVisibility(View.VISIBLE);
            cvUpgradeBanner.setOnClickListener(v -> {
                dismiss();
                goToSubscription();
            });
        } else {
            cvUpgradeBanner.setVisibility(View.GONE);
        }
    }

    private void applyLocked(MaterialCardView card, LinearLayout strip,
                             MaterialCardView iconBg, ImageView chevron) {
        if (strip != null) strip.setVisibility(View.VISIBLE);
        if (card != null) card.setAlpha(0.78f);
        if (iconBg != null) iconBg.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
        if (chevron != null) chevron.setImageTintList(ColorStateList.valueOf(Color.parseColor("#BDBDBD")));
    }

    private void applyUnlocked(MaterialCardView card, LinearLayout strip,
                               MaterialCardView iconBg, ImageView chevron) {
        if (strip != null) strip.setVisibility(View.GONE);
        if (card != null) card.setAlpha(1f);
        if (iconBg != null) iconBg.setCardBackgroundColor(Color.parseColor("#EFF7F0"));
        if (chevron != null) chevron.setImageTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
    }

    private void goToSubscription() {
        startActivity(new Intent(requireContext(), SubscriptionActivity.class));
    }
}
