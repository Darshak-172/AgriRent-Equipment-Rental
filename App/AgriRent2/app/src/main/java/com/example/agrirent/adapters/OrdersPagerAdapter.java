package com.example.agrirent.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.agrirent.fragments.BookingsTabFragment;
import com.example.agrirent.fragments.ProductOrdersTabFragment;

public class OrdersPagerAdapter extends FragmentStateAdapter {

    public OrdersPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new BookingsTabFragment();
        } else {
            return new ProductOrdersTabFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
