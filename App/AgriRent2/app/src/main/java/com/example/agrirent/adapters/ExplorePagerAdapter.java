package com.example.agrirent.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.agrirent.fragments.EquipmentTabFragment;
import com.example.agrirent.fragments.ProductsTabFragment;

public class ExplorePagerAdapter extends FragmentStateAdapter {

    public ExplorePagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new EquipmentTabFragment();
        } else {
            return new ProductsTabFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
