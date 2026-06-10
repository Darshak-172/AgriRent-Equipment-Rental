package com.example.agrirent.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.agrirent.R;
import com.example.agrirent.activities.AddEquipmentActivity;
import com.example.agrirent.activities.AddProductActivity;

public class AddFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        CardView cardAddEquipment = view.findViewById(R.id.cardAddEquipment);
        CardView cardAddProduct = view.findViewById(R.id.cardAddProduct);

        cardAddEquipment.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), AddEquipmentActivity.class));
        });

        cardAddProduct.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), AddProductActivity.class));
        });

        return view;
    }
}
